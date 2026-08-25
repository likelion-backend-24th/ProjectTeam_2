import { ChevronLeft, Image, Plus, Send, Star, X } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { expertApi, feedbackApi } from '../../api'
import ReportButton from '../../components/common/ReportButton'
import SiteHeader from '../../components/common/SiteHeader'
import { useAuth } from '../../context/AuthContext'
import { getAvatarColor } from '../../utils/avatarColor'
import { formatDateTime } from '../../utils/formatDate'
import styles from './FeedbackThreadPage.module.css'

// 첨부 이미지 제약. 백엔드 ImageValidator(jpg/jpeg/png/gif, 장당 5MB)와 동일한 기준.
const ALLOWED_IMAGE_EXTENSIONS = ['jpg', 'jpeg', 'png', 'gif']
const MAX_IMAGE_SIZE = 5 * 1024 * 1024
const MAX_IMAGE_COUNT = 5

// 문의 스레드 상세(채팅). 요청자 화면일 땐 expertProfileId로 전문가 프로필(이름)을 별도 조회하고,
// 전문가 화면일 땐 피드백 응답에 포함된 요청자 닉네임을 그대로 사용한다.
export default function FeedbackThreadPage() {
  const { feedbackId } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()
  const bottomRef = useRef(null)
  const fileInputRef = useRef(null)
  const attachMenuRef = useRef(null)

  const [feedback, setFeedback] = useState(null)
  const [otherParty, setOtherParty] = useState(null) // { displayName, career? }
  const [messages, setMessages] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState('')

  const [content, setContent] = useState('')
  const [images, setImages] = useState([])
  const [imagePreviews, setImagePreviews] = useState([])
  const [imageError, setImageError] = useState('')
  const [isSending, setIsSending] = useState(false)
  const [sendError, setSendError] = useState('')
  const [isClosing, setIsClosing] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)
  const [isAttachMenuOpen, setIsAttachMenuOpen] = useState(false)

  const isRequesterView = feedback && user && feedback.requesterId === user.id

  function refetch() {
    return Promise.all([feedbackApi.getFeedback(feedbackId), feedbackApi.getMessages(feedbackId)]).then(
      ([feedbackRes, messagesRes]) => {
        setFeedback(feedbackRes.data.data)
        setMessages(messagesRes.data.data)
        return feedbackRes.data.data
      },
    )
  }

  useEffect(() => {
    let ignore = false
    setIsLoading(true)
    setLoadError('')

    refetch()
      .then((feedbackData) => {
        if (ignore || !user) return
        if (feedbackData.requesterId !== user.id) {
          setOtherParty({ displayName: feedbackData.requesterNickname })
          return
        }
        return expertApi.getExpertDetail(feedbackData.expertProfileId).then(({ data }) => {
          if (ignore) return
          const primaryCareer = data.data.careers?.[0]
          const career = primaryCareer
            ? `${primaryCareer.companyName} · ${primaryCareer.position} · ${primaryCareer.years}년차`
            : null
          setOtherParty({ displayName: data.data.name, career })
        })
      })
      .catch((err) => {
        if (ignore) return
        setLoadError(err.response?.status === 404 ? '존재하지 않는 문의입니다.' : '문의를 불러오지 못했습니다.')
      })
      .finally(() => {
        if (!ignore) setIsLoading(false)
      })

    return () => {
      ignore = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [feedbackId])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ block: 'end' })
  }, [messages])

  // File -> object URL 미리보기. 언마운트/변경 시 반드시 해제해서 메모리 누수를 막는다.
  useEffect(() => {
    const urls = images.map((file) => URL.createObjectURL(file))
    setImagePreviews(urls)
    return () => urls.forEach((url) => URL.revokeObjectURL(url))
  }, [images])

  // + 메뉴 바깥을 클릭하면 닫는다.
  useEffect(() => {
    function handleClickOutside(event) {
      if (attachMenuRef.current && !attachMenuRef.current.contains(event.target)) {
        setIsAttachMenuOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  function handlePickImageClick() {
    setIsAttachMenuOpen(false)
    fileInputRef.current?.click()
  }

  function handleImagesSelected(event) {
    const files = Array.from(event.target.files ?? [])
    event.target.value = '' // 같은 파일을 다시 선택해도 onChange가 발생하도록 초기화
    if (files.length === 0) return

    setImageError('')

    if (images.length + files.length > MAX_IMAGE_COUNT) {
      setImageError(`이미지는 최대 ${MAX_IMAGE_COUNT}장까지 첨부할 수 있어요.`)
      return
    }

    for (const file of files) {
      const ext = file.name.includes('.') ? file.name.split('.').pop().toLowerCase() : ''
      if (!ALLOWED_IMAGE_EXTENSIONS.includes(ext)) {
        setImageError(`지원하지 않는 파일 형식이에요: ${file.name}`)
        return
      }
      if (file.size > MAX_IMAGE_SIZE) {
        setImageError(`${file.name} 파일이 5MB를 넘어요.`)
        return
      }
    }

    setImages((prev) => [...prev, ...files])
  }

  function removeImageAt(index) {
    setImages((prev) => prev.filter((_, i) => i !== index))
  }

  async function handleSend(event) {
    event.preventDefault()
    if (!content.trim()) return

    setSendError('')
    setIsSending(true)
    try {
      await feedbackApi.addMessage(feedbackId, { content }, images)
      setContent('')
      setImages([])
      setImageError('')
      await refetch()
    } catch (err) {
      setSendError(err.response?.data?.message ?? '메시지 전송에 실패했습니다.')
    } finally {
      setIsSending(false)
    }
  }

  async function handleClose() {
    if (!window.confirm('상담을 종료할까요? 종료하면 더 이상 메시지를 보낼 수 없어요.')) return
    setIsClosing(true)
    try {
      await feedbackApi.closeFeedback(feedbackId)
      await refetch()
    } catch (err) {
      window.alert(err.response?.data?.message ?? '상담 종료에 실패했습니다.')
    } finally {
      setIsClosing(false)
    }
  }

  async function handleDelete() {
    // 진행 중이면 종료까지 함께 일어나므로 문구를 나눈다.
    const message = isClosed
        ? '이 상담을 삭제할까요? 목록에서 사라지며 되돌릴 수 없어요.'
        : '진행 중인 상담입니다. 삭제하면 종료되며 되돌릴 수 없어요. 계속할까요?'
    if (!window.confirm(message)) return

    setIsDeleting(true)
    try {
      await feedbackApi.deleteFeedback(feedbackId)
      navigate('/experts')
    } catch (err) {
      window.alert(err.response?.data?.message ?? '상담 삭제에 실패했습니다.')
    } finally {
      setIsDeleting(false)
    }
  }

  const isClosed = Boolean(feedback?.closedBy)
  const isBlockedBySubscription = isRequesterView && !feedback?.requesterSubscribed
  const canSend = feedback && !isClosed && !isBlockedBySubscription

  return (
    <>
      <SiteHeader />
      <main className={styles.main}>
        <Link to="/experts" className={styles.breadcrumb}>
          <ChevronLeft size={16} />
          전문가 상담
        </Link>

        {isLoading && <p className={styles.state}>불러오는 중...</p>}
        {!isLoading && loadError && <p className={styles.state}>{loadError}</p>}

        {!isLoading && feedback && otherParty && (
          <>
            <div className={styles.headerRow}>
              <span className={styles.avatar} style={{ backgroundColor: getAvatarColor(otherParty.displayName) }}>
                {otherParty.displayName?.[0]}
              </span>
              <div>
                <div className={styles.nameRow}>
                  <span className={styles.name}>{otherParty.displayName}</span>
                  {isRequesterView && (
                    <span className={styles.badge}>
                      <Star size={9} />
                      전문가
                    </span>
                  )}
                </div>
                {otherParty.career && <p className={styles.subInfo}>{otherParty.career}</p>}
              </div>

              <div className={styles.headerActions}>
                <ReportButton targetType="FEEDBACK" targetId={feedbackId} variant="icon" />
                {isRequesterView && !isClosed && (
                    <button type="button" className={styles.closeButton} onClick={handleClose} disabled={isClosing}>
                      {isClosing ? '종료 중...' : '상담 종료'}
                    </button>
                )}
                {isRequesterView && (
                    <button type="button" className={styles.deleteButton} onClick={handleDelete} disabled={isDeleting}>
                      {isDeleting ? '삭제 중...' : '상담 삭제'}
                    </button>
                )}
              </div>
            </div>

            <div className={styles.topicBox}>
              <p className={styles.topicLabel}>상담 주제</p>
              <p className={styles.topicText}>{feedback.topic}</p>
            </div>

            <div className={styles.messages}>
              {messages.map((message) => {
                const isMine = message.senderId === user.id
                return (
                  <div key={message.id} className={`${styles.messageRow} ${isMine ? styles.messageRowMine : ''}`}>
                    {!isMine && (
                      <span
                        className={styles.messageAvatar}
                        style={{ backgroundColor: getAvatarColor(otherParty.displayName) }}
                      >
                        {otherParty.displayName?.[0]}
                      </span>
                    )}
                    <div className={styles.messageBody}>
                      <div className={`${styles.bubble} ${isMine ? styles.bubbleMine : ''}`}>
                        {message.content}
                        {message.imageUrls?.length > 0 && (
                          <div className={styles.messageImages}>
                            {message.imageUrls.map((url) => (
                              <a key={url} href={url} target="_blank" rel="noreferrer">
                                <img src={url} alt="첨부 이미지" className={styles.messageImage} />
                              </a>
                            ))}
                          </div>
                        )}
                      </div>
                      <span className={styles.messageTime}>{formatDateTime(message.createdAt)}</span>
                    </div>
                  </div>
                )
              })}
              <div ref={bottomRef} />
            </div>

            <div className={styles.footer}>
              {!canSend ? (
                <p className={styles.disabledNotice}>
                  {isClosed
                    ? '종료된 상담이에요. 더 이상 메시지를 보낼 수 없어요.'
                    : '구독이 만료되어 더 이상 메시지를 보낼 수 없어요.'}
                </p>
              ) : (
                <form className={styles.composeForm} onSubmit={handleSend}>
                  {imagePreviews.length > 0 && (
                    <div className={styles.previewStrip}>
                      {imagePreviews.map((url, index) => (
                        <div key={url} className={styles.previewThumb}>
                          <img src={url} alt={`첨부 이미지 ${index + 1}`} />
                          <button
                            type="button"
                            className={styles.previewRemove}
                            onClick={() => removeImageAt(index)}
                            aria-label="이미지 제거"
                          >
                            <X size={11} />
                          </button>
                        </div>
                      ))}
                    </div>
                  )}
                  {imageError && <p className={styles.error}>{imageError}</p>}
                  <div className={styles.inputRow}>
                    <div className={styles.attachWrapper} ref={attachMenuRef}>
                      <button
                        type="button"
                        className={styles.attachButton}
                        onClick={() => setIsAttachMenuOpen((prev) => !prev)}
                        disabled={isSending || images.length >= MAX_IMAGE_COUNT}
                        aria-label="첨부"
                      >
                        <Plus size={18} />
                      </button>
                      {isAttachMenuOpen && (
                        <div className={styles.attachMenu}>
                          <button type="button" className={styles.attachMenuItem} onClick={handlePickImageClick}>
                            <Image size={15} />
                            이미지
                          </button>
                        </div>
                      )}
                    </div>
                    <input
                      ref={fileInputRef}
                      type="file"
                      accept="image/jpeg,image/png,image/gif"
                      multiple
                      hidden
                      onChange={handleImagesSelected}
                    />
                    <input
                      className={styles.messageInput}
                      placeholder="메시지를 입력하세요..."
                      value={content}
                      onChange={(event) => setContent(event.target.value)}
                      disabled={isSending}
                    />
                    <button type="submit" className={styles.sendButton} disabled={isSending || !content.trim()}>
                      <Send size={16} />
                    </button>
                  </div>
                </form>
              )}
              {sendError && <p className={styles.error}>{sendError}</p>}
            </div>
          </>
        )}
      </main>
    </>
  )
}
