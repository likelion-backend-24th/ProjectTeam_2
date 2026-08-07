import { Plus } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { postApi } from '../../api'
import Pagination from '../../components/common/Pagination'
import SiteFooter from '../../components/common/SiteFooter'
import SiteHeader from '../../components/common/SiteHeader'
import CategoryFilterTabs from '../../components/posts/CategoryFilterTabs'
import PostListItem from '../../components/posts/PostListItem'
import styles from './PostListPage.module.css'

export default function PostListPage() {
  const [category, setCategory] = useState(null)
  const [page, setPage] = useState(0)
  const [posts, setPosts] = useState([])
  const [totalPages, setTotalPages] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let ignore = false
    setIsLoading(true)
    setError('')

    const params = { page }
    if (category) params.category = category

    postApi
      .getPosts(params)
      .then(({ data }) => {
        if (ignore) return
        // GET /api/posts는 PostResponse를 감싼 Spring Page 객체를 그대로 data에 담아 내려준다
        // (다른 목록 API처럼 meta.pagination을 쓰지 않는다).
        const pageData = data.data
        setPosts(pageData.content)
        setTotalPages(pageData.totalPages)
      })
      .catch(() => {
        if (!ignore) setError('게시글 목록을 불러오지 못했습니다.')
      })
      .finally(() => {
        if (!ignore) setIsLoading(false)
      })

    return () => {
      ignore = true
    }
  }, [category, page])

  function handleCategoryChange(next) {
    setCategory(next)
    setPage(0)
  }

  return (
    <>
      <SiteHeader backTo="/" />
      <main className={styles.main}>
        <div className={styles.headingRow}>
          <div>
            <p className={styles.eyebrow}>COMMUNITY</p>
            <h1 className={styles.title}>게시글</h1>
          </div>
          <Link to="/posts/new" className={styles.writeButton}>
            <Plus size={16} />
            글쓰기
          </Link>
        </div>

        <CategoryFilterTabs value={category} onChange={handleCategoryChange} />

        {isLoading && <p className={styles.state}>불러오는 중...</p>}
        {!isLoading && error && <p className={styles.errorState}>{error}</p>}
        {!isLoading && !error && posts.length === 0 && <p className={styles.state}>등록된 게시글이 없어요.</p>}

        {!isLoading &&
          !error &&
          posts.map((post) => <PostListItem key={post.id} post={post} />)}

        <Pagination page={page} totalPages={totalPages} onChange={setPage} />
      </main>
      <SiteFooter />
    </>
  )
}
