import { POST_CATEGORIES } from '../../constants/postCategory'
import styles from './CategoryPicker.module.css'

// value: 카테고리 값 문자열 | '', onChange: (value: string) => void
// categories: {value,label} 배열. 게시글 카테고리가 기본값이고, 스터디 등 다른 도메인에서도 재사용한다.
export default function CategoryPicker({ value, onChange, categories = POST_CATEGORIES }) {
  return (
    <div className={styles.picker}>
      {categories.map((category) => (
        <button
          key={category.value}
          type="button"
          className={`${styles.option} ${value === category.value ? styles.selected : ''}`}
          onClick={() => onChange(category.value)}
        >
          {category.label}
        </button>
      ))}
    </div>
  )
}
