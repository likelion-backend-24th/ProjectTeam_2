import { ChevronLeft } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { Link, Navigate, useLocation, useParams } from 'react-router-dom'
import { postApi } from '../../api'
import SiteHeader from '../../components/common/SiteHeader'
import CommentForm from '../../components/posts/CommentForm'
import CommentItem from '../../components/posts/CommentItem'
import { getPostCategoryMeta } from '../../constants/postCategory'
import { getAvatarColor } from '../../utils/avatarColor'
import { formatDate } from '../../utils/formatDate'
import styles from './PostDetailPage.module.css'

export default function PostDetailPage() {
  const { postId } = useParams()
  const location = useLocation()

  const [post, setPost] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [requiresLogin, setRequiresLogin] = useState(false)
  // 댓글 등록 후 다시 불러올 때 useEffect 밖에서도 호출할 수 있도록 트리거만 갈아끼운다
  // (React 19 StrictMode가 dev에서 effect를 두 번 실행해 요청이 겹치는 걸 막기 위해
  // effect 안에서 ignore 플래그로 경쟁 상태를 방지한다).
  const [refetchTrigger, setRefetchTrigger] = useState(0)
  // 게시글을 처음 열 때만 전체 로딩 문구를 보여주고, 댓글 등록 후 refetch할 때는
  // 이미 화면에 떠 있는 내용을 그대로 유지해 "불러오는 중..."으로 깜빡이지 않게 한다.
  const isFirstLoadRef = useRef(true)
  // 로컬/빠른 네트워크에서는 요청이 200ms 안에 끝나버려서 "불러오는 중..." 문구가
  // 한 프레임 반짝이고 사라지는 것처럼 보인다(그게 바로 깜빡임으로 느껴짐).
  // 로딩이 200ms 넘게 걸릴 때만 문구를 보여주고, 그 전에 끝나면 문구 없이
  // 목록 -> 본문으로 바로 이어지게 한다.
  const [showLoadingText, setShowLoadingText] = useState(false)

  useEffect(() => {
    isFirstLoadRef.current = true
  }, [postId])

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
    setRequiresLogin(false)

    postApi
      .getPostDetail(postId)
      .then(({ data }) => {
        if (!ignore) setPost(data.data)
      })
      .catch((err) => {
        if (ignore) return
        if (err.response?.status === 403) {
          // NOTE: 백엔드 SecurityConfig가 GET /api/posts(목록)만 permitAll이고
          // GET /api/posts/{id}(상세)는 인증을 요구한다(미인증 시 403). 비로그인 사용자는 상세를 볼 수 없다.
          setRequiresLogin(true)
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
  }, [postId, refetchTrigger])

  function refetch() {
    setRefetchTrigger((prev) => prev + 1)
  }

  // 비로그인 사용자는 안내 문구 없이 곧장 로그인 페이지로 보내고,
  // 로그인 후에는 이 게시글로 되돌아올 수 있도록 원래 경로를 state에 담아 전달한다.
  if (!isLoading && requiresLogin) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  const categoryMeta = post ? getPostCategoryMeta(post.category) : null

  return (
    <>
      <SiteHeader backTo="/posts" />
      <main className={styles.main}>
        <Link to="/posts" className={styles.breadcrumb}>
          <ChevronLeft size={16} />
          게시글 목록
        </Link>

        {isLoading && showLoadingText && <p className={styles.state}>불러오는 중...</p>}

        {!isLoading && error && <p className={styles.state}>{error}</p>}

        {!isLoading && post && (
          <div className={styles.fadeIn}>
            {categoryMeta && (
              <span className={styles.badge} style={{ backgroundColor: categoryMeta.bg, color: categoryMeta.color }}>
                {post.categoryLabel}
              </span>
            )}
            <h1 className={styles.title}>{post.title}</h1>

            <div className={styles.meta}>
              <span className={styles.avatar} style={{ backgroundColor: getAvatarColor(post.authorNickname) }}>
                {post.authorNickname?.[0]}
              </span>
              <span className={styles.name}>{post.authorNickname}</span>
              <span>·</span>
              <span>{formatDate(post.createdAt)}</span>
            </div>

            <div className={styles.divider} />

            <p className={styles.content}>{post.content}</p>

            <div className={styles.divider} />

            <h2 className={styles.commentsHeading}>댓글 {post.totalComments}개</h2>

            <CommentForm postId={postId} onCommentAdded={refetch} />

            {post.comments.map((comment) => (
              <CommentItem key={comment.id} postId={postId} comment={comment} onChanged={refetch} />
            ))}
          </div>
        )}
      </main>
    </>
  )
}
