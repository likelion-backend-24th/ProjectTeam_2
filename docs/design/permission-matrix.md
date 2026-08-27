# 권한매트릭스

# 1. 역할 정의

| 역할 | 설명 |
| --- | --- |
| GUEST(비로그인) | 토큰없음 |
| USER(일반회원) | 일반회원 |
| USER(구독자) | 구독 상태 ACTIVE인 회원 |
| EXPERT(전문가) | 승인된 전문가 |
| ADMIN(관리자) | 전체 관리 권한 |

# 2. API x 역할 표

## 2.1 회원/인증(F-01~03)

| API | GUEST | USER | ADMIN |
| --- | --- | --- | --- |
| `POST /api/auth/signup` (회원가입) | O | ㅡ | ㅡ |
| `POST /api/auth/login` (로그인) | O | ㅡ | ㅡ |
| `POST /api/auth/kakao` · `/google` · `/naver` (소셜 로그인) | O | ㅡ | ㅡ |
| `POST /api/auth/reissue` (토큰 재발급) | O(쿠키 필요) | ㅡ | ㅡ |
| `POST /api/auth/logout` (로그아웃) | 401 | O | O |

## 2.2 Q&A 게시판(F-04~08)

| API | GUEST | USER(본인) | USER(타인) | ADMIN |
| --- | --- | --- | --- | --- |
| `GET /api/posts` (목록) | O | O | O | O |
| `GET /api/posts/{id}` (상세) | **401** ⚠️ | O | O | O |
| `POST /api/posts` (등록, 이미지 첨부 가능) | 401 | O | ㅡ | O |
| `PUT /api/posts/{id}` (수정) | 401 | O | 403 | **403** ⚠️ |
| `DELETE /api/posts/{id}` (삭제) | 401 | O | 403 | **403** ⚠️ |
| `POST /.../comments` (댓글 등록) | 401 | O | O | O |
| `PUT /.../comments/{id}` (댓글 수정) | 401 | O | 403 | **403** ⚠️ |
| `DELETE /.../comments/{id}` (댓글 삭제) | 401 | O | 403 | **403** ⚠️ |
- ⚠️ **상세 조회 401**: `SecurityConfig`가 `GET /api/posts`(목록)만 `permitAll`이고 `/{id}`는 인증 필요 상태. 명세(F-05)는 비로그인 열람 허용이므로 **미해결 이슈**(3장 검증 위치 참고).
- ⚠️ **수정/삭제 ADMIN 403**: 정규 경로(`PostService`/`CommentService`)는 `isOwner()`만 검사하고 **ADMIN 예외가 없음**. 관리자는 **수정 불가**(전용 엔드포인트 없음), 삭제는 **2.4의 `/api/admin/posts/{id}` 등 강제삭제(소프트딜리트)** 로만 처리.

## 2.3 스터디(F-09~20)

| API | GUEST | USER | USER(방장) | USER(비방장/비멤버) | ADMIN |
| --- | --- | --- | --- | --- | --- |
| `POST /api/studies` (개설) | 401 | O | ㅡ | ㅡ | O |
| `GET /api/studies` (목록) | O | O | O | O | O |
| `GET /api/studies/my` (내가 가입한 스터디) | 401 | O | O | O | O |
| `GET /api/studies/{id}` (상세) | **401** ⚠️ | O | O | O | O |
| `PUT /api/studies/{id}` (수정) | 401 | ㅡ | O | 403 | 403* |
| `PATCH /api/studies/{id}/bump` (모집글 끌어올리기) 🆕 | 401 | ㅡ | O(구독자만) | 403 | 403* |
| `POST /api/studies/{id}/members` (가입) | 401 | O | 400(본인) | ㅡ | ㅡ |
| `GET /api/studies/{id}/members` (멤버 목록) | O | O | O | O | O |
| `GET/POST /.../posts` (스터디 게시판, 이미지 첨부 가능) | 401 | 403(미가입) | O | 403(미가입) | O |
| `PUT/DELETE /.../posts/{postId}` (스터디 게시글 수정/삭제) | 401 | 403(비작성자) | O(작성자) | ㅡ | O |
| `POST /.../posts/{postId}/comments` (스터디 댓글 등록) | 401 | 403(미가입) | O | ㅡ | O |
| `PUT/DELETE /.../comments/{commentId}` (스터디 댓글 수정/삭제) | 401 | 403(비작성자) | O(작성자) | ㅡ | O |
| `DELETE /api/studies/{id}/members/{userId}` (강퇴) | 401 | ㅡ | O | 403 | ㅡ |
| `PATCH /api/studies/{id}/leader` (위임) | 401 | ㅡ | O | 403 | ㅡ |
| `DELETE /api/studies/{id}/leave` (탈퇴) | 401 | O | O(위임 완료 시) | ㅡ | ㅡ |
- ⚠️ **상세 조회 401**: 게시글과 동일하게 `GET /api/studies`(목록)만 `permitAll`, `/{id}`는 인증 필요. 명세(F-10) 위반 미해결.
- `PUT /api/studies/{id}` 는 현재 방장 여부만 검증하고 ADMIN 예외가 없어, ADMIN 계정으로 호출해도 방장이 아니면 403이 발생함(아래 검증 위치 참고).
- 🆕 `PATCH /api/studies/{id}/bump` 는 **방장 + 구독자**만 가능. 비구독 방장은 403(`STUDY_BUMP_SUBSCRIBER_ONLY`), 모집 마감/쿨다운 시에도 차단.
- ⚠️ **사용자용 스터디 삭제 API는 없음**: `StudyService.deleteStudy()`는 구현돼 있으나 컨트롤러에 연결된 엔드포인트가 없음(죽은 코드). 방장이 유일 멤버로 탈퇴(`DELETE /leave`)할 때만 자동 삭제되고, 그 외 삭제는 2.4 관리자 강제삭제로만 가능.

## 2.4 관리자(F-21~22)

| API | GUEST | USER | ADMIN |
| --- | --- | --- | --- |
| `GET /api/admin/users` (유저 목록, 닉네임/이메일 검색·role 필터) | 401 | 403 | O |
| `PATCH /api/admin/users/{id}/status` (상태변경 ACTIVE/SUSPENDED) | 401 | 403 | O |
| `PATCH /api/admin/users/{id}/role` (권한 변경) 🆕 | 401 | 403 | **❌ 미구현** (엔드포인트 없음) |
| `DELETE /api/admin/posts/{id}` · `/comments/{id}` · `/studies/{id}` · `/study-posts/{id}` · `/study-post-comments/{id}` (강제삭제/소프트딜리트) | 401 | 403 | O |
| `GET /api/admin/studies` · `/subscriptions` · `/payments` · `/posts` (관리 목록 조회) 🆕 | 401 | 403 | O |
| `GET /api/admin/reports` (신고 목록, status 필터) 🆕 | 401 | 403 | O |
| `PATCH /api/admin/reports/{id}/resolve` (삭제 처리) · `/reject` (반려) 🆕 | 401 | 403 | O |
- USER→ADMIN 승격 API가 없음. `role` 변경은 미구현이라 관리자 계정이 필요하면 DB에서 직접 role을 수정해야 함 — 운영 전 별도 기능 추가 검토 필요.

## 2.4-1 이미지 첨부 (F-39)

이미지 첨부는 **별도 엔드포인트가 아니라 각 생성 API에 `multipart/form-data`로 통합**되어 있습니다.

| 경로 | 형식 | 비고 |
| --- | --- | --- |
| `POST /api/posts` | `data`(JSON) + `images[]` | 게시글 등록 시 첨부, `PostImage` 연결 |
| `POST /api/studies/{id}/posts` | `data`(JSON) + `images[]` | 스터디 게시글 등록 시 첨부, `StudyPostImage` 연결 |
| `POST /api/feedbacks`, `POST /.../{id}/messages` | `data`(JSON) + `images[]` | 상담 개설·메시지 첨부(2.8 참고) |
- 공통 `FileStorageService`(S3)로 업로드. 원본이 소프트 삭제되면 연관 이미지도 함께 정리.
- ⚠️ **수정 시 이미지 교체는 미지원**(현재 등록 시에만 첨부). 게시글/스터디게시글 `PUT`은 텍스트만 갱신.

## 2.5 공통(F-24 회원탈퇴 외)

| API | GUEST | USER | ADMIN |
| --- | --- | --- | --- |
| `GET /api/users/me` (내 정보 조회) | 401 | O | O |
| `PATCH /api/users/me/nickname` (닉네임 수정) | 401 | O | O |
| `PATCH /api/users/me/password` (비밀번호 변경) | 401 | O(소셜 로그인 계정 제외) | O |
| `DELETE /api/users/me` (탈퇴) | 401 | O* | O |

💡 방장으로 있는 스터디가 있으면 회원 탈퇴가 막힘(먼저 위임 또는 스터디 탈퇴로 정리 필요).
회원 탈퇴 시: 진행 중 상담 스레드가 `REQUESTER_WITHDRAWN`으로 일괄 종료되고 구독 자동갱신이 해지됨(F-75).

## 2.5-1 알림 (신규 추가)

| API | GUEST | USER | ADMIN |
| --- | --- | --- | --- |
| `GET /api/notifications` (내 알림 목록) | 401 | O | O |
| `GET /api/notifications/count` (안 읽은 개수) | 401 | O | O |
| `PATCH /api/notifications/{id}/read` (읽음 처리) | 401 | O(본인 알림만) | O |
- 알림 수신자 본인만 조회/읽음 처리. 상담 답변 등 이벤트 발생 시 생성.

## 2.5-2 신고 — 공통 (신규 추가)

| API | GUEST | USER | ADMIN |
| --- | --- | --- | --- |
| `POST /api/reports` (신고 등록) | 401 | O | O |
- `ReportTargetType` = **POST · COMMENT · STUDY_POST · STUDY_POST_COMMENT · FEEDBACK** 5종에 공통 사용.
- 본인 대상·중복 신고는 서비스 계층에서 차단(409/400). 처리는 관리자(2.4 신고관리).

## 2.6 전문가 (F-25~27, F-32)

| API | GUEST | USER | EXPERT | ADMIN |
| --- | --- | --- | --- | --- |
| `POST /api/experts/signup` (신청/재신청) | 401 | O | ㅡ | ㅡ |
| `GET /api/experts` (공개 목록) | O | O | O | O |
| `GET /api/experts/{id}` (프로필 상세) | O | O | O | O |
| `GET /api/experts/me` (본인 신청 현황) | 401 | O(신청자 본인) | O | - |
| `PATCH /api/experts/me` (신청서 수정) | 401 | O(PENDING일 때만) | - | - |
| `PATCH /api/admin/experts/{id}/approve` · `/reject` | 401 | 403 | 403 | O |
| `GET /api/admin/experts` (목록 관리) | 401 | 403 | 403 | O |
| `DELETE /api/admin/experts/{id}` (자격 박탈) | 401 | 403 | 403 | O |

> 전문가 공개 목록·상세(`GET /api/experts`, `/api/experts/{id}`)는 게시글/스터디와 달리 **상세까지 `permitAll`** 이라 비로그인 열람이 정상 동작함.
> 

**상태별 조건**

| 상황 | 결과 |
| --- | --- |
| PENDING 상태에서 재신청 | 409 |
| APPROVED 상태에서 신청서 수정 | 409 |
| PENDING 아닌 대상 승인·거절 | 409 |
| APPROVED 아닌 대상 박탈 | 409 |
| 경력 0건으로 신청 | 400 |
| 소개글 500자 초과 | 400 |

> **정책** — 박탈된 전문가도 즉시 재신청 가능. `status`를 REJECTED로 재사용하므로 재신청 경로가 그대로 적용된다. 소명 가능한 경우를 열어두기 위한 의도이며, 실질적 통제는 ADMIN 심사에서 이뤄진다.
> 

> **⚠️ 오픈 이슈** — 전문가 심사 탭에 박탈 버튼이 없다. 신고 관리 탭에서 신고된 상담의 담당 전문가를 박탈하는 경로로만 호출된다. 관리자가 신고 없이 임의 박탈할 UI는 미구현.
> 

## 2.7 구독 (F-28~29, F-31, F-60~67)

| API | GUEST | USER | USER(구독자) | ADMIN |
| --- | --- | --- | --- | --- |
| `POST /api/billing-keys/prepare` (카드 등록 준비) | 401 | O | O | O |
| `POST /api/billing-keys/complete` (카드 등록 완료) | 401 | O | O | O |
| `GET /api/billing-keys/me` (내 카드 조회) | 401 | O | O | O |
| `DELETE /api/billing-keys` (카드 삭제) | 401 | O | O | O |
| `POST /api/payments/subscriptions/billing-key` (구독 시작) | 401 | O | 409(이미 구독 중) | - |
| `DELETE /api/subscriptions` (해지) | 401 | 404 | O | - |
| `GET /api/subscriptions/me` (조회) | 401 | 404 | O | - |
| `POST /api/payments/subscriptions/resume` (자동갱신 재개) | 401 | 404 | O(해지 예약 상태만) | - |
| `POST /api/payments/subscriptions/retry` (수동 재시도) | 401 | 404 | O(PAST_DUE만) | - |
| `POST /api/payments/webhook` (PortOne 웹훅) | 인증 없음 (서명 검증) | - | - | - |
| 스터디 개설/참여 2개 제한(F-31) | - | 제한 O(2개) | 제한 X | - |

**상태별 조건**

| 상황 | 결과 |
| --- | --- |
| 이미 구독 중(ACTIVE/PAST_DUE) 재신청 | 409 |
| 등록된 카드 없이 구독 시작·재개 | 404 |
| 결제 검증 6항목 중 하나라도 불일치 | 409 |
| 이미 자동갱신 중인데 재개 요청 | 409 |
| PAST_DUE 아닌 상태에서 수동 재시도 | 409 |
| 수동 재시도 60초 이내 재호출 | 429 |
| 탈퇴·정지 계정의 결제 시도 | 409 |
| 웹훅 서명 검증 실패 | 400 |
| 동일 webhook-id 재수신 | 200 (스킵) |
| PortOne 통신 오류 | 502 |

> PortOne V2 빌링키 기반 정기결제로 구현되어 있다. 구독 시작은 카드 등록(F-60)이 선행되어야 하며, 결제 성공 여부는 PG 응답이 아닌 단건 재조회 결과(F-66)로 판정한다. 해지는 상태 전이가 아니라 `autoRenew=false`로 표현되어 만료일까지 이용 가능하다. 자동 갱신은 PortOne 예약 API가 아닌 애플리케이션 스케줄러(매일 04:00)가 담당한다.
> 

> **⚠️ 오픈 이슈 (확인됨, 미해결)** — `POST /api/subscriptions`는 MVP2 Mock 시절 경로가 그대로 남아 있다(`SubscriptionController`에 여전히 존재). 프론트 호출 함수는 제거됐으나 **백엔드 엔드포인트는 열려 있어**, 로그인만 하면 결제 없이 구독 상태를 켤 수 있다. 제거 필요.
> 

## 2.8 전문가 1:1 상담 (F-30, F-57~59, F-69~72, F-76)

| API | GUEST | USER(비구독) | USER(구독, 본인 스레드) | USER(구독, 타인 스레드) | EXPERT(담당) | EXPERT(비담당) |
| --- | --- | --- | --- | --- | --- | --- |
| `POST /api/feedbacks` (개설) | 401 | 403 | O | ㅡ | ㅡ | ㅡ |
| `GET /api/feedbacks/{id}` (조회) | 401 | 403 | O | 403 | O | 403 |
| `POST /.../{id}/messages` (메시지) | 401 | 403 | O | 403 | O | 403 |
| `GET /.../{id}/messages` (메시지 목록) | 401 | 403 | O | 403 | O | ㅡ |
| `PATCH /.../{id}/close` (종료) | 401 | 403 | O | 403 | 403 | O |
| `DELETE /api/feedbacks/{id}` (삭제) | 401 | 403 | O | 403 | 403 | ㅡ |
| `GET /api/feedbacks/me` (내 문의 목록) | 401 | 403 | O | O | - | ㅡ |
| `GET /api/feedbacks/expert` (받은 문의 목록) | 401 | - | - | - | O | ㅡ |
| `POST /api/reports` (스레드 신고) | 401 | O | O | O | O | ㅡ |
| `GET /api/admin/feedbacks/{id}` (관리자 열람) | 401 | 403 | 403 | 403 | 403 | ㅡ |
| `GET /api/admin/feedbacks/{id}/messages` | 401 | 403 | 403 | 403 | 403 | ㅡ |

---

**종료·삭제는 요청자 전용입니다.** 담당 전문가도 할 수 없습니다. 상담을 요청한 쪽이 고객이고 전문가는 응답하는 입장이라는 정책입니다.

**관리자 열람은 신고 이력이 있는 스레드만** 가능합니다. 없으면 ADMIN이어도 403입니다.

**상태별 조건**

| 상황 | 결과 |
| --- | --- |
| 종료된 스레드에 메시지 작성 | 409 |
| 요청자 구독 만료 후 메시지 작성 | 409 (전문가도 동일하게 차단) |
| 동일 전문가와 진행 중 스레드 존재 시 개설 | 409 |
| 진행 중 스레드 5개인 상태에서 개설 | 409 |
| 미승인 전문가 지정 | 403 |
| 탈퇴·정지 회원이 상대인 스레드 | 409 |
| 신고 접수된 스레드 삭제 | 409 |
| 동일 스레드 중복 신고 | 409 |
| 관리자가 신고 이력 없는 스레드 열람 | 403 |

**연쇄 처리**

| 사건 | 결과 |
| --- | --- |
| 전문가 자격 박탈(F-27) | 해당 전문가의 진행 중 스레드가 EXPERT_REVOKED로 일괄 종료 |
| 회원 탈퇴(F-75) | 요청자의 진행 중 스레드가 REQUESTER_WITHDRAWN으로 일괄 종료 + 구독 자동갱신 해지 |
| 스레드 삭제(F-76) | 진행 중이면 REQUESTER_CLOSED로 종료 후 소프트 삭제. 양쪽 목록·관리자 열람에서 모두 제외 |
| 박탈 후 재승인(F-74) | 종료된 스레드는 그대로 두고, 동일 조합으로 신규 개설 가능 |

**요청·응답 형식**

| 항목 | 내용 |
| --- | --- |
| 개설·메시지 전송 | `multipart/form-data` — `data`(JSON) + `images[]` |
| 목록 조회 | `?page=0&size=20`, 기본 최신순. `data`에 목록, `meta.pagination`에 페이징 정보 |
| 동시성 | 개설 시 요청자 User 행에 비관적 쓰기 락(F-73) |

> **⚠️ 오픈 이슈** — 삭제가 신고보다 먼저 이뤄지면 관리자가 심사할 수 없다. 부적절한 대화 직후 요청자가 삭제하면 전문가가 신고할 대상이 사라진다.
> 

> **⚠️ 정책 확인 필요** — 요청자 구독이 만료되면 담당 전문가도 답변을 작성할 수 없다. 명세(F-59)와는 일치하나, 전문가가 작성 중이던 답변을 완성하지 못하는 상황이 생긴다. “요청자만 차단” 방식이 더 자연스러울 수 있어 팀 논의 필요.
> 

---

# 3. 검증 위치

**Q&A/댓글** (담당: @김태영)

- [x]  조회(GET /api/posts, /comments)는 비로그인도 허용, 쓰기(POST/PUT/DELETE)는 인증 필요 → SecurityConfig에서 확인
- [x]  수정/삭제 시 토큰의 사용자 ID == 게시글/댓글 작성자 ID 비교 (`PostService.isOwner`, `CommentService.isOwner`)
- [ ]  ⚠️ **정정**: ADMIN은 정규 경로(`PUT/DELETE /api/posts|comments`)에서 **작성자 아니면 통과 못 함**(403). `isOwnerOrAdmin()` 같은 메서드는 없고 `isOwner()`만 있음. 관리자 처리는 `/api/admin/**` 강제삭제(소프트딜리트) 전용 경로로 분리돼 있고, **관리자용 수정 엔드포인트는 없음**. → 매트릭스 2.2 ADMIN 열을 403으로 정정 완료.
- [ ]  ⚠️ **미해결**: `GET /api/posts/{id}` 상세가 `SecurityConfig`에서 `permitAll`에 빠져 401 발생(F-05 위반). 목록만 `permitAll`. → 상세도 `permitAll` 추가 필요.

**스터디** (담당: @JS K)

- [x]  스터디 게시판(F-13~17), 그룹유저관리(F-18), 위임(F-19), 탈퇴(F-20) — ✅ 구현 완료 확인
- [ ]  `PUT /api/studies/{id}` 수정 시 방장 본인만 체크하고 ADMIN 예외 없음 (`StudyService.updateStudy`) — 미해결. ADMIN도 방장이 아니면 403 발생
- [ ]  ⚠️ **미해결**: `GET /api/studies/{id}` 상세도 `permitAll`에 빠져 401(F-10 위반). 게시글과 동일 사유.
- [x]  `StudyService.deleteStudy(userId, id)`는 컨트롤러에 연결된 엔드포인트가 없음(죽은 코드). 사용자용 “스터디 삭제” API는 없고, 방장이 유일 멤버로 탈퇴할 때만 자동 삭제됨 — 의도된 설계인지 결정 필요
- [x]  F-11/F-20 “멤버가 방장 1인뿐이면 위임 없이 삭제/탈퇴 가능” — ✅ `StudyMemberService.leaveStudy()`에서 정확히 구현됨
- [x]  강퇴 API `DELETE /api/studies/{id}/members/{userId}`의 경로변수는 대상 유저 ID를 받음(`findByStudyIdAndUserId`) — 명세서의 “StudyMember 레코드 ID” 표기 정정 필요
- [x]  가입/탈퇴 경로: 가입 `POST /api/studies/{id}/members`, 탈퇴 `DELETE /api/studies/{id}/leave` — 문서 표기 통일 필요
- [x]  🆕 `PATCH /api/studies/{id}/bump`(구독자 전용 끌어올리기) 매트릭스에 추가함 — 방장+구독자 조건 확인
- [x]  F-09/F-12(미구독자 2개 제한) 로직 구현됨. 구독 연동 후 재확인만

**전문가** (담당: @sunwoo jeong)

- [x]  GET /api/experts(F-32) — ✅ 목록·상세 모두 `permitAll` 확인, 정상 동작
- [x]  전문가 상담(F-30) 전체 — ✅ `FeedbackController`/`FeedbackService` 완전 구현, 구독 검증 포함
- [x]  F-25/26/27(신청·승인·거절·목록·박탈) — ✅ `ExpertProfileController`/`AdminController` 정상 구현
- [x]  F-27 박탈: `status=REJECTED` 재사용(`ExpertProfile.revoke()`) → 박탈된 전문가 즉시 재신청 가능. 관리자 심사로 통제하므로 의도된 정책으로 확정
- [x]  🆕 `GET /api/feedbacks/expert`(전문가용 받은 문의 목록) 존재 확인. 컨트롤러 주석의 “명세서 미확정” → 정식 스펙 확정 필요

**유저** (담당: @승환 최)

- [ ]  ⚠️ **미해결**: GET /api/posts, GET /api/studies가 목록만 permitAll, 상세(`/{id}`)는 401(F-05/F-10 위반). experts만 상세까지 열려 있음 — 게시글/스터디도 상세 `permitAll` 추가 필요
- [x]  `POST /api/auth/kakao`(F-03) — ✅ 카카오·구글·네이버 3종 구현 확인
- [x]  `DELETE /api/users/me`(F-24 회원탈퇴) — ✅ 해결됨 (탈퇴 시 상담 스레드/구독 연쇄 처리 포함, F-75)
- [x]  로그인 응답: `refreshToken`은 `@JsonIgnore`로 바디 미노출(HttpOnly 쿠키). `tokenType` 필드 필요 여부만 확인
- [x]  `User.name` 필드 — ✅ ERD·API명세·코드 3곳 필수 필드로 통일
- [ ]  🆕 USER→ADMIN 승격 API 없음. `PATCH /api/admin/users/{id}/status`는 상태(ACTIVE/SUSPENDED)만 변경, role 변경 미구현 — 운영 전 기능 추가 검토. (단, `GET /api/admin/users`의 role **필터**는 이미 구현됨 — 필터와 변경은 별개)

**관리자**

- [x]  F-21(강제삭제), F-22(유저 목록/상태변경) — ✅ 구현 완료. `AdminController`/`AdminService` 정상 동작
- [x]  🆕 관리 목록조회(`/studies`,`/subscriptions`,`/payments`,`/posts`)·신고관리(`/reports`,`/reports/{id}/resolve|reject`)도 구현돼 있어 매트릭스 2.4에 추가함