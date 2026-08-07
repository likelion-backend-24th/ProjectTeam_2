import { ChevronLeft } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { postApi } from '../../api'
import SiteFooter from '../../components/common/SiteFooter'
import SiteHeader from '../../components/common/SiteHeader'
import CommentForm from '../../components/posts/CommentForm'
import CommentItem from '../../components/posts/CommentItem'
import { getPostCategoryMeta } from '../../constants/postCategory'
import { getAvatarColor } from '../../utils/avatarColor'
import { formatDate } from '../../utils/formatDate'
import styles from './PostDetailPage.module.css'

export default function PostDetailPage() {
  const { postId } = useParams()

  const [post, setPost] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [requiresLogin, setRequiresLogin] = useState(false)
  // 댓글 등록 후 다시 불러올 때 useEffect 밖에서도 호출할 수 있도록 트리거만 갈아끼운다
  // (React 19 StrictMode가 dev에서 effect를 두 번 실행해 요청이 겹치는 걸 막기 위해
  // effect 안에서 ignore 플래그로 경쟁 상태를 방지한다).
  const [refetchTrigger, setRefetchTrigger] = useState(0)

  useEffect(() => {
    let ignore = false
    setIsLoading(true)
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
        if (!ignore) setIsLoading(false)
      })

    return () => {
      ignore = true
    }
  }, [postId, refetchTrigger])

  function refetch() {
    setRefetchTrigger((prev) => prev + 1)
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

        {isLoading && <p className={styles.state}>불러오는 중...</p>}

        {!isLoading && requiresLogin && (
          <p className={styles.loginNotice}>
            이 글을 보려면 로그인이 필요해요.
            <br />
            <Link to="/login">로그인하러 가기</Link>
          </p>
        )}

        {!isLoading && error && <p className={styles.state}>{error}</p>}

        {!isLoading && post && (
          <>
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
              <CommentItem key={comment.id} comment={comment} />
            ))}
          </>
        )}
      </main>
      <SiteFooter />
    </>
  )
}
