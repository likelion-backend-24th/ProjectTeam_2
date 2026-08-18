import { Clock, Crown, Lock, MessageSquarePlus, XCircle } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { expertApi, feedbackApi } from '../../api'
import ExpertCard from '../../components/experts/ExpertCard'
import ExpertProfileModal from '../../components/experts/ExpertProfileModal'
import NewConsultModal from '../../components/experts/NewConsultModal'
import SiteHeader from '../../components/common/SiteHeader'
import { useAuth } from '../../context/AuthContext'
import { getAvatarColor } from '../../utils/avatarColor'
import { formatDate } from '../../utils/formatDate'
import styles from './ExpertConsultPage.module.css'

function StatusBadge({ status, closed }) {
  if (closed) return <span className={`${styles.statusBadge} ${styles.statusClosed}`}>완료</span>
  if (status === 'ANSWERED') return <span className={`${styles.statusBadge} ${styles.statusAnswered}`}>답변 완료</span>
  return <span className={`${styles.statusBadge} ${styles.statusPending}`}>답변 대기</span>
}

export default function ExpertConsultPage() {
  const { user, isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const isExpert = user?.role === 'EXPERT'

  const [experts, setExperts] = useState([])
  const [isLoadingExperts, setIsLoadingExperts] = useState(true)

  const [myFeedbacks, setMyFeedbacks] = useState(null) // null = 아직 안 불러옴/구독 아님
  const [expertFeedbacks, setExpertFeedbacks] = useState([])
  // 전문가 계정은 항상 전문가 모드 화면만 본다(토글 없음).
  const expertMode = isExpert

  const [profileExpertId, setProfileExpertId] = useState(null)
  const [consultModal, setConsultModal] = useState(null) // null | { preselected }

  // 전문가가 아닌 로그인 유저의 신청 이력(심사중/반려)을 조회한다. 신청한 적 없으면 404.
  const [applicationStatus, setApplicationStatus] = useState(null)

  useEffect(() => {
    if (!isAuthenticated || isExpert) return
    let ignore = false
    expertApi
      .getMyApplicationStatus()
      .then(({ data }) => {
        if (!ignore) setApplicationStatus(data.data.status)
      })
      .catch(() => {
        // 신청 이력 없음(404) 등은 무시하고 기본 "신청하기" 안내를 보여준다.
      })
    return () => {
      ignore = true
    }
  }, [isAuthenticated, isExpert])

  useEffect(() => {
    let ignore = false
    expertApi
      .getPublicExperts()
      .then(({ data }) => {
        if (!ignore) setExperts(data.data)
      })
      .finally(() => {
        if (!ignore) setIsLoadingExperts(false)
      })
    return () => {
      ignore = true
    }
  }, [])

  useEffect(() => {
    if (!isAuthenticated || !user?.subscribed) return
    let ignore = false
    feedbackApi.getMyFeedbacks().then(({ data }) => {
      if (!ignore) setMyFeedbacks(data.data.feedbacks)
    })
    return () => {
      ignore = true
    }
  }, [isAuthenticated, user])

  useEffect(() => {
    if (!isExpert || !expertMode) return
    let ignore = false
    feedbackApi.getMyExpertFeedbacks().then(({ data }) => {
      if (!ignore) setExpertFeedbacks(data.data.feedbacks)
    })
    return () => {
      ignore = true
    }
  }, [isExpert, expertMode])

  function openProfile(expertId) {
    setProfileExpertId(expertId)
  }

  function handleStartConsult(expertId, name, career) {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: location } })
      return
    }
    if (!user.subscribed) {
      navigate('/subscription')
      return
    }
    setProfileExpertId(null)
    setConsultModal({ preselected: { expertId, name, career } })
  }

  function handleNewConsultClick() {
    setConsultModal({ preselected: null })
  }

  function handleConsultCreated(feedback) {
    setConsultModal(null)
    navigate(`/experts/consult/${feedback.id}`)
  }

  const pending = expertFeedbacks.filter((f) => f.status === 'PENDING' && !f.closedBy)
  const inProgress = expertFeedbacks.filter((f) => f.status === 'ANSWERED' && !f.closedBy)
  const closed = expertFeedbacks.filter((f) => Boolean(f.closedBy))

  return (
    <>
      <SiteHeader />
      <main className={styles.main}>
        <div className={styles.headingRow}>
          <div>
            <p className={styles.eyebrow}>EXPERT CONSULT</p>
            <h1 className={styles.title}>전문가 상담</h1>
            <p className={styles.subtitle}>현직자·합격자에게 커리어 고민을 1:1로 질문하세요.</p>
          </div>
        </div>

        {!expertMode && applicationStatus && applicationStatus !== 'APPROVED' && (
          <ApplicationStatusBanner status={applicationStatus} onClick={() => navigate('/experts/apply/status')} />
        )}

        {expertMode ? (
          <>
            <div className={styles.statsRow}>
              <div className={styles.statBox}>
                <span className={styles.statValue}>{pending.length}</span>
                <span className={styles.statLabel}>대기 중</span>
              </div>
              <div className={styles.statBox}>
                <span className={styles.statValue}>{inProgress.length}</span>
                <span className={styles.statLabel}>진행 중</span>
              </div>
              <div className={styles.statBox}>
                <span className={styles.statValue}>{closed.length}</span>
                <span className={styles.statLabel}>완료</span>
              </div>
            </div>

            <FeedbackGroup title="대기 중인 문의" feedbacks={pending} onClick={(id) => navigate(`/experts/consult/${id}`)} />
            <FeedbackGroup
              title="진행 중인 상담"
              feedbacks={inProgress}
              onClick={(id) => navigate(`/experts/consult/${id}`)}
            />
            <FeedbackGroup title="완료된 상담" feedbacks={closed} onClick={(id) => navigate(`/experts/consult/${id}`)} />
          </>
        ) : (
          <>
            {isAuthenticated && user.subscribed ? (
              <>
                <div className={styles.sectionHeader}>
                  <p className={styles.sectionTitle}>
                    {myFeedbacks === null ? '내 상담' : `${myFeedbacks.length}개의 진행 중인 상담`}
                  </p>
                  <button type="button" className={styles.newConsultButton} onClick={handleNewConsultClick}>
                    <MessageSquarePlus size={14} />
                    새 상담 추가
                  </button>
                </div>

                {myFeedbacks === null && <p className={styles.state}>불러오는 중...</p>}
                {myFeedbacks?.length === 0 && (
                  <div className={styles.emptyState}>아직 진행 중인 상담이 없어요. 전문가에게 첫 질문을 남겨보세요.</div>
                )}
                {myFeedbacks && myFeedbacks.length > 0 && (
                  <div className={styles.threadList}>
                    {myFeedbacks.map((feedback) => (
                      <button
                        key={feedback.feedbackId}
                        type="button"
                        className={styles.threadRow}
                        onClick={() => navigate(`/experts/consult/${feedback.feedbackId}`)}
                      >
                        <span
                          className={styles.avatar}
                          style={{ backgroundColor: getAvatarColor(feedback.expertName) }}
                        >
                          {feedback.expertName?.[0]}
                        </span>
                        <div className={styles.threadBody}>
                          <div className={styles.threadTopRow}>
                            <span className={styles.threadName}>{feedback.expertName}</span>
                          </div>
                          <p className={styles.threadTopic}>{feedback.topic}</p>
                        </div>
                        <div className={styles.threadMeta}>
                          {feedback.answeredAt && <span className={styles.threadDate}>{formatDate(feedback.answeredAt)}</span>}
                          <StatusBadge status={feedback.status} />
                        </div>
                      </button>
                    ))}
                  </div>
                )}
              </>
            ) : (
              <div className={styles.lockedTeaser}>
                <div className={styles.lockedRows}>
                  {[0, 1, 2].map((i) => (
                    <div key={i} className={styles.lockedRow}>
                      <span className={styles.lockedAvatar} style={{ backgroundColor: getAvatarColor(String(i)) }} />
                      <div style={{ flex: 1 }}>
                        <span className={styles.lockedLine} style={{ display: 'block', width: '40%', marginBottom: 8 }} />
                        <span className={styles.lockedLine} style={{ display: 'block', width: '70%' }} />
                      </div>
                    </div>
                  ))}
                </div>
                <div className={styles.lockedOverlay}>
                  <span className={styles.lockIcon}>
                    <Lock size={20} />
                  </span>
                  <p className={styles.lockedTitle}>프리미엄 구독 전용</p>
                  <p className={styles.lockedSubtitle}>구독 후 전문가에게 직접 1:1 상담을 시작할 수 있어요.</p>
                  <button type="button" className={styles.subscribeButton} onClick={() => navigate('/subscription')}>
                    <Crown size={16} />
                    구독하고 시작하기
                  </button>
                </div>
              </div>
            )}

            <div className={styles.expertGridHeader}>
              <p className={styles.expertGridTitle}>활동 중인 전문가</p>
              <span className={styles.expertGridCount}>{experts.length}명</span>
            </div>
            {isLoadingExperts && <p className={styles.state}>불러오는 중...</p>}
            {!isLoadingExperts && experts.length === 0 && <p className={styles.state}>아직 활동 중인 전문가가 없어요.</p>}
            {!isLoadingExperts && experts.length > 0 && (
              <div className={styles.expertGrid}>
                {experts.map((expert) => (
                  <ExpertCard key={expert.expertId} expert={expert} onClick={openProfile} />
                ))}
              </div>
            )}

            {isAuthenticated && !isExpert && !applicationStatus && (
              <p className={styles.applyFooter}>
                전문가로 활동하고 싶으신가요?{' '}
                <button type="button" className={styles.applyFooterLink} onClick={() => navigate('/experts/apply')}>
                  전문가 신청하기 →
                </button>
              </p>
            )}
          </>
        )}
      </main>

      {profileExpertId && (
        <ExpertProfileModal
          expertId={profileExpertId}
          isSubscribed={Boolean(isAuthenticated && user.subscribed)}
          onClose={() => setProfileExpertId(null)}
          onStartConsult={handleStartConsult}
        />
      )}

      {consultModal && (
        <NewConsultModal
          experts={experts}
          preselected={consultModal.preselected}
          onClose={() => setConsultModal(null)}
          onCreated={handleConsultCreated}
        />
      )}
    </>
  )
}

function ApplicationStatusBanner({ status, onClick }) {
  const isPending = status === 'PENDING'
  return (
    <button
      type="button"
      className={`${styles.applyStatusBanner} ${isPending ? styles.applyStatusBannerPending : styles.applyStatusBannerRejected}`}
      onClick={onClick}
    >
      <span className={styles.applyStatusIcon}>{isPending ? <Clock size={18} /> : <XCircle size={18} />}</span>
      <span className={styles.applyStatusBody}>
        <span className={styles.applyStatusTitle}>
          {isPending ? '전문가 신청 심사중이에요' : '전문가 신청이 반려됐어요'}
        </span>
        <span className={styles.applyStatusSubtitle}>
          {isPending ? '3~5 영업일 이내에 결과를 안내드려요.' : '사유를 확인하고 재신청할 수 있어요.'}
        </span>
      </span>
      <span className={styles.applyStatusLink}>신청 현황 보기 →</span>
    </button>
  )
}

function FeedbackGroup({ title, feedbacks, onClick }) {
  return (
    <div>
      <div className={styles.sectionHeader}>
        <p className={styles.sectionTitle}>
          {title} ({feedbacks.length}건)
        </p>
      </div>
      {feedbacks.length === 0 ? (
        <div className={styles.emptyState}>해당하는 문의가 없어요.</div>
      ) : (
        <div className={styles.threadList}>
          {feedbacks.map((feedback) => (
            <button key={feedback.id} type="button" className={styles.threadRow} onClick={() => onClick(feedback.id)}>
              <span className={styles.avatar} style={{ backgroundColor: getAvatarColor(feedback.requesterNickname) }}>
                {feedback.requesterNickname?.[0]}
              </span>
              <div className={styles.threadBody}>
                <div className={styles.threadTopRow}>
                  <span className={styles.threadName}>{feedback.topic}</span>
                </div>
                <p className={styles.threadTopic}>{feedback.requesterNickname}</p>
              </div>
              <div className={styles.threadMeta}>
                <span className={styles.threadDate}>{formatDate(feedback.createdAt)}</span>
                <StatusBadge status={feedback.status} closed={Boolean(feedback.closedBy)} />
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
