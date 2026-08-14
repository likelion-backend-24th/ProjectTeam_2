import {
  Activity,
  AlertCircle,
  Award,
  Ban,
  Briefcase,
  Check,
  CheckCircle2,
  Clock,
  Crown,
  Eye,
  FileText,
  LayoutGrid,
  Shield,
  ShieldAlert,
  Trash2,
  UserCheck,
  Users,
  X,
  XCircle,
  Zap,
} from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { adminApi, expertApi, postApi, studyApi } from '../../api'
import SiteHeader from '../../components/common/SiteHeader'
import { getJobFieldLabel } from '../../constants/jobField'
import { getAvatarColor } from '../../utils/avatarColor'
import { formatDate } from '../../utils/formatDate'
import styles from './AdminPanelPage.module.css'

const TABS = [
  { key: 'dashboard', label: '대시보드', icon: LayoutGrid },
  { key: 'members', label: '회원 관리', icon: Users },
  { key: 'experts', label: '전문가 심사', icon: Shield },
]

const ROLE_META = {
  USER: { label: 'USER', color: '#c6ff3d' },
  EXPERT: { label: 'EXPERT', color: '#60a5fa' },
  ADMIN: { label: 'ADMIN', color: '#fb923c' },
}

export default function AdminPanelPage() {
  const [activeTab, setActiveTab] = useState('dashboard')
  const [pendingCount, setPendingCount] = useState(0)

  return (
    <>
      <SiteHeader />
      <main className={styles.main}>
        <div className={styles.headingRow}>
          <div>
            <span className={styles.adminBadge}>
              <Zap size={12} />
              ADMIN ONLY
            </span>
            <h1 className={styles.title}>어드민 패널</h1>
            <p className={styles.subtitle}>JOBtogether 운영 관리 콘솔</p>
          </div>
          <span className={styles.statusPill}>
            <Activity size={14} />
            서비스 정상
          </span>
        </div>

        <nav className={styles.tabs}>
          {TABS.map((tab) => (
            <button
              key={tab.key}
              type="button"
              className={`${styles.tabButton} ${activeTab === tab.key ? styles.tabActive : ''}`}
              onClick={() => setActiveTab(tab.key)}
            >
              <tab.icon size={15} />
              {tab.label}
              {tab.key === 'experts' && pendingCount > 0 && <span className={styles.tabCount}>{pendingCount}</span>}
            </button>
          ))}
        </nav>

        {activeTab === 'dashboard' && (
          <DashboardTab onGoToExperts={() => setActiveTab('experts')} />
        )}
        {activeTab === 'members' && <MembersTab />}
        {activeTab === 'experts' && <ExpertReviewTab onPendingCountChange={setPendingCount} />}
      </main>
    </>
  )
}

// ---------------- 대시보드 ----------------

function DashboardTab({ onGoToExperts }) {
  const [stats, setStats] = useState(null)
  const [recentPosts, setRecentPosts] = useState([])
  const [expertPreview, setExpertPreview] = useState([])
  const [studies, setStudies] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let ignore = false

    Promise.all([
      adminApi.getUsers({ page: 0, size: 1 }),
      adminApi.getUsers({ page: 0, size: 1000 }),
      studyApi.getStudies({ page: 0, size: 1 }),
      studyApi.getStudies({ page: 0, size: 4 }),
      postApi.getPosts({ page: 0, size: 4 }),
      expertApi.getExperts(),
    ])
      .then(([usersRes, allUsersRes, studyCountRes, studiesRes, postsRes, expertsRes]) => {
        if (ignore) return
        const experts = expertsRes.data.data.experts
        setStats({
          totalUsers: usersRes.data.meta.pagination.totalItems,
          subscriberCount: allUsersRes.data.data.filter((u) => u.subscribed).length,
          totalStudies: studyCountRes.data.meta.pagination.totalItems,
          totalPosts: postsRes.data.data.totalElements,
          pendingExperts: experts.filter((e) => e.status === 'PENDING').length,
        })
        setExpertPreview(experts.slice(0, 4))
        setRecentPosts(postsRes.data.data.content)
        setStudies(studiesRes.data.data)
      })
      .catch(() => {
        if (!ignore) setError('대시보드 데이터를 불러오지 못했습니다.')
      })
      .finally(() => {
        if (!ignore) setIsLoading(false)
      })

    return () => {
      ignore = true
    }
  }, [])

  async function handleDeletePost(post) {
    if (!window.confirm(`"${post.title}" 게시글을 강제 삭제할까요?`)) return
    try {
      await adminApi.deletePost(post.id)
      setRecentPosts((prev) => prev.filter((p) => p.id !== post.id))
    } catch (err) {
      window.alert(err.response?.data?.message ?? '게시글 삭제에 실패했습니다.')
    }
  }

  async function handleDeleteStudy(study) {
    if (!window.confirm(`"${study.title}" 스터디를 강제 삭제할까요?`)) return
    try {
      await adminApi.deleteStudy(study.id)
      setStudies((prev) => prev.filter((s) => s.id !== study.id))
    } catch (err) {
      window.alert(err.response?.data?.message ?? '스터디 삭제에 실패했습니다.')
    }
  }

  if (isLoading) return <p className={styles.state}>불러오는 중...</p>
  if (error) return <p className={styles.state}>{error}</p>

  return (
    <>
      <div className={styles.statsGrid}>
        <StatCard icon={<Users size={18} />} color="#60a5fa" value={stats.totalUsers} label="총 회원" />
        <StatCard icon={<Award size={18} />} color="#8b5cf6" value={stats.totalStudies} label="전체 스터디" />
        <StatCard icon={<FileText size={18} />} color="#34d399" value={stats.totalPosts} label="전체 게시글" />
        <StatCard icon={<Crown size={18} />} color="#c6ff3d" value={stats.subscriberCount} label="구독자" />
        <StatCard icon={<ShieldAlert size={18} />} color="#fb923c" value={stats.pendingExperts} label="전문가 신청" />
      </div>

      <div className={styles.dashboardGrid}>
        <section>
          <div className={styles.sectionHeader}>
            <p className={styles.sectionTitle}>최근 게시글</p>
          </div>
          {recentPosts.length === 0 ? (
            <p className={styles.emptyState}>게시글이 없어요.</p>
          ) : (
            <div className={styles.list}>
              {recentPosts.map((post) => (
                <div key={post.id} className={styles.row}>
                  <div className={styles.rowMain}>
                    <p className={styles.rowTitle}>{post.title}</p>
                    <span className={styles.rowMeta}>
                      {post.authorNickname} · {formatDate(post.createdAt)}
                    </span>
                  </div>
                  <span className={styles.rowMeta}>
                    <Eye size={13} />
                    {post.viewCount}
                  </span>
                  <button
                    type="button"
                    className={styles.iconButton}
                    onClick={() => handleDeletePost(post)}
                    aria-label="게시글 강제 삭제"
                  >
                    <Trash2 size={15} />
                  </button>
                </div>
              ))}
            </div>
          )}
        </section>

        <section>
          <div className={styles.sectionHeader}>
            <p className={styles.sectionTitle}>전문가 신청 현황</p>
            <button type="button" className={styles.sectionLink} onClick={onGoToExperts}>
              심사하기 →
            </button>
          </div>
          {expertPreview.length === 0 ? (
            <p className={styles.emptyState}>신청 내역이 없어요.</p>
          ) : (
            <div className={styles.list}>
              {expertPreview.map((expert) => (
                <div key={expert.id} className={styles.row}>
                  <span className={styles.avatar} style={{ backgroundColor: getAvatarColor(String(expert.userId)) }}>
                    {expert.userId}
                  </span>
                  <div className={styles.rowMain}>
                    <p className={styles.rowTitle}>신청자 #{expert.userId}</p>
                    <span className={styles.rowMeta}>
                      {expert.careers[0]
                        ? `${expert.careers[0].companyName} · ${expert.careers[0].position} · ${expert.careers[0].years}년차`
                        : '경력 정보 없음'}
                    </span>
                  </div>
                  <ExpertStatusBadge status={expert.status} />
                </div>
              ))}
            </div>
          )}
        </section>
      </div>

      <div className={styles.sectionHeader}>
        <p className={styles.sectionTitle}>스터디 현황</p>
      </div>
      {studies.length === 0 ? (
        <p className={styles.emptyState}>스터디가 없어요.</p>
      ) : (
        <div className={styles.studyGrid}>
          {studies.map((study) => {
            const ratio = study.capacity > 0 ? Math.min(100, (study.currentMemberCount / study.capacity) * 100) : 0
            return (
              <div key={study.id} className={styles.studyCard}>
                <div className={styles.studyCardTop}>
                  <p className={styles.studyTitle}>{study.title}</p>
                  <button
                    type="button"
                    className={styles.iconButton}
                    onClick={() => handleDeleteStudy(study)}
                    aria-label="스터디 강제 삭제"
                  >
                    <Trash2 size={14} />
                  </button>
                </div>
                <span className={styles.studyCount}>
                  {study.currentMemberCount}/{study.capacity}명
                </span>
                <div className={styles.progressTrack}>
                  <div className={styles.progressFill} style={{ width: `${ratio}%` }} />
                </div>
              </div>
            )
          })}
        </div>
      )}
    </>
  )
}

function StatCard({ icon, color, value, label }) {
  return (
    <div className={styles.statCard}>
      <span className={styles.statIcon} style={{ backgroundColor: `${color}24`, color }}>
        {icon}
      </span>
      <span className={styles.statValue}>{value.toLocaleString()}</span>
      <span className={styles.statLabel}>{label}</span>
    </div>
  )
}

// ---------------- 회원 관리 ----------------

const ROLE_FILTERS = ['ALL', 'USER', 'EXPERT', 'ADMIN']

function MembersTab() {
  const [users, setUsers] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [keyword, setKeyword] = useState('')
  const [roleFilter, setRoleFilter] = useState('ALL')
  const [updatingId, setUpdatingId] = useState(null)

  useEffect(() => {
    let ignore = false
    adminApi
      .getUsers({ page: 0, size: 500 })
      .then(({ data }) => {
        if (!ignore) setUsers(data.data)
      })
      .catch(() => {
        if (!ignore) setError('회원 목록을 불러오지 못했습니다.')
      })
      .finally(() => {
        if (!ignore) setIsLoading(false)
      })
    return () => {
      ignore = true
    }
  }, [])

  async function handleToggleStatus(user) {
    const nextStatus = user.status === 'SUSPENDED' ? 'ACTIVE' : 'SUSPENDED'
    const actionLabel = nextStatus === 'SUSPENDED' ? '정지' : '정지 해제'
    if (!window.confirm(`${user.nickname}님을 ${actionLabel} 처리할까요?`)) return

    setUpdatingId(user.id)
    try {
      await adminApi.updateUserStatus(user.id, nextStatus)
      setUsers((prev) => prev.map((u) => (u.id === user.id ? { ...u, status: nextStatus } : u)))
    } catch (err) {
      window.alert(err.response?.data?.message ?? '상태 변경에 실패했습니다.')
    } finally {
      setUpdatingId(null)
    }
  }

  const filtered = users.filter((u) => {
    if (roleFilter !== 'ALL' && u.role !== roleFilter) return false
    if (!keyword.trim()) return true
    const k = keyword.trim().toLowerCase()
    return u.nickname.toLowerCase().includes(k) || u.username.toLowerCase().includes(k)
  })

  if (isLoading) return <p className={styles.state}>불러오는 중...</p>
  if (error) return <p className={styles.state}>{error}</p>

  return (
    <>
      <div className={styles.filterRow}>
        <input
          className={styles.searchInput}
          placeholder="닉네임, 아이디로 검색"
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
        />
        <div className={styles.roleTabs}>
          {ROLE_FILTERS.map((role) => (
            <button
              key={role}
              type="button"
              className={`${styles.roleTab} ${roleFilter === role ? styles.roleTabActive : ''}`}
              onClick={() => setRoleFilter(role)}
            >
              {role === 'ALL' ? '전체' : role}
            </button>
          ))}
        </div>
      </div>

      <p className={styles.countLabel}>{filtered.length}명</p>

      {filtered.length === 0 ? (
        <p className={styles.emptyState}>조건에 맞는 회원이 없어요.</p>
      ) : (
        <div className={styles.tableWrap}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>회원</th>
                <th>권한</th>
                <th>구독</th>
                <th>가입일</th>
                <th>상태</th>
                <th>관리</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((u) => {
                const roleMeta = ROLE_META[u.role] ?? ROLE_META.USER
                const isWithdrawn = u.status === 'WITHDRAWN'
                return (
                  <tr key={u.id}>
                    <td>
                      <div className={styles.memberCell}>
                        <span className={styles.avatar} style={{ backgroundColor: getAvatarColor(u.nickname) }}>
                          {u.nickname?.[0]}
                        </span>
                        <div>
                          <p className={styles.memberName}>{u.nickname}</p>
                          <p className={styles.memberUsername}>@{u.username}</p>
                        </div>
                      </div>
                    </td>
                    <td>
                      <span
                        className={styles.roleBadge}
                        style={{ color: roleMeta.color, borderColor: roleMeta.color }}
                      >
                        {roleMeta.label}
                      </span>
                    </td>
                    <td>
                      {u.subscribed ? (
                        <Crown size={15} className={styles.subscribedIcon} />
                      ) : (
                        <span className={styles.dash}>—</span>
                      )}
                    </td>
                    <td className={styles.dateCell}>{formatDate(u.createdAt)}</td>
                    <td>
                      <span
                        className={`${styles.memberStatusBadge} ${
                          isWithdrawn ? styles.memberStatusMuted : u.status === 'SUSPENDED' ? styles.memberStatusSuspended : styles.memberStatusActive
                        }`}
                      >
                        {isWithdrawn ? '탈퇴' : u.status === 'SUSPENDED' ? '정지' : '정상'}
                      </span>
                    </td>
                    <td>
                      {!isWithdrawn && (
                        <button
                          type="button"
                          className={u.status === 'SUSPENDED' ? styles.unsuspendButton : styles.suspendButton}
                          disabled={updatingId === u.id}
                          onClick={() => handleToggleStatus(u)}
                        >
                          {u.status === 'SUSPENDED' ? (
                            <>
                              <UserCheck size={13} />
                              해제
                            </>
                          ) : (
                            <>
                              <Ban size={13} />
                              정지
                            </>
                          )}
                        </button>
                      )}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </>
  )
}

// ---------------- 전문가 심사 ----------------

function ExpertReviewTab({ onPendingCountChange }) {
  const [experts, setExperts] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [selectedId, setSelectedId] = useState(null)
  const [actingId, setActingId] = useState(null)

  const load = useCallback(() => {
    return expertApi.getExperts().then(({ data }) => {
      const list = data.data.experts
      setExperts(list)
      onPendingCountChange?.(list.filter((e) => e.status === 'PENDING').length)
    })
  }, [onPendingCountChange])

  useEffect(() => {
    let ignore = false
    setIsLoading(true)
    load().finally(() => {
      if (!ignore) setIsLoading(false)
    })
    return () => {
      ignore = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const pending = experts.filter((e) => e.status === 'PENDING')
  const done = experts.filter((e) => e.status !== 'PENDING')
  const selected = experts.find((e) => e.id === selectedId) ?? null

  async function handleApprove(expert) {
    if (!window.confirm(`신청자 #${expert.userId}를 전문가로 승인할까요?`)) return
    setActingId(expert.id)
    try {
      await expertApi.approveExpert(expert.id)
      await load()
    } catch (err) {
      window.alert(err.response?.data?.message ?? '승인에 실패했습니다.')
    } finally {
      setActingId(null)
    }
  }

  async function handleReject(expert) {
    const reason = window.prompt('반려 사유를 입력해주세요. (선택 사항, 취소하면 반려하지 않습니다)')
    if (reason === null) return
    setActingId(expert.id)
    try {
      await expertApi.rejectExpert(expert.id, reason || undefined)
      await load()
    } catch (err) {
      window.alert(err.response?.data?.message ?? '반려에 실패했습니다.')
    } finally {
      setActingId(null)
    }
  }

  if (isLoading) return <p className={styles.state}>불러오는 중...</p>

  return (
    <div className={selected ? styles.reviewLayoutSplit : styles.reviewLayout}>
      <div className={styles.reviewList}>
        <p className={styles.sectionTitle}>심사 대기 ({pending.length}건)</p>
        {pending.length === 0 ? (
          <p className={styles.emptyState}>대기 중인 신청이 없어요.</p>
        ) : (
          <div className={styles.list}>
            {pending.map((expert) => (
              <ExpertRow
                key={expert.id}
                expert={expert}
                active={expert.id === selectedId}
                onClick={() => setSelectedId(expert.id)}
                actions={
                  <>
                    <button
                      type="button"
                      className={styles.approveButton}
                      disabled={actingId === expert.id}
                      onClick={() => handleApprove(expert)}
                    >
                      <Check size={13} />
                      승인
                    </button>
                    <button
                      type="button"
                      className={styles.rejectButton}
                      disabled={actingId === expert.id}
                      onClick={() => handleReject(expert)}
                    >
                      <X size={13} />
                      반려
                    </button>
                  </>
                }
              />
            ))}
          </div>
        )}

        <p className={styles.sectionTitle} style={{ marginTop: 32 }}>
          처리 완료 ({done.length}건)
        </p>
        {done.length === 0 ? (
          <p className={styles.emptyState}>처리된 신청이 없어요.</p>
        ) : (
          <div className={styles.list}>
            {done.map((expert) => (
              <ExpertRow
                key={expert.id}
                expert={expert}
                active={expert.id === selectedId}
                onClick={() => setSelectedId(expert.id)}
                actions={<ExpertStatusBadge status={expert.status} />}
              />
            ))}
          </div>
        )}
      </div>

      {selected && (
        <div className={styles.detailPanel}>
          <div className={styles.detailHeader}>
            <p className={styles.sectionTitle}>신청서 상세</p>
            <button type="button" className={styles.iconButton} onClick={() => setSelectedId(null)} aria-label="닫기">
              <X size={16} />
            </button>
          </div>

          <div className={styles.detailProfile}>
            <span className={styles.avatar} style={{ backgroundColor: getAvatarColor(String(selected.userId)) }}>
              {selected.userId}
            </span>
            <div>
              <p className={styles.rowTitle}>신청자 #{selected.userId}</p>
              <ExpertStatusBadge status={selected.status} />
            </div>
          </div>

          {selected.careers.length > 0 && (
            <div className={styles.detailSection}>
              <p className={styles.detailSectionTitle}>
                <Briefcase size={13} />
                경력
              </p>
              {selected.careers.map((career) => (
                <div key={career.id} className={styles.detailCard}>
                  <div className={styles.detailCardTop}>
                    <span className={styles.detailCardName}>{career.companyName}</span>
                    <span className={styles.detailCardYears}>{career.years}년</span>
                  </div>
                  <p className={styles.detailCardSub}>{career.position}</p>
                  <span className={styles.detailTag}>{getJobFieldLabel(career.jobField)}</span>
                </div>
              ))}
            </div>
          )}

          {selected.certifications.length > 0 && (
            <div className={styles.detailSection}>
              <p className={styles.detailSectionTitle}>
                <Award size={13} />
                자격증
              </p>
              {selected.certifications.map((cert) => (
                <div key={cert.id} className={styles.detailCard}>
                  <div className={styles.detailCardTop}>
                    <span className={styles.detailCardName}>{cert.name}</span>
                    <span className={styles.detailCardYears}>{cert.acquiredYear}</span>
                  </div>
                  <p className={styles.detailCardSub}>{cert.issuer}</p>
                </div>
              ))}
            </div>
          )}

          <div className={styles.detailSection}>
            <p className={styles.detailSectionTitle}>
              <FileText size={13} />
              소개글
            </p>
            <p className={styles.introText}>{selected.introduction || '작성된 소개글이 없어요.'}</p>
          </div>

          {selected.status === 'REJECTED' && selected.rejectReason && (
            <div className={styles.detailSection}>
              <p className={styles.detailSectionTitle}>
                <AlertCircle size={13} />
                반려 사유
              </p>
              <p className={styles.introText}>{selected.rejectReason}</p>
            </div>
          )}

          {selected.status === 'PENDING' && (
            <div className={styles.detailActions}>
              <button
                type="button"
                className={styles.approveButtonLg}
                disabled={actingId === selected.id}
                onClick={() => handleApprove(selected)}
              >
                <Check size={15} />
                승인
              </button>
              <button
                type="button"
                className={styles.rejectButtonLg}
                disabled={actingId === selected.id}
                onClick={() => handleReject(selected)}
              >
                <X size={15} />
                반려
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

function ExpertRow({ expert, active, onClick, actions }) {
  return (
    <div
      className={`${styles.reviewRow} ${active ? styles.reviewRowActive : ''}`}
      onClick={onClick}
      role="button"
      tabIndex={0}
      onKeyDown={(event) => {
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault()
          onClick()
        }
      }}
    >
      <span className={styles.avatar} style={{ backgroundColor: getAvatarColor(String(expert.userId)) }}>
        {expert.userId}
      </span>
      <div className={styles.rowMain}>
        <p className={styles.rowTitle}>신청자 #{expert.userId}</p>
        <span className={styles.rowMeta}>
          {expert.careers[0]
            ? `${expert.careers[0].companyName} · ${expert.careers[0].position} · ${expert.careers[0].years}년차`
            : '경력 정보 없음'}
        </span>
      </div>
      <div className={styles.reviewRowActions} onClick={(event) => event.stopPropagation()}>
        {actions}
      </div>
    </div>
  )
}

function ExpertStatusBadge({ status }) {
  if (status === 'APPROVED') {
    return (
      <span className={`${styles.expertStatusBadge} ${styles.expertStatusApproved}`}>
        <CheckCircle2 size={12} />
        승인 완료
      </span>
    )
  }
  if (status === 'REJECTED') {
    return (
      <span className={`${styles.expertStatusBadge} ${styles.expertStatusRejected}`}>
        <XCircle size={12} />
        반려
      </span>
    )
  }
  return (
    <span className={`${styles.expertStatusBadge} ${styles.expertStatusPending}`}>
      <Clock size={12} />
      심사중
    </span>
  )
}
