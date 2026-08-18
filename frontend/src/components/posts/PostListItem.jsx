import { ChevronRight, Eye } from 'lucide-react'
import { Link } from 'react-router-dom'
import { getPostCategoryMeta } from '../../constants/postCategory'
import { getAvatarColor } from '../../utils/avatarColor'
import { formatDateTime } from '../../utils/formatDate'
import styles from './PostListItem.module.css'

export default function PostListItem({ post }) {
  const categoryMeta = getPostCategoryMeta(post.category)

  return (
    <Link to={`/posts/${post.id}`} className={styles.item}>
      <div className={styles.main}>
        {categoryMeta && (
          <span className={styles.badge} style={{ backgroundColor: categoryMeta.bg, color: categoryMeta.color }}>
            {post.categoryLabel}
          </span>
        )}
        <p className={styles.title}>{post.title}</p>
        <div className={styles.meta}>
          <span className={styles.avatar} style={{ backgroundColor: getAvatarColor(post.authorNickname) }}>
            {post.authorNickname?.[0]}
          </span>
          <span>{post.authorNickname}</span>
          <span>·</span>
          <span>{formatDateTime(post.createdAt)}</span>
          <span>·</span>
          <span className={styles.viewCount}>
            <Eye size={13} />
            {post.viewCount}
          </span>
        </div>
      </div>
      <ChevronRight className={styles.chevron} size={18} />
    </Link>
  )
}
