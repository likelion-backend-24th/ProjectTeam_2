import apiClient from './client'

// POST /api/reports (인증 필요)
function createReport({ targetType, targetId, reason, detail }) {
  // targetType: 'POST' | 'COMMENT' | 'STUDY_POST' | 'STUDY_POST_COMMENT' | 'FEEDBACK'
  return apiClient.post('/api/reports', {
    targetType,
    targetId: Number(targetId),
    reason,
    detail: detail?.trim() || undefined,
  })
}

export const reportApi = {
  createReport,
}
