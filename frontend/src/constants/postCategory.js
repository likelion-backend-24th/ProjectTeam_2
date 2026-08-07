// 백엔드 PostCategory enum과 1:1로 맞춘 값. (org.example.backend.post.entity.PostCategory)
export const POST_CATEGORIES = [
  { value: 'JOB_INFO', label: '취업정보', color: '#60a5fa', bg: 'rgba(59, 130, 246, 0.16)' },
  { value: 'INTERVIEW_REVIEW', label: '면접후기', color: '#c084fc', bg: 'rgba(168, 85, 247, 0.16)' },
  { value: 'RESUME', label: '자소서', color: '#34d399', bg: 'rgba(52, 211, 153, 0.16)' },
  { value: 'FREE', label: '자유', color: '#fb923c', bg: 'rgba(251, 146, 60, 0.16)' },
]

export function getPostCategoryMeta(value) {
  return POST_CATEGORIES.find((category) => category.value === value)
}
