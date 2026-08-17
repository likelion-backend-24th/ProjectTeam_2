import { ChevronLeft, ChevronRight, MessageSquarePlus } from 'lucide-react'
import { useState } from 'react'
import { feedbackApi } from '../../api'
import { getAvatarColor } from '../../utils/avatarColor'
import Modal from '../common/Modal'
import styles from './NewConsultModal.module.css'

const TOPIC_MAX_LENGTH = 100
const CONTENT_MAX_LENGTH = 2000

// 새 상담 추가: 1) 전문가 선택 -> 2) 상담 주제/내용 입력. preselected가 있으면 1단계를 건너뛴다
// (프로필 모달에서 "상담 시작하기"로 들어온 경우).
export default function NewConsultModal({ experts, preselected, onClose, onCreated }) {
  const [selectedExpert, setSelectedExpert] = useState(preselected ?? null)
  const [topic, setTopic] = useState('')
  const [content, setContent] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState('')

  const step = selectedExpert ? 2 : 1

  async function handleSubmit(event) {
    event.preventDefault()
    if (!topic.trim() || !content.trim()) {
      setError('상담 제목과 내용을 모두 입력해주세요.')
      return
    }
    setError('')
    setIsSubmitting(true)
    try {
      const { data } = await feedbackApi.createFeedback({
        expertProfileId: selectedExpert.expertId,
        topic,
        content,
      })
      onCreated(data.data)
    } catch (err) {
      setError(err.response?.data?.message ?? '상담 신청에 실패했습니다.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Modal onClose={onClose} maxWidth={480}>
      <div className={styles.body}>
        <div className={styles.headRow}>
          {step === 2 && !preselected && (
            <button
              type="button"
              className={styles.backButton}
              onClick={() => setSelectedExpert(null)}
              aria-label="이전"
            >
              <ChevronLeft size={18} />
            </button>
          )}
          <p className={styles.title}>{step === 1 ? '전문가 선택' : '상담 주제 입력'}</p>
        </div>

        <div className={styles.progress}>
          <span className={`${styles.progressSegment} ${styles.progressSegmentActive}`} />
          <span className={`${styles.progressSegment} ${step === 2 ? styles.progressSegmentActive : ''}`} />
        </div>

        {step === 1 ? (
          <>
            <p className={styles.hint}>상담받고 싶은 전문가를 선택하세요.</p>
            {experts.length === 0 ? (
              <p className={styles.state}>활동 중인 전문가가 없어요.</p>
            ) : (
              <div className={styles.expertList}>
                {experts.map((expert) => (
                  <button
                    key={expert.expertId}
                    type="button"
                    className={styles.expertRow}
                    onClick={() => setSelectedExpert(expert)}
                  >
                    <span className={styles.avatar} style={{ backgroundColor: getAvatarColor(expert.name) }}>
                      {expert.name?.[0]}
                    </span>
                    <div className={styles.expertRowBody}>
                      <div className={styles.nameRow}>
                        <span className={styles.name}>{expert.name}</span>
                        <span className={styles.badge}>전문가</span>
                      </div>
                      <p className={styles.career}>{expert.career ?? '경력 정보 없음'}</p>
                    </div>
                    <ChevronRight size={16} />
                  </button>
                ))}
              </div>
            )}
          </>
        ) : (
          <>
            <div className={styles.selectedExpertCard}>
              <span className={styles.avatar} style={{ backgroundColor: getAvatarColor(selectedExpert.name) }}>
                {selectedExpert.name?.[0]}
              </span>
              <div>
                <p className={styles.name}>{selectedExpert.name}</p>
                {selectedExpert.career && <p className={styles.career}>{selectedExpert.career}</p>}
              </div>
            </div>

            <form className={styles.form} onSubmit={handleSubmit}>
              <div className={styles.field}>
                <label className={styles.label} htmlFor="topic">
                  상담 제목
                </label>
                <input
                  id="topic"
                  className={styles.input}
                  placeholder="예) 프론트엔드 포트폴리오 피드백 요청"
                  maxLength={TOPIC_MAX_LENGTH}
                  value={topic}
                  onChange={(event) => setTopic(event.target.value)}
                />
              </div>

              <div className={styles.field}>
                <label className={styles.label} htmlFor="content">
                  상담 내용 <span className={styles.hintSmall}>(상세히 작성할수록 빠른 답변을 받을 수 있어요)</span>
                </label>
                <textarea
                  id="content"
                  className={styles.textarea}
                  placeholder="예) 카카오, 네이버 지원 예정이고 주로 React, TypeScript를 사용했습니다. GitHub 링크도 함께 공유드릴게요!"
                  maxLength={CONTENT_MAX_LENGTH}
                  value={content}
                  onChange={(event) => setContent(event.target.value)}
                />
              </div>

              {error && <p className={styles.error}>{error}</p>}

              <button type="submit" className={styles.submitButton} disabled={isSubmitting}>
                <MessageSquarePlus size={16} />
                {isSubmitting ? '신청 중...' : '상담 신청하기'}
              </button>
              <p className={styles.caption}>전문가가 답변을 남기면 알림 없이도 상담 목록에서 바로 확인할 수 있어요.</p>
            </form>
          </>
        )}
      </div>
    </Modal>
  )
}
