import { ChevronRight, Crown, Users } from 'lucide-react'
import { Link } from 'react-router-dom'
import { getStudyCategoryMeta } from '../../constants/studyCategory'
import { getAvatarColor } from '../../utils/avatarColor'
import styles from './StudyCard.module.css'

// recruitEnd(YYYY-MM-DD) 기준으로 모집 마감 여부를 계산한다. (백엔드에 별도 상태 필드가 없음)
function isRecruitClosed(recruitEnd) {
  if (!recruitEnd) return false
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return new Date(recruitEnd) < today
}

export default function StudyCard({ study }) {
  const categoryMeta = getStudyCategoryMeta(study.category)
  const closed = isRecruitClosed(study.recruitEnd)

  return (
    <Link
      to={`/studies/${study.id}`}
      className={`${styles.card} ${study.leaderSubscribed ? styles.cardSubscribed : ''}`}
    >
      <div className={styles.badgeRow}>
        {categoryMeta && (
          <span className={styles.badge} style={{ backgroundColor: categoryMeta.bg, color: categoryMeta.color }}>
            {study.categoryLabel}
          </span>
        )}
        {closed && <span className={styles.closedBadge}>마감</span>}
      </div>

      <h3 className={styles.title}>{study.title}</h3>
      <p className={styles.description}>{study.description}</p>

      <div className={styles.footer}>
        <div className={styles.leader}>
          <span className={styles.avatar} style={{ backgroundColor: getAvatarColor(study.leaderNickname) }}>
            {study.leaderNickname?.[0]}
          </span>
          <div className={styles.leaderInfo}>
            <span className={styles.leaderName}>{study.leaderNickname}</span>
            <span className={styles.leaderTag}>
              <Crown size={10} />
              방장
            </span>
          </div>
        </div>

        <span className={styles.capacity}>
          <Users size={13} />
          정원 {study.capacity}명
        </span>

        <ChevronRight className={styles.chevron} size={18} />
      </div>
    </Link>
  )
}
