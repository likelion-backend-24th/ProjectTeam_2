import { useState } from 'react'
import { studyPostApi } from '../../api'
import styles from './StudyPostCommentForm.module.css'

export default function StudyPostCommentForm({ studyId, postId, onCommentAdded }) {
  const [content, setContent] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState('')

  async function handleSubmit(event) {
    event.preventDefault()
    if (!content.trim()) return

    setError('')
    setIsSubmitting(true)
    try {
      await studyPostApi.createStudyPostComment(studyId, postId, { content })
      setContent('')
      onCommentAdded()
    } catch (err) {
      setError(err.response?.data?.message ?? '댓글 등록에 실패했습니다.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <form className={styles.wrapper} onSubmit={handleSubmit}>
      <textarea
        className={styles.textarea}
        placeholder="댓글을 입력하세요."
        value={content}
        onChange={(event) => setContent(event.target.value)}
      />
      <div className={styles.footer}>
        <button type="submit" className={styles.submitButton} disabled={isSubmitting || !content.trim()}>
          {isSubmitting ? '등록 중...' : '등록'}
        </button>
      </div>
      {error && <p className={styles.error}>{error}</p>}
    </form>
  )
}
