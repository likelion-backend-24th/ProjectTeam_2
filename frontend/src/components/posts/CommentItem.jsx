import { Pencil, Trash2 } from 'lucide-react'
import { useState } from 'react'
import { commentApi } from '../../api'
import { useAuth } from '../../context/AuthContext'
import { getAvatarColor } from '../../utils/avatarColor'
import { formatDate } from '../../utils/formatDate'
import styles from './CommentItem.module.css'

export default function CommentItem({ postId, comment, onChanged }) {
  const { user } = useAuth()
  // 백엔드가 댓글에 작성자 id를 내려주지 않아서(닉네임만 제공), 닉네임으로 소유 여부를 판별한다.
  // User.nickname은 DB에서 unique 제약이 걸려 있어 닉네임 일치 = 본인으로 봐도 안전하다.
  const isOwner = Boolean(user) && user.nickname === comment.authorNickname

  const [isEditing, setIsEditing] = useState(false)
  const [content, setContent] = useState(comment.content)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState('')

  function startEdit() {
    setContent(comment.content)
    setError('')
    setIsEditing(true)
  }

  function cancelEdit() {
    setIsEditing(false)
    setError('')
  }

  async function handleSave(event) {
    event.preventDefault()
    if (!content.trim()) return

    setIsSubmitting(true)
    setError('')
    try {
      await commentApi.updateComment(postId, comment.id, { content })
      setIsEditing(false)
      onChanged()
    } catch (err) {
      setError(err.response?.data?.message ?? '댓글 수정에 실패했습니다.')
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleDelete() {
    if (!window.confirm('댓글을 삭제할까요?')) return

    try {
      await commentApi.deleteComment(postId, comment.id)
      onChanged()
    } catch (err) {
      window.alert(err.response?.data?.message ?? '댓글 삭제에 실패했습니다.')
    }
  }

  return (
    <div className={styles.item}>
      <span className={styles.avatar} style={{ backgroundColor: getAvatarColor(comment.authorNickname) }}>
        {comment.authorNickname?.[0]}
      </span>
      <div className={styles.body}>
        <div className={styles.meta}>
          <span className={styles.name}>{comment.authorNickname}</span>
          <span className={styles.date}>{formatDate(comment.createdAt)}</span>

          {isOwner && !isEditing && (
            <span className={styles.ownerActions}>
              <button type="button" className={styles.iconButton} onClick={startEdit} aria-label="댓글 수정">
                <Pencil size={13} />
              </button>
              <button type="button" className={styles.iconButton} onClick={handleDelete} aria-label="댓글 삭제">
                <Trash2 size={13} />
              </button>
            </span>
          )}
        </div>

        {isEditing ? (
          <form className={styles.editForm} onSubmit={handleSave}>
            <textarea
              className={styles.editTextarea}
              value={content}
              onChange={(event) => setContent(event.target.value)}
              autoFocus
            />
            {error && <p className={styles.error}>{error}</p>}
            <div className={styles.editActions}>
              <button type="button" className={styles.cancelButton} onClick={cancelEdit}>
                취소
              </button>
              <button type="submit" className={styles.saveButton} disabled={isSubmitting || !content.trim()}>
                {isSubmitting ? '저장 중...' : '저장'}
              </button>
            </div>
          </form>
        ) : (
          <p className={styles.content}>{comment.content}</p>
        )}
      </div>
    </div>
  )
}
