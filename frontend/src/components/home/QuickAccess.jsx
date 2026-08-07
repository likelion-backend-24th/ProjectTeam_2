import {
  BookOpen,
  ChevronRight,
  Crown,
  Edit3,
  FileText,
  MessageCircle,
  Search,
  User,
  Users,
  Zap,
} from 'lucide-react'
import { Link } from 'react-router-dom'
import styles from './QuickAccess.module.css'

const ICON_COLORS = {
  blue: { bg: 'rgba(59, 130, 246, 0.14)', fg: '#60a5fa' },
  lime: { bg: 'rgba(198, 255, 61, 0.14)', fg: '#c6ff3d' },
  purple: { bg: 'rgba(168, 85, 247, 0.14)', fg: '#c084fc' },
  orange: { bg: 'rgba(251, 146, 60, 0.14)', fg: '#fb923c' },
  gold: { bg: 'rgba(250, 204, 21, 0.14)', fg: '#fbbf24' },
  teal: { bg: 'rgba(45, 212, 191, 0.14)', fg: '#2dd4bf' },
  indigo: { bg: 'rgba(129, 140, 248, 0.14)', fg: '#818cf8' },
  slate: { bg: 'rgba(148, 163, 184, 0.14)', fg: '#94a3b8' },
  red: { bg: 'rgba(248, 113, 113, 0.14)', fg: '#f87171' },
}

// TODO: 스터디/전문가/구독 플랜/마이페이지 페이지가 생기면 실제 경로(to) 연결
const QUICK_LINKS = [
  { icon: FileText, color: 'blue', title: '게시글', subtitle: '커뮤니티 게시판', tag: '전체 공개', to: '/posts' },
  { icon: Edit3, color: 'lime', title: '글쓰기', subtitle: '새 게시글 작성', tag: '로그인 필요', to: '/posts/new' },
  { icon: Search, color: 'purple', title: '스터디', subtitle: '그룹 찾기 · 신청', tag: '전체 공개', to: null },
  { icon: Users, color: 'orange', title: '스터디 개설', subtitle: '방장 되기', tag: '로그인 필요', to: null },
  {
    icon: Crown,
    color: 'gold',
    title: '구독 플랜',
    subtitle: '프리미엄 멤버십',
    tag: '유료',
    badge: 'PRO',
    to: null,
  },
  {
    icon: MessageCircle,
    color: 'teal',
    title: '전문가 상담',
    subtitle: '1:1 스레드',
    tag: '구독 필요',
    badge: 'PRO',
    to: null,
  },
  { icon: BookOpen, color: 'indigo', title: '전문가 등록', subtitle: '현직자 신청', tag: '신청 가능', to: null },
  { icon: User, color: 'slate', title: '마이페이지', subtitle: '내 활동 관리', tag: '로그인 필요', to: null },
]

function QuickLinkCard({ icon: Icon, color, title, subtitle, tag, badge, to }) {
  const { bg, fg } = ICON_COLORS[color]
  const Component = to ? Link : 'a'
  return (
    <Component to={to} href={to ? undefined : '#'} className={styles.card}>
      {badge && <span className={styles.badge}>★ {badge}</span>}
      <span className={styles.iconBox} style={{ backgroundColor: bg, color: fg }}>
        <Icon size={20} />
      </span>
      <p className={styles.cardTitle}>{title}</p>
      <p className={styles.cardSubtitle}>{subtitle}</p>
      <span className={styles.cardFooter}>
        <span className={styles.cardTag}>{tag}</span>
        <ChevronRight size={16} />
      </span>
    </Component>
  )
}

export default function QuickAccess() {
  const { bg: adminBg, fg: adminFg } = ICON_COLORS.red

  return (
    <section className={styles.section}>
      <div className={styles.inner}>
        <div className={styles.heading}>
          <div>
            <p className={styles.eyebrow}>QUICK ACCESS</p>
            <h2 className={styles.title}>바로가기</h2>
          </div>
          <p className={styles.description}>원하는 페이지로 바로 이동하세요.</p>
        </div>

        <div className={styles.grid}>
          {QUICK_LINKS.map((link) => (
            <QuickLinkCard key={link.title} {...link} />
          ))}
        </div>

        <a href="#" className={styles.adminRow}>
          <span className={styles.iconBox} style={{ backgroundColor: adminBg, color: adminFg }}>
            <Zap size={20} />
          </span>
          <span className={styles.adminText}>
            <p className={styles.cardTitle}>어드민 패널</p>
            <p className={styles.cardSubtitle}>게시글 · 스터디 관리, 유저 상태 변경, 전문가 승인</p>
          </span>
          <span className={styles.adminBadge}>ADMIN ONLY</span>
          <ChevronRight size={18} />
        </a>
      </div>
    </section>
  )
}
