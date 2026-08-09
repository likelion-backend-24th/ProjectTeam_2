import { Check, ChevronLeft, Crown, Lock, PenLine } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { studyApi, studyPostApi } from '../../api'
import SiteHeader from '../../components/common/SiteHeader'
import { getStudyCategoryMeta } from '../../constants/studyCategory'
import { useAuth } from '../../context/AuthContext'
import { getAvatarColor } from '../../utils/avatarColor'
import { formatDate } from '../../utils/formatDate'
import { isOverFreeStudyLimit } from '../../utils/studyLimit'
import styles from './StudyDetailPage.module.css'

const TABS = [
  { key: 'intro', label: '소개' },
  { key: 'board', label: '게시판' },
  { key: 'members', label: '멤버' },
]

function isRecruitClosed(recruitEnd) {
  if (!recruitEnd) return false
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return new Date(recruitEnd) < today
}

export default function StudyDetailPage() {
  const { studyId } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()

  const [study, setStudy] = useState(null)
  const [members, setMembers] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [activeTab, setActiveTab] = useState('intro')
  const [isJoining, setIsJoining] = useState(false)
  const [actionError, setActionError] = useState('')

  const [posts, setPosts] = useState([])
  const [isBoardLoading, setIsBoardLoading] = useState(false)
  const [boardError, setBoardError] = useState('')
  const [boardLoaded, setBoardLoaded] = useState(false)

  function refetchStudy() {
    return Promise.all([studyApi.getStudyById(studyId), studyApi.getStudyMembers(studyId)]).then(
      ([studyRes, membersRes]) => {
        setStudy(studyRes.data.data)
        setMembers(membersRes.data.data)
      },
    )
  }

  useEffect(() => {
    let ignore = false
    setIsLoading(true)
    setError('')

    refetchStudy()
      .catch((err) => {
        if (ignore) return
        setError(err.response?.status === 404 ? '존재하지 않는 스터디입니다.' : '스터디 정보를 불러오지 못했습니다.')
      })
      .finally(() => {
        if (!ignore) setIsLoading(false)
      })

    return () => {
      ignore = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [studyId])

  const isLeader = Boolean(study) && Boolean(user) && study.leaderId === user.id
  const isMember = members.some((member) => member.userId === user?.id)

  useEffect(() => {
    if (activeTab !== 'board' || !isMember || boardLoaded) return
    let ignore = false
    setIsBoardLoading(true)

    studyPostApi
      .getStudyPosts(studyId)
      .then(({ data }) => {
        if (ignore) return
        const sorted = [...data.data].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
        setPosts(sorted)
        setBoardLoaded(true)
      })
      .catch(() => {
        if (!ignore) setBoardError('게시판을 불러오지 못했습니다.')
      })
      .finally(() => {
        if (!ignore) setIsBoardLoading(false)
      })

    return () => {
      ignore = true
    }
  }, [activeTab, isMember, boardLoaded, studyId])

  async function handleJoin() {
    setActionError('')

    if (await isOverFreeStudyLimit(user)) {
      navigate('/subscription')
      return
    }

    setIsJoining(true)
    try {
      await studyApi.joinStudy(studyId)
      await refetchStudy()
    } catch (err) {
      setActionError(err.response?.data?.message ?? '스터디 가입에 실패했습니다.')
    } finally {
      setIsJoining(false)
    }
  }

  async function handleLeave() {
    if (!window.confirm('스터디를 탈퇴할까요?')) return
    try {
      await studyApi.leaveStudy(studyId)
      navigate('/studies')
    } catch (err) {
      window.alert(err.response?.data?.message ?? '탈퇴에 실패했습니다.')
    }
  }

  async function handleDeleteStudy() {
    if (!window.confirm('스터디를 삭제할까요? 게시글과 댓글이 모두 함께 삭제되고 되돌릴 수 없어요.')) return
    try {
      await studyApi.deleteStudy(studyId)
      navigate('/studies')
    } catch (err) {
      window.alert(err.response?.data?.message ?? '스터디 삭제에 실패했습니다.')
    }
  }

  if (isLoading) {
    return (
      <>
        <SiteHeader />
        <main className={styles.main}>
          <p className={styles.state}>불러오는 중...</p>
        </main>
      </>
    )
  }

  if (error || !study) {
    return (
      <>
        <SiteHeader />
        <main className={styles.main}>
          <p className={styles.state}>{error || '스터디 정보를 불러오지 못했습니다.'}</p>
        </main>
      </>
    )
  }

  const categoryMeta = getStudyCategoryMeta(study.category)
  const closed = isRecruitClosed(study.recruitEnd)
  const isFull = study.currentMemberCount >= study.capacity

  return (
    <>
      <SiteHeader />
      <main className={styles.main}>
        <Link to="/studies" className={styles.breadcrumb}>
          <ChevronLeft size={16} />
          스터디 목록
        </Link>

        <section className={styles.card}>
          {categoryMeta && (
            <span className={styles.badge} style={{ backgroundColor: categoryMeta.bg, color: categoryMeta.color }}>
              {study.categoryLabel}
            </span>
          )}
          <h1 className={styles.title}>{study.title}</h1>

          <div className={styles.statRow}>
            <div className={styles.statBox}>
              <span className={styles.statLabel}>멤버</span>
              <span className={styles.statValue}>
                {study.currentMemberCount}/{study.capacity}명
              </span>
            </div>
            <div className={styles.statBox}>
              <span className={styles.statLabel}>가입 방식</span>
              <span className={styles.statValue}>자유 가입</span>
            </div>
            <div className={styles.statBox}>
              <span className={styles.statLabel}>모집 마감일</span>
              <span className={styles.statValue}>{study.recruitEnd}</span>
            </div>
          </div>

          <div className={styles.leaderRow}>
            <span className={styles.avatar} style={{ backgroundColor: getAvatarColor(study.leaderNickname) }}>
              {study.leaderNickname?.[0]}
            </span>
            <div>
              <p className={styles.leaderName}>
                <Crown size={14} />
                {study.leaderNickname}
              </p>
              <p className={styles.leaderTag}>스터디 방장</p>
            </div>
          </div>

          {isLeader ? (
            <div className={styles.leaderActionBox}>
              <span className={styles.memberButtonDone}>
                <Check size={16} />
                내가 만든 스터디예요
              </span>
              <Link to={`/studies/${studyId}/edit`} className={styles.editLink}>
                수정
              </Link>
              <button type="button" className={styles.dangerLink} onClick={handleDeleteStudy}>
                스터디 삭제
              </button>
            </div>
          ) : isMember ? (
            <div className={styles.leaderActionBox}>
              <span className={styles.memberButtonDone}>
                <Check size={16} />
                스터디 멤버
              </span>
              <button type="button" className={styles.dangerLink} onClick={handleLeave}>
                탈퇴하기
              </button>
            </div>
          ) : (
            <>
              <button
                type="button"
                className={styles.joinButton}
                onClick={handleJoin}
                disabled={isJoining || isFull || closed}
              >
                {isJoining ? '가입 중...' : closed ? '모집이 마감됐어요' : isFull ? '모집 정원이 가득 찼어요' : '스터디 가입하기'}
              </button>
              {actionError && <p className={styles.actionError}>{actionError}</p>}
            </>
          )}
        </section>

        <nav className={styles.tabs}>
          {TABS.map((tab) => (
            <button
              key={tab.key}
              type="button"
              className={`${styles.tabButton} ${activeTab === tab.key ? styles.tabActive : ''}`}
              onClick={() => setActiveTab(tab.key)}
            >
              {tab.label}
              {tab.key === 'board' && boardLoaded && <span className={styles.tabCount}>{posts.length}</span>}
            </button>
          ))}
        </nav>

        {activeTab === 'intro' && (
          <section className={styles.panel}>
            <p className={styles.description}>{study.description}</p>
          </section>
        )}

        {activeTab === 'board' && !isMember && (
          <section className={styles.lockedPanel}>
            <Lock size={28} className={styles.lockIcon} />
            <p className={styles.lockedTitle}>가입 멤버 전용 게시판</p>
            <p className={styles.lockedSubtitle}>스터디에 가입하면 게시판을 이용할 수 있어요.</p>
          </section>
        )}

        {activeTab === 'board' && isMember && (
          <section className={styles.panel}>
            <div className={styles.boardHeader}>
              <Link to={`/studies/${studyId}/posts/new`} className={styles.writeButton}>
                <PenLine size={14} />
                글쓰기
              </Link>
            </div>

            {isBoardLoading && <p className={styles.state}>불러오는 중...</p>}
            {!isBoardLoading && boardError && <p className={styles.state}>{boardError}</p>}
            {!isBoardLoading && !boardError && posts.length === 0 && (
              <p className={styles.state}>아직 등록된 게시글이 없어요.</p>
            )}
            {!isBoardLoading && !boardError && posts.length > 0 && (
              <div className={styles.postList}>
                {posts.map((post) => (
                  <Link key={post.id} to={`/studies/${studyId}/posts/${post.id}`} className={styles.postRow}>
                    <p className={styles.postTitle}>{post.title}</p>
                    <span className={styles.postMeta}>
                      <span>{post.authorNickname}</span>
                      <span>·</span>
                      <span>{formatDate(post.createdAt)}</span>
                    </span>
                  </Link>
                ))}
              </div>
            )}
          </section>
        )}

        {activeTab === 'members' && (
          <section className={styles.panel}>
            <div className={styles.memberList}>
              {members.map((member) => {
                const isMemberLeader = member.userId === study.leaderId
                return (
                  <div key={member.memberId} className={styles.memberRow}>
                    <span className={styles.avatar} style={{ backgroundColor: getAvatarColor(member.nickname) }}>
                      {member.nickname?.[0]}
                    </span>
                    <div>
                      <p className={styles.memberName}>
                        {isMemberLeader && <Crown size={12} />}
                        {member.nickname}
                      </p>
                      <p className={styles.memberTag}>{isMemberLeader ? '방장' : '멤버'}</p>
                    </div>
                  </div>
                )
              })}
            </div>
          </section>
        )}
      </main>
    </>
  )
}
