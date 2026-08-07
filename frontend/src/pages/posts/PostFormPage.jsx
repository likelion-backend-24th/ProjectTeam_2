import { ChevronLeft } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { postApi } from '../../api'
import SiteFooter from '../../components/common/SiteFooter'
import SiteHeader from '../../components/common/SiteHeader'
import CategoryPicker from '../../components/posts/CategoryPicker'
import styles from './PostFormPage.module.css'

const TITLE_MAX_LENGTH = 100

export default function PostFormPage() {
  const navigate = useNavigate()

  const [category, setCategory] = useState('')
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState('')

  async function handleSubmit(event) {
    event.preventDefault()

    if (!category) {
      setError('카테고리를 선택해주세요.')
      return
    }
    if (!title.trim()) {
      setError('제목을 입력해주세요.')
      return
    }
    if (!content.trim()) {
      setError('내용을 입력해주세요.')
      return
    }

    setError('')
    setIsSubmitting(true)
    try {
      const { data } = await postApi.createPost({ title, content, category })
      navigate(`/posts/${data.data.id}`)
    } catch (err) {
      setError(err.response?.data?.message ?? '게시글 등록에 실패했습니다.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <>
      <SiteHeader backTo="/posts" />
      <main className={styles.main}>
        <Link to="/posts" className={styles.breadcrumb}>
          <ChevronLeft size={16} />
          게시글 목록
        </Link>

        <p className={styles.eyebrow}>COMMUNITY</p>
        <h1 className={styles.title}>글쓰기</h1>

        <form onSubmit={handleSubmit}>
          <div className={styles.field}>
            <label className={styles.label}>
              카테고리
              <span className={styles.required}>*</span>
            </label>
            <CategoryPicker value={category} onChange={setCategory} />
          </div>

          <div className={styles.field}>
            <label className={styles.label} htmlFor="title">
              제목
              <span className={styles.required}>*</span>
            </label>
            <input
              id="title"
              type="text"
              className={styles.input}
              placeholder="제목을 입력하세요"
              maxLength={TITLE_MAX_LENGTH}
              value={title}
              onChange={(event) => setTitle(event.target.value)}
            />
            <p className={styles.counter}>
              {title.length}/{TITLE_MAX_LENGTH}
            </p>
          </div>

          <div className={styles.field}>
            <label className={styles.label} htmlFor="content">
              내용
              <span className={styles.required}>*</span>
            </label>
            <textarea
              id="content"
              className={styles.textarea}
              placeholder="취업 정보, 면접 후기, 자소서 팁 등 자유롭게 작성해보세요."
              value={content}
              onChange={(event) => setContent(event.target.value)}
            />
            <p className={styles.counter}>{content.length}자</p>
          </div>

          <p className={styles.notice}>
            📌 커뮤니티 이용 규칙을 위반한 게시글은 운영자에 의해 삭제될 수 있어요. 타인을 존중하는 글 문화를
            함께 만들어 나가요.
          </p>

          {error && <p className={styles.error}>{error}</p>}

          <div className={styles.actions}>
            <button type="button" className={styles.cancelButton} onClick={() => navigate('/posts')}>
              취소
            </button>
            <button type="submit" className={styles.submitButton} disabled={isSubmitting}>
              {isSubmitting ? '등록 중...' : '게시글 등록하기'}
            </button>
          </div>
        </form>
      </main>
      <SiteFooter />
    </>
  )
}
