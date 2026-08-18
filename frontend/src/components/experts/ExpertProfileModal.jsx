import { Award, Briefcase, Crown, Star } from 'lucide-react'
import { useEffect, useState } from 'react'
import { expertApi } from '../../api'
import { getJobFieldLabel } from '../../constants/jobField'
import { getAvatarColor } from '../../utils/avatarColor'
import Modal from '../common/Modal'
import styles from './ExpertProfileModal.module.css'

// 전문가 프로필 상세(경력 전체·자격증·소개글) 모달. 승인된 전문가만 조회 가능(비로그인도 OK).
export default function ExpertProfileModal({ expertId, isSubscribed, onClose, onStartConsult }) {
  const [profile, setProfile] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let ignore = false
    setIsLoading(true)
    setError('')

    expertApi
      .getExpertDetail(expertId)
      .then(({ data }) => {
        if (!ignore) setProfile(data.data)
      })
      .catch(() => {
        if (!ignore) setError('프로필을 불러오지 못했습니다.')
      })
      .finally(() => {
        if (!ignore) setIsLoading(false)
      })

    return () => {
      ignore = true
    }
  }, [expertId])

  const primaryCareer = profile?.careers?.[0]
  const careerSummary = primaryCareer
    ? `${primaryCareer.companyName} · ${primaryCareer.position} · ${primaryCareer.years}년차`
    : null

  return (
    <Modal onClose={onClose} maxWidth={480}>
      <div className={styles.body}>
        <p className={styles.title}>전문가 프로필</p>

        {isLoading && <p className={styles.state}>불러오는 중...</p>}
        {!isLoading && error && <p className={styles.state}>{error}</p>}

        {!isLoading && profile && (
          <>
            <div className={styles.head}>
              <span className={styles.avatar} style={{ backgroundColor: getAvatarColor(profile.name) }}>
                {profile.name?.[0]}
              </span>
              <div>
                <div className={styles.nameRow}>
                  <p className={styles.name}>{profile.name}</p>
                  <span className={styles.badge}>
                    <Star size={10} />
                    전문가
                  </span>
                </div>
                {careerSummary && <p className={styles.career}>{careerSummary}</p>}
                {primaryCareer && <span className={styles.tag}>{getJobFieldLabel(primaryCareer.jobField)}</span>}
              </div>
            </div>

            {profile.introduction && (
              <div className={styles.section}>
                <p className={styles.sectionHeading}>소개글</p>
                <div className={styles.introBox}>{profile.introduction}</div>
              </div>
            )}

            {profile.careers?.length > 0 && (
              <div className={styles.section}>
                <p className={styles.sectionHeading}>
                  <Briefcase size={13} />
                  경력
                </p>
                {profile.careers.map((career) => (
                  <div key={career.id} className={styles.listItem}>
                    <div>
                      <p className={styles.itemTitle}>{career.companyName}</p>
                      <p className={styles.itemSubtitle}>{career.position}</p>
                    </div>
                    <div className={styles.itemMeta}>
                      <span className={styles.tag}>{getJobFieldLabel(career.jobField)}</span>
                      <span className={styles.itemYear}>{career.years}년</span>
                    </div>
                  </div>
                ))}
              </div>
            )}

            {profile.certifications?.length > 0 && (
              <div className={styles.section}>
                <p className={styles.sectionHeading}>
                  <Award size={13} />
                  자격증
                </p>
                {profile.certifications.map((cert) => (
                  <div key={cert.id} className={styles.listItem}>
                    <div>
                      <p className={styles.itemTitle}>{cert.name}</p>
                      <p className={styles.itemSubtitle}>{cert.issuer}</p>
                    </div>
                    <span className={styles.itemYear}>{cert.acquiredYear}</span>
                  </div>
                ))}
              </div>
            )}

            <button
              type="button"
              className={styles.ctaButton}
              onClick={() => onStartConsult(profile.expertId, profile.name, careerSummary)}
            >
              <Crown size={16} />
              {isSubscribed ? '새 상담 시작하기' : '구독하고 상담 시작하기'}
            </button>
            {!isSubscribed && <p className={styles.ctaCaption}>구독 회원만 1:1 상담을 시작할 수 있어요.</p>}
          </>
        )}
      </div>
    </Modal>
  )
}
