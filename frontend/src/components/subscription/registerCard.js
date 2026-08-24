import { requestIssueBillingKey } from '@portone/browser-sdk/v2'
import { billingKeyApi } from '../../api'

/**
 * PortOne 카드 등록 창을 띄우고, 발급된 빌링키를 서버에 등록한다.
 * 카드 정보는 PortOne 창에만 입력되며 우리 서버·프론트를 거치지 않는다.
 *
 * 구독 시작(PaymentModal)과 마이페이지의 카드 등록·변경이 이 함수를 공유한다.
 * 구독 시작 여부는 호출하는 쪽이 결정한다 — 여기서는 카드만 등록한다.
 * 유저당 활성 카드는 한 장이라, 이미 카드가 있으면 서버가 기존 카드를 교체한다.
 *
 * @throws {Error} 사용자가 등록 창을 닫았거나 발급에 실패한 경우
 */
export async function registerCard() {
  const { data: prepareRes } = await billingKeyApi.prepareIssue()
  const { storeId, channelKey, issueId, customerId } = prepareRes.data

  const issued = await requestIssueBillingKey({
    storeId,
    channelKey,
    billingKeyMethod: 'CARD',
    issueId,
    issueName: 'prep2gether 정기결제 카드 등록',
    customer: { customerId },
  })

  if (!issued || issued.code) {
    throw new Error(issued?.message ?? '카드 등록이 취소되었어요.')
  }

  // 채널이 수동 승인이면 billingKey는 'NEEDS_CONFIRMATION' 자리표시자로 오고,
  // billingIssueToken으로 서버가 PortOne에 발급을 확정해야 진짜 빌링키를 받을 수 있다.
  await billingKeyApi.completeIssue(issued.billingKey, issued.billingIssueToken)
}
