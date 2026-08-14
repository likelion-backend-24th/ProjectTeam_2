import { AlertCircle, CheckCircle2, ChevronLeft, Clock, XCircle } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { expertApi } from '../../api'
import SiteHeader from '../../components/common/SiteHeader'
import { useAuth } from '../../context/AuthContext'
import { getAvatarColor } from '../../utils/avatarColor'
import styles from './ExpertApplicationStatusPage.module.css'

const TABS = [
  { key: 'PENDING', label: '심사중' },
  { key: 'APPROVED', label: '승인 완료' },
  { key: 'REJECTED', label: '반려' },
]

// 참고: 백엔드 GET /api/experts/me가 status/reason만 내려주고 신청일은 안 줘서,
// 이 화면에는 신청일을 표시하지 못한다(심사 기간 안내만 고정 문구로 노출).
export default function ExpertApplicationStatusPage() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const [status, setStatus] = useState(null)
  const [reason, setReason] = useState('')
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let ignore = false
    expertApi
      .getMyApplicationStatus()
      .then(({ data }) => {
        if (ignore) return
        setStatus(data.data.status)
        setReason(data.data.reason ?? '')
      })
      .catch((err) => {
        if (ignore) return
        if (err.response?.status === 404) {
          navigate('/experts/apply', { replace: true })
          return
        }
        setError('신청 현황을 불러오지 못했습니다.')
      })
      .finally(() => {
        if (!ignore) setIsLoading(false)
      })
    return () => {
      ignore = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return (
    <>
      <SiteHeader />
      <main className={styles.main}>
        <Link to="/experts/apply" className={styles.breadcrumb}>
          <ChevronLeft size={16} />
          전문가 등록 신청
        </Link>

        <p className={styles.eyebrow}>APPLICATION STATUS</p>
        <h1 className={styles.title}>신청 현황</h1>

        {isLoading && <p className={styles.state}>불러오는 중...</p>}
        {!isLoading && error && <p className={styles.state}>{error}</p>}

        {!isLoading && status && (
          <>
            <div className={styles.tabs}>
              {TABS.map((tab) => (
                <span
                  key={tab.key}
                  className={`${styles.tab} ${tab.key === status ? styles[`tabActive${capitalize(tab.key)}`] : ''}`}
                >
                  {tab.label}
                </span>
              ))}
            </div>

            {status === 'PENDING' && (
              <div className={`${styles.card} ${styles.cardPending}`}>
                <span className={`${styles.icon} ${styles.iconPending}`}>
                  <Clock size={26} />
                </span>
                <span className={`${styles.statusBadge} ${styles.badgePending}`}>심사중</span>
                <p className={styles.cardTitle}>신청서를 검토하고 있어요</p>
                <p className={styles.cardSubtitle}>
                  보통 3~5 영업일 이내에 심사 결과를 안내드립니다. 조금만 기다려주세요!
                </p>
                <div className={styles.actions}>
                  <button type="button" className={styles.secondaryButton} onClick={() => navigate('/experts/apply')}>
                    신청서 수정
                  </button>
                  <button type="button" className={styles.secondaryButton} onClick={() => navigate('/')}>
                    홈으로
                  </button>
                </div>
              </div>
            )}

            {status === 'APPROVED' && (
              <div className={`${styles.card} ${styles.cardApproved}`}>
                <span className={`${styles.icon} ${styles.iconApproved}`}>
                  <CheckCircle2 size={26} />
                </span>
                <span className={`${styles.statusBadge} ${styles.badgeApproved}`}>승인 완료</span>
                <p className={styles.cardTitle}>전문가로 승인됐어요! 🎉</p>
                <p className={styles.cardSubtitle}>
                  이제 JOBtogether 전문가로 활동할 수 있습니다. 전문가 상담 페이지에서 받은 문의를 확인해보세요.
                </p>

                <div className={styles.previewCard}>
                  <span className={styles.previewAvatar} style={{ backgroundColor: getAvatarColor(user?.nickname) }}>
                    {user?.nickname?.[0]}
                  </span>
                  <p className={styles.previewName}>
                    {user?.nickname}
                    <span className={styles.previewBadge}>☆ 전문가</span>
                  </p>
                </div>

                <div className={styles.actions}>
                  <button
                    type="button"
                    className={`${styles.primaryButton} ${styles.primaryButtonApproved}`}
                    onClick={() => navigate('/experts')}
                  >
                    전문가 페이지로 이동
                  </button>
                  <button type="button" className={styles.secondaryButton} onClick={() => navigate('/')}>
                    홈으로
                  </button>
                </div>
              </div>
            )}

            {status === 'REJECTED' && (
              <div className={`${styles.card} ${styles.cardRejected}`}>
                <span className={`${styles.icon} ${styles.iconRejected}`}>
                  <XCircle size={26} />
                </span>
                <span className={`${styles.statusBadge} ${styles.badgeRejected}`}>신청 반려</span>
                <p className={styles.cardTitle}>아쉽게도 이번엔 반려됐어요</p>
                <p className={styles.cardSubtitle}>아래 사유를 확인하고 내용을 보완한 후 재신청해주세요. 더 좋은 결과를 응원합니다!</p>

                {reason && (
                  <div className={styles.reasonBox}>
                    <p className={styles.reasonHeading}>
                      <AlertCircle size={13} />
                      반려 사유
                    </p>
                    <p className={styles.reasonText}>{reason}</p>
                  </div>
                )}

                <div className={styles.actions}>
                  <button
                    type="button"
                    className={`${styles.primaryButton} ${styles.primaryButtonRejected}`}
                    onClick={() => navigate('/experts/apply')}
                  >
                    재신청하기
                  </button>
                  <button type="button" className={styles.secondaryButton} onClick={() => navigate('/')}>
                    홈으로
                  </button>
                </div>
              </div>
            )}

            <p className={styles.footerNote}>심사 기간: 3~5 영업일</p>
          </>
        )}
      </main>
    </>
  )
}

function capitalize(text) {
  return text.charAt(0) + text.slice(1).toLowerCase()
}
