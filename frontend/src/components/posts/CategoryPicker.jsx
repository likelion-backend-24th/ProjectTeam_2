import { POST_CATEGORIES } from '../../constants/postCategory'
import styles from './CategoryPicker.module.css'

// value: PostCategory 값 문자열 | '', onChange: (value: string) => void
export default function CategoryPicker({ value, onChange }) {
  return (
    <div className={styles.picker}>
      {POST_CATEGORIES.map((category) => (
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
