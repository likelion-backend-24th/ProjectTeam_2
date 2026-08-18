import { ChevronLeft, Pencil, Pin, Trash2 } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { studyApi, studyPostApi } from '../../api'
import ImageGallery from '../../components/common/ImageGallery'
import ReportButton from '../../components/common/ReportButton'
import SiteHeader from '../../components/common/SiteHeader'
import StudyPostCommentForm from '../../components/studies/StudyPostCommentForm'
import StudyPostCommentItem from '../../components/studies/StudyPostCommentItem'
import { useAuth } from '../../context/AuthContext'
import { getAvatarColor } from '../../utils/avatarColor'
import { formatDateTime } from '../../utils/formatDate'
import styles from './StudyPostDetailPage.module.css'

export default function StudyPostDetailPage() {
  const { studyId, postId } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()

  const [post, setPost] = useState(null)
  const [study, setStudy] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [refetchTrigger, setRefetchTrigger] = useState(0)
  const isFirstLoadRef = useRef(true)
  const [showLoadingText, setShowLoadingText] = useState(false)

  useEffect(() => {
    isFirstLoadRef.current = true
  }, [studyId, postId])

  useEffect(() => {
    if (!isLoading) {
      setShowLoadingText(false)
      return
    }
    const timer = setTimeout(() => setShowLoadingText(true), 200)
    return () => clearTimeout(timer)
  }, [isLoading])

  useEffect(() => {
    let ignore = false
    if (isFirstLoadRef.current) setIsLoading(true)
    setError('')

    Promise.all([studyPostApi.getStudyPostDetail(studyId, postId), studyApi.getStudyById(studyId)])
      .then(([postRes, studyRes]) => {
        if (ignore) return
        setPost(postRes.data.data)
        setStudy(studyRes.data.data)
      })
      .catch((err) => {
        if (ignore) return
        if (err.response?.status === 403) {
          setError('스터디 멤버만 볼 수 있는 게시글이에요.')
        } else if (err.response?.status === 404) {
          setError('존재하지 않는 게시글입니다.')
        } else {
          setError('게시글을 불러오지 못했습니다.')
        }
      })
      .finally(() => {
        if (!ignore) {
          setIsLoading(false)
          isFirstLoadRef.current = false
        }
      })

    return () => {
      ignore = true
    }
  }, [studyId, postId, refetchTrigger])

  function refetch() {
    setRefetchTrigger((prev) => prev + 1)
  }

  const isOwner = Boolean(user) && Boolean(post) && user.id === post.authorId
  const isLeader = Boolean(user) && Boolean(study) && user.id === study.leaderId

  async function handleTogglePin() {
    try {
      await studyPostApi.updateStudyPostPin(studyId, postId, !post.pinned)
      refetch()
    } catch (err) {
      window.alert(err.response?.data?.message ?? '고정 상태 변경에 실패했습니다.')
    }
  }

  async function handleDelete() {
    if (!window.confirm('게시글을 삭제할까요? 삭제하면 되돌릴 수 없어요.')) return

    try {
      await studyPostApi.deleteStudyPost(studyId, postId)
      navigate(`/studies/${studyId}`)
    } catch (err) {
      window.alert(err.response?.data?.message ?? '게시글 삭제에 실패했습니다.')
    }
  }

  return (
    <>
      <SiteHeader />
      <main className={styles.main}>
        <Link to={`/studies/${studyId}`} className={styles.breadcrumb}>
          <ChevronLeft size={16} />
          스터디로 돌아가기
        </Link>

        {isLoading && showLoadingText && <p className={styles.state}>불러오는 중...</p>}

        {!isLoading && error && <p className={styles.state}>{error}</p>}

        {!isLoading && post && (
          <div className={styles.fadeIn}>
            <h1 className={styles.title}>
              {post.pinned && <Pin size={20} className={styles.pinIcon} />}
              {post.title}
            </h1>

            <div className={styles.meta}>
              <span className={styles.avatar} style={{ backgroundColor: getAvatarColor(post.authorNickname) }}>
                {post.authorNickname?.[0]}
              </span>
              <span className={styles.name}>{post.authorNickname}</span>
              <span>·</span>
              <span>{formatDateTime(post.createdAt)}</span>

              {isLeader && (
                <span className={styles.ownerActions}>
                  <button
                    type="button"
                    className={styles.iconButton}
                    onClick={handleTogglePin}
                    aria-label={post.pinned ? '게시글 고정 해제' : '게시글 고정'}
                  >
                    <Pin size={14} />
                    {post.pinned ? '고정 해제' : '고정'}
                  </button>
                </span>
              )}

              {isOwner && (
                <span className={styles.ownerActions}>
                  <button
                    type="button"
                    className={styles.iconButton}
                    onClick={() => navigate(`/studies/${studyId}/posts/${postId}/edit`)}
                    aria-label="게시글 수정"
                  >
                    <Pencil size={14} />
                    수정
                  </button>
                  <button type="button" className={styles.iconButton} onClick={handleDelete} aria-label="게시글 삭제">
                    <Trash2 size={14} />
                    삭제
                  </button>
                </span>
              )}

              {Boolean(user) && !isOwner && (
                <span className={styles.ownerActions}>
                  <ReportButton targetType="STUDY_POST" targetId={post.id} />
                </span>
              )}
            </div>

            <div className={styles.divider} />

            <p className={styles.content}>{post.content}</p>
            <ImageGallery imageUrls={post.imageUrls} />

            <div className={styles.divider} />

            <h2 className={styles.commentsHeading}>댓글 {post.comments.length}개</h2>

            <StudyPostCommentForm studyId={studyId} postId={postId} onCommentAdded={refetch} />

            {post.comments.map((comment) => (
              <StudyPostCommentItem
                key={comment.id}
                studyId={studyId}
                postId={postId}
                comment={comment}
                onChanged={refetch}
              />
            ))}
          </div>
        )}
      </main>
    </>
  )
}
