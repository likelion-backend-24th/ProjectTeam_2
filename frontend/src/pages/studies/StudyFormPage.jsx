import { ArrowRight, ChevronLeft } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { studyApi } from '../../api'
import SiteHeader from '../../components/common/SiteHeader'
import CategoryPicker from '../../components/posts/CategoryPicker'
import { STUDY_CATEGORIES } from '../../constants/studyCategory'
import { useAuth } from '../../context/AuthContext'
import styles from './StudyFormPage.module.css'

const TITLE_MAX_LENGTH = 100
const DESCRIPTION_MAX_LENGTH = 2000

function today() {
  return new Date().toISOString().slice(0, 10)
}

export default function StudyFormPage() {
  const navigate = useNavigate()
  const { studyId } = useParams()
  const { user } = useAuth()
  const isEditMode = Boolean(studyId)

  const [step, setStep] = useState(1)
  const [isLoading, setIsLoading] = useState(isEditMode)
  const [loadError, setLoadError] = useState('')

  const [title, setTitle] = useState('')
  const [category, setCategory] = useState('')
  const [description, setDescription] = useState('')

  const [capacity, setCapacity] = useState('')
  const [recruitEnd, setRecruitEnd] = useState('')
  // 모집 시작일은 사용자에게 입력받지 않고 내부적으로만 들고 있는다.
  // 개설 시엔 오늘 날짜로, 수정 시엔 기존 값을 그대로 유지해서 백엔드에 함께 보낸다
  // (StudyRequest/StudyUpdateRequest가 recruitStart를 필수로 요구하기 때문).
  const recruitStartRef = useRef(today())

  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!isEditMode) return
    let ignore = false

    studyApi
      .getStudyById(studyId)
      .then(({ data }) => {
        if (ignore) return
        const study = data.data
        if (user && study.leaderId !== user.id) {
          setLoadError('방장만 스터디를 수정할 수 있어요.')
          return
        }
        setTitle(study.title)
        setCategory(study.category)
        setDescription(study.description ?? '')
        setCapacity(String(study.capacity))
        setRecruitEnd(study.recruitEnd)
        recruitStartRef.current = study.recruitStart
      })
      .catch(() => {
        if (!ignore) setLoadError('스터디 정보를 불러오지 못했습니다.')
      })
      .finally(() => {
        if (!ignore) setIsLoading(false)
      })

    return () => {
      ignore = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [studyId, isEditMode])

  function handleNextStep(event) {
    event.preventDefault()
    if (!title.trim()) {
      setError('스터디 이름을 입력해주세요.')
      return
    }
    if (!category) {
      setError('카테고리를 선택해주세요.')
      return
    }
    if (!description.trim()) {
      setError('스터디 소개를 입력해주세요.')
      return
    }
    setError('')
    setStep(2)
  }

  async function handleSubmit(event) {
    event.preventDefault()

    if (!capacity || Number(capacity) < 1) {
      setError('모집 인원을 입력해주세요.')
      return
    }
    if (!recruitEnd) {
      setError('모집 마감일을 입력해주세요.')
      return
    }
    if (recruitEnd < recruitStartRef.current) {
      setError('모집 마감일이 너무 이릅니다.')
      return
    }

    const payload = {
      title,
      description,
      capacity: Number(capacity),
      recruitStart: recruitStartRef.current,
      recruitEnd,
      category,
    }

    setError('')
    setIsSubmitting(true)
    try {
      if (isEditMode) {
        await studyApi.updateStudy(studyId, payload)
        navigate(`/studies/${studyId}`)
      } else {
        const { data } = await studyApi.createStudy(payload)
        navigate(`/studies/${data.data.id}`)
      }
    } catch (err) {
      setError(err.response?.data?.message ?? (isEditMode ? '스터디 수정에 실패했습니다.' : '스터디 개설에 실패했습니다.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  const backTo = isEditMode ? `/studies/${studyId}` : '/studies'

  return (
    <>
      <SiteHeader />
      <main className={styles.main}>
        <Link to={backTo} className={styles.breadcrumb}>
          <ChevronLeft size={16} />
          {isEditMode ? '스터디로 돌아가기' : '스터디 목록'}
        </Link>

        <p className={styles.eyebrow}>STUDY GROUP</p>
        <h1 className={styles.title}>{isEditMode ? '스터디 수정' : '스터디 개설'}</h1>

        {isLoading ? (
          <p className={styles.counter}>불러오는 중...</p>
        ) : loadError ? (
          <p className={styles.error}>{loadError}</p>
        ) : (
          <>
            <div className={styles.steps}>
              <div className={styles.stepItem}>
                <span className={`${styles.stepNumber} ${step >= 1 ? styles.stepNumberActive : ''}`}>1</span>
                <span className={step === 1 ? styles.stepLabelActive : styles.stepLabel}>기본 정보</span>
              </div>
              <div className={styles.stepDivider} />
              <div className={styles.stepItem}>
                <span className={`${styles.stepNumber} ${step >= 2 ? styles.stepNumberActive : ''}`}>2</span>
                <span className={step === 2 ? styles.stepLabelActive : styles.stepLabel}>세부 설정</span>
              </div>
            </div>

            {step === 1 && (
              <form onSubmit={handleNextStep}>
                <div className={styles.field}>
                  <label className={styles.label} htmlFor="title">
                    스터디 이름
                    <span className={styles.required}>*</span>
                  </label>
                  <input
                    id="title"
                    type="text"
                    className={styles.input}
                    placeholder="예: 알고리즘 스터디 | 코테 전문 (골드~플래)"
                    maxLength={TITLE_MAX_LENGTH}
                    value={title}
                    onChange={(event) => setTitle(event.target.value)}
                  />
                  <p className={styles.counter}>
                    {title.length}/{TITLE_MAX_LENGTH}
                  </p>
                </div>

                <div className={styles.field}>
                  <label className={styles.label}>
                    카테고리
                    <span className={styles.required}>*</span>
                  </label>
                  <CategoryPicker value={category} onChange={setCategory} categories={STUDY_CATEGORIES} />
                </div>

                <div className={styles.field}>
                  <label className={styles.label} htmlFor="description">
                    스터디 소개
                    <span className={styles.required}>*</span>
                  </label>
                  <textarea
                    id="description"
                    className={styles.textarea}
                    placeholder="어떤 스터디인지, 어떻게 진행하는지, 어떤 분을 찾는지 알려주세요."
                    maxLength={DESCRIPTION_MAX_LENGTH}
                    value={description}
                    onChange={(event) => setDescription(event.target.value)}
                  />
                  <p className={styles.counter}>
                    {description.length}/{DESCRIPTION_MAX_LENGTH}
                  </p>
                </div>

                {error && <p className={styles.error}>{error}</p>}

                <button type="submit" className={styles.submitButton}>
                  다음 단계
                  <ArrowRight size={16} />
                </button>
              </form>
            )}

            {step === 2 && (
              <form onSubmit={handleSubmit}>
                <div className={styles.field}>
                  <label className={styles.label} htmlFor="capacity">
                    모집 인원
                    <span className={styles.required}>*</span>
                  </label>
                  <input
                    id="capacity"
                    type="number"
                    min={1}
                    className={styles.input}
                    placeholder="예: 6"
                    value={capacity}
                    onChange={(event) => setCapacity(event.target.value)}
                  />
                </div>

                <div className={styles.field}>
                  <label className={styles.label} htmlFor="recruitEnd">
                    모집 마감일
                    <span className={styles.required}>*</span>
                  </label>
                  <input
                    id="recruitEnd"
                    type="date"
                    className={styles.input}
                    value={recruitEnd}
                    onChange={(event) => setRecruitEnd(event.target.value)}
                  />
                </div>

                {error && <p className={styles.error}>{error}</p>}

                <div className={styles.actions}>
                  <button type="button" className={styles.cancelButton} onClick={() => setStep(1)}>
                    이전
                  </button>
                  <button type="submit" className={styles.submitButton} disabled={isSubmitting}>
                    {isSubmitting ? '저장 중...' : isEditMode ? '수정 완료' : '스터디 개설하기'}
                  </button>
                </div>
              </form>
            )}
          </>
        )}
      </main>
    </>
  )
}
