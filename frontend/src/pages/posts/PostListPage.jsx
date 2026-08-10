import { Plus, Search } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { postApi } from '../../api'
import Pagination from '../../components/common/Pagination'
import SiteHeader from '../../components/common/SiteHeader'
import CategoryFilterTabs from '../../components/posts/CategoryFilterTabs'
import PostListItem from '../../components/posts/PostListItem'
import styles from './PostListPage.module.css'

export default function PostListPage() {
  const [category, setCategory] = useState(null)
  const [page, setPage] = useState(0)
  const [searchInput, setSearchInput] = useState('')
  const [keyword, setKeyword] = useState('')
  const [posts, setPosts] = useState([])
  const [totalPages, setTotalPages] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [isFetching, setIsFetching] = useState(false)
  const [error, setError] = useState('')
  // 처음 목록을 열 때만 "불러오는 중..." 문구를 보여준다. 카테고리 탭/페이지/검색어를 바꿀 때는
  // 이미 그려진 목록을 유지한 채 살짝 흐리게만 표시해서, 목록이 통째로 사라졌다가 다시
  // 나타나는 깜빡임을 없앤다.
  const isFirstLoadRef = useRef(true)

  useEffect(() => {
    let ignore = false
    setIsFetching(true)
    if (isFirstLoadRef.current) setIsLoading(true)
    setError('')

    const params = { page }
    if (category) params.category = category
    if (keyword) params.keyword = keyword

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
        if (!ignore) {
          setIsLoading(false)
          setIsFetching(false)
          isFirstLoadRef.current = false
        }
      })

    return () => {
      ignore = true
    }
  }, [category, keyword, page])

  // 입력을 멈추고 300ms가 지나면 자동으로 검색어를 반영한다(디바운스).
  // 검색창을 비우면 별도 조작 없이 곧바로 전체 목록으로 돌아간다.
  useEffect(() => {
    const timer = setTimeout(() => {
      setKeyword(searchInput.trim())
    }, 300)
    return () => clearTimeout(timer)
  }, [searchInput])

  // 카테고리나 검색어가 바뀌면 항상 1페이지부터 다시 보여준다.
  useEffect(() => {
    setPage(0)
  }, [category, keyword])

  function handleCategoryChange(next) {
    setCategory(next)
  }

  function handleSearchSubmit(event) {
    // 입력 중 Enter를 누르면 디바운스를 기다리지 않고 바로 검색한다.
    event.preventDefault()
    setKeyword(searchInput.trim())
  }

  return (
    <>
      <SiteHeader />
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

        <form className={styles.searchForm} onSubmit={handleSearchSubmit}>
          <Search size={18} className={styles.searchIcon} />
          <input
            className={styles.searchInput}
            placeholder="제목, 내용으로 검색하세요"
            value={searchInput}
            onChange={(event) => setSearchInput(event.target.value)}
          />
        </form>

        <CategoryFilterTabs value={category} onChange={handleCategoryChange} />

        {isLoading && <p className={styles.state}>불러오는 중...</p>}
        {!isLoading && error && <p className={styles.errorState}>{error}</p>}
        {!isLoading && !error && posts.length === 0 && (
          <p className={styles.state}>
            {keyword ? `"${keyword}"에 대한 검색 결과가 없어요.` : '등록된 게시글이 없어요.'}
          </p>
        )}

        {!isLoading && !error && posts.length > 0 && (
          <div className={isFetching ? styles.listFetching : styles.list}>
            {posts.map((post) => (
              <PostListItem key={post.id} post={post} />
            ))}
          </div>
        )}

        <Pagination page={page} totalPages={totalPages} onChange={setPage} />
      </main>
    </>
  )
}
