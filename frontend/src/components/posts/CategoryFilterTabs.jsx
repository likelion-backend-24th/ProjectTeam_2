import { POST_CATEGORIES } from '../../constants/postCategory'
import styles from './CategoryFilterTabs.module.css'

// value: null(전체) | PostCategory 값 문자열, onChange: (value: string | null) => void
export default function CategoryFilterTabs({ value, onChange }) {
  return (
    <div className={styles.tabs}>
      <button
        type="button"
        className={`${styles.tab} ${value === null ? styles.active : ''}`}
        onClick={() => onChange(null)}
      >
        전체
      </button>
      {POST_CATEGORIES.map((category) => (
        <button
          key={category.value}
          type="button"
          className={`${styles.tab} ${value === category.value ? styles.active : ''}`}
          onClick={() => onChange(category.value)}
        >
          {category.label}
        </button>
      ))}
    </div>
  )
}
