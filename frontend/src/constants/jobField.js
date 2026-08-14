// 백엔드 JobField enum과 1:1로 맞춘 값. (org.example.backend.expert.entity.JobField)
export const JOB_FIELDS = [
  { value: 'IT_DEVELOPMENT', label: 'IT/개발' },
  { value: 'DESIGN_UX', label: '디자인/UX' },
  { value: 'MARKETING', label: '마케팅' },
  { value: 'MANAGEMENT_STRATEGY', label: '경영/전략' },
  { value: 'FINANCE_ACCOUNTING', label: '금융/회계' },
  { value: 'SALES_CS', label: '영업/CS' },
  { value: 'ETC', label: '기타' },
]

export function getJobFieldLabel(value) {
  return JOB_FIELDS.find((field) => field.value === value)?.label ?? value
}
