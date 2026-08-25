import {
  ChevronRight,
  CreditCard,
  Crown,
  Eye,
  LayoutGrid,
  Lock,
  MessageCircle,
  MessageSquarePlus,
  Pencil,
  Settings,
  Sparkles,
  Star,
  Trash2,
  User as UserIcon,
  Users,
} from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { billingKeyApi, feedbackApi, postApi, studyApi, subscriptionApi, userApi } from '../api'
import Pagination from '../components/common/Pagination'
import SiteHeader from '../components/common/SiteHeader'
import { getPostCategoryMeta } from '../constants/postCategory'
import { useAuth } from '../context/AuthContext'
import { getAvatarColor } from '../utils/avatarColor'
import { formatDate, formatDateTime } from '../utils/formatDate'
import { registerCard } from '../components/subscription/registerCard'
import { formatCardTail } from '../utils/formatCard'
import styles from './MyPage.module.css'

const ROLE_META = {
  USER: { label: 'USER', color: '#c6ff3d' },
  EXPERT: { label: '전문가', color: '#60a5fa' },
  ADMIN: { label: '관리자', color: '#fb923c' },
}

const SUBSCRIPTION_META = {
  true: { label: '구독중', className: 'subscriptionBadgePremium', Icon: Crown },
  false: { label: '무료회원', className: 'subscriptionBadgeFree', Icon: Sparkles },
}

const TABS = [
  { key: 'activity', label: '내 활동', icon: UserIcon },
  { key: 'posts', label: '게시글', icon: LayoutGrid },
  { key: 'studies', label: '스터디', icon: Users },
  { key: 'consults', label: '전문가 상담', icon: MessageCircle },
  { key: 'settings', label: '설정', icon: Settings },
]

export default function MyPage() {
  const { user, refetchMe, logout } = useAuth()
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState('activity')

  // 프로필 카드의 "게시글 N개"와 [내 활동] 탭의 최근 글 3건은 [게시글] 탭 페이징과 무관하게
  // 항상 최신 상태를 보여줘야 해서 별도로 관리한다. postsVersion을 올리면 다시 불러온다.
  const [recentPosts, setRecentPosts] = useState([])
  const [postCount, setPostCount] = useState(0)
  const [postsVersion, setPostsVersion] = useState(0)

  useEffect(() => {
    let ignore = false
    postApi.getMyPosts({ page: 0, size: 3 }).then(({ data }) => {
      if (ignore) return
      const pageData = data.data
      setRecentPosts(pageData.content)
      setPostCount(pageData.totalElements)
    })
    return () => {
      ignore = true
    }
  }, [postsVersion])

  // 프로필 카드의 "스터디 N개"와 [내 활동] 탭의 최근 스터디 3건.
  const [recentStudies, setRecentStudies] = useState([])
  const [studyCount, setStudyCount] = useState(0)

  useEffect(() => {
    let ignore = false
    studyApi.getMyStudies({ page: 0, size: 3 }).then(({ data }) => {
      if (ignore) return
      setRecentStudies(data.data)
      setStudyCount(data.meta.pagination.totalItems)
    })
    return () => {
      ignore = true
    }
  }, [])

  // 프로필 카드의 "전문가 상담 N개". 목록 자체는 [전문가 상담] 탭이 따로 불러오므로 개수만 필요.
  const [consultCount, setConsultCount] = useState(0)

  useEffect(() => {
    let ignore = false
    feedbackApi.getMyFeedbacks({ page: 0, size: 1 }).then(({ data }) => {
      if (!ignore) setConsultCount(data.data.totalElements)
    })
    return () => {
      ignore = true
    }
  }, [])

  if (!user) return null

  const roleMeta = ROLE_META[user.role] ?? ROLE_META.USER
  const subscriptionMeta = SUBSCRIPTION_META[String(user.subscribed)]

  return (
    <>
      <SiteHeader />
      <main className={styles.main}>
        <section className={styles.profileCard}>
          <span className={styles.avatar} style={{ backgroundColor: getAvatarColor(user.nickname) }}>
            {user.nickname?.[0]}
          </span>
          <div className={styles.profileInfo}>
            <div className={styles.nameRow}>
              <h1 className={styles.nickname}>{user.nickname}</h1>
              <span className={styles.roleBadge} style={{ color: roleMeta.color, borderColor: roleMeta.color }}>
                {roleMeta.label}
              </span>
              <span className={`${styles.subscriptionBadge} ${styles[subscriptionMeta.className]}`}>
                <subscriptionMeta.Icon size={12} />
                {subscriptionMeta.label}
              </span>
            </div>
            <p className={styles.realName}>{user.name}</p>
            <p className={styles.username}>{user.username}</p>
          </div>
          <div className={styles.statsRow}>
            <div className={styles.postCountPill}>
              <strong>{postCount}</strong>
              게시글
            </div>
            <div className={styles.postCountPill}>
              <strong>{studyCount}</strong>
              스터디
            </div>
            <div className={styles.postCountPill}>
              <strong>{consultCount}</strong>
              전문가 상담
            </div>
          </div>
        </section>

        <nav className={styles.tabs}>
          {TABS.map((tab) => (
            <button
              key={tab.key}
              type="button"
              className={`${styles.tabButton} ${activeTab === tab.key ? styles.tabActive : ''}`}
              onClick={() => setActiveTab(tab.key)}
            >
              <tab.icon size={16} />
              {tab.label}
            </button>
          ))}
        </nav>

        {activeTab === 'activity' && (
          <ActivityTab
            recentPosts={recentPosts}
            onSeeAllPosts={() => setActiveTab('posts')}
            recentStudies={recentStudies}
            onSeeAllStudies={() => setActiveTab('studies')}
          />
        )}

        {activeTab === 'posts' && (
          <PostsTab onPostsChanged={() => setPostsVersion((prev) => prev + 1)} />
        )}

        {activeTab === 'studies' && <StudiesTab />}

        {activeTab === 'consults' && <ConsultsTab user={user} />}

        {activeTab === 'settings' && (
          <SettingsTab
            user={user}
            onNicknameChanged={refetchMe}
            onLogout={async () => {
              await logout()
              navigate('/')
            }}
          />
        )}
      </main>
    </>
  )
}

function ActivityTab({ recentPosts, onSeeAllPosts, recentStudies, onSeeAllStudies }) {
  return (
    <section className={styles.section}>
      <div className={styles.sectionHeadingRow}>
        <h2 className={styles.sectionHeading}>최근 게시글</h2>
        <button type="button" className={styles.seeAllButton} onClick={onSeeAllPosts}>
          전체 보기
          <ChevronRight size={14} />
        </button>
      </div>

      {recentPosts.length === 0 ? (
        <p className={styles.emptyState}>아직 작성한 게시글이 없어요.</p>
      ) : (
        <div className={styles.list}>
          {recentPosts.map((post) => (
            <Link key={post.id} to={`/posts/${post.id}`} className={styles.activityRow}>
              <span className={styles.activityTitle}>{post.title}</span>
              <span className={styles.activityMeta}>
                <span>{formatDateTime(post.createdAt)}</span>
                <span className={styles.dot}>·</span>
                <Eye size={13} />
                <span>{post.viewCount}</span>
              </span>
            </Link>
          ))}
        </div>
      )}

      <div className={styles.sectionHeadingRow}>
        <h2 className={styles.sectionHeading}>최근 스터디</h2>
        <button type="button" className={styles.seeAllButton} onClick={onSeeAllStudies}>
          전체 보기
          <ChevronRight size={14} />
        </button>
      </div>

      {recentStudies.length === 0 ? (
        <p className={styles.emptyState}>아직 가입한 스터디가 없어요.</p>
      ) : (
        <div className={styles.list}>
          {recentStudies.map((study) => (
            <Link key={study.id} to={`/studies/${study.id}`} className={styles.activityRow}>
              <span className={styles.activityTitle}>{study.title}</span>
              <span className={styles.activityMeta}>
                <span>{study.categoryLabel}</span>
                <span className={styles.dot}>·</span>
                <Users size={13} />
                <span>정원 {study.capacity}명</span>
              </span>
            </Link>
          ))}
        </div>
      )}
    </section>
  )
}

function PostsTab({ onPostsChanged }) {
  const navigate = useNavigate()
  const [page, setPage] = useState(0)
  const [posts, setPosts] = useState([])
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let ignore = false
    setIsLoading(true)
    postApi
      .getMyPosts({ page })
      .then(({ data }) => {
        if (ignore) return
        const pageData = data.data
        setPosts(pageData.content)
        setTotalPages(pageData.totalPages)
        setTotalElements(pageData.totalElements)
      })
      .catch(() => {
        if (!ignore) setError('게시글 목록을 불러오지 못했습니다.')
      })
      .finally(() => {
        if (!ignore) setIsLoading(false)
      })
    return () => {
      ignore = true
    }
  }, [page])

  async function handleDelete(post) {
    if (!window.confirm(`"${post.title}" 게시글을 삭제할까요?`)) return
    try {
      await postApi.deletePost(post.id)
      // 마지막 페이지의 마지막 글을 지운 경우를 대비해 한 페이지 앞으로 당겨준다.
      setPosts((prev) => prev.filter((item) => item.id !== post.id))
      setTotalElements((prev) => Math.max(0, prev - 1))
      setPage((prev) => (posts.length === 1 && prev > 0 ? prev - 1 : prev))
      onPostsChanged()
    } catch (err) {
      window.alert(err.response?.data?.message ?? '게시글 삭제에 실패했습니다.')
    }
  }

  return (
    <section className={styles.section}>
      <div className={styles.sectionHeadingRow}>
        <h2 className={styles.sectionHeading}>총 {totalElements}개의 게시글</h2>
        <Link to="/posts/new" className={styles.newPostButton}>
          + 새 글쓰기
        </Link>
      </div>

      {isLoading && <p className={styles.emptyState}>불러오는 중...</p>}
      {!isLoading && error && <p className={styles.emptyState}>{error}</p>}
      {!isLoading && !error && posts.length === 0 && <p className={styles.emptyState}>아직 작성한 게시글이 없어요.</p>}

      {!isLoading && !error && posts.length > 0 && (
        <div className={styles.list}>
          {posts.map((post) => {
            const categoryMeta = getPostCategoryMeta(post.category)
            return (
              <div key={post.id} className={styles.postRow}>
                <Link to={`/posts/${post.id}`} className={styles.postRowMain}>
                  {categoryMeta && (
                    <span
                      className={styles.badge}
                      style={{ backgroundColor: categoryMeta.bg, color: categoryMeta.color }}
                    >
                      {post.categoryLabel}
                    </span>
                  )}
                  <p className={styles.postRowTitle}>{post.title}</p>
                  <span className={styles.activityMeta}>
                    <span>{formatDateTime(post.createdAt)}</span>
                    <span className={styles.dot}>·</span>
                    <Eye size={13} />
                    <span>{post.viewCount}</span>
                  </span>
                </Link>
                <div className={styles.postRowActions}>
                  <button
                    type="button"
                    className={styles.iconButton}
                    onClick={() => navigate(`/posts/${post.id}/edit`)}
                    aria-label="게시글 수정"
                  >
                    <Pencil size={15} />
                  </button>
                  <button
                    type="button"
                    className={styles.iconButton}
                    onClick={() => handleDelete(post)}
                    aria-label="게시글 삭제"
                  >
                    <Trash2 size={15} />
                  </button>
                </div>
              </div>
            )
          })}
        </div>
      )}

      <Pagination page={page} totalPages={totalPages} onChange={setPage} />
    </section>
  )
}

function StudiesTab() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [studies, setStudies] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  // 카테고리 필터 없이 내가 가입한 스터디 전체를 받아온 뒤, 방장 여부로 프론트에서 두 섹션으로 나눈다.
  useEffect(() => {
    let ignore = false
    setIsLoading(true)
    studyApi
      .getMyStudies({ size: 100 })
      .then(({ data }) => {
        if (ignore) return
        setStudies(data.data)
        setError('')
      })
      .catch(() => {
        if (!ignore) setError('스터디 목록을 불러오지 못했습니다.')
      })
      .finally(() => {
        if (!ignore) setIsLoading(false)
      })
    return () => {
      ignore = true
    }
  }, [])

  async function handleLeave(study) {
    if (!window.confirm(`"${study.title}" 스터디를 탈퇴할까요?`)) return
    try {
      await studyApi.leaveStudy(study.id)
      setStudies((prev) => prev.filter((item) => item.id !== study.id))
    } catch (err) {
      window.alert(err.response?.data?.message ?? '탈퇴에 실패했습니다.')
    }
  }

  if (isLoading) {
    return (
      <section className={styles.section}>
        <p className={styles.emptyState}>불러오는 중...</p>
      </section>
    )
  }

  if (error) {
    return (
      <section className={styles.section}>
        <p className={styles.emptyState}>{error}</p>
      </section>
    )
  }

  const ledStudies = studies.filter((study) => study.leaderId === user.id)
  const joinedStudies = studies.filter((study) => study.leaderId !== user.id)

  return (
    <section className={styles.section}>
      <div className={styles.sectionHeadingRow}>
        <h2 className={styles.sectionHeading}>내가 개설한 스터디</h2>
      </div>

      {ledStudies.length === 0 ? (
        <p className={styles.emptyState}>아직 개설한 스터디가 없어요.</p>
      ) : (
        <div className={styles.list}>
          {ledStudies.map((study) => (
            <div key={study.id} className={styles.studyRow}>
              <span className={styles.studyAvatar} style={{ backgroundColor: getAvatarColor(study.title) }}>
                {study.title?.[0]}
              </span>
              <Link to={`/studies/${study.id}`} className={styles.studyRowMain}>
                <p className={styles.studyRowTitle}>
                  {study.title}
                  <span className={styles.leaderBadge}>
                    <Crown size={11} />
                    방장
                  </span>
                </p>
                <span className={styles.activityMeta}>
                  <span>{study.categoryLabel}</span>
                  <span className={styles.dot}>·</span>
                  <Users size={13} />
                  <span>정원 {study.capacity}명</span>
                </span>
              </Link>
              <button
                type="button"
                className={styles.rowActionButton}
                onClick={() => navigate(`/studies/${study.id}/edit`)}
              >
                관리
              </button>
            </div>
          ))}
        </div>
      )}

      <div className={styles.sectionHeadingRow}>
        <h2 className={styles.sectionHeading}>참여 중인 스터디</h2>
        <Link to="/studies" className={styles.seeAllButton}>
          스터디 찾기
          <ChevronRight size={14} />
        </Link>
      </div>

      {joinedStudies.length === 0 ? (
        <p className={styles.emptyState}>아직 참여 중인 스터디가 없어요.</p>
      ) : (
        <div className={styles.list}>
          {joinedStudies.map((study) => (
            <div key={study.id} className={styles.studyRow}>
              <span className={styles.studyAvatar} style={{ backgroundColor: getAvatarColor(study.title) }}>
                {study.title?.[0]}
              </span>
              <Link to={`/studies/${study.id}`} className={styles.studyRowMain}>
                <p className={styles.studyRowTitle}>{study.title}</p>
                <span className={styles.activityMeta}>
                  <span>{study.categoryLabel}</span>
                  <span className={styles.dot}>·</span>
                  <Users size={13} />
                  <span>정원 {study.capacity}명</span>
                </span>
              </Link>
              <button type="button" className={styles.rowActionButton} onClick={() => handleLeave(study)}>
                탈퇴
              </button>
            </div>
          ))}
        </div>
      )}
    </section>
  )
}

function ConsultStatusBadge({ status }) {
  if (status === 'ANSWERED') {
    return <span className={`${styles.consultStatusBadge} ${styles.consultStatusAnswered}`}>답변 완료</span>
  }
  return <span className={`${styles.consultStatusBadge} ${styles.consultStatusPending}`}>답변 대기</span>
}

function ConsultsTab({ user }) {
  const navigate = useNavigate()
  const [feedbacks, setFeedbacks] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!user.subscribed) return
    let ignore = false
    feedbackApi
      .getMyFeedbacks()
      .then(({ data }) => {
        if (!ignore) setFeedbacks(data.data.feedbacks)
      })
      .catch(() => {
        if (!ignore) setError('상담 목록을 불러오지 못했습니다.')
      })
    return () => {
      ignore = true
    }
  }, [user.subscribed])

  if (!user.subscribed) {
    return (
      <section className={styles.section}>
        <div className={styles.consultLocked}>
          <Lock size={20} />
          <p className={styles.consultLockedTitle}>구독 후 전문가에게 1:1 상담을 받아보세요.</p>
          <Link to="/subscription" className={styles.primaryButton}>
            <Crown size={14} />
            구독하고 시작하기
          </Link>
        </div>
      </section>
    )
  }

  return (
    <section className={styles.section}>
      <div className={styles.sectionHeadingRow}>
        <h2 className={styles.sectionHeading}>총 {feedbacks?.length ?? 0}개의 상담</h2>
        <Link to="/experts" className={styles.newPostButton}>
          <MessageSquarePlus size={13} style={{ marginRight: 4 }} />
          새 상담 시작
        </Link>
      </div>

      {feedbacks === null && !error && <p className={styles.emptyState}>불러오는 중...</p>}
      {error && <p className={styles.emptyState}>{error}</p>}
      {feedbacks?.length === 0 && (
        <p className={styles.emptyState}>아직 진행 중인 상담이 없어요. 전문가에게 첫 질문을 남겨보세요.</p>
      )}

      {feedbacks?.length > 0 && (
        <div className={styles.list}>
          {feedbacks.map((feedback) => (
            <button
              key={feedback.feedbackId}
              type="button"
              className={styles.consultRow}
              onClick={() => navigate(`/experts/consult/${feedback.feedbackId}`)}
            >
              <span className={styles.consultAvatar} style={{ backgroundColor: getAvatarColor(feedback.expertName) }}>
                {feedback.expertName?.[0]}
              </span>
              <div className={styles.consultRowMain}>
                <p className={styles.consultRowTitle}>
                  {feedback.expertName}
                  <span className={styles.expertBadge}>
                    <Star size={11} />
                    전문가
                  </span>
                </p>
                <span className={styles.activityMeta}>{feedback.topic}</span>
              </div>
              <div className={styles.consultMeta}>
                {feedback.answeredAt && <span className={styles.consultDate}>{formatDateTime(feedback.answeredAt)}</span>}
                <ConsultStatusBadge status={feedback.status} />
                <ChevronRight size={16} />
              </div>
            </button>
          ))}
        </div>
      )}
    </section>
  )
}

function SettingsTab({ user, onNicknameChanged, onLogout }) {
  const [nickname, setNickname] = useState(user.nickname)
  const [nicknameMessage, setNicknameMessage] = useState('')
  const [isSavingNickname, setIsSavingNickname] = useState(false)

  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [newPasswordConfirm, setNewPasswordConfirm] = useState('')
  const [passwordMessage, setPasswordMessage] = useState('')
  const [isSavingPassword, setIsSavingPassword] = useState(false)

  const [withdrawPassword, setWithdrawPassword] = useState('')
  const [withdrawMessage, setWithdrawMessage] = useState('')

  async function handleNicknameSubmit(event) {
    event.preventDefault()
    if (!nickname.trim() || nickname === user.nickname) return

    setIsSavingNickname(true)
    setNicknameMessage('')
    try {
      await userApi.updateNickname(nickname)
      await onNicknameChanged()
      setNicknameMessage('닉네임이 변경되었습니다.')
    } catch (err) {
      setNicknameMessage(err.response?.data?.message ?? '닉네임 변경에 실패했습니다.')
    } finally {
      setIsSavingNickname(false)
    }
  }

  async function handlePasswordSubmit(event) {
    event.preventDefault()
    setIsSavingPassword(true)
    setPasswordMessage('')
    try {
      await userApi.updatePassword({ currentPassword, newPassword, newPasswordConfirm })
      setPasswordMessage('비밀번호가 변경되었습니다.')
      setCurrentPassword('')
      setNewPassword('')
      setNewPasswordConfirm('')
    } catch (err) {
      setPasswordMessage(err.response?.data?.message ?? '비밀번호 변경에 실패했습니다.')
    } finally {
      setIsSavingPassword(false)
    }
  }

  async function handleWithdraw() {
    if (!withdrawPassword) {
      setWithdrawMessage('비밀번호를 입력해주세요.')
      return
    }
    if (!window.confirm('정말 탈퇴하시겠어요? 이 작업은 되돌릴 수 없어요.')) return

    try {
      await userApi.withdraw(withdrawPassword)
      await onLogout()
    } catch (err) {
      setWithdrawMessage(err.response?.data?.message ?? '회원 탈퇴에 실패했습니다.')
    }
  }

  return (
    <section className={styles.section}>
      <h2 className={styles.sectionHeading}>프로필 수정</h2>
      <form className={styles.settingsForm} onSubmit={handleNicknameSubmit}>
        <label className={styles.settingsLabel} htmlFor="nickname">
          닉네임
        </label>
        <input
          id="nickname"
          className={styles.settingsInput}
          value={nickname}
          onChange={(event) => setNickname(event.target.value)}
          maxLength={12}
        />
        {nicknameMessage && <p className={styles.settingsMessage}>{nicknameMessage}</p>}
        <button type="submit" className={styles.primaryButton} disabled={isSavingNickname}>
          {isSavingNickname ? '저장 중...' : '변경 저장'}
        </button>
      </form>

      <h2 className={styles.sectionHeading}>비밀번호 변경</h2>
      <form className={styles.settingsForm} onSubmit={handlePasswordSubmit}>
        <label className={styles.settingsLabel} htmlFor="currentPassword">
          현재 비밀번호
        </label>
        <input
          id="currentPassword"
          type="password"
          className={styles.settingsInput}
          placeholder="현재 비밀번호 입력"
          value={currentPassword}
          onChange={(event) => setCurrentPassword(event.target.value)}
        />

        <label className={styles.settingsLabel} htmlFor="newPassword">
          새 비밀번호
        </label>
        <input
          id="newPassword"
          type="password"
          className={styles.settingsInput}
          placeholder="8자 이상 입력"
          value={newPassword}
          onChange={(event) => setNewPassword(event.target.value)}
        />

        <label className={styles.settingsLabel} htmlFor="newPasswordConfirm">
          새 비밀번호 확인
        </label>
        <input
          id="newPasswordConfirm"
          type="password"
          className={styles.settingsInput}
          placeholder="새 비밀번호 재입력"
          value={newPasswordConfirm}
          onChange={(event) => setNewPasswordConfirm(event.target.value)}
        />

        {passwordMessage && <p className={styles.settingsMessage}>{passwordMessage}</p>}
        <button
          type="submit"
          className={styles.secondaryButton}
          disabled={isSavingPassword || !currentPassword || !newPassword || !newPasswordConfirm}
        >
          {isSavingPassword ? '변경 중...' : '비밀번호 변경'}
        </button>
      </form>

      <PaymentMethodSection />

      <h2 className={styles.sectionHeading}>계정</h2>
      <div className={styles.accountActions}>
        <div className={styles.withdrawBox}>
          <input
            type="password"
            className={styles.settingsInput}
            placeholder="탈퇴하려면 비밀번호를 입력하세요"
            value={withdrawPassword}
            onChange={(event) => setWithdrawPassword(event.target.value)}
          />
          {withdrawMessage && <p className={styles.settingsMessage}>{withdrawMessage}</p>}
          <button type="button" className={styles.dangerButton} onClick={handleWithdraw}>
            회원 탈퇴
          </button>
        </div>
      </div>
    </section>
  )
}

/**
 * 정기결제 카드는 유저당 한 장만 등록된다. 카드번호는 우리 서버가 보관하지 않아
 * PortOne이 내려준 표시용 정보(카드전표인자명·마스킹 번호)만 보여준다 — 둘 다 없을 수 있다.
 */
function PaymentMethodSection() {
  const [card, setCard] = useState(undefined) // undefined(조회 전) | null(없음) | { ... }
  const [subscription, setSubscription] = useState(null)
  const [message, setMessage] = useState('')
  const [isDeleting, setIsDeleting] = useState(false)
  const [isRegistering, setIsRegistering] = useState(false)

  useEffect(() => {
    let ignore = false

    billingKeyApi
      .getMy()
      .then(({ data }) => {
        if (!ignore) setCard(data.data.registered ? data.data : null)
      })
      .catch(() => {
        if (!ignore) setCard(null)
      })

    // 카드 삭제가 자동 갱신 해지로 이어지는지 안내하려면 구독 상태가 필요하다. 없으면 404.
    subscriptionApi
      .getMy()
      .then(({ data }) => {
        if (!ignore) setSubscription(data.data)
      })
      .catch(() => {
        if (!ignore) setSubscription(null)
      })

    return () => {
      ignore = true
    }
  }, [])

  const willCancelRenewal = Boolean(subscription?.autoRenew)

  async function refetchCard() {
    try {
      const { data } = await billingKeyApi.getMy()
      setCard(data.data.registered ? data.data : null)
    } catch {
      setCard(null)
    }
  }

  // 카드만 등록한다. 구독 시작은 구독 페이지가 맡는다 — 여기서는 결제수단 교체·재등록이 목적이다.
  async function handleRegister() {
    setMessage('')
    setIsRegistering(true)
    try {
      await registerCard()
      await refetchCard()
      setMessage(
        subscription && !subscription.autoRenew
          ? '카드가 등록되었어요. 구독 페이지에서 자동 결제를 다시 켤 수 있어요.'
          : '카드가 등록되었어요.',
      )
    } catch (err) {
      setMessage(err.response?.data?.message ?? err.message ?? '카드 등록에 실패했어요.')
    } finally {
      setIsRegistering(false)
    }
  }

  async function handleDelete() {
    const confirmMessage = willCancelRenewal
      ? '등록된 카드를 삭제할까요? 다음 회차 자동 결제가 멈추고, 남은 이용 기간이 끝나면 프리미엄이 종료돼요.'
      : '등록된 카드를 삭제할까요?'
    if (!window.confirm(confirmMessage)) return

    setMessage('')
    setIsDeleting(true)
    try {
      const { data } = await billingKeyApi.remove()
      setCard(null)
      setMessage(data.message ?? '카드가 삭제되었습니다.')
      // 카드 삭제는 자동 갱신도 함께 끄므로 구독 상태를 다시 읽어온다.
      const { data: refreshed } = await subscriptionApi.getMy().catch(() => ({ data: { data: null } }))
      setSubscription(refreshed.data)
    } catch (err) {
      setMessage(err.response?.data?.message ?? '카드 삭제에 실패했어요.')
    } finally {
      setIsDeleting(false)
    }
  }

  if (card === undefined) return null

  return (
    <>
      <h2 className={styles.sectionHeading}>결제수단</h2>
      {card ? (
        <div className={styles.paymentMethodCard}>
          <span className={styles.paymentMethodIcon}>
            <CreditCard size={18} />
          </span>
          <div className={styles.paymentMethodInfo}>
            <p className={styles.paymentMethodName}>
              {card.cardName ?? '등록된 카드'}
              {card.cardNumberMasked && <span className={styles.paymentMethodNumber}>{formatCardTail(card.cardNumberMasked)}</span>}
            </p>
            <p className={styles.paymentMethodMeta}>
              {formatDate(card.issuedAt)} 등록
              {willCancelRenewal && subscription?.expiredAt && ` · 다음 결제일 ${formatDate(subscription.expiredAt)}`}
            </p>
          </div>
          <div className={styles.paymentMethodActions}>
            <button
              type="button"
              className={styles.paymentMethodChange}
              onClick={handleRegister}
              disabled={isRegistering || isDeleting}
            >
              {isRegistering ? '등록 중...' : '변경'}
            </button>
            <button
              type="button"
              className={styles.paymentMethodDelete}
              onClick={handleDelete}
              disabled={isDeleting || isRegistering}
            >
              {isDeleting ? '삭제 중...' : '삭제'}
            </button>
          </div>
        </div>
      ) : (
        <div className={styles.paymentMethodEmptyBox}>
          <p className={styles.paymentMethodEmpty}>
            {subscription
              ? '등록된 카드가 없어요. 카드를 등록하면 정기결제를 이어갈 수 있어요.'
              : '등록된 카드가 없어요.'}
          </p>
          <button type="button" className={styles.secondaryButton} onClick={handleRegister} disabled={isRegistering}>
            {isRegistering ? '등록 중...' : '카드 등록'}
          </button>
        </div>
      )}
      {card && willCancelRenewal && (
        <p className={styles.paymentMethodMeta}>카드를 삭제하면 다음 회차 자동 결제가 함께 해지돼요.</p>
      )}
      {message && <p className={styles.settingsMessage}>{message}</p>}
    </>
  )
}
