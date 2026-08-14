import { X } from 'lucide-react'
import styles from './Modal.module.css'

// 오버레이 + 중앙 카드 + 닫기 버튼만 담당하는 공용 모달 뼈대. 내용은 children으로 받는다.
export default function Modal({ onClose, children, maxWidth = 420, hideCloseButton = false }) {
  return (
    <div className={styles.overlay} onClick={onClose}>
      <div className={styles.card} style={{ maxWidth }} onClick={(event) => event.stopPropagation()}>
        {!hideCloseButton && (
          <button type="button" className={styles.closeButton} onClick={onClose} aria-label="닫기">
            <X size={18} />
          </button>
        )}
        {children}
      </div>
    </div>
  )
}
