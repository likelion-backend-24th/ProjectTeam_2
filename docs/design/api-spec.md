# API명세

## ⚠️ 알려진 이슈 (코드 수정 필요, 문서 반영 보류)

- **비로그인 접근 제한 버그**: `SecurityConfig`의 permitAll 매칭이 `/api/posts`, `/api/studies` **정확히 그 경로만** 허용하고 하위 경로(`/api/posts/{id}`, `/api/studies/{id}` 등 상세 조회)는 커버하지 못함 → 실제로는 비로그인 상세 조회가 막혀 있음. `GET /api/experts`(전문가 공개 목록)도 permitAll 목록에 없어 로그인 필요 상태. auth/config 담당 확인 필요.
- **F-28 구독(Subscription) API 미구현**: `Subscription` 엔티티는 존재하나 이를 다루는 Controller가 없음. 신청/취소/조회 API 전부 미구현 상태.

---

## 0. 공통

- **Base URL**: `/api`
- **인증**: JWT Bearer (`Authorization: Bearer {accessToken}`). refreshToken은 HttpOnly 쿠키.
- **공통 상태 코드**: 200 조회/처리/생성/삭제 성공, 400 입력 검증, 401 인증 실패, 403 권한 없음, 404 없음, 409 상태 충돌
- **공통 에러 포맷**: `{ "success": false, "message": "...", "data": null, "meta": null, "errorCode": "POST_NOT_FOUND" }`
- **페이징 공통 파라미터**: `page`(기본 0), `size`(기본 10)

## 1. 회원/인증

| ID | API | 설명 | 요청 | 응답 | 인증/권한 | 예외 |
| --- | --- | --- | --- | --- | --- | --- |
| F-01 | POST /api/auth/signup | 회원가입 | Body: name, username, password, nickname, termsAgreed | 201 | GUEST | 400 형식오류 · 409 중복(username/nickname) · 400 이메일 미인증 · 400 약관 미동의 |
| F-02 | POST /api/auth/login | 로그인(JWT) | Body: username, password | 200 {accessToken} (refreshToken은 쿠키) | GUEST | 401 불일치 · 404 없음 · 403 정지 · 403 탈퇴 · 403 잠금(5회 실패 시 10분) |
| F-03 | POST /api/auth/kakao · POST /api/auth/google · POST /api/auth/naver | OAuth2 로그인(최초 자동가입) | Body: kakaoAccessToken/googleAccessToken/naverAccessToken | 200 {accessToken} (refreshToken은 쿠키) | GUEST | 401 토큰무효 · 403 정지 · 403 탈퇴 |
| F-03-1🆕 | POST /api/auth/reissue | accessToken 재발급 | Cookie: refreshToken | 200 {accessToken} (refreshToken 갱신) | GUEST | 401 만료/불일치 · 403 정지 · 403 탈퇴 |
| F-03-2🆕 | POST /api/auth/logout | 로그아웃 (refreshToken 폐기) | - | 200 | USER | 401 |
| F-24 | DELETE /api/users/me | 회원 탈퇴 | Body: password | 200✏️ | USER | 401 불일치  |
| F-33🆕 | GET /api/users/me | 내 정보 조회 | - | 200 | USER | 401 |
| F-33-01🆕 | PATCH /api/users/me/nickname | 닉네임 수정 | Body: nickname | 200 | USER | 400 형식오류(2~12자) · 409 중복 |
| F-34🆕 | PATCH /api/users/me/password | 비밀번호 변경 | Body: currentPassword, newPassword, newPasswordConfirm | 200(성공 시 refreshToken 폐기) | USER (소셜 로그인 계정 불가) | 401 현재비번불일치 · 403 소셜전용계정 · 400 확인불일치 |
| F-39 | POST /api/auth/email/send · POST /api/auth/email/verify | 이메일 인증코드 발송·검증 | 발송 Body: email / 검증 Body: email, code | 200 | GUEST | 429 쿨다운(30초) · 429 횟수초과(10분 5회) · 400 코드불일치/만료(5분) |
| F-40 | POST /api/auth/resetpassword | 비밀번호 재설정(이메일 인증 기반) | -Body: username, newPassword | 200(성공 시 refreshToken 폐기) | GUEST(F-39 인증 완료 필요) | 404 계정없음 · 403 소셜전용계정 · 400 이메일미인증 |

## 2. 게시판 (Q&A)

| ID | API | 설명 | 요청 | 응답 | 인증/권한 | 예외 |
| --- | --- | --- | --- | --- | --- | --- |
| F-04 | POST /api/posts | 게시글 등록 | Body: title, content, category | 201 | USER, ADMIN | 401 · 400 title빈값 |
| F-39🆕 | ❌ 미구현 | 게시글/스터디 게시글 이미지 첨부 | Body: image(파일) | - | USER(작성자 본인), ADMIN | 저장방식(S3/로컬)·업로드 API 형태 등 설계 필요 |
| F-05 | GET /api/posts · /me · /{id} | 목록/상세/내 게시글 | Query: page, size, category(선택), keyword(선택)🆕 | 200 Page / 상세 | 전체(비로그인 가능, ⚠️알려진 이슈 참고), /me는 인증 필요 | 404 · /me 401 |
| F-06 | PUT/DELETE /api/posts/{id} | 수정/삭제 | Body: title, content, category | 200 | 본인, ADMIN | 401 · 403 타인 · 404 |
| F-07 | POST /api/posts/{postId}/comments | 댓글 등록 | Body: content | 201 | USER, ADMIN | 401 · 404 · 400 |
| F-08 | PUT/DELETE /api/posts/{postId}/comments/{id} | 댓글 수정/삭제 | Body: content | 200 | 본인, ADMIN | 401 · 403 · 404 |

## 3. 스터디

| ID | API | 설명 | 요청 | 응답 | 인증/권한 | 예외 |
| --- | --- | --- | --- | --- | --- | --- |
| F-09 | POST /api/studies | 스터디 개설 | `data`: title, description, capacity, category(필수), recruit_end(과거 불가)
`images`: 파일 다건(선택, jpg/jpeg/png/gif, 5MB↓) | 201 | USER, ADMIN | 401 · 400 |
| F-09-1 | GET /api/studies/my | 내가 가입된 스터디 목록 | Query: page, size | 200 | USER | 401 |
| F-10/F-29 | GET /api/studies · /{id} | 목록/상세
구독자인 방장이 끌올(F-29-1)하면 상단 노출됨. 끌올 시각(없으면 개설일) 내림차순 정렬 | Query: page, size, keyword | 200 | USER | 403 |
| F-29-1 | PATCH /api/studies/{id}/bump | 스터디 끌올(목록 최상단 노출) |  |  |  |  |
| F-11 | PUT /api/studies/{id} | 수정 | Body: 개설 항목과 동일 | 200 | 방장 | 401 · 403 · 404 |
| F-11-1 | (DELETE 없음) | 스터디 삭제는 사용자용 엔드포인트가 존재하지 않음. 관리자 강제삭제(F-21)만 가능 | - | - | - | - |
| F-12 | POST /api/studies/{id}/members | 스터디 가입(자유가입, 즉시 승인) | - | 200 `{memberId}` | USER (방장 아님, 미가입) | 400 · 409 정원초과 |
| F-12-1 | GET /api/studies/{id}/members | 스터디 멤버 목록 조회 | - | 200 | 전체 | 404 |
| F-13 | POST /api/studies/{id}/posts | 게시판 글 등록 | Body: title, content | 201 | 가입 완료 멤버 | 403 미가입 · 404 |
| F-14 | GET /api/studies/{id}/posts · /{postId} | 게시판 목록/상세 |  | 200 | 가입 완료 멤버 | 403 · 404 |
| F-15 | PUT/DELETE /api/studies/{id}/posts/{postId} | 게시판 글 수정/삭제 | Body: title, content | 200/204 | 본인, ADMIN | 403 · 404 |
| F-15-1 | PATCH .../posts/{postId}/pin | 게시글 고정/고정 해제 | Body: pinned(boolean) | 200 | USER(방장) | 403 |
| F-16 | POST …/posts/{postId}/comments | 게시판 댓글 등록 | Body: content | 201 | 가입 완료 멤버 | 403 · 404 · 400 |
| F-17 | PUT/DELETE …/comments/{commentId} | 게시판 댓글 수정/삭제 | Body: content | 200/204 | 본인, ADMIN | 403 · 404 |
| F-18 | DELETE /api/studies/{id}/members/{memberId} | 멤버 강퇴 | - | 200 | 방장만 | 403 · 404 |
| F-19 | PATCH /api/studies/{id}/leader | 방장 위임 | Body: newLeaderId | 200 | 방장만 | 403 · 400 |
| F-20 | DELETE /api/studies/{id}/leave ✏️ | 스터디 탈퇴
일반/방장 겸용 엔드포인트. 방장이 유일한 멤버면 위임 없이 탈퇴 가능(스터디 자동 삭제 ) | - | 200 | 가입 멤버 | 409 미위임 |
| F-OPT-01 | (미구현) | 가입 승인/거절(선택기능) — 현재 즉시가입 방식이라 미구현 상태 유지 | - | - | - | - |

## 4. 관리자

| ID | API | 설명 | 요청 | 응답 | 인증/권한 | 예외 |
| --- | --- | --- | --- | --- | --- | --- |
| F-21 | DELETE /api/admin/posts/{id} · /comments/{id} · /studies/{id} · /study-posts/{id} · /study-post-comments/{id} | 게시판·스터디 콘텐츠 강제 삭제(소프트 딜리트) | Path: id | 200✏️ | ADMIN | 403 권한없음 · 404 없음 |
| F-22 | GET /api/admin/users · PATCH /api/admin/users/{id}/status | 유저 목록 조회(검색·필터) 및 상태 변경 | 조회 Query: page, size, keyword, role / 변경 Body: status | 조회 200 {users[]} / 변경 200(SUSPENDED 시 refreshToken 폐기) | ADMIN | 403 권한없음 · 404 없음 · 400 WITHDRAWN 임의지정 |
| F-23 | 전 API 공통 | 입력 검증(빈값·길이) | - | - | - | 400 `{field, message}` |

## 5. 전문가

| ID | API | 설명 | 요청 | 응답 | 인증/권한 | 예외 |
| --- | --- | --- | --- | --- | --- | --- |
| F-25 | POST /api/experts/signup | 전문가 신청 / 재신청 | Body: careers(1건 이상 필수), certifications(선택), introduction(선택, 500자 이하) | 201 `{expertId, status:"PENDING"}` | USER | 400 경력 0건·소개글 초과 · 401 · 409 REJECTED 아닌 상태에서 재신청 |
| F-26 | PATCH /api/admin/experts/{id}/approve · /reject | 승인 / 거절 | Body(거절 시): reason | 200 | ADMIN | 403 · 404 · 409 PENDING 아닌 상태 |
| F-27 | GET /api/admin/experts · DELETE /api/admin/experts/{id} | 목록 조회 / 자격 박탈 | Query: status, page, size / Body(박탈 시): reason(선택) | 200 Page / 204 | ADMIN | 403 · 404 · 409 APPROVED 아닌 대상 박탈 |
| F-32 | GET /api/experts | 승인된 전문가 공개 목록 | Query: page, size(기본 12) | 200 `data:[{expertId, nickname, career}]` + `meta.pagination` | 전체 공개 | - |
| F-35 | GET /api/experts/{id} | 전문가 프로필 상세 | Path: id | 200 `{expertId, nickname, introduction, careers[], certifications[]}` | 전체 공개 | 404 없음·APPROVED 아님 |
| F-36 | GET /api/experts/me | 본인 신청 현황 조회 | - | 200 `{status, reason}` | USER(신청자 본인) | 401 · 404 신청 이력 없음 |
| F-37 | PATCH /api/experts/me | 신청서 수정 (PENDING 상태) | Body: careers, certifications, introduction | 200 | USER(신청자 본인) | 401 · 409 PENDING 아닌 상태 |
- 박탈은 `status`를 REJECTED로 재사용하고 `role`을 USER로 원복하는 방식. **박탈된 전문가도 즉시 재신청 가능**하며, 이는 소명 가능한 경우를 열어두기 위한 의도된 정책이다. 실질적 통제는 ADMIN 심사 단계에서 이뤄진다.

## 6-0. 유료 구독 / 전문가 피드백

| ID | API | 설명 | 요청 | 응답 | 인증/권한 | 예외 |
| --- | --- | --- | --- | --- | --- | --- |
| F-28 | POST /api/payments/subscriptions/billing-key · DELETE /api/subscriptions · GET /api/subscriptions/me | 구독 신청 / 해지 / 조회 | - (카드 등록 선행 필요) | 201 / 200 `{status, expiredAt, autoRenew, remainingRetryCount}` | USER | 401 · 404 카드 없음·구독 없음 · 409 이미 구독 중·결제 검증 실패 |
| F-30 | POST /api/feedbacks · GET /{id} · POST/GET /{id}/messages | 전문가 1:1 문의(스레드) | multipart: data(JSON) + images[] | 201 / 200 | USER(구독자), EXPERT(담당자) | 403 비구독·미승인 전문가·참여자 아님 · 404 · 409 종료됨·구독만료·탈퇴회원 |
| F-30-1 | GET /api/feedbacks/me | 요청자용 내 문의 목록 | Query: page, size(기본 20), sort | 200 `{feedbacks:[{feedbackId, expertName, topic, status, answeredAt, closedBy, closedAt}]}` + `meta.pagination` | USER(구독자) | 401 |
| F-30-2 | GET /api/feedbacks/expert | 전문가용 받은 문의 목록 | Query: page, size(기본 20), sort | 200 + `meta.pagination` | EXPERT | 401 |
| F-31 | (F-09/F-12와 동일) | 구독자 스터디 개설·참여 무제한 | - | - | USER(구독자) | - |
| F-58 | PATCH /api/feedbacks/{id}/close | 스레드 종료 | - | 200 | USER(요청자 본인) | 403 요청자 아님 · 404 · 409 이미 종료 |
| F-76 | DELETE /api/feedbacks/{id} | 스레드 삭제 (소프트 삭제) | - | 200 | USER(요청자 본인) | 403 요청자 아님(전문가 포함) · 404 · 409 신고 접수된 스레드 |
| F-72 | GET /api/admin/feedbacks/{id} · /{id}/messages | 관리자 스레드 열람 | Path: id | 200 | ADMIN (신고 이력 있는 스레드만) | 403 신고 이력 없음·ADMIN 아님 · 404 |
| F-71 | POST /api/reports | 스레드 신고 | Body: targetType="FEEDBACK", targetId, reason, detail | 201 | USER, EXPERT | 403 · 404 · 409 중복 신고 |

---

## 6-1. 결제수단 (F-60, F-64, F-65)

| ID | API | 설명 | 요청 | 응답 | 인증/권한 | 예외 |
| --- | --- | --- | --- | --- | --- | --- |
| F-60-1 | POST /api/billing-keys/prepare | 카드 등록 준비 | - | 200 `{storeId, channelKey, issueId, customerId}` | USER | 401 · 409 탈퇴·정지 계정 |
| F-60-2 | POST /api/billing-keys/complete | 카드 등록 완료 | Body: billingKey, billingIssueToken(수동 승인형) | 200 | USER | 401 · 409 발급 검증 실패 · 502 PG 통신 오류 |
| F-64-1 | GET /api/billing-keys/me | 내 카드 조회 | - | 200 `{cardCompany, cardNumberMasked, issuedAt}` | USER | 401 · 404 카드 없음 |
| F-64-2 | DELETE /api/billing-keys | 카드 삭제 | - | 200 | USER | 401 · 404 카드 없음 |

---

## 6-2. 구독 (F-28, F-63)

| ID | API | 설명 | 요청 | 응답 | 인증/권한 | 예외 |
| --- | --- | --- | --- | --- | --- | --- |
| F-28-1 | POST /api/payments/subscriptions/billing-key | 구독 시작 (첫 달 즉시 청구) | - (카드 등록 선행 필요) | 201 | USER | 401 · 404 카드 없음 · 409 이미 구독 중·결제 검증 실패 |
| F-28-2 | GET /api/subscriptions/me | 내 구독 조회 | - | 200 `{status, startedAt, expiredAt, autoRenew, remainingRetryCount}` | USER | 401 · 404 구독 없음 |
| F-28-3 | DELETE /api/subscriptions | 구독 해지 | - | 200 | USER | 401 · 404 구독 없음 |
| F-63 | POST /api/payments/subscriptions/resume | 자동 갱신 재개 | - | 200 | USER | 401 · 404 카드 없음·구독 없음 · 409 이미 자동갱신 중 |

---

## 6-3. 정기결제 실행 (F-61, F-62)

| ID | API | 설명 | 요청 | 응답 | 인증/권한 | 예외 |
| --- | --- | --- | --- | --- | --- | --- |
| F-62-1 | POST /api/payments/subscriptions/retry | 수동 재시도 | - | 200 `{구독 정보}` | USER | 401 · 404 카드 없음 · 409 PAST_DUE 아님 · **429 60초 쿨다운** |
| F-61 | (API 없음 — 배치) | 자동 갱신 | - | - | 시스템 | - |

---

## 6-4. 결제 검증·웹훅 (F-66, F-28)

| ID | API | 설명 | 요청 | 응답 | 인증/권한 | 예외 |
| --- | --- | --- | --- | --- | --- | --- |
| F-28-4 | POST /api/payments/webhook | PortOne 결제 결과 수신 | Header: `webhook-id`, `webhook-timestamp`, `webhook-signature` / Body: raw JSON | 200 | **사용자 인증 없음** (PortOne 서명으로 검증) | 400 서명 검증 실패 |
| F-66 | (API 없음 — 내부 로직) | 결제 검증 | - | - | 시스템 | 409 검증 실패 |

---

## 6-5. 환불 (F-67)

| ID | API | 설명 |
| --- | --- | --- |
| F-67 | **API 없음 (의도적 부재)** | 결제 취소(환불) API를 두지 않는다 |

구독은 결제 즉시 유료 기능을 이용할 수 있어 일할 환불이 성립하지 않는다. 취소 요청은 해지(F-28-3)로 처리해 다음 회차 결제만 중단하고, 이미 결제된 기간은 만료일까지 이용 가능하다. 예외 상황은 PortOne 관리자 콘솔에서 수동 처리한다.

---