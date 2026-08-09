// 백엔드 StudyCategory enum과 1:1로 맞춘 값. (org.example.backend.study.entity.StudyCategory)
export const STUDY_CATEGORIES = [
  { value: 'IT_DEVELOPMENT', label: 'IT개발', color: '#60a5fa', bg: 'rgba(59, 130, 246, 0.16)' },
  { value: 'LANGUAGE', label: '언어', color: '#34d399', bg: 'rgba(52, 211, 153, 0.16)' },
  { value: 'CERTIFICATE', label: '자격증', color: '#fb923c', bg: 'rgba(251, 146, 60, 0.16)' },
  { value: 'JOB_PREP', label: '취업준비', color: '#c084fc', bg: 'rgba(168, 85, 247, 0.16)' },
  { value: 'ETC', label: '기타', color: '#9aa5a1', bg: 'rgba(154, 165, 161, 0.16)' },
]

export function getStudyCategoryMeta(value) {
  return STUDY_CATEGORIES.find((category) => category.value === value)
}
