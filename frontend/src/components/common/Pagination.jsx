import { ChevronLeft, ChevronRight } from 'lucide-react'
import styles from './Pagination.module.css'

// page: 0-based 현재 페이지, totalPages: 전체 페이지 수, onChange: (0-based) => void
export default function Pagination({ page, totalPages, onChange }) {
  if (totalPages <= 1) return null

  const current = page + 1
  const items = buildPageItems(current, totalPages)

  return (
    <nav className={styles.pagination} aria-label="페이지네이션">
      <button
        type="button"
        className={styles.pageButton}
        disabled={current === 1}
        onClick={() => onChange(page - 1)}
        aria-label="이전 페이지"
      >
        <ChevronLeft size={16} />
      </button>

      {items.map((item, index) =>
        item === '...' ? (
          <span key={`ellipsis-${index}`} className={styles.ellipsis}>
            …
          </span>
        ) : (
          <button
            key={item}
            type="button"
            className={`${styles.pageButton} ${item === current ? styles.active : ''}`}
            onClick={() => onChange(item - 1)}
          >
            {item}
          </button>
        ),
      )}

      <button
        type="button"
        className={styles.pageButton}
        disabled={current === totalPages}
        onClick={() => onChange(page + 1)}
        aria-label="다음 페이지"
      >
        <ChevronRight size={16} />
      </button>
    </nav>
  )
}

function buildPageItems(current, totalPages) {
  const items = []
  const windowStart = Math.max(2, current - 1)
  const windowEnd = Math.min(totalPages - 1, current + 1)

  items.push(1)
  if (windowStart > 2) items.push('...')
  for (let page = windowStart; page <= windowEnd; page += 1) items.push(page)
  if (windowEnd < totalPages - 1) items.push('...')
  if (totalPages > 1) items.push(totalPages)

  return items
}
