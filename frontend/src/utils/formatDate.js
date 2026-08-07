// 백엔드 LocalDateTime(타임존 없음) 문자열 -> "YYYY-MM-DD"
export function formatDate(isoString) {
  if (!isoString) return ''
  return isoString.slice(0, 10)
}
