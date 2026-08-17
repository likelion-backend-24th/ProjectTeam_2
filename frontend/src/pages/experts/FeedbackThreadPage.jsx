import { ChevronLeft, Send, Star } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { expertApi, feedbackApi } from '../../api'
import SiteHeader from '../../components/common/SiteHeader'
import { useAuth } from '../../context/AuthContext'
import { getAvatarColor } from '../../utils/avatarColor'
import { formatDateTime } from '../../utils/formatDate'
import styles from './FeedbackThreadPage.module.css'

// 문의 스레드 상세(채팅). 요청자 화면일 땐 expertProfileId로 전문가 프로필을 별도 조회하고,
// 전문가 화면일 땐 피드백 응답에 포함된 요청자 닉네임을 그대로 사용한다.
export default function FeedbackThreadPage() {
  const { feedbackId } = useParams()
  const { user } = useAuth()
  const bottomRef = useRef(null)

  const [feedback, setFeedback] = useState(null)
  const [otherParty, setOtherParty] = useState(null) // { nickname, career? }
  const [messages, setMessages] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState('')

  const [content, setContent] = useState('')
  const [isSending, setIsSending] = useState(false)
  const [sendError, setSendError] = useState('')
  const [isClosing, setIsClosing] = useState(false)

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
          setOtherParty({ nickname: feedbackData.requesterNickname })
          return
        }
        return expertApi.getExpertDetail(feedbackData.expertProfileId).then(({ data }) => {
          if (!ignore) setOtherParty({ nickname: data.data.nickname })
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

  async function handleSend(event) {
    event.preventDefault()
    if (!content.trim()) return

    setSendError('')
    setIsSending(true)
    try {
      await feedbackApi.addMessage(feedbackId, { content })
      setContent('')
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
              <span className={styles.avatar} style={{ backgroundColor: getAvatarColor(otherParty.nickname) }}>
                {otherParty.nickname?.[0]}
              </span>
              <div>
                <div className={styles.nameRow}>
                  <span className={styles.name}>{otherParty.nickname}</span>
                  {isRequesterView && (
                    <span className={styles.badge}>
                      <Star size={9} />
                      전문가
                    </span>
                  )}
                </div>
                {otherParty.career && <p className={styles.subInfo}>{otherParty.career}</p>}
              </div>

              {isRequesterView && !isClosed && (
                <button type="button" className={styles.closeButton} onClick={handleClose} disabled={isClosing}>
                  {isClosing ? '종료 중...' : '상담 종료'}
                </button>
              )}
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
                        style={{ backgroundColor: getAvatarColor(otherParty.nickname) }}
                      >
                        {otherParty.nickname?.[0]}
                      </span>
                    )}
                    <div className={styles.messageBody}>
                      <div className={`${styles.bubble} ${isMine ? styles.bubbleMine : ''}`}>{message.content}</div>
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
                <form className={styles.inputRow} onSubmit={handleSend}>
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
