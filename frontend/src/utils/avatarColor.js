const PALETTE = ['#3b82f6', '#ec4899', '#8b5cf6', '#f97316', '#10b981', '#14b8a6', '#f43f5e', '#eab308']

// 닉네임 등 문자열을 시드로 팔레트에서 결정적으로 색을 골라준다 (아바타 배경색용).
export function getAvatarColor(seed = '') {
  let hash = 0
  for (let i = 0; i < seed.length; i += 1) {
    hash = seed.charCodeAt(i) + ((hash << 5) - hash)
  }
  return PALETTE[Math.abs(hash) % PALETTE.length]
}
