import { Flag } from 'lucide-react'
import { useState } from 'react'
import { reportApi } from '../../api'
import { REPORT_REASONS } from '../../constants/reportReason'
import styles from './ReportButton.module.css'

// 게시글/댓글/스터디게시글/스터디댓글/전문가 상담 공용 신고 버튼 + 모달.
// 백엔드 POST /api/reports 하나로 다섯 종류(targetType) 모두 처리한다.
// variant: 'text'(아이콘+텍스트, 게시글 상단용) | 'icon'(아이콘만, 댓글 목록용)
export default function ReportButton({ targetType, targetId, variant = 'text' }) {
  const [isOpen, setIsOpen] = useState(false)
  const [reason, setReason] = useState(REPORT_REASONS[0].value)
  const [detail, setDetail] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState('')

  function openModal() {
    setReason(REPORT_REASONS[0].value)
    setDetail('')
    setError('')
    setIsOpen(true)
  }

  function closeModal() {
    if (isSubmitting) return
    setIsOpen(false)
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setIsSubmitting(true)
    setError('')
    try {
      await reportApi.createReport({ targetType, targetId, reason, detail })
      setIsOpen(false)
      window.alert('신고가 접수되었습니다.')
    } catch (err) {
      setError(err.response?.data?.message ?? '신고 접수에 실패했습니다.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <>
      <button
        type="button"
        className={variant === 'icon' ? styles.iconButton : styles.textButton}
        onClick={openModal}
        aria-label="신고하기"
      >
        <Flag size={variant === 'icon' ? 13 : 14} />
        {variant === 'text' && '신고'}
      </button>

      {isOpen && (
        <div className={styles.overlay} onClick={closeModal}>
          <div className={styles.modal} onClick={(event) => event.stopPropagation()}>
            <h2 className={styles.title}>신고하기</h2>

            <form onSubmit={handleSubmit}>
              <div className={styles.reasonList}>
                {REPORT_REASONS.map((item) => (
                  <label key={item.value} className={styles.reasonItem}>
                    <input
                      type="radio"
                      name="reportReason"
                      value={item.value}
                      checked={reason === item.value}
                      onChange={() => setReason(item.value)}
                    />
                    {item.label}
                  </label>
                ))}
              </div>

              <textarea
                className={styles.detailInput}
                placeholder="상세 사유(선택)"
                value={detail}
                onChange={(event) => setDetail(event.target.value)}
                maxLength={500}
              />

              {error && <p className={styles.error}>{error}</p>}

              <div className={styles.actions}>
                <button type="button" className={styles.cancelButton} onClick={closeModal} disabled={isSubmitting}>
                  취소
                </button>
                <button type="submit" className={styles.submitButton} disabled={isSubmitting}>
                  {isSubmitting ? '접수 중...' : '신고하기'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </>
  )
}
