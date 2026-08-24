// PortOne이 내려주는 마스킹 카드번호(예: "54287967****055*")에서 뒤 4자리만 노출한다.
// 카드번호 전체 자릿수를 화면에 보여줄 이유가 없어 마지막 4자리만 표시한다(실무 관행).
export function formatCardTail(maskedNumber) {
  if (!maskedNumber) return ''
  return maskedNumber.slice(-4)
}

// "신한카드 055*" 처럼 카드사명과 뒤 4자리를 함께 표시한다. 카드사명이 없으면 "카드"로 폴백.
export function formatCardLabel(cardName, maskedNumber) {
  const name = cardName ?? '카드'
  const tail = formatCardTail(maskedNumber)
  return tail ? `${name} ${tail}` : name
}
