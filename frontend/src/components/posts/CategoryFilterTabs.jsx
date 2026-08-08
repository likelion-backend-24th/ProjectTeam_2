import { POST_CATEGORIES } from '../../constants/postCategory'
import styles from './CategoryFilterTabs.module.css'

// value: null(전체) | 카테고리 값 문자열, onChange: (value: string | null) => void
// categories: {value,label} 배열. 게시글 카테고리가 기본값이고, 스터디 등 다른 도메인에서도 재사용한다.
export default function CategoryFilterTabs({ value, onChange, categories = POST_CATEGORIES }) {
  return (
    <div className={styles.tabs}>
      <button
        type="button"
        className={`${styles.tab} ${value === null ? styles.active : ''}`}
        onClick={() => onChange(null)}
      >
        전체
      </button>
      {categories.map((category) => (
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
