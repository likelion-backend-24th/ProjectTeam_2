import { useState } from 'react'
import { commentApi } from '../../api'
import { useAuth } from '../../context/AuthContext'
import styles from './CommentForm.module.css'

export default function CommentForm({ postId, onCommentAdded }) {
  const { isAuthenticated } = useAuth()
  const [content, setContent] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState('')

  async function handleSubmit(event) {
    event.preventDefault()
    if (!content.trim()) return

    setError('')
    setIsSubmitting(true)
    try {
      await commentApi.createComment(postId, { content })
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
        placeholder={isAuthenticated ? '댓글을 입력하세요.' : '로그인 후 댓글을 작성할 수 있어요.'}
        value={content}
        onChange={(event) => setContent(event.target.value)}
        disabled={!isAuthenticated}
      />
      {isAuthenticated && (
        <div className={styles.footer}>
          <button type="submit" className={styles.submitButton} disabled={isSubmitting || !content.trim()}>
            {isSubmitting ? '등록 중...' : '등록'}
          </button>
        </div>
      )}
      {error && <p className={styles.error}>{error}</p>}
    </form>
  )
}
