import { ArrowRight, Award, Briefcase, ChevronLeft, PenLine, Plus, Trash2 } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { expertApi } from '../../api'
import SiteHeader from '../../components/common/SiteHeader'
import { JOB_FIELDS } from '../../constants/jobField'
import { useAuth } from '../../context/AuthContext'
import { getAvatarColor } from '../../utils/avatarColor'
import styles from './ExpertApplyFormPage.module.css'

const INTRO_MAX_LENGTH = 500

function emptyCareer() {
  return { companyName: '', position: '', years: '', jobField: '' }
}

function emptyCertification() {
  return { name: '', issuer: '', acquiredYear: '' }
}

const STEP_LABELS = ['경력 입력', '자격증', '소개글 작성']

// 전문가 신청/수정/재신청을 한 폼에서 처리한다.
// - 신청 이력이 없으면(404) 신규 신청 -> POST /api/experts/signup
// - PENDING이면 수정 -> PATCH /api/experts/me
// - REJECTED면 재신청 -> POST /api/experts/signup (백엔드가 내부적으로 재신청 처리)
// - APPROVED면 더 손댈 게 없어서 현황 페이지로 보낸다
// 주의: 기존 신청 내용(경력/자격증/소개글)을 다시 불러오는 API가 없어서 수정/재신청도 빈 폼으로 시작한다.
export default function ExpertApplyFormPage() {
  const navigate = useNavigate()
  const { user } = useAuth()

  const [mode, setMode] = useState(null) // 'create' | 'edit' | 'reapply'
  const [rejectReason, setRejectReason] = useState('')
  const [isLoadingMode, setIsLoadingMode] = useState(true)

  const [step, setStep] = useState(1)
  const [careers, setCareers] = useState([emptyCareer()])
  const [certifications, setCertifications] = useState([])
  const [introduction, setIntroduction] = useState('')

  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    let ignore = false

    expertApi
      .getMyApplicationStatus()
      .then(({ data }) => {
        if (ignore) return
        const { status, reason } = data.data
        if (status === 'APPROVED') {
          navigate('/experts/apply/status', { replace: true })
          return
        }
        setMode(status === 'PENDING' ? 'edit' : 'reapply')
        setRejectReason(reason ?? '')
      })
      .catch(() => {
        // 404(EXPERT_PROFILE_NOT_FOUND) = 신청 이력 없음. 그 밖의 실패도 일단 신규 신청으로 안내한다.
        if (!ignore) setMode('create')
      })
      .finally(() => {
        if (!ignore) setIsLoadingMode(false)
      })

    return () => {
      ignore = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  function updateCareer(index, patch) {
    setCareers((prev) => prev.map((career, i) => (i === index ? { ...career, ...patch } : career)))
  }

  function updateCertification(index, patch) {
    setCertifications((prev) => prev.map((cert, i) => (i === index ? { ...cert, ...patch } : cert)))
  }

  function isCareerStepValid() {
    return careers.every((c) => c.companyName.trim() && c.position.trim() && c.years && c.jobField)
  }

  function isCertificationStepValid() {
    return certifications.every((c) => c.name.trim() && c.issuer.trim() && c.acquiredYear)
  }

  function handleNext(event) {
    event.preventDefault()
    if (step === 1 && !isCareerStepValid()) {
      setError('경력 정보를 모두 입력해주세요.')
      return
    }
    if (step === 2 && !isCertificationStepValid()) {
      setError('자격증 정보를 모두 입력하거나, 필요 없는 항목은 삭제해주세요.')
      return
    }
    setError('')
    setStep((prev) => prev + 1)
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setIsSubmitting(true)

    const payload = {
      careers: careers.map((c) => ({ ...c, years: Number(c.years) })),
      certifications: certifications.map((c) => ({ ...c, acquiredYear: Number(c.acquiredYear) })),
      introduction: introduction.trim() || null,
    }

    try {
      if (mode === 'edit') {
        await expertApi.updateMyApplication(payload)
      } else {
        await expertApi.signupExpert(payload)
      }
      navigate('/experts/apply/status')
    } catch (err) {
      setError(err.response?.data?.message ?? '신청에 실패했습니다.')
    } finally {
      setIsSubmitting(false)
    }
  }

  const previewCareer = careers[0]

  if (isLoadingMode) {
    return (
      <>
        <SiteHeader />
        <main className={styles.main}>
          <p className={styles.state}>불러오는 중...</p>
        </main>
      </>
    )
  }

  return (
    <>
      <SiteHeader />
      <main className={styles.main}>
        <Link to="/experts" className={styles.breadcrumb}>
          <ChevronLeft size={16} />
          전문가 상담
        </Link>

        <p className={styles.eyebrow}>EXPERT APPLY</p>
        <h1 className={styles.title}>{mode === 'edit' ? '전문가 신청서 수정' : '전문가 등록 신청'}</h1>
        <p className={styles.subtitle}>심사 후 3~5일 이내에 결과를 안내드려요.</p>

        {mode === 'reapply' && rejectReason && (
          <p className={styles.rejectBanner}>이전 반려 사유: {rejectReason}</p>
        )}

        <div className={styles.steps}>
          {STEP_LABELS.map((label, index) => {
            const num = index + 1
            const isLast = num === STEP_LABELS.length
            return (
              <div key={label} className={styles.stepItem} style={{ flex: isLast ? 'none' : 1 }}>
                <span className={`${styles.stepNumber} ${step >= num ? styles.stepNumberActive : ''}`}>{num}</span>
                <span className={step === num ? styles.stepLabelActive : styles.stepLabel}>{label}</span>
                {!isLast && <span className={styles.stepDivider} />}
              </div>
            )
          })}
        </div>

        {step === 1 && (
          <form onSubmit={handleNext}>
            <p className={styles.sectionHeading}>
              <Briefcase size={14} />
              경력 사항
            </p>

            {careers.map((career, index) => (
              <div key={index} className={styles.itemCard}>
                <div className={styles.itemCardHeader}>
                  <span className={styles.itemCardTitle}>경력 {index + 1}</span>
                  {careers.length > 1 && (
                    <button
                      type="button"
                      className={styles.deleteButton}
                      onClick={() => setCareers((prev) => prev.filter((_, i) => i !== index))}
                      aria-label="경력 삭제"
                    >
                      <Trash2 size={14} />
                    </button>
                  )}
                </div>
                <div className={styles.fieldGrid}>
                  <div className={styles.field}>
                    <label className={styles.label}>회사명</label>
                    <input
                      className={styles.input}
                      placeholder="예) 카카오, 토스"
                      value={career.companyName}
                      onChange={(event) => updateCareer(index, { companyName: event.target.value })}
                    />
                  </div>
                  <div className={styles.field}>
                    <label className={styles.label}>직함</label>
                    <input
                      className={styles.input}
                      placeholder="예) 시니어 개발자"
                      value={career.position}
                      onChange={(event) => updateCareer(index, { position: event.target.value })}
                    />
                  </div>
                  <div className={styles.field}>
                    <label className={styles.label}>경력 연차</label>
                    <input
                      className={styles.input}
                      type="number"
                      min={0}
                      placeholder="예) 5"
                      value={career.years}
                      onChange={(event) => updateCareer(index, { years: event.target.value })}
                    />
                  </div>
                  <div className={styles.field}>
                    <label className={styles.label}>직무 분야</label>
                    <select
                      className={styles.select}
                      value={career.jobField}
                      onChange={(event) => updateCareer(index, { jobField: event.target.value })}
                    >
                      <option value="">분야 선택</option>
                      {JOB_FIELDS.map((field) => (
                        <option key={field.value} value={field.value}>
                          {field.label}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>
              </div>
            ))}

            <button
              type="button"
              className={styles.addButton}
              onClick={() => setCareers((prev) => [...prev, emptyCareer()])}
            >
              <Plus size={15} />
              경력 추가
            </button>

            {error && <p className={styles.error}>{error}</p>}

            <div className={styles.actionsEnd}>
              <button type="submit" className={styles.submitButton}>
                다음 단계
                <ArrowRight size={15} style={{ marginLeft: 6, verticalAlign: -2 }} />
              </button>
            </div>
          </form>
        )}

        {step === 2 && (
          <form onSubmit={handleNext}>
            <p className={styles.sectionHeading}>
              <Award size={14} />
              자격증 / 수료증 <span className={styles.sectionNote}>(없으면 건너뛰어도 됩니다)</span>
            </p>

            {certifications.map((cert, index) => (
              <div key={index} className={styles.itemCard}>
                <div className={styles.itemCardHeader}>
                  <span className={styles.itemCardTitle}>자격증 {index + 1}</span>
                  <button
                    type="button"
                    className={styles.deleteButton}
                    onClick={() => setCertifications((prev) => prev.filter((_, i) => i !== index))}
                    aria-label="자격증 삭제"
                  >
                    <Trash2 size={14} />
                  </button>
                </div>
                <div className={styles.fieldGrid}>
                  <div className={styles.field}>
                    <label className={styles.label}>자격증명</label>
                    <input
                      className={styles.input}
                      placeholder="예) AWS SAA"
                      value={cert.name}
                      onChange={(event) => updateCertification(index, { name: event.target.value })}
                    />
                  </div>
                  <div className={styles.field}>
                    <label className={styles.label}>발급 기관</label>
                    <input
                      className={styles.input}
                      placeholder="예) Amazon"
                      value={cert.issuer}
                      onChange={(event) => updateCertification(index, { issuer: event.target.value })}
                    />
                  </div>
                  <div className={styles.field}>
                    <label className={styles.label}>취득 연도</label>
                    <input
                      className={styles.input}
                      type="number"
                      min={1970}
                      placeholder="예) 2024"
                      value={cert.acquiredYear}
                      onChange={(event) => updateCertification(index, { acquiredYear: event.target.value })}
                    />
                  </div>
                </div>
              </div>
            ))}

            <button
              type="button"
              className={styles.addButton}
              onClick={() => setCertifications((prev) => [...prev, emptyCertification()])}
            >
              <Plus size={15} />
              자격증 추가
            </button>

            {error && <p className={styles.error}>{error}</p>}

            <div className={styles.actions}>
              <button type="button" className={styles.cancelButton} onClick={() => setStep(1)}>
                이전
              </button>
              <button type="submit" className={styles.submitButton}>
                다음 단계
              </button>
            </div>
          </form>
        )}

        {step === 3 && (
          <form onSubmit={handleSubmit}>
            <p className={styles.sectionHeading}>
              <PenLine size={14} />
              소개글 <span className={styles.sectionNote}>({introduction.length}/{INTRO_MAX_LENGTH})</span>
            </p>
            <p className={styles.helperText}>현직자로서 어떤 경험과 강점을 가지고 있는지, 취준생에게 어떤 도움을 줄 수 있는지 자유롭게 작성해주세요.</p>

            <textarea
              className={styles.textarea}
              placeholder="예시) 안녕하세요! 카카오 프론트엔드 개발자 5년차입니다. 포트폴리오 리뷰, 기술 면접 코칭, 커리어 방향 상담을 전문으로 합니다."
              maxLength={INTRO_MAX_LENGTH}
              value={introduction}
              onChange={(event) => setIntroduction(event.target.value)}
            />

            <p className={styles.previewLabel}>카드 미리보기</p>
            <div className={styles.previewCard}>
              <div className={styles.previewHead}>
                <span className={styles.previewAvatar} style={{ backgroundColor: getAvatarColor(user?.name) }}>
                  {user?.name?.[0]}
                </span>
                <div>
                  <p className={styles.previewName}>{user?.name}</p>
                  {previewCareer?.companyName && (
                    <p className={styles.previewCareer}>
                      {previewCareer.position || '직함'} · {previewCareer.companyName} · {previewCareer.years || 0}년
                    </p>
                  )}
                </div>
              </div>
              {introduction ? (
                <p className={styles.previewIntro}>{introduction}</p>
              ) : (
                <p className={styles.previewPlaceholder}>소개글을 입력하면 여기에 표시됩니다.</p>
              )}
            </div>

            {error && <p className={styles.error}>{error}</p>}

            <div className={styles.actions}>
              <button type="button" className={styles.cancelButton} onClick={() => setStep(2)}>
                이전
              </button>
              <button type="submit" className={styles.submitButton} disabled={isSubmitting}>
                {isSubmitting ? '제출 중...' : '신청하기'}
              </button>
            </div>
          </form>
        )}
      </main>
    </>
  )
}
