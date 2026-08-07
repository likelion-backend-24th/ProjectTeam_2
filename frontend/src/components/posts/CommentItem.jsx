import { getAvatarColor } from '../../utils/avatarColor'
import { formatDate } from '../../utils/formatDate'
import styles from './CommentItem.module.css'

export default function CommentItem({ comment }) {
  return (
    <div className={styles.item}>
      <span className={styles.avatar} style={{ backgroundColor: getAvatarColor(comment.authorNickname) }}>
        {comment.authorNickname?.[0]}
      </span>
      <div className={styles.body}>
        <div className={styles.meta}>
          <span className={styles.name}>{comment.authorNickname}</span>
          <span className={styles.date}>{formatDate(comment.createdAt)}</span>
        </div>
        <p className={styles.content}>{comment.content}</p>
      </div>
    </div>
  )
}
