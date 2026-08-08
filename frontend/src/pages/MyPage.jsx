import { ChevronRight, Eye, LayoutGrid, Pencil, Settings, Trash2, User as UserIcon } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { postApi, userApi } from '../api'
import Pagination from '../components/common/Pagination'
import SiteHeader from '../components/common/SiteHeader'
import { getPostCategoryMeta } from '../constants/postCategory'
import { useAuth } from '../context/AuthContext'
import { getAvatarColor } from '../utils/avatarColor'
import { formatDate } from '../utils/formatDate'
import styles from './MyPage.module.css'

const ROLE_META = {
  USER: { label: 'USER', color: '#c6ff3d' },
  EXPERT: { label: '전문가', color: '#60a5fa' },
  ADMIN: { label: '관리자', color: '#fb923c' },
}

const TABS = [
  { key: 'activity', label: '내 활동', icon: UserIcon },
  { key: 'posts', label: '게시글', icon: LayoutGrid },
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

  if (!user) return null

  const roleMeta = ROLE_META[user.role] ?? ROLE_META.USER

  return (
    <>
      <SiteHeader backTo="/" />
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
            </div>
            <p className={styles.username}>@{user.username}</p>
          </div>
          <div className={styles.postCountPill}>
            <strong>{postCount}</strong>
            게시글
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
          <ActivityTab recentPosts={recentPosts} onSeeAllPosts={() => setActiveTab('posts')} />
        )}

        {activeTab === 'posts' && (
          <PostsTab onPostsChanged={() => setPostsVersion((prev) => prev + 1)} />
        )}

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

function ActivityTab({ recentPosts, onSeeAllPosts }) {
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
                <span>{formatDate(post.createdAt)}</span>
                <span className={styles.dot}>·</span>
                <Eye size={13} />
                <span>{post.viewCount}</span>
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
                    <span>{formatDate(post.createdAt)}</span>
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

      <h2 className={styles.sectionHeading}>계정</h2>
      <div className={styles.accountActions}>
        <button type="button" className={styles.secondaryButton} onClick={onLogout}>
          로그아웃
        </button>

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
