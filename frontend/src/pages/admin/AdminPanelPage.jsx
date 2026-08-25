import {
  Activity,
  AlertCircle,
  AlertTriangle,
  Award,
  Ban,
  BookOpen,
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
  UserX,
  X,
  XCircle,
  Zap,
} from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { adminApi, expertApi, postApi, studyApi } from '../../api'
import Pagination from '../../components/common/Pagination'
import SiteHeader from '../../components/common/SiteHeader'
import { getJobFieldLabel } from '../../constants/jobField'
import { POST_CATEGORIES, getPostCategoryMeta } from '../../constants/postCategory'
import { REPORT_REASONS } from '../../constants/reportReason'
import { STUDY_CATEGORIES, getStudyCategoryMeta } from '../../constants/studyCategory'
import { getAvatarColor } from '../../utils/avatarColor'
import { formatDate } from '../../utils/formatDate'
import styles from './AdminPanelPage.module.css'

// 회원/전문가 심사 탭은 전체를 한 번에 불러와 화면에서 검색·필터링하므로, 페이지 이동도
// 서버가 아니라 이 크기 단위로 화면에서 직접 나눈다.
const ADMIN_PAGE_SIZE = 10

const TABS = [
  { key: 'dashboard', label: '대시보드', icon: LayoutGrid },
  { key: 'members', label: '회원 관리', icon: Users },
  { key: 'studies', label: '스터디 관리', icon: BookOpen },
  { key: 'posts', label: '게시글 관리', icon: FileText },
  { key: 'subscribers', label: '구독자 관리', icon: Crown },
  { key: 'experts', label: '전문가 심사', icon: Shield },
  { key: 'reports', label: '신고 관리', icon: AlertTriangle },
]

const REPORT_REASON_LABELS = Object.fromEntries(REPORT_REASONS.map((item) => [item.value, item.label]))

const REPORT_TARGET_TYPE_META = {
  POST: {
    label: '게시글',
    deleteAction: (id) => adminApi.deletePost(id),
    actionLabel: '삭제',
    actionIcon: Trash2,
    resolvedLabel: '삭제됨',
    confirmMessage: '이 게시글을 삭제 처리할까요? 콘텐츠가 실제로 삭제됩니다.',
  },
  COMMENT: {
    label: '댓글',
    deleteAction: (id) => adminApi.deleteComment(id),
    actionLabel: '삭제',
    actionIcon: Trash2,
    resolvedLabel: '삭제됨',
    confirmMessage: '이 댓글을 삭제 처리할까요? 콘텐츠가 실제로 삭제됩니다.',
  },
  STUDY_POST: {
    label: '스터디 게시글',
    deleteAction: (id) => adminApi.deleteStudyPost(id),
    actionLabel: '삭제',
    actionIcon: Trash2,
    resolvedLabel: '삭제됨',
    confirmMessage: '이 스터디 게시글을 삭제 처리할까요? 콘텐츠가 실제로 삭제됩니다.',
  },
  STUDY_POST_COMMENT: {
    label: '스터디 댓글',
    deleteAction: (id) => adminApi.deleteStudyPostComment(id),
    actionLabel: '삭제',
    actionIcon: Trash2,
    resolvedLabel: '삭제됨',
    confirmMessage: '이 스터디 댓글을 삭제 처리할까요? 콘텐츠가 실제로 삭제됩니다.',
  },
  FEEDBACK: {
    label: '전문가 상담',
    // 상담 메시지 자체는 삭제할 방법이 없어서, 신고 처리는 담당 전문가 자격 박탈로 대신한다.
    // 박탈 시 그 전문가가 진행 중인 다른 상담들도 함께 강제 종료된다(ExpertProfileService.revoke).
    deleteAction: (id) =>
      adminApi
        .getFeedback(id)
        .then(({ data }) => expertApi.revokeExpert(data.data.expertProfileId, '신고 접수로 인한 전문가 자격 박탈')),
    actionLabel: '전문가 박탈',
    actionIcon: UserX,
    resolvedLabel: '박탈됨',
    confirmMessage:
      '이 상담의 담당 전문가 자격을 박탈할까요? 박탈되면 해당 전문가가 진행 중인 다른 모든 상담도 함께 종료됩니다.',
  },
}

const ROLE_META = {
  USER: { label: 'USER', color: '#c6ff3d' },
  EXPERT: { label: 'EXPERT', color: '#60a5fa' },
  ADMIN: { label: 'ADMIN', color: '#fb923c' },
}

export default function AdminPanelPage() {
  const [activeTab, setActiveTab] = useState('dashboard')
  const [pendingCount, setPendingCount] = useState(0)
  const [reportPendingCount, setReportPendingCount] = useState(0)

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
            <p className={styles.subtitle}>prep2gether 운영 관리 콘솔</p>
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
              {tab.key === 'reports' && reportPendingCount > 0 && (
                <span className={styles.tabCount}>{reportPendingCount}</span>
              )}
            </button>
          ))}
        </nav>

        {activeTab === 'dashboard' && <DashboardTab onNavigate={setActiveTab} />}
        {activeTab === 'members' && <MembersTab />}
        {activeTab === 'studies' && <StudiesTab />}
        {activeTab === 'posts' && <PostsTab />}
        {activeTab === 'subscribers' && <SubscribersTab />}
        {activeTab === 'experts' && <ExpertReviewTab onPendingCountChange={setPendingCount} />}
        {activeTab === 'reports' && <ReportsTab onPendingCountChange={setReportPendingCount} />}
      </main>
    </>
  )
}

// ---------------- 대시보드 ----------------

function DashboardTab({ onNavigate }) {
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
      expertApi.getExperts(undefined, { size: 500 }),
      adminApi.getReports({ status: 'PENDING', size: 1 }),
    ])
      .then(([usersRes, allUsersRes, studyCountRes, studiesRes, postsRes, expertsRes, reportsRes]) => {
        if (ignore) return
        const experts = expertsRes.data.data
        setStats({
          totalUsers: usersRes.data.meta.pagination.totalItems,
          subscriberCount: allUsersRes.data.data.filter((u) => u.subscribed).length,
          totalStudies: studyCountRes.data.meta.pagination.totalItems,
          totalPosts: postsRes.data.data.totalElements,
          pendingExperts: experts.filter((e) => e.status === 'PENDING').length,
          pendingReports: reportsRes.data.meta.pagination.totalItems,
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
        <StatCard
          icon={<Users size={18} />}
          color="#60a5fa"
          value={stats.totalUsers}
          label="총 회원"
          onClick={() => onNavigate('members')}
        />
        <StatCard
          icon={<Award size={18} />}
          color="#8b5cf6"
          value={stats.totalStudies}
          label="전체 스터디"
          onClick={() => onNavigate('studies')}
        />
        <StatCard
          icon={<FileText size={18} />}
          color="#34d399"
          value={stats.totalPosts}
          label="전체 게시글"
          onClick={() => onNavigate('posts')}
        />
        <StatCard
          icon={<Crown size={18} />}
          color="#c6ff3d"
          value={stats.subscriberCount}
          label="구독자"
          onClick={() => onNavigate('subscribers')}
        />
        <StatCard
          icon={<ShieldAlert size={18} />}
          color="#fb923c"
          value={stats.pendingExperts}
          label="전문가 신청"
          onClick={() => onNavigate('experts')}
        />
        <StatCard
          icon={<AlertTriangle size={18} />}
          color="#f87171"
          value={stats.pendingReports}
          label="신고 접수"
          onClick={() => onNavigate('reports')}
        />
      </div>

      <div className={styles.dashboardGrid}>
        <section>
          <div className={styles.sectionHeader}>
            <p className={styles.sectionTitle}>최근 게시글</p>
            <button type="button" className={styles.sectionLink} onClick={() => onNavigate('posts')}>
              전체보기 →
            </button>
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
            <button type="button" className={styles.sectionLink} onClick={() => onNavigate('experts')}>
              심사하기 →
            </button>
          </div>
          {expertPreview.length === 0 ? (
            <p className={styles.emptyState}>신청 내역이 없어요.</p>
          ) : (
            <div className={styles.list}>
              {expertPreview.map((expert) => (
                <div key={expert.id} className={styles.row}>
                  <span className={styles.avatar} style={{ backgroundColor: getAvatarColor(expert.name) }}>
                    {expert.name?.[0]}
                  </span>
                  <div className={styles.rowMain}>
                    <p className={styles.rowTitle}>{expert.name}</p>
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
        <button type="button" className={styles.sectionLink} onClick={() => onNavigate('studies')}>
          전체보기 →
        </button>
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

function StatCard({ icon, color, value, label, onClick }) {
  return (
    <div
      className={styles.statCard}
      onClick={onClick}
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
      onKeyDown={
        onClick
          ? (event) => {
              if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault()
                onClick()
              }
            }
          : undefined
      }
    >
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
  const [page, setPage] = useState(0)

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

  // 검색어/필터가 바뀌면 항상 1페이지부터 다시 보여준다.
  useEffect(() => {
    setPage(0)
  }, [keyword, roleFilter])

  const totalPages = Math.ceil(filtered.length / ADMIN_PAGE_SIZE)
  const paged = filtered.slice(page * ADMIN_PAGE_SIZE, page * ADMIN_PAGE_SIZE + ADMIN_PAGE_SIZE)

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
              {paged.map((u) => {
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

      <Pagination page={page} totalPages={totalPages} onChange={setPage} />
    </>
  )
}

// ---------------- 스터디 관리 ----------------

const CATEGORY_FILTERS = ['ALL', ...STUDY_CATEGORIES.map((c) => c.value)]

function StudiesTab() {
  const [studies, setStudies] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [keyword, setKeyword] = useState('')
  const [categoryFilter, setCategoryFilter] = useState('ALL')
  const [deletingId, setDeletingId] = useState(null)
  const [page, setPage] = useState(0)

  const load = useCallback(() => {
    return adminApi.getStudies({ page: 0, size: 500 }).then(({ data }) => {
      setStudies(data.data)
    })
  }, [])

  useEffect(() => {
    let ignore = false
    load()
      .catch(() => {
        if (!ignore) setError('스터디 목록을 불러오지 못했습니다.')
      })
      .finally(() => {
        if (!ignore) setIsLoading(false)
      })
    return () => {
      ignore = true
    }
  }, [load])

  async function handleDelete(study) {
    if (!window.confirm(`"${study.title}" 스터디를 강제 삭제할까요? 연관된 게시글/댓글/멤버도 함께 숨김처리됩니다.`)) return

    setDeletingId(study.id)
    try {
      await adminApi.deleteStudy(study.id)
      setStudies((prev) => prev.filter((s) => s.id !== study.id))
    } catch (err) {
      window.alert(err.response?.data?.message ?? '스터디 삭제에 실패했습니다.')
    } finally {
      setDeletingId(null)
    }
  }

  const filtered = studies.filter((s) => {
    if (categoryFilter !== 'ALL' && s.category !== categoryFilter) return false
    if (!keyword.trim()) return true
    return s.title.toLowerCase().includes(keyword.trim().toLowerCase())
  })

  // 검색어/필터가 바뀌면 항상 1페이지부터 다시 보여준다.
  useEffect(() => {
    setPage(0)
  }, [keyword, categoryFilter])

  const totalPages = Math.ceil(filtered.length / ADMIN_PAGE_SIZE)
  const paged = filtered.slice(page * ADMIN_PAGE_SIZE, page * ADMIN_PAGE_SIZE + ADMIN_PAGE_SIZE)

  if (isLoading) return <p className={styles.state}>불러오는 중...</p>
  if (error) return <p className={styles.state}>{error}</p>

  return (
    <>
      <div className={styles.filterRow}>
        <input
          className={styles.searchInput}
          placeholder="제목으로 검색"
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
        />
        <div className={styles.roleTabs}>
          {CATEGORY_FILTERS.map((category) => (
            <button
              key={category}
              type="button"
              className={`${styles.roleTab} ${categoryFilter === category ? styles.roleTabActive : ''}`}
              onClick={() => setCategoryFilter(category)}
            >
              {category === 'ALL' ? '전체' : getStudyCategoryMeta(category)?.label}
            </button>
          ))}
        </div>
      </div>

      <p className={styles.countLabel}>{filtered.length}개</p>

      {filtered.length === 0 ? (
        <p className={styles.emptyState}>조건에 맞는 스터디가 없어요.</p>
      ) : (
        <div className={styles.tableWrap}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>스터디</th>
                <th>카테고리</th>
                <th>인원</th>
                <th>모집기간</th>
                <th>개설일</th>
                <th>관리</th>
              </tr>
            </thead>
            <tbody>
              {paged.map((study) => {
                const categoryMeta = getStudyCategoryMeta(study.category)
                return (
                  <tr key={study.id}>
                    <td>
                      <div className={styles.memberCell}>
                        <span className={styles.avatar} style={{ backgroundColor: getAvatarColor(study.title) }}>
                          {study.title?.[0]}
                        </span>
                        <div>
                          <p className={styles.memberName}>{study.title}</p>
                          <p className={styles.memberUsername}>{study.leaderNickname}</p>
                        </div>
                      </div>
                    </td>
                    <td>
                      <span
                        className={styles.roleBadge}
                        style={{ color: categoryMeta?.color, borderColor: categoryMeta?.color }}
                      >
                        {categoryMeta?.label ?? study.category}
                      </span>
                    </td>
                    <td className={styles.dateCell}>
                      {study.currentMemberCount}/{study.capacity}명
                    </td>
                    <td className={styles.dateCell}>
                      {study.recruitStart} ~ {study.recruitEnd ?? '상시'}
                    </td>
                    <td className={styles.dateCell}>{formatDate(study.createdAt)}</td>
                    <td>
                      <button
                        type="button"
                        className={styles.suspendButton}
                        disabled={deletingId === study.id}
                        onClick={() => handleDelete(study)}
                      >
                        <Trash2 size={13} />
                        삭제
                      </button>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}

      <Pagination page={page} totalPages={totalPages} onChange={setPage} />
    </>
  )
}

// ---------------- 게시글 관리 ----------------

const POST_CATEGORY_FILTERS = ['ALL', ...POST_CATEGORIES.map((c) => c.value)]

function PostsTab() {
  const [posts, setPosts] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [keyword, setKeyword] = useState('')
  const [categoryFilter, setCategoryFilter] = useState('ALL')
  const [deletingId, setDeletingId] = useState(null)
  const [page, setPage] = useState(0)

  const load = useCallback(() => {
    return adminApi.getPosts({ page: 0, size: 500 }).then(({ data }) => {
      setPosts(data.data)
    })
  }, [])

  useEffect(() => {
    let ignore = false
    load()
      .catch(() => {
        if (!ignore) setError('게시글 목록을 불러오지 못했습니다.')
      })
      .finally(() => {
        if (!ignore) setIsLoading(false)
      })
    return () => {
      ignore = true
    }
  }, [load])

  async function handleDelete(post) {
    if (!window.confirm(`"${post.title}" 게시글을 강제 삭제할까요? 연관된 댓글도 함께 숨김처리됩니다.`)) return

    setDeletingId(post.id)
    try {
      await adminApi.deletePost(post.id)
      setPosts((prev) => prev.filter((p) => p.id !== post.id))
    } catch (err) {
      window.alert(err.response?.data?.message ?? '게시글 삭제에 실패했습니다.')
    } finally {
      setDeletingId(null)
    }
  }

  const filtered = posts.filter((p) => {
    if (categoryFilter !== 'ALL' && p.category !== categoryFilter) return false
    if (!keyword.trim()) return true
    return p.title.toLowerCase().includes(keyword.trim().toLowerCase())
  })

  // 검색어/필터가 바뀌면 항상 1페이지부터 다시 보여준다.
  useEffect(() => {
    setPage(0)
  }, [keyword, categoryFilter])

  const totalPages = Math.ceil(filtered.length / ADMIN_PAGE_SIZE)
  const paged = filtered.slice(page * ADMIN_PAGE_SIZE, page * ADMIN_PAGE_SIZE + ADMIN_PAGE_SIZE)

  if (isLoading) return <p className={styles.state}>불러오는 중...</p>
  if (error) return <p className={styles.state}>{error}</p>

  return (
    <>
      <div className={styles.filterRow}>
        <input
          className={styles.searchInput}
          placeholder="제목으로 검색"
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
        />
        <div className={styles.roleTabs}>
          {POST_CATEGORY_FILTERS.map((category) => (
            <button
              key={category}
              type="button"
              className={`${styles.roleTab} ${categoryFilter === category ? styles.roleTabActive : ''}`}
              onClick={() => setCategoryFilter(category)}
            >
              {category === 'ALL' ? '전체' : getPostCategoryMeta(category)?.label}
            </button>
          ))}
        </div>
      </div>

      <p className={styles.countLabel}>{filtered.length}개</p>

      {filtered.length === 0 ? (
        <p className={styles.emptyState}>조건에 맞는 게시글이 없어요.</p>
      ) : (
        <div className={styles.tableWrap}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>게시글</th>
                <th>카테고리</th>
                <th>조회수</th>
                <th>작성일</th>
                <th>관리</th>
              </tr>
            </thead>
            <tbody>
              {paged.map((post) => {
                const categoryMeta = getPostCategoryMeta(post.category)
                return (
                  <tr key={post.id}>
                    <td>
                      <div className={styles.memberCell}>
                        <span className={styles.avatar} style={{ backgroundColor: getAvatarColor(post.title) }}>
                          {post.title?.[0]}
                        </span>
                        <div>
                          <p className={styles.memberName}>{post.title}</p>
                          <p className={styles.memberUsername}>{post.authorNickname}</p>
                        </div>
                      </div>
                    </td>
                    <td>
                      <span
                        className={styles.roleBadge}
                        style={{ color: categoryMeta?.color, borderColor: categoryMeta?.color }}
                      >
                        {categoryMeta?.label ?? post.category}
                      </span>
                    </td>
                    <td className={styles.dateCell}>
                      <Eye size={13} style={{ verticalAlign: 'text-bottom', marginRight: 4 }} />
                      {post.viewCount}
                    </td>
                    <td className={styles.dateCell}>{formatDate(post.createdAt)}</td>
                    <td>
                      <button
                        type="button"
                        className={styles.suspendButton}
                        disabled={deletingId === post.id}
                        onClick={() => handleDelete(post)}
                      >
                        <Trash2 size={13} />
                        삭제
                      </button>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}

      <Pagination page={page} totalPages={totalPages} onChange={setPage} />
    </>
  )
}

// ---------------- 구독자 관리 ----------------

const SUBSCRIBER_SUB_TABS = [
  { key: 'subscriptions', label: '구독 현황' },
  { key: 'payments', label: '결제 이력' },
]

const SUBSCRIPTION_STATUS_META = {
  ACTIVE: { label: '활성', color: '#34d399' },
  PAST_DUE: { label: '결제실패', color: '#f87171' },
  EXPIRED: { label: '만료', color: '#94a3b8' },
}

const PAYMENT_STATUS_META = {
  READY: { label: '준비', color: '#fbbf24' },
  PAID: { label: '완료', color: '#34d399' },
  FAILED: { label: '실패', color: '#f87171' },
}

function SubscribersTab() {
  const [subTab, setSubTab] = useState('subscriptions')

  return (
    <>
      <div className={styles.roleTabs} style={{ marginBottom: 16 }}>
        {SUBSCRIBER_SUB_TABS.map((tab) => (
          <button
            key={tab.key}
            type="button"
            className={`${styles.roleTab} ${subTab === tab.key ? styles.roleTabActive : ''}`}
            onClick={() => setSubTab(tab.key)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {subTab === 'subscriptions' ? <SubscriptionsSubTab /> : <PaymentsSubTab />}
    </>
  )
}

const SUBSCRIPTION_STATUS_FILTERS = ['ALL', 'ACTIVE', 'PAST_DUE', 'EXPIRED']

function SubscriptionsSubTab() {
  const [subscriptions, setSubscriptions] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [keyword, setKeyword] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [page, setPage] = useState(0)

  useEffect(() => {
    let ignore = false
    adminApi
      .getSubscriptions({ page: 0, size: 500 })
      .then(({ data }) => {
        if (!ignore) setSubscriptions(data.data)
      })
      .catch(() => {
        if (!ignore) setError('구독 목록을 불러오지 못했습니다.')
      })
      .finally(() => {
        if (!ignore) setIsLoading(false)
      })
    return () => {
      ignore = true
    }
  }, [])

  const filtered = subscriptions.filter((s) => {
    if (statusFilter !== 'ALL' && s.status !== statusFilter) return false
    if (!keyword.trim()) return true
    const k = keyword.trim().toLowerCase()
    return s.userNickname.toLowerCase().includes(k) || s.userUsername.toLowerCase().includes(k)
  })

  useEffect(() => {
    setPage(0)
  }, [keyword, statusFilter])

  const totalPages = Math.ceil(filtered.length / ADMIN_PAGE_SIZE)
  const paged = filtered.slice(page * ADMIN_PAGE_SIZE, page * ADMIN_PAGE_SIZE + ADMIN_PAGE_SIZE)

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
          {SUBSCRIPTION_STATUS_FILTERS.map((status) => (
            <button
              key={status}
              type="button"
              className={`${styles.roleTab} ${statusFilter === status ? styles.roleTabActive : ''}`}
              onClick={() => setStatusFilter(status)}
            >
              {status === 'ALL' ? '전체' : SUBSCRIPTION_STATUS_META[status]?.label}
            </button>
          ))}
        </div>
      </div>

      <p className={styles.countLabel}>{filtered.length}건</p>

      {filtered.length === 0 ? (
        <p className={styles.emptyState}>조건에 맞는 구독이 없어요.</p>
      ) : (
        <div className={styles.tableWrap}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>회원</th>
                <th>상태</th>
                <th>시작일</th>
                <th>만료일</th>
                <th>자동갱신</th>
                <th>결제 재시도</th>
              </tr>
            </thead>
            <tbody>
              {paged.map((s) => {
                const statusMeta = SUBSCRIPTION_STATUS_META[s.status]
                return (
                  <tr key={s.id}>
                    <td>
                      <div className={styles.memberCell}>
                        <span className={styles.avatar} style={{ backgroundColor: getAvatarColor(s.userNickname) }}>
                          {s.userNickname?.[0]}
                        </span>
                        <div>
                          <p className={styles.memberName}>{s.userNickname}</p>
                          <p className={styles.memberUsername}>@{s.userUsername}</p>
                        </div>
                      </div>
                    </td>
                    <td>
                      <span className={styles.roleBadge} style={{ color: statusMeta?.color, borderColor: statusMeta?.color }}>
                        {statusMeta?.label ?? s.status}
                      </span>
                    </td>
                    <td className={styles.dateCell}>{formatDate(s.startedAt)}</td>
                    <td className={styles.dateCell}>{s.expiredAt ? formatDate(s.expiredAt) : '—'}</td>
                    <td>{s.autoRenew ? <Check size={15} /> : <span className={styles.dash}>—</span>}</td>
                    <td className={styles.dateCell}>{s.retryCount}/3</td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}

      <Pagination page={page} totalPages={totalPages} onChange={setPage} />
    </>
  )
}

const PAYMENT_STATUS_FILTERS = ['ALL', 'READY', 'PAID', 'FAILED']

function PaymentsSubTab() {
  const [payments, setPayments] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [keyword, setKeyword] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [page, setPage] = useState(0)

  useEffect(() => {
    let ignore = false
    adminApi
      .getPayments({ page: 0, size: 500 })
      .then(({ data }) => {
        if (!ignore) setPayments(data.data)
      })
      .catch(() => {
        if (!ignore) setError('결제 이력을 불러오지 못했습니다.')
      })
      .finally(() => {
        if (!ignore) setIsLoading(false)
      })
    return () => {
      ignore = true
    }
  }, [])

  const filtered = payments.filter((p) => {
    if (statusFilter !== 'ALL' && p.status !== statusFilter) return false
    if (!keyword.trim()) return true
    const k = keyword.trim().toLowerCase()
    return p.userNickname.toLowerCase().includes(k) || p.userUsername.toLowerCase().includes(k)
  })

  useEffect(() => {
    setPage(0)
  }, [keyword, statusFilter])

  const totalPages = Math.ceil(filtered.length / ADMIN_PAGE_SIZE)
  const paged = filtered.slice(page * ADMIN_PAGE_SIZE, page * ADMIN_PAGE_SIZE + ADMIN_PAGE_SIZE)

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
          {PAYMENT_STATUS_FILTERS.map((status) => (
            <button
              key={status}
              type="button"
              className={`${styles.roleTab} ${statusFilter === status ? styles.roleTabActive : ''}`}
              onClick={() => setStatusFilter(status)}
            >
              {status === 'ALL' ? '전체' : PAYMENT_STATUS_META[status]?.label}
            </button>
          ))}
        </div>
      </div>

      <p className={styles.countLabel}>{filtered.length}건</p>

      {filtered.length === 0 ? (
        <p className={styles.emptyState}>조건에 맞는 결제 내역이 없어요.</p>
      ) : (
        <div className={styles.tableWrap}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>회원</th>
                <th>주문명</th>
                <th>금액</th>
                <th>상태</th>
                <th>실패 사유</th>
                <th>결제일</th>
              </tr>
            </thead>
            <tbody>
              {paged.map((p) => {
                const statusMeta = PAYMENT_STATUS_META[p.status]
                return (
                  <tr key={p.id}>
                    <td>
                      <div className={styles.memberCell}>
                        <span className={styles.avatar} style={{ backgroundColor: getAvatarColor(p.userNickname) }}>
                          {p.userNickname?.[0]}
                        </span>
                        <div>
                          <p className={styles.memberName}>{p.userNickname}</p>
                          <p className={styles.memberUsername}>@{p.userUsername}</p>
                        </div>
                      </div>
                    </td>
                    <td>{p.orderName}</td>
                    <td className={styles.dateCell}>{p.amount.toLocaleString()}{p.currency === 'KRW' ? '원' : ` ${p.currency}`}</td>
                    <td>
                      <span className={styles.roleBadge} style={{ color: statusMeta?.color, borderColor: statusMeta?.color }}>
                        {statusMeta?.label ?? p.status}
                      </span>
                    </td>
                    <td className={styles.dateCell}>{p.failReason ?? <span className={styles.dash}>—</span>}</td>
                    <td className={styles.dateCell}>{p.paidAt ? formatDate(p.paidAt) : formatDate(p.createdAt)}</td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}

      <Pagination page={page} totalPages={totalPages} onChange={setPage} />
    </>
  )
}

// ---------------- 전문가 심사 ----------------

function ExpertReviewTab({ onPendingCountChange }) {
  const [experts, setExperts] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [selectedId, setSelectedId] = useState(null)
  const [actingId, setActingId] = useState(null)
  const [pendingPage, setPendingPage] = useState(0)
  const [donePage, setDonePage] = useState(0)

  const load = useCallback(() => {
    // 다른 관리자 탭(회원/신고 관리)과 동일하게, 서버 페이지네이션 없이 대량으로 한 번에 불러와
    // "심사 대기"/"처리 완료" 두 섹션으로 나눈 뒤, 각각 화면에서 직접 페이지를 나눠 보여준다.
    return expertApi.getExperts(undefined, { size: 500 }).then(({ data }) => {
      const list = data.data
      setExperts(list)
      setPendingPage(0)
      setDonePage(0)
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

  const pendingTotalPages = Math.ceil(pending.length / ADMIN_PAGE_SIZE)
  const pagedPending = pending.slice(pendingPage * ADMIN_PAGE_SIZE, pendingPage * ADMIN_PAGE_SIZE + ADMIN_PAGE_SIZE)
  const doneTotalPages = Math.ceil(done.length / ADMIN_PAGE_SIZE)
  const pagedDone = done.slice(donePage * ADMIN_PAGE_SIZE, donePage * ADMIN_PAGE_SIZE + ADMIN_PAGE_SIZE)

  async function handleApprove(expert) {
    if (!window.confirm(`${expert.name}님을 전문가로 승인할까요?`)) return
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
            {pagedPending.map((expert) => (
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
        <Pagination page={pendingPage} totalPages={pendingTotalPages} onChange={setPendingPage} />

        <p className={styles.sectionTitle} style={{ marginTop: 32 }}>
          처리 완료 ({done.length}건)
        </p>
        {done.length === 0 ? (
          <p className={styles.emptyState}>처리된 신청이 없어요.</p>
        ) : (
          <div className={styles.list}>
            {pagedDone.map((expert) => (
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
        <Pagination page={donePage} totalPages={doneTotalPages} onChange={setDonePage} />
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
            <span className={styles.avatar} style={{ backgroundColor: getAvatarColor(selected.name) }}>
              {selected.name?.[0]}
            </span>
            <div>
              <p className={styles.rowTitle}>{selected.name}</p>
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
      <span className={styles.avatar} style={{ backgroundColor: getAvatarColor(expert.name) }}>
        {expert.name?.[0]}
      </span>
      <div className={styles.rowMain}>
        <p className={styles.rowTitle}>{expert.name}</p>
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

// ---------------- 신고 관리 ----------------

const REPORT_FILTERS = ['ALL', 'PENDING', 'DONE']

// 신고는 신고자 수만큼 행이 따로 쌓이므로(같은 글을 여러 명이 신고 가능),
// targetType+targetId 기준으로 묶어서 "콘텐츠 1개당 카드 1개"로 보여준다.
function groupReportsByTarget(reports) {
  const groups = new Map()
  for (const report of reports) {
    const key = `${report.targetType}-${report.targetId}`
    if (!groups.has(key)) {
      groups.set(key, {
        key,
        targetType: report.targetType,
        targetId: report.targetId,
        targetTitle: report.targetTitle,
        targetContentPreview: report.targetContentPreview,
        targetAuthorNickname: report.targetAuthorNickname,
        reports: [],
      })
    }
    groups.get(key).reports.push(report)
  }

  return [...groups.values()].map((group) => {
    const latest = group.reports.reduce((a, b) => (a.createdAt > b.createdAt ? a : b))
    const hasPending = group.reports.some((r) => r.status === 'PENDING')
    // 그룹 안 신고들이 전부 같은 상태로 처리되는 게 정상 흐름이라, 처리됐다면 첫 건 상태를 대표값으로 쓴다.
    const status = hasPending ? 'PENDING' : group.reports[0].status
    return { ...group, status, latestCreatedAt: latest.createdAt }
  })
}

function ReportsTab({ onPendingCountChange }) {
  const [reports, setReports] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [filter, setFilter] = useState('ALL')
  const [actingKey, setActingKey] = useState(null)

  const load = useCallback(() => {
    return adminApi.getReports({ size: 500 }).then(({ data }) => {
      setReports(data.data)
      onPendingCountChange?.(
        groupReportsByTarget(data.data).filter((g) => g.status === 'PENDING').length,
      )
    })
  }, [onPendingCountChange])

  useEffect(() => {
    let ignore = false
    setIsLoading(true)
    load()
      .catch(() => {
        if (!ignore) setError('신고 목록을 불러오지 못했습니다.')
      })
      .finally(() => {
        if (!ignore) setIsLoading(false)
      })
    return () => {
      ignore = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const groups = groupReportsByTarget(reports).sort((a, b) => (a.latestCreatedAt < b.latestCreatedAt ? 1 : -1))
  const pending = groups.filter((g) => g.status === 'PENDING')
  const done = groups.filter((g) => g.status !== 'PENDING')
  const visible = filter === 'PENDING' ? pending : filter === 'DONE' ? done : groups

  async function handleDelete(group) {
    const meta = REPORT_TARGET_TYPE_META[group.targetType]
    if (!meta.deleteAction) return
    if (!window.confirm(meta.confirmMessage ?? `이 ${meta.label}을(를) 삭제 처리할까요? 콘텐츠가 실제로 삭제됩니다.`)) return

    setActingKey(group.key)
    try {
      await meta.deleteAction(group.targetId)
      await Promise.all(
        group.reports.filter((r) => r.status === 'PENDING').map((r) => adminApi.resolveReport(r.id)),
      )
      await load()
    } catch (err) {
      window.alert(err.response?.data?.message ?? '처리에 실패했습니다.')
    } finally {
      setActingKey(null)
    }
  }

  async function handleReject(group) {
    if (!window.confirm('신고를 반려할까요? 콘텐츠는 그대로 유지됩니다.')) return

    setActingKey(group.key)
    try {
      await Promise.all(
        group.reports.filter((r) => r.status === 'PENDING').map((r) => adminApi.rejectReport(r.id)),
      )
      await load()
    } catch (err) {
      window.alert(err.response?.data?.message ?? '반려 처리에 실패했습니다.')
    } finally {
      setActingKey(null)
    }
  }

  if (isLoading) return <p className={styles.state}>불러오는 중...</p>
  if (error) return <p className={styles.state}>{error}</p>

  return (
    <>
      <div className={styles.statsGrid} style={{ gridTemplateColumns: 'repeat(3, minmax(0, 1fr))' }}>
        <StatCard icon={<Clock size={18} />} color="#fbbf24" value={pending.length} label="대기중" />
        <StatCard icon={<CheckCircle2 size={18} />} color="#34d399" value={done.length} label="처리완료" />
        <StatCard icon={<AlertTriangle size={18} />} color="#f87171" value={groups.length} label="전체 접수" />
      </div>

      <div className={styles.roleTabs} style={{ marginBottom: 16 }}>
        {REPORT_FILTERS.map((key) => (
          <button
            key={key}
            type="button"
            className={`${styles.roleTab} ${filter === key ? styles.roleTabActive : ''}`}
            onClick={() => setFilter(key)}
          >
            {key === 'ALL' ? '전체' : key === 'PENDING' ? `대기중 (${pending.length})` : '처리완료'}
          </button>
        ))}
      </div>

      {visible.length === 0 ? (
        <p className={styles.emptyState}>해당하는 신고가 없어요.</p>
      ) : (
        <div className={styles.list}>
          {visible.map((group) => (
            <ReportCard
              key={group.key}
              group={group}
              isActing={actingKey === group.key}
              onDelete={() => handleDelete(group)}
              onReject={() => handleReject(group)}
            />
          ))}
        </div>
      )}
    </>
  )
}

function ReportCard({ group, isActing, onDelete, onReject }) {
  const meta = REPORT_TARGET_TYPE_META[group.targetType]

  return (
    <div className={styles.reportCard}>
      <div className={styles.reportCardHead}>
        <div className={styles.reportCardBadges}>
          <span className={styles.reportTypeBadge}>{meta.label}</span>
          <ReportStatusBadge status={group.status} resolvedLabel={meta.resolvedLabel} ResolvedIcon={meta.actionIcon} />
        </div>
        {group.status === 'PENDING' && (
          <div className={styles.reviewRowActions}>
            {meta.deleteAction && (
              <button type="button" className={styles.rejectButton} disabled={isActing} onClick={onDelete}>
                <meta.actionIcon size={13} />
                {meta.actionLabel}
              </button>
            )}
            <button type="button" className={styles.approveButton} disabled={isActing} onClick={onReject}>
              <X size={13} />
              반려
            </button>
          </div>
        )}
      </div>

      <p className={styles.reportCardMeta}>
        작성자: {group.targetAuthorNickname ?? '알 수 없음'} · {formatDate(group.latestCreatedAt)}
      </p>
      {group.targetTitle && <p className={styles.reportCardTitle}>{group.targetTitle}</p>}
      {group.targetContentPreview && <p className={styles.reportCardPreview}>{group.targetContentPreview}</p>}

      <div className={styles.reportReasonBox}>
        <p className={styles.reportReasonHeading}>신고자 ({group.reports.length}명)</p>
        {group.reports.map((r) => (
          <p key={r.id} className={styles.reportReasonRow}>
            <span className={styles.reportReasonNickname}>{r.reporterNickname}</span>
            {' · '}
            {REPORT_REASON_LABELS[r.reason] ?? r.reason}
            {r.detail && ` — ${r.detail}`}
          </p>
        ))}
      </div>
    </div>
  )
}

function ReportStatusBadge({ status, resolvedLabel = '삭제됨', ResolvedIcon = Trash2 }) {
  if (status === 'DELETED') {
    return (
      <span className={`${styles.expertStatusBadge} ${styles.expertStatusRejected}`}>
        <ResolvedIcon size={12} />
        {resolvedLabel}
      </span>
    )
  }
  if (status === 'REJECTED') {
    return (
      <span className={styles.expertStatusBadge} style={{ backgroundColor: 'rgba(148, 163, 184, 0.14)', color: '#94a3b8' }}>
        <X size={12} />
        반려됨
      </span>
    )
  }
  return (
    <span className={`${styles.expertStatusBadge} ${styles.expertStatusPending}`}>
      <Clock size={12} />
      대기중
    </span>
  )
}
