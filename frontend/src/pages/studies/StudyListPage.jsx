import { Plus, Search } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { studyApi } from '../../api'
import CategoryFilterTabs from '../../components/posts/CategoryFilterTabs'
import Pagination from '../../components/common/Pagination'
import SiteHeader from '../../components/common/SiteHeader'
import StudyCard from '../../components/studies/StudyCard'
import { STUDY_CATEGORIES } from '../../constants/studyCategory'
import styles from './StudyListPage.module.css'

const PAGE_SIZE = 9

export default function StudyListPage() {
  const [category, setCategory] = useState(null)
  const [page, setPage] = useState(0)
  const [searchInput, setSearchInput] = useState('')
  const [keyword, setKeyword] = useState('')

  const [studies, setStudies] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [isFetching, setIsFetching] = useState(false)
  const [error, setError] = useState('')

  // NOTE: 백엔드 GET /api/studies가 카테고리 필터 파라미터를 지원하지 않아서(키워드만 가능),
  // 키워드로 넉넉히(size=100) 받아온 뒤 카테고리 필터링 + 페이지 나누기를 프론트에서 처리한다.
  useEffect(() => {
    let ignore = false
    setIsFetching(true)

    studyApi
      .getStudies({ keyword: keyword || undefined, size: 100, sort: 'createdAt,desc' })
      .then(({ data }) => {
        if (ignore) return
        setStudies(data.data)
        setError('')
      })
      .catch(() => {
        if (!ignore) setError('스터디 목록을 불러오지 못했습니다.')
      })
      .finally(() => {
        if (!ignore) {
          setIsLoading(false)
          setIsFetching(false)
        }
      })

    return () => {
      ignore = true
    }
  }, [keyword])

  useEffect(() => {
    setPage(0)
  }, [category, keyword])

  function handleSearchSubmit(event) {
    event.preventDefault()
    setKeyword(searchInput.trim())
  }

  const filtered = category ? studies.filter((study) => study.category === category) : studies
  const totalPages = Math.ceil(filtered.length / PAGE_SIZE)
  const pageItems = filtered.slice(page * PAGE_SIZE, page * PAGE_SIZE + PAGE_SIZE)

  return (
    <>
      <SiteHeader />
      <main className={styles.main}>
        <div className={styles.headingRow}>
          <div>
            <p className={styles.eyebrow}>STUDY GROUP</p>
            <h1 className={styles.title}>스터디</h1>
          </div>
          <Link to="/studies/new" className={styles.newButton}>
            <Plus size={16} />
            스터디 개설
          </Link>
        </div>

        <form className={styles.searchForm} onSubmit={handleSearchSubmit}>
          <Search size={18} className={styles.searchIcon} />
          <input
            className={styles.searchInput}
            placeholder="스터디 이름으로 검색하세요"
            value={searchInput}
            onChange={(event) => setSearchInput(event.target.value)}
          />
        </form>

        <CategoryFilterTabs value={category} onChange={setCategory} categories={STUDY_CATEGORIES} />

        {isLoading && <p className={styles.state}>불러오는 중...</p>}
        {!isLoading && error && <p className={styles.errorState}>{error}</p>}
        {!isLoading && !error && filtered.length === 0 && <p className={styles.state}>등록된 스터디가 없어요.</p>}

        {!isLoading && !error && filtered.length > 0 && (
          <>
            <p className={styles.count}>{filtered.length}개의 스터디</p>
            <div className={isFetching ? styles.gridFetching : styles.grid}>
              {pageItems.map((study) => (
                <StudyCard key={study.id} study={study} />
              ))}
            </div>
          </>
        )}

        <Pagination page={page} totalPages={totalPages} onChange={setPage} />
      </main>
    </>
  )
}
