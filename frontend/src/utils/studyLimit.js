import { studyApi } from '../api'

// 비구독 회원이 개설/참여할 수 있는 최대 스터디 수. 백엔드 StudyErrorCode.STUDY_JOIN_LIMIT_EXCEEDED와 동일한 기준.
export const FREE_STUDY_LIMIT = 2

// 스터디 개설/가입 요청을 보내기 전에 무료 회원의 정원 초과 여부를 먼저 확인한다.
// (백엔드 에러 메시지가 아직 "이미 가입한 스터디"로 뭉뚱그려져 있어, 프론트에서 미리 걸러
// 구독 플랜 페이지로 안내하는 편이 사용자에게 더 명확하다.)
export async function isOverFreeStudyLimit(user) {
  if (!user || user.subscribed) return false
  const { data } = await studyApi.getMyStudies({ size: 1 })
  return data.meta.pagination.totalItems >= FREE_STUDY_LIMIT
}
