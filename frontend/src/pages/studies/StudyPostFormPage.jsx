import { ChevronLeft } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { studyPostApi } from '../../api'
import SiteHeader from '../../components/common/SiteHeader'
import styles from './StudyPostFormPage.module.css'

const TITLE_MAX_LENGTH = 100
const CONTENT_MAX_LENGTH = 5000

export default function StudyPostFormPage() {
  const { studyId, postId } = useParams()
  const navigate = useNavigate()
  const isEditMode = Boolean(postId)

  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [isLoading, setIsLoading] = useState(isEditMode)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!isEditMode) return
    let ignore = false

    studyPostApi
      .getStudyPostDetail(studyId, postId)
      .then(({ data }) => {
        if (ignore) return
        setTitle(data.data.title)
        setContent(data.data.content)
      })
      .catch(() => {
        if (!ignore) setError('게시글을 불러오지 못했습니다.')
      })
      .finally(() => {
        if (!ignore) setIsLoading(false)
      })

    return () => {
      ignore = true
    }
  }, [studyId, postId, isEditMode])

  async function handleSubmit(event) {
    event.preventDefault()

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
      if (isEditMode) {
        await studyPostApi.updateStudyPost(studyId, postId, { title, content })
        navigate(`/studies/${studyId}/posts/${postId}`)
      } else {
        const { data } = await studyPostApi.createStudyPost(studyId, { title, content })
        navigate(`/studies/${studyId}/posts/${data.data.id}`)
      }
    } catch (err) {
      setError(err.response?.data?.message ?? (isEditMode ? '게시글 수정에 실패했습니다.' : '게시글 등록에 실패했습니다.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <>
      <SiteHeader backTo={`/studies/${studyId}`} />
      <main className={styles.main}>
        <Link to={`/studies/${studyId}`} className={styles.breadcrumb}>
          <ChevronLeft size={16} />
          스터디로 돌아가기
        </Link>

        <p className={styles.eyebrow}>STUDY BOARD</p>
        <h1 className={styles.title}>{isEditMode ? '게시글 수정' : '게시글 작성'}</h1>

        {isLoading ? (
          <p className={styles.counter}>불러오는 중...</p>
        ) : (
          <form onSubmit={handleSubmit}>
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
                placeholder="스터디원들과 나눌 이야기를 작성해보세요."
                maxLength={CONTENT_MAX_LENGTH}
                value={content}
                onChange={(event) => setContent(event.target.value)}
              />
              <p className={styles.counter}>
                {content.length}/{CONTENT_MAX_LENGTH}
              </p>
            </div>

            {error && <p className={styles.error}>{error}</p>}

            <div className={styles.actions}>
              <button
                type="button"
                className={styles.cancelButton}
                onClick={() => navigate(isEditMode ? `/studies/${studyId}/posts/${postId}` : `/studies/${studyId}`)}
              >
                취소
              </button>
              <button type="submit" className={styles.submitButton} disabled={isSubmitting}>
                {isSubmitting ? '저장 중...' : isEditMode ? '수정 완료' : '게시글 등록하기'}
              </button>
            </div>
          </form>
        )}
      </main>
    </>
  )
}
