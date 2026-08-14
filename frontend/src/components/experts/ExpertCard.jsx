import { Star } from 'lucide-react'
import { getAvatarColor } from '../../utils/avatarColor'
import styles from './ExpertCard.module.css'

// "활동 중인 전문가" 그리드에 쓰는 카드. 클릭하면 프로필 상세를 연다.
export default function ExpertCard({ expert, onClick }) {
  return (
    <button type="button" className={styles.card} onClick={() => onClick(expert.expertId)}>
      <div className={styles.head}>
        <span className={styles.avatar} style={{ backgroundColor: getAvatarColor(expert.nickname) }}>
          {expert.nickname?.[0]}
        </span>
        <div>
          <p className={styles.name}>{expert.nickname}</p>
          <span className={styles.badge}>
            <Star size={10} />
            전문가
          </span>
        </div>
      </div>

      <p className={styles.career}>{expert.career ?? '경력 정보 없음'}</p>
      <span className={styles.viewLink}>프로필 보기 →</span>
    </button>
  )
}
