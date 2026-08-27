# 기능명세서

---

### MVP1

#### F-01 회원가입

| 항목 | 내용 |
| --- | --- |
| 액터 | GUEST |
| 사전조건 | 동일 username(email 형식)이 없어야 한다 |
| 입력 | name, username(email), password, nickname |
| 처리 | 비밀번호를 BCrypt로 암호화해 `users` 테이블에 저장(username 컬럼에 email 저장, Spring Security 컨텍스트의 username = DB의 email). 역할(role)은 기본 USER, 상태(status)는 기본 ACTIVE, is_subscribed는 false |
| 출력(성공) | 201 Created |
| 예외 | username 중복 → 409 / 형식 오류 → 400 |
| 관련 화면 | S-02(회원가입) |
| 관련 API | POST /api/auth/signup |

#### F-02 로그인 (JWT)

| 항목 | 내용 |
| --- | --- |
| 액터 | GUEST |
| 사전조건 | 가입된 계정이 있다 |
| 입력 | (일반) username(email), password |
| 처리 | `users.username`(email) 기준 조회 후 비밀번호 검증, 계정 상태가 ACTIVE가 아니면(SUSPENDED/WITHDRAWN) 차단, JWT accessToken 발급 |
| 출력(성공) | 200 + { accessToken }, refreshToken은 HttpOnly 쿠키로만 전달(응답 바디에는 포함되지 않음. ✏️ `tokenType` 필드는 응답에 없음) |
| 예외 | 비밀번호 불일치 → 401 / 계정 없음 → 404 / 정지·탈퇴 계정 → 403 |
| 관련 화면 | S-01(로그인) |
| 관련 API | POST /api/auth/login |

#### F-03 로그인 (소셜)

| 항목 | 내용 |
| --- | --- |
| 액터 | GUEST |
| 사전조건 | 없음(최초 로그인 시 자동 가입) |
| 입력 | (카카오) kakaoAccessToken / (구글) googleAccessToken / (네이버) naverAccessToken |
| 처리 | provider별 토큰 검증 후, `oauth_account`에서 (provider, provider_id) 조합으로 조회. 최초 로그인이면 `users` 신규 생성(password는 NULL) + `oauth_account` 신규 row 저장(provider, provider_id, linked_at=현재시각), JWT accessToken 발급 |
| 출력(성공) | 200 + { accessToken } (✏️ tokenType 필드 없음, F-02와 동일) |
| 예외 | 카카오/구글/네이버 토큰 무효 → 401 |
| 관련 화면 | S-01(로그인) |
| 관련 API | POST /api/auth/kakao · POST /api/auth/google · POST /api/auth/naver |

#### F-04 게시글 등록

| 항목 | 내용 |
| --- | --- |
| 액터 | USER, ADMIN |
| 사전조건 | 유효한 JWT |
| 입력 | title, content, category (enum: JOB_INFO/INTERVIEW_REVIEW/RESUME/FREE 중 하나) |
| 처리 | 토큰의 사용자를 author로 POST 저장 |
| 출력(성공) | 201 Created |
| 예외 | 토큰 없음 → 401 / title 비어있음 → 400 /존재하지 않는 카테고리 값 → 400 / 제목 100자 초과 → 400 |
| 관련 화면 | S-05(게시글 작성) |
| 관련 API | POST /api/posts |

#### F-05 게시글 목록/상세

| 항목 | 내용 |
| --- | --- |
| 액터 | GUEST, USER, ADMIN |
| 사전조건 | 없음 (비로그인 조회 가능) / 단, 내 게시글 조회는 유효한 JWT 필요 |
| 입력 | page, size, category(선택), keyword(선택) / (내 게시글 조회 시) page, size / (상세) postId, page, size(댓글 페이징용) |
| 처리 | 최신순 페이징 목록 조회(keyword로 제목/본문 검색 가능) / 로그인 사용자의 경우 본인 작성 글만 필터링 조회 가능 / 단건 조회 시 댓글 페이징 포함, 조회수(viewCount) 1 증가 |
| 출력(성공) | 200 + Page(content, totalPages, category, categoryLabel 포함) / 200 + 상세 JSON(comments 페이징, viewCount 포함) |
| 예외 | 없는 postId → 404 / 내 게시글 조회 시 토큰 없음 → 401 |
| 관련 화면 | S-03(목록), S-04(상세), S-14(마이페이지) |
| 관련 API | GET /api/posts · GET /api/posts/me · GET /api/posts/{id} |

#### F-06 게시글 수정/삭제

| 항목 | 내용 |
| --- | --- |
| 액터 | USER(본인), ADMIN |
| 사전조건 | 유효한 JWT |
| 입력 | postId + (수정 시) title, content, category |
| 처리 | 작성자 == 토큰 사용자이거나 ADMIN이면 허용 |
| 출력(성공) | 200 (ApiResponse 공통 포맷, success/message/data 포함) |
| 예외 | 토큰 없음 → 401 / 타인(본인·ADMIN 아님) → 403 / 없는 id → 404 |
| 관련 화면 | S-05(작성/수정) |
| 관련 API | PUT /api/posts/{id} · DELETE /api/posts/{id} |

#### F-07 댓글 등록

| 항목 | 내용 |
| --- | --- |
| 액터 | USER, ADMIN |
| 사전조건 | 유효한 JWT, 게시글이 존재 |
| 입력 | postId, content |
| 처리 | Comment 저장(author=토큰 사용자, post FK) |
| 출력(성공) | 201 Created |
| 예외 | 토큰 없음 → 401 / 게시글 없음 → 404 / content 빈값 → 400 |
| 관련 화면 | S-04(상세) |
| 관련 API | POST /api/posts/{postId}/comments |

#### F-08 댓글 수정/삭제

| 항목 | 내용 |
| --- | --- |
| 액터 | USER, ADMIN |
| 사전조건 | 유효한 JWT |
| 입력 | commentId + (수정 시) content |
| 처리 | 작성자 == 토큰 사용자이거나 ADMIN이면 허용 |
| 출력(성공) | 200 (ApiResponse 공통 포맷, success/message/data 포함) |
| 예외 | 토큰 없음 → 401 / 타인(본인·ADMIN 아님) → 403 / 없는 id → 404 |
| 관련 화면 | S-04(상세) |
| 관련 API | PUT /api/posts/{postId}/comments/{commentId} · DELETE /api/posts/{postId}/comments/{commentId} |

#### F-09 스터디 개설

| 항목 | 내용 |
| --- | --- |
| 액터 | USER, ADMIN |
| 사전조건 | 유효한 JWT, 미구독 시 개설+참여 합산 2개 미만이어야 함 |
| 입력 |  (multipart/form-data) data 파트(title, description, capacity, recruit_end, category) + images      파트(파일 다건, 선택) — recruit_start는 입력받지 않고 개설일로 자동 설정 |
| 처리 | 토큰의 사용자를 study.leader_id로 저장, category 필수 선택, 개설자는 자동으로 study_member에도 등록됨(개설+참여 합산 카운트에 포함). images는 ImageValidator(jpg/jpeg/png/gif, 파일당 최대 5MB) 검증 후 업로드해 StudyImage로 연결 |
| 출력(성공) |  201 Created — 응답에 imageUrls 포함 |
| 예외 | 토큰 없음 → 401 / 가입 한도 초과 → 403(STUDY_JOIN_LIMIT_EXCEEDED) / title 빈값·100자 초과 → 400 / capacity 2명 미만 → 400 / recruit_end가 과거 → 400 / 허용되지 않는 이미지 형식·5MB 초과 → 400 |
| 관련 화면 | S-08(스터디 개설) |
| 관련 API | POST /api/studies |

#### F-10 스터디 목록/상세

| 항목 | 내용 |
| --- | --- |
| 액터 | GUEST, USER, ADMIN |
| 사전조건 | 없음 (비로그인 조회 가능) |
| 입력 | page, size, keyword(선택) / (상세) studyId |
| 처리 | 목록은 bumped_at(끌올 시각, 없으면 created_at) 내림차순 정렬(F-29 참고). 응답에 방장 구독 여부(leaderSubscribed, 목록 강조 표시용)·imageUrls·bumpedAt 포함 |
| 출력(성공) | 200 + Page(content, totalPages) / 200 + 상세 JSON |
| 예외 | 없는 studyId → 404 |
| 관련 화면 | S-06(목록), S-07(상세) |
| 관련 API | GET /api/studies · GET /api/studies/{id} |

#### F-11 스터디 수정

| 항목 | 내용 |
| --- | --- |
| 액터 | USER(방장) |
| 사전조건 | 유효한 JWT |
| 입력 | studyId + title, description, capacity, recruit_end, category |
| 처리 | `study.leader_id` == 토큰 사용자인 경우에만 허용 |
| 출력(성공) | 200 |
| 예외 | 토큰 없음 → 401 / 방장 아님 → 403 / 없는 id → 404 / capacity < 현재 인원 → 409 |
| 관련 화면 | S-08(개설/수정) |
| 관련 API | PUT /api/studies/{id} |

#### F-12 스터디 신청(자유가입)

| 항목 | 내용 |
| --- | --- |
| 액터 | USER |
| 사전조건 | 유효한 JWT, 방장 본인 아님, 미가입 상태, 모집 마감일이 지나지 않음 |
| 입력 | studyId |
| 처리 | 신청 즉시 가입 처리(승인 절차 없음). 검사 순서: 방장 본인 여부 → 중복 가입 여부 → 모집 마감 여부 → 정원 → (구독자는 F-31에 따라) 가입 한도 |
| 출력(성공) | 200 + { memberId } |
| 예외 | 방장 본인 → 409 / 중복 가입 → 409 / 모집 마감일 지남 → 409 / 정원 초과 → 409 / 미구독자 2개 초과 → 403 |
| 관련 화면 | S-07(상세) |
| 관련 API | POST /api/studies/{id}/members |

#### F-13 스터디 게시판(게시글 등록)

| 항목 | 내용 |
| --- | --- |
| 액터 | USER(가입 완료 멤버) |
| 사전조건 | 유효한 JWT, 해당 스터디 가입 완료 상태 |
| 입력 | studyId, title, content |
| 처리 | 가입 멤버만 게시글 작성 |
| 출력(성공) | 201 Created |
| 예외 | 미가입 유저 → 403 / 없는 studyId → 404 |
| 관련 화면 | S-07(스터디 상세·게시판) |
| 관련 API | POST /api/studies/{id}/posts |

#### F-14 스터디 게시판 목록/상세

| 항목 | 내용 |
| --- | --- |
| 액터 | USER(가입 완료 멤버) |
| 사전조건 | 유효한 JWT, 해당 스터디 가입 완료 상태 |
| 입력 | studyId / studyPostId |
| 처리 | 가입 멤버만 목록/상세 조회. 목록은 고정(pinned) 게시글 우선, 그다음 최신순 정렬 |
| 출력(성공) | 200 + List / 200 + 상세 JSON |
| 예외 | 미가입 유저 → 403 / 없는 id → 404 |
| 관련 화면 | S-07(스터디 상세·게시판) |
| 관련 API | GET /api/studies/{id}/posts · GET /api/studies/{id}/posts/{postId} |

#### F-15 스터디 게시판 수정/삭제

| 항목 | 내용 |
| --- | --- |
| 액터 | USER(본인), ADMIN |
| 사전조건 | 유효한 JWT |
| 입력 | studyPostId + (수정 시) title, content |
| 처리 | 작성자 본인이면 허용 |
| 출력(성공) | 200 |
| 예외 | 타인(본인·ADMIN 아님) → 403 / 없는 id → 404 |
| 관련 화면 | S-07(스터디 상세·게시판) |
| 관련 API | PUT /api/studies/{id}/posts/{postId} · DELETE /api/studies/{id}/posts/{postId} |

#### F-15-1 스터디 게시판 수정/삭제

| 항목 | 내용 |
| --- | --- |
| 액터 | USER(방장) |
| 사전조건 | 유효한 JWT, 해당 스터디 가입 완료 상태 + 방장 |
| 입력 | studyId, postId, pinned(boolean) |
| 처리 | 방장이 게시글의 고정 여부를 토글. 고정된 게시글은 F-14 목록에서 최상단에 노출됨 |
| 출력(성공) | 200 |
| 예외 | 미가입/방장 아님 → 403 / 없는 글 → 404 |
| 관련 화면 | S-07(스터디 상세·게시판) |
| 관련 API | PUT /api/studies/{id}/posts/{postId} · DELETE /api/studies/{id}/posts/{postId} |

#### F-16 스터디 게시판 댓글 등록

| 항목 | 내용 |
| --- | --- |
| 액터 | USER(가입 완료 멤버) |
| 사전조건 | 유효한 JWT, 해당 스터디 가입 완료 상태, 게시글이 존재 |
| 입력 | studyPostId, content |
| 처리 | Comment 저장(author=토큰 사용자, studyPost FK) |
| 출력(성공) | 201 Created |
| 예외 | 미가입 유저 → 403 / 게시글 없음 → 404 / content 빈값 → 400 |
| 관련 화면 | S-07(스터디 상세·게시판) |
| 관련 API | POST /api/studies/{id}/posts/{postId}/comments |

#### F-17 스터디 게시판 댓글 수정/삭제

| 항목 | 내용 |
| --- | --- |
| 액터 | USER(본인), ADMIN |
| 사전조건 | 유효한 JWT |
| 입력 | commentId + (수정 시) content |
| 처리 | 작성자 본인 허용 |
| 출력(성공) | 200 |
| 예외 | 타인(본인·ADMIN 아님) → 403 / 없는 id → 404 |
| 관련 화면 | S-07(스터디 상세·게시판) |
| 관련 API | PUT /api/studies/{id}/posts/{postId}/comments/{commentId} · DELETE /api/studies/{id}/posts/{postId}/comments/{commentId} |

#### F-18 그룹 내 유저 관리

| 항목 | 내용 |
| --- | --- |
| 액터 | USER(방장) |
| 사전조건 | 유효한 JWT, 요청자가 해당 스터디 방장 |
| 입력 | studyId, 대상 userId |
| 처리 | 방장이 멤버 강퇴(방장 본인은 강퇴 대상이 될 수 없음) |
| 출력(성공) | 200 |
| 예외 | 방장 아님 → 403 / 대상 없음 → 404 |
| 관련 화면 | S-07(상세) |
| 관련 API | DELETE /api/studies/{id}/members/{userId} |

#### F-19 방장 위임

| 항목 | 내용 |
| --- | --- |
| 액터 | USER(방장) |
| 사전조건 | 유효한 JWT, 요청자가 해당 스터디 방장, 대상이 가입 완료 멤버 |
| 입력 | studyId, newLeaderId |
| 처리 | study.leader_id를 newLeaderId로 변경 |
| 출력(성공) | 200 |
| 예외 | 방장 아님 → 403 / 대상이 가입 멤버 아님 → 403 / 본인에게 위임 시도 → 409 |
| 관련 화면 | S-08(방장 위임 화면) |
| 관련 API | PATCH /api/studies/{id}/leader |

#### F-20 스터디 탈퇴 처리

| 항목 | 내용 |
| --- | --- |
| 액터 | USER(일반 멤버 또는 방장) |
| 사전조건 | 유효한 JWT, 해당 스터디 가입 상태 |
| 처리 | 일반 멤버는 즉시 탈퇴 처리. 방장인 경우 멤버가 1인(본인뿐)이면 위임 없이 탈퇴 가능하며 스터디가 함께 삭제됨. 멤버가 2인 이상인데 미위임 상태면 탈퇴 거부 |
| 출력(성공) | 200 |
| 예외 | 멤버 2인 이상인데 미위임 상태로 방장이 탈퇴 시도 → 409 |
| 관련 화면 | S-08(방장 위임 화면) |
| 관련 API | DELETE /api/studies/{id}/leave |

#### F-21 전체 게시판·스터디 그룹 관리

| 항목 | 내용 |
| --- | --- |
| 액터 | ADMIN |
| 사전조건 | 유효한 JWT, role=ADMIN |
| 입력 | postId 또는 studyId 등 대상 id |
| 처리 | ADMIN 권한으로 게시글·댓글·스터디·스터디게시글·스터디게시글댓글 강제 삭제 |
| 출력(성공) | 200 |
| 예외 | ADMIN 아님 → 403 / 없는 id → 404 |
| 관련 화면 | S-13(어드민) |
| 관련 API | DELETE /api/admin/posts/{id} · DELETE /api/admin/comments/{id} · DELETE /api/admin/studies/{id} · DELETE /api/admin/study-posts/{id} · DELETE /api/admin/study-post-comments/{id} |

#### F-22 유저 관리

| 항목 | 내용 |
| --- | --- |
| 액터 | ADMIN |
| 사전조건 | 유효한 JWT, role=ADMIN |
| 입력 | page, size / (상태변경) userId, status |
| 처리 | 유저 목록 조회 및 계정 상태 변경. `users.status`는 ACTIVE/SUSPENDED/WITHDRAWN 3가지 값 중 하나이며, ADMIN은 ACTIVE↔︎SUSPENDED만 변경 가능(WITHDRAWN은 F-24 자진 탈퇴 전용, ADMIN이 임의 지정 불가) |
| 출력(성공) | 200 + { users[] } / 200 |
| 예외 | ADMIN 아님 → 403 / 없는 userId → 404 / 관리자 실수로 WITHDRAWN 지정 시도 → 400 |
| 관련 화면 | S-13(어드민) |
| 관련 API | GET /api/admin/users · PATCH /api/admin/users/{id}/status |
| ⚠️오픈 이슈 | 와이어프레임(S-13 회원관리 탭)엔 닉네임/아이디 검색, 역할별 필터 UI가 있으나 `GET /api/admin/users`는 페이징 파라미터만 지원함 — keyword/role 필터 추가 필요 |

#### F-22-1 유저 권한(role) 변경

| 항목 | 내용 |
| --- | --- |
| 액터 | ADMIN |
| 사전조건 | 유효한 JWT, role=ADMIN |
| 입력 | userId, role |
| 처리 | 특정 유저의 role을 변경 |
| 출력(성공) | 200 |
| 예외 | ADMIN 아님 → 403 / 없는 userId → 404 |
| 관련 화면 | S-13(어드민, 회원관리 탭) |
| 관련 API | ❌ 미구현 — `PATCH /api/admin/users/{id}/status`는 계정 상태(ACTIVE/SUSPENDED)만 변경 가능하고 role 변경 기능 자체가 없음. 신규 엔드포인트 정의 필요 |
| ⚠️오픈 이슈 | 현재 USER→ADMIN으로 승격시킬 방법이 API로는 없어 DB에서 직접 role을 수정해야 함(운영 전 반드시 보완 필요) |

#### F-23 입력 검증

| 항목 | 내용 |
| --- | --- |
| 액터 | 전 API |
| 사전조건 | — |
| 입력 | 빈 제목·과도한 길이 등 |
| 처리 | Bean Validation / 서비스 계층 검증 |
| 출력(성공) | (해당 API 성공 응답) |
| 예외 | 400 + { success, message, data, meta, errorCode } (ApiResponse 공통 포맷) |
| 관련 화면 | 폼 화면 공통 |
| 관련 API | 쓰기 API 공통 |

#### F-24 회원 탈퇴

| 항목 | 내용 |
| --- | --- |
| 액터 | USER |
| 사전조건 | 유효한 JWT |
| 입력 | password(재확인용, 소셜 로그인 전용 계정은 비밀번호가 없어 이 검증이 생략됨) |
| 처리 | `users.status`=WITHDRAWN 전환, `name`·`nickname`을 “탈퇴한사용자” 형태로 마스킹, `withdrawn_at` 기록, `refresh_token`에서 해당 user_id row 삭제 |
| 출력(성공) | 200 (ApiResponse 공통 포맷, success/message/data 포함) |
| 예외 | 비밀번호 불일치 → 401 |
| 관련 화면 | S-14(마이페이지-설정) |
| 관련 API | DELETE /api/users/me |
| ⚠️오픈 이슈 | 원래 설계는 “방장인 스터디가 있으면 탈퇴 차단(409)”, “ACTIVE 구독이 있으면 CANCELLED로 전환”이었으나, 현재 `UserService.withdrawAccount()`에는 두 로직이 모두 없음. 방장 상태여도 그대로 탈퇴되어 스터디가 방장 없는 상태로 남을 수 있음 — 우선순위 높은 보완 필요 |

#### F-33 프로필(닉네임) 수정

| 항목 | 내용 |
| --- | --- |
| 액터 | USER |
| 사전조건 | 유효한 JWT |
| 입력 | nickname |
| 처리 | 요청자의 `users.nickname` 갱신 |
| 출력(성공) | 200 |
| 예외 | 토큰 없음 → 401 / 닉네임 2~12자 범위 초과 → 400 / 닉네임 중복 → 400 |
| 관련 화면 | S-14(마이페이지-설정) |
| 관련 API | PATCH /api/users/me/nickname |

#### F-34 비밀번호 변경

| 항목 | 내용 |
| --- | --- |
| 액터 | USER |
| 사전조건 | 유효한 JWT, 소셜 로그인 전용 계정(비밀번호 없음)이 아니어야 함 |
| 입력 | currentPassword, newPassword, newPasswordConfirm |
| 처리 | 현재 비밀번호 검증 후 `users.password`를 BCrypt로 재암호화하여 갱신 |
| 출력(성공) | 200 |
| 예외 | 토큰 없음 → 401 / 현재 비밀번호 불일치 → 400 / newPassword·newPasswordConfirm 불일치 → 400 / 소셜 로그인 전용 계정 → 400 |
| 관련 화면 | S-14(마이페이지-설정) |
| 관련 API | PATCH /api/users/me/password |

---

### MVP2

#### F-01  MVP2-회원가입

| 항목 | 내용 |
| --- | --- |
| 액터 | GUEST |
| 사전조건 | 동일 username(email 형식)이 없거나, 있어도 password가 없는(소셜 전용) 계정일 것 / 이메일 인증 완료(verified=true) / 이용약관 동의 |
| 입력 | name, username(email), password, nickname,termsAgreed |
| 처리 | 이메일 인증 여부 확인(checkVerified) → 인증 기록 소비(삭제) → 약관 동의 여부 확인 → 비밀번호를 BCrypt로 암호화해 users 테이블에 저장. 단, 동일 email의 소셜 전용 계정(password=null)이 이미 있으면 신규 생성 대신 그 계정에 비밀번호만 연결(계정 통합). role은 기본 USER, status는 기본 ACTIVE, is_subscribed는 false, terms_agree_at은 동의 시각 저장 |
| 출력(성공) | 201 Created |
| 예외 | username 중복 → 409 / 형식 오류 → 400 |
| 관련 화면 | S-02(회원가입) |
| 관련 API | POST /api/auth/signup |

#### F-02  MVP2-로그인(JWT)

| 항목 | 내용 |
| --- | --- |
| 액터 | GUEST |
| 사전조건 | 가입된 계정이 있다 / 계정이 잠금 상태가 아니다 |
| 입력 | username(email), password |
| 처리 | users.username(email) 기준 조회 후 잠금 상태(locked_until) 우선 확인 → 비밀번호 검증(F-79: 실패 시 카운트 증가, 5회 도달 시 10분 잠금) → 성공 시 카운트/잠금 초기화 → 계정 상태 확인(SUSPENDED/WITHDRAWN이면 각각 다른 메시지로 차단) → JWT accessToken 발급 |
| 출력(성공) | 200 + { accessToken }, refreshToken은 HttpOnly 쿠키로만 전달(응답 바디에는 포함되지 않음) |
| 예외 | 비밀번호 불일치 → 401 / 계정 없음 → 404 / 정지 계정 → 403("정지된 계정입니다.", ACCOUNT_SUSPENDED) / 탈퇴 계정 → 403("회원탈퇴한 사용자입니다.", ACCOUNT_WITHDRAWN) / 잠금 상태 → 403(ACCOUNT_LOCKED) |
| 관련 화면 | S-01(로그인) |
| 관련 API | POST /api/auth/login |

#### F-02-1 토큰 재발급

| 항목 | 내용 |
| --- | --- |
| 액터 | USER |
| 사전조건 | 유효한 refreshToken(쿠키) 보유, 계정 상태 ACTIVE |
| 입력 | refreshToken(쿠키에서 자동 추출) |
| 처리 | refreshToken 서명·만료 검증 → DB 저장 토큰(해시)과 일치 확인 → 계정 상태 ACTIVE 확인 → 신규 accessToken/refreshToken 발급 및 저장(로테이션) |
| 출력(성공) | 200 + { accessToken }, 새 refreshToken은 HttpOnly 쿠키로 갱신 |
| 예외 | 	토큰 무효/불일치 → 401(INVALID_REFRESH_TOKEN) / 정지 계정 → 403(ACCOUNT_SUSPENDED) / 탈퇴 계정 → 403(ACCOUNT_WITHDRAWN) |
| 관련 화면 | 화면 없음(백그라운드 자동 갱신) |
| 관련 API | POST /api/auth/reissue |

#### F-02-3 로그아웃

| 항목 | 내용 |
| --- | --- |
| 액터 | USER |
| 사전조건 | 유효한 JWT |
| 입력 | 없음 |
| 처리 | DB에 저장된 refreshToken 삭제, 클라이언트 쿠키도 즉시 만료 처리 |
| 출력(성공) | 200 |
| 예외 | 계정 없음 → 404 |
| 관련 화면 | 전역(헤더 로그아웃 버튼) |
| 관련 API | POST /api/auth/logout |

#### F-02-2 로그인 실패 횟수 제한 (브루트포스 방어)

| 항목 | 내용 |
| --- | --- |
| 액터 | GUEST |
| 사전조건 | F-02(로그인) 처리 중 내장 |
| 입력 | 없음(F-02 입력에 포함) |
| 처리 | 비밀번호 불일치 시마다 failed_login_attempts 증가(별도 트랜잭션으로 처리하여 로그인 실패에 따른 예외 롤백과 무관하게 기록 유지 — self-invocation 문제로 별도 클래스 LoginAttemptService로 분리). 5회 도달 시 locked_until을 현재 시각+10분으로 설정하고 카운트 리셋. 로그인 성공 시 카운트 및 잠금 상태 초기화 |
| 출력(성공) | (F-02와 동일) |
| 예외 | 잠금 상태에서 로그인 시도 → 403(ACCOUNT_LOCKED) |
| 관련 화면 | S-01(로그인) |
| 관련 API | POST /api/auth/login (F-02와 동일 엔드포인트에 |

#### F-03 MVP2-소셜 로그인 (Kakao/Google/Naver)

| 항목 | 내용 |
| --- | --- |
| 액터 | GUEST |
| 사전조건 | 없음(최초 로그인 시 자동 가입) |
| 입력 | (카카오) kakaoAccessToken /          (구글) googleAccessToken /           (네이버) naverAccessToken |
| 처리 | 각 provider API로 사용자 정보 조회 → oauth_account에 기존 연동 여부 확인 → 없으면: 동일 email의 일반 회원가입 계정이 있으면 그 계정에 oauth_account만 신규 연결(F-41), 없으면 신규 유저 생성(닉네임 "이름_랜덤4자리숫자" 자동 생성, 중복 시 재생성) → 계정 상태 확인(F-02와 동일 기준) → JWT 발급. 카카오는 가상 이메일(kakao_{providerId}@kakao.local) 사용 |
| 출력(성공) | 200 + { accessToken }, refreshToken은 HttpOnly 쿠키로 전달 |
| 예외 | 소셜 토큰 유효하지 않음 → 401(OAUTH_TOKEN_INVALID) /       정지·탈퇴 계정 → 403 |
| 관련 화면 | S-01(로그인) |
| 관련 API | POST /api/auth/kakao                 POST /api/auth/google              POST /api/auth/naver |

#### F-21 MVP2-전체 게시판 관리, 스터디 그룹 관리

| 항목 | 내용 |
| --- | --- |
| 액터 | ADMIN |
| 사전조건 | 유효한 JWT, role=ADMIN |
| 입력 | postId, commentId, studyId, studyPostId, studyPostCommentId (대상 id) |
| 처리 | 대상 조회 후 소프트 딜리트(@SoftDelete) 처리. 게시글 삭제 시 연관 댓글도 함께 소프트 딜리트. 스터디/스터디게시글 삭제 시 하위 데이터(댓글→게시글→멤버→스터디) 순으로 함께 처리 |
| 출력(성공) | 200 |
| 예외 | ADMIN 아님 → 403 / 없는 id → 404 |
| 관련 화면 | S-13(어드민) |
| 관련 API | DELETE /api/admin/posts/{id} · DELETE /api/admin/comments/{id} · DELETE /api/admin/studies/{id} · DELETE /api/admin/study-posts/{id} · DELETE /api/admin/study-post-comments/{id} |

#### F-22 MVP2-유저 관리

| 항목 | 내용 |
| --- | --- |
| 액터 | ADMIN |
| 사전조건 | 유효한 JWT, role=ADMIN |
| 입력 | page, size / keyword(닉네임·이메일 검색, 선택) / role(권한 필터, 선택) / (상태변경) userId, status |
| 처리 | 유저 목록을 keyword(닉네임 또는 이메일 부분일치) 및 role로 필터링하여 페이징 조회. 계정 상태 변경은 ACTIVE↔SUSPENDED만 허용(WITHDRAWN은 ADMIN이 임의 지정 불가, F-24 자진 탈퇴 전용) |
| 출력(성공) | 조회: 200 + { users[] } / 상태변경: 200 |
| 예외 | ADMIN 아님 → 403 / 없는 userId → 404 / WITHDRAWN 지정 시도 → 400(INVALID_STATUS_CHANGE) |
| 관련 화면 | S-13(어드민, 회원관리 탭) |
| 관련 API | GET /api/admin/users · PATCH /api/admin/users/{id}/status |

#### F-24 MVP2-회원 탈퇴

| 항목 | 내용 |
| --- | --- |
| 액터 | USER |
| 사전조건 | 유효한 JWT / 본인 비밀번호 확인(소셜 전용 계정은 검증 생략) |
| 입력 | password |
| 처리 | 비밀번호 확인 → 구독 자동 갱신 해지(`disableAutoRenewIfUsable`, 구독이 없거나 이미 해지 상태면 무동작) → 진행 중 상담 스레드를 REQUESTER_WITHDRAWN 사유로 일괄 종료(F-75) → 본인이 방장인 스터디를 댓글→게시글→멤버→스터디 순으로 소프트 딜리트 → 멤버로 가입한 다른 스터디는 멤버십만 삭제 → `name`/`nickname`을 "탈퇴한사용자"/"탈퇴한사용자_{id}"로 마스킹, `status`=WITHDRAWN, `withdrawn_at` 기록 → refreshToken 삭제 |
| 출력(성공) | 200 |
| 예외 | 비밀번호 불일치 → 401 |
| 관련 화면 | S-14(마이페이지-설정) |
| 관련 API | DELETE /api/users/me |
| 변경 | 기존 오픈 이슈(구독 취소·방장 처리 누락) 모두 해소됨. 방장 스터디는 탈퇴 차단이 아니라 소프트 딜리트로 처리하는 방식으로 확정 |

#### F-33 내 정보 조회

| 항목 | 내용 |
| --- | --- |
| 액터 | USER |
| 사전조건 | 유효한 JWT |
| 입력 | 없음 |
| 처리 | JWT에서 추출한 로그인 정보로 본인 User 조회 → 비밀번호 등 민감정보를 제외한 UserResponse로 변환하여 반환 |
| 출력(성공) | 200 + { id, username, name, nickname, role, status, subscribed, createdAt } |
| 예외 | 인증되지 않음 → 401 |
| 관련 화면 | S-14(마이페이지)  |
| 관련 API | GET /api/users/me |

#### F-33-01  MVP2-프로필(닉네임) 수정

| 항목 | 내용 |
| --- | --- |
| 액터 | USER |
| 사전조건 | 유효한 JWT |
| 입력 | nickname |
| 처리 | 닉네임 중복 확인 후 저장 |
| 출력(성공) | 200 |
| 예외 | 닉네임 중복 → 409 / 형식 오류(2~12자) → 400 |
| 관련 화면 | S-14(닉네임 수정) |
| 관련 API | PATCH /api/users/me/nickname |

#### F-34 MVP2-비밀번호 변경

| 항목 | 내용 |
| --- | --- |
| 액터 | USER |
| 사전조건 | 유효한 JWT / 소셜 전용 계정(password=null)이 아닐 것 |
| 입력 | currentPassword, newPassword, newPasswordConfirm |
| 처리 | 소셜 전용 계정 여부 확인 → 현재 비밀번호 일치 확인 → 새 비밀번호와 확인값 일치 확인 → 암호화 저장 → **refreshToken 삭제(기존 로그인 세션 무효화)** |
| 출력(성공) | 200 |
| 예외 | 소셜 전용 계정 → 403 / 현재 비밀번호 불일치 → 401 / 새 비밀번호 확인 불일치 → 400 |
| 관련 화면 | S-XX(마이페이지, 설정 탭) — 확인 필요 |
| 관련 API | PATCH /api/users/me/password |

#### F-39 이메일 인증

| 항목 | 내용 |
| --- | --- |
| 액터 | GUEST |
| 사전조건 | 없음 |
| 입력 | (발송) email / (검증) email, code |
| 처리 | 발송: 마지막 발송 후 30초 미경과 시 차단(쿨다운), 최근 10분 내 5회 초과 시 차단(횟수 제한) → 6자리 랜덤 코드 생성 후 DB 저장(5분 만료) → Gmail SMTP로 발송(발신자 표시명 "prep2gether"). 검증: 해당 email의 최신 인증기록 조회 → 만료 확인 → 코드 일치 확인 → verified=true 처리. **F-01(회원가입), F-40(비밀번호 재설정)에서 공통 사용** |
| 출력(성공) | 발송: 200 / 검증: 200 |
| 예외 | 재발송 쿨다운 미경과 → 429(TOO_MANY_REQUESTS_COOLDOWN) / 발송 횟수 초과 → 429(TOO_MANY_REQUESTS_LIMIT) / 코드 불일치 또는 인증기록 없음 → 400(INVALID_VERIFICATION_CODE) / 코드 만료 → 400(VERIFICATION_CODE_EXPIRED) |
| 관련 화면 | S-02(회원가입) |
| 관련 API | POST /api/auth/email/send       POST /api/auth/email/verify |

#### F-40 비밀번호 재설정(이메일 인증 기반)

| 항목 | 내용 |
| --- | --- |
| 액터 | GUEST |
| 사전조건 | 가입된 계정이 있다 / 소셜 전용 계정이 아니다 / 이메일 인증을 새로 완료했다(F-39, 과거 인증 기록은 재사용 불가 — 확인 시점에 즉시 소비됨) |
| 입력 | username(email), newPassword |
| 처리 | 유저 조회 → 소셜 전용 계정 여부 확인 → 이메일 인증 여부 확인(F-39, 확인과 동시에 인증 기록 삭제) → 새 비밀번호 암호화 저장 → **refreshToken 삭제(기존 로그인 세션 무효화)** |
| 출력(성공) | 200 |
| 예외 | 계정 없음 → 404 / 소셜 전용 계정 → 403 / 이메일 미인증 → 400(EMAIL_NOT_VERIFIED) |
| 관련 화면 | S-01(로그인, 비밀번호 찾기) |
| 관련 API | POST /api/auth/resetpassword |

#### F-41 일반 로그인·소셜 로그인 계정 통합

| 항목 | 내용 |
| --- | --- |
| 액터 | GUEST |
| 사전조건 | 없음(F-01, F-03 처리 과정에 내장) |
| 입력 | (F-01, F-03과 동일) |
| 처리 | 동일 이메일 기준 양방향 자동 연동. (1) 소셜 로그인으로 먼저 가입된 상태에서 동일 이메일로 일반 회원가입 시도 → 이메일 인증 완료를 전제로 기존 계정에 비밀번호만 추가(신규 계정 생성 안 함). (2) 일반 회원가입이 먼저 되어 있는 상태에서 동일 이메일로 소셜 로그인 시도 → 신규 계정 생성 없이 oauth_account만 기존 계정에 추가 연결(소셜 로그인은 provider가 이메일 소유를 이미 검증했으므로 별도 인증 불필요). 연동 이후에는 소셜/일반 로그인 모두 동일 계정으로 접근 가능 |
| 출력(성공) | (F-01, F-03과 동일) |
| 예외 | 이미 비밀번호가 설정된 계정에 재가입 시도 → 409(DUPLICATE_USERNAME) |
| 관련 화면 | S-01, S-02 |
| 관련 API | POST /api/auth/signup · POST /api/auth/kakao · POST /api/auth/google · POST /api/auth/naver |

#### F-44 게시글 이미지 첨부

| 항목 | 내용 |
| --- | --- |
| 액터 | USER(작성자 본인), ADMIN |
| 사전조건 | 유효한 JWT, 게시글/스터디 게시글 작성 또는 수정 시 |
| 입력 | image(파일, 다건 가능 여부는 정책 결정 필요) |
| 처리 | 이미지를 스토리지(예: S3 또는 서버 로컬)에 저장하고, 게시글/스터디 게시글 엔티티에 이미지 URL을 연결 |
| 출력(성공) | 201(업로드) — 상세 조회 시 imageUrls 목록 포함 |
| 예외 | 토큰 없음 → 401 / 허용되지 않는 파일 형식·용량 초과 → 400 |
| 관련 화면 | S-05(게시글 작성/수정), S-07(스터디 게시판 작성/수정) |
| 관련 API | POST /api/posts (multipart) · POST /api/studies/{id}/posts (multipart) |

#### F-48 스터디 게시판 이미지 첨부

| 항목 | 내용 |
| --- | --- |
| 액터 | USER(가입 완료 멤버), ADMIN |
| 사전조건 | 유효한 JWT, 해당 스터디 가입 완료 상태, 스터디 게시글 등록 시 |
| 입력 | (multipart/form-data) `data` 파트(스터디 게시글 JSON: title/content) + `images` 파트(파일 다건, 선택) |
| 처리 | multipart로 받아 `ImageValidator` 검증(jpg·jpeg·png·gif, 파일당 최대 5MB) 후 **AWS S3** 업로드, `StudyPostImage`로 연결. 가입 완료 멤버만 첨부 가능(F-13 권한 재사용). **이미지 수정은 범위 제외**, 게시글 삭제 시 함께 제거 |
| 출력(성공) | 201 Created — 상세 조회 시 `imageUrls`(String 배열) 포함 |
| 예외 | 미가입 유저 → 403 / 허용되지 않는 형식·용량 초과 → 400 |
| 관련 화면 | S-07(스터디 게시판 작성) |
| 관련 API | POST /api/studies/{id}/posts (multipart/form-data) |

#### F-25 전문가 신청

| 항목 | 내용 |
| --- | --- |
| 액터 | USER |
| 사전조건 | 유효한 JWT. **기존 신청 이력이 있으면 REJECTED 상태여야 함**(PENDING·APPROVED 상태에서는 재신청 불가) |
| 입력 | careers[{companyName, position, years, jobField}] (**최소 1건 필수**), certifications[{name, issuer, acquiredYear}] (선택), introduction(선택, 최대 500자) |
| 처리 | 신청 이력이 **없으면** `expert_profile`을 신규 생성한다. **있으면 기존 row를 재사용**해 `reapply()`로 status를 PENDING으로 되돌리고 소개글·경력·자격증을 덮어쓴다(dirty checking). 즉 유저당 `expert_profile`은 항상 1건만 존재한다. role은 승인 전까지 USER 유지, 자격증은 승인 기준이 아니라 프로필 노출용 |
| 출력(성공) | 201 Created |
| 예외 | 경력 0건 → 400 / 소개글 500자 초과 → 400 / REJECTED 아닌 상태에서 재신청 → 409(EXPERT_REAPPLY_INVALID_STATUS) / 토큰 없음 → 401 |
| 관련 화면 | S-10(전문가 가입/승인대기) |
| 관련 API | POST /api/experts/signup |
| 변경 | 기존 명세는 "PENDING 이력이 없어야 함 / 중복 시 409"였으나, 실제로는 **row를 재사용하는 upsert 구조**다. 신규 생성과 재신청이 같은 엔드포인트를 공유한다 |

#### F-26 전문가 승인/거절

| 항목 | 내용 |
| --- | --- |
| 액터 | ADMIN |
| 사전조건 | 유효한 JWT, role=ADMIN, 대상이 PENDING 상태 |
| 입력 | expertId + (거절 시, 선택) reason |
| 처리 | 신청서의 경력 연차를 ADMIN이 참고해 최종 승인/거절. 승인 시 `users.role`=EXPERT, `expert_profile.status`=APPROVED. 거절 시 `expert_profile.status`=REJECTED + `reject_reason` 저장(재신청 가능) |
| 출력(성공) | 200 |
| 예외 | ADMIN 아님 → 403 / 없는 expertId → 404(EXPERT_PROFILE_NOT_FOUND) / PENDING 아닌 상태에서 승인 시도 → 409(EXPERT_APPROVE_INVALID_STATUS) / PENDING 아닌 상태에서 거절 시도 → 409(EXPERT_REJECT_INVALID_STATUS) |
| 관련 화면 | S-13(전문가 심사 탭) |
| 관련 API | PATCH /api/admin/experts/{id}/approve · PATCH /api/admin/experts/{id}/reject |

#### F-27 전문가 목록 관리

| 항목 | 내용 |
| --- | --- |
| 액터 | ADMIN |
| 사전조건 | 유효한 JWT, role=ADMIN, 대상이 APPROVED 상태 |
| 입력 | (목록) status, page, size / (박탈) expertId, reason(선택) |
| 처리 | 목록 조회는 status로 필터링. 박탈 시 `expert_profile.status`=REJECTED로 재사용 + `users.role`=USER 원복 + 해당 전문가의 진행 중 스레드를 EXPERT_REVOKED 사유로 일괄 종료(F-58) |
| 출력(성공) | 200 + Page / 204 No Content |
| 예외 | ADMIN 아님 → 403 / 없는 expertId → 404(EXPERT_PROFILE_NOT_FOUND) / APPROVED 아닌 대상 박탈 시도 → 409(EXPERT_REVOKE_INVALID_STATUS) |
| 관련 화면 | S-13(전문가 심사 탭) |
| 관련 API | GET /api/admin/experts · DELETE /api/admin/experts/{id} |
| 정책 | 박탈된 전문가도 **즉시 재신청 가능**하다. `expert_profile.status`를 REJECTED로 재사용하므로 F-25의 재신청 경로가 그대로 적용된다. 별도 쿨다운이나 영구 차단은 두지 않으며, 재신청 건은 ADMIN이 다시 심사해 판단한다. 재승인되면 F-74에 따라 같은 구독자와 새 스레드도 열 수 있다 |
| ✏️ 프론트 | 전문가 심사 탭에는 박탈 버튼이 없고, 신고 관리 탭에서 신고된 상담의 담당 전문가를 박탈하는 경로로만 호출된다. 관리자가 신고 없이 임의 박탈할 UI는 미구현 |

#### F-28 구독 신청/취소/조회

| 항목 | 내용 |
| --- | --- |
| 액터 | USER |
| 사전조건 | 유효한 JWT, 계정 상태 ACTIVE, 이용 가능한 구독(ACTIVE/PAST_DUE)이 없어야 함 |
| 입력 | (신청) 없음 — 카드 등록(F-60) 선행 필요 / (해지·조회) 없음 |
| 처리 | **Mock이 아니라 PortOne V2 실연동이다.** 등록된 빌링키로 첫 달을 즉시 청구하고, PG 응답이 아닌 결제 단건 재조회 결과(F-66)로 성공을 판정한 뒤 구독을 생성한다. `subscription`은 status=ACTIVE, `auto_renew`=true, `expired_at`=현재+1개월로 저장되고 `users.is_subscribed`도 함께 갱신. 해지는 상태 전이가 아니라 `auto_renew`=false로 처리해 만료일까지 이용 가능하게 둔다 |
| 출력(성공) | 201 + 구독 정보 / 200 + { status, expiredAt, autoRenew, remainingRetryCount } |
| 예외 | 미로그인 → 401 / 이미 구독 중 재신청 → SUBSCRIPTION_ALREADY_ACTIVE / 등록된 카드 없음 → BILLING_KEY_NOT_FOUND / 결제 검증 실패 → PAYMENT_VERIFICATION_FAILED / 구독 내역 없음 → 404(SUBSCRIPTION_NOT_FOUND) |
| 관련 화면 | S-09(구독 결제) |
| 관련 API | POST /api/payments/subscriptions/billing-key · DELETE /api/subscriptions · GET /api/subscriptions/me |
| 변경 | 기존 오픈 이슈("Controller 없어 전부 미구현") 해소. 상태값이 ACTIVE/CANCELLED 2종에서 **ACTIVE/PAST_DUE/EXPIRED 3종 + autoRenew 플래그**로 변경됨 |
| ⚠️오픈 이슈 | `POST /api/subscriptions`(결제 없이 구독을 생성하는 구버전 Mock 경로)가 여전히 살아 있다. 이 엔드포인트를 호출하면 **결제 없이 구독이 생성**되므로 제거 또는 차단이 필요하다 |

#### F-29 모집글 끌올 기능

| 항목 | 내용 |
| --- | --- |
| 액터 | USER(구독자이자 방장) |
| 사전조건 | 없음 |
| 입력 | keyword, page, size, studyId |
| 처리 | 기존 명세의 "구독 중인 방장 스터디를 자동으로 상단 고정"하는 방식이 아니라, 방장이 직접 "끌올" 처리 버튼을 눌러야 목록 상단으로 올라가는 수동 방식으로 구현되어 있음. 목록은 bumped_at(끌올 시각, 끌올한 적 없으면 created_at) 내림차순 정렬. 끌올은 구독 중인 방장만, 24시간에 한 번, 모집 마감일이 지나지 않은 스터디에서만 가능 |
| 출력(성공) | 200 + Page(content, totalPages) |
| 예외 | 방장 아님 → 403 / 비구독자 → 403 / 모집 마감된 스터디 → 409 / 24시간 이내 재요청 → 409 |
| 관련 화면 | S-06(스터디 목록) |
| 관련 API | GET /api/studies |

#### F-30 전문가 1:1 문의

| 항목 | 내용 |
| --- | --- |
| 액터 | USER(구독자), EXPERT |
| 사전조건 | (개설) 로그인 + 구독 이용 가능 + 대상 전문가 APPROVED / (메시지) 요청자 본인 또는 담당 전문가 + 스레드가 종료되지 않음 |
| 입력 | (개설, multipart) topic, expertProfileId, content, images[] / (메시지, multipart) content, images[] / (목록) page, size |
| 처리 | 구독자가 담당 전문가를 지정해 `feedback`(status=PENDING)을 생성하고, 이후 양측이 `feedback_message`로 대화한다. 요청은 이미지 첨부를 위해 **multipart/form-data**로 받는다(F-70). 전문가 첫 답변 시 status가 PENDING→ANSWERED로 전이되며 질문자에게 메일이 발송된다(F-54). 개설 시 중복·개수 제한과 동시성 제어가 적용된다(F-57, F-73). 목록 조회는 페이지 단위로 반환된다(F-69) |
| 출력(성공) | 201 Created(개설·메시지) / 200 + Page(목록) / 200 + 상세·메시지 목록 |
| 예외 | 비구독자 개설 → 403(SUBSCRIPTION_REQUIRED) / 미승인 전문가 지정 → 403(FEEDBACK_EXPERT_NOT_APPROVED) / 참여자 아닌 자의 메시지 전송 → 403(FEEDBACK_ACCESS_DENIED) / 종료된 스레드에 작성 → 409(FEEDBACK_CLOSED) / 구독 만료 상태에서 작성 → 409(FEEDBACK_SUBSCRIPTION_EXPIRED) / 탈퇴·정지 회원 대상 → 409(FEEDBACK_USER_INACTIVE) / 없는 스레드 → 404(FEEDBACK_NOT_FOUND) |
| 관련 화면 | S-11(전문가 상담) 
· DELETE /api/feedbacks/{id} |
| 관련 API | POST /api/feedbacks · GET /api/feedbacks/{id} · POST /api/feedbacks/{id}/messages · GET /api/feedbacks/{id}/messages · GET /api/feedbacks/me · GET /api/feedbacks/expert · PATCH /api/feedbacks/{id}/close |
| 변경 | multipart 전송, 목록 페이지네이션, 종료 상태 처리, 예외 코드 5종이 명세에 없었음 |

#### F-31 스터디 개설/참가 무제한

| 항목 | 내용 |
| --- | --- |
| 액터 | USER(구독자) |
| 사전조건 | 로그인 + 구독 상태 ACTIVE(`users.is_subscribed`=true) |
| 처리 | F-09(스터디 개설), F-12(스터디 신청) 로직에서 구독자는 개설+참여 합산 2개 제한 미적용 |
| 출력(성공) | 200/201(기존 F-09/F-12 API에 조건만 추가) |
| 예외 | 없음(기존 F-09/F-12 예외 그대로) |
| 관련 화면 | 없음(서버 로직만) |
| 관련 API | F-09/F-12와 동일 엔드포인트 |
| 비고 | `isSubscribed()` 플래그만 참조하므로, F-28(구독 API) 없이도 DB에서 플래그를 직접 켜면 이 로직 자체는 정상 동작 확인 가능 |

#### F-32 전문가 목록 조회(공개)

| 항목 | 내용 |
| --- | --- |
| 액터 | GUEST, USER, ADMIN, EXPERT (전체 공개) |
| 사전조건 | 없음 |
| 입력 | page, size(**기본 12**) |
| 처리 | `expert_profile.status`=APPROVED인 건을 페이징 조회한다. 닉네임과 대표 경력 1건 요약("회사명 · 직함 · N년차")만 노출하며, 전체 경력·자격증·소개글은 F-35 상세에서 제공한다. 상담 내역·스레드 정보는 포함하지 않는다 |
| 출력(성공) | 200 + data: [{ expertId, nickname, career }] + meta.pagination: { totalPages, totalItems } |
| 예외 | 없음 |
| 관련 화면 | S-11(전문가 상담) - "활동 중인 전문가" 섹션 |
| 관련 API | GET /api/experts |
| 변경 | 페이징 지원(기본 12건)이 명세에 없었음 |

#### F-35 전문가 프로필 상세

| 항목 | 내용 |
| --- | --- |
| 액터 | GUEST, USER, ADMIN, EXPERT (전체 공개) |
| 사전조건 | 없음 |
| 입력 | expertId |
| 처리 | `expert_profile.status`=APPROVED인 대상의 닉네임·전체 경력 목록·자격증 목록·소개글을 조회 |
| 출력(성공) | 200 + { expertId, nickname, introduction, careers[], certifications[] } |
| 예외 | 없는 expertId → 404 / APPROVED 아닌 대상 → 404 |
| 관련 화면 | S-11(전문가 상담) - 프로필 상세 모달 |
| 관련 API | GET /api/experts/{id} |

#### F-36 전문가 신청 현황 조회

| 항목 | 내용 |
| --- | --- |
| 액터 | USER(신청자 본인) |
| 사전조건 | 유효한 JWT, 본인의 전문가 신청 이력 존재 |
| 입력 | 없음(토큰 사용자 기준) |
| 처리 | 요청자 본인의 `expert_profile` 상태(PENDING/APPROVED/REJECTED)와 반려 사유를 조회. 신청 내용 자체는 반환하지 않음 |
| 출력(성공) | 200 + { status, reason } |
| 예외 | 토큰 없음 → 401 / 신청 이력 없음 → 404(EXPERT_PROFILE_NOT_FOUND) |
| 관련 화면 | S-10-2(신청 현황) |
| 관련 API | GET /api/experts/me |

#### F-37 전문가 신청서 수정/재신청

| 항목 | 내용 |
| --- | --- |
| 액터 | USER(신청자 본인) |
| 사전조건 | 유효한 JWT, 상태가 PENDING(수정) 또는 REJECTED(재신청) |
| 입력 | careers, certifications, introduction |
| 처리 | PENDING 상태면 `PATCH /api/experts/me`로 신청 내용을 덮어쓰기 수정. REJECTED 상태면 `POST /api/experts/signup` 재호출로 새 신청 제출(status→PENDING) |
| 출력(성공) | 200(수정) / 201(재신청) |
| 예외 | 토큰 없음 → 401 / APPROVED 상태에서 수정 시도 → 409(EXPERT_UPDATE_INVALID_STATUS) / REJECTED 아닌 상태에서 재신청 → 409(EXPERT_REAPPLY_INVALID_STATUS) |
| 관련 화면 | S-10-2(신청 현황) |
| 관련 API | PATCH /api/experts/me · POST /api/experts/signup |

---

#### F-57 스레드 중복 개설 및 개수 제한

| 항목 | 내용 |
| --- | --- |
| 액터 | USER(구독자) |
| 사전조건 | 로그인 + 구독 상태 이용 가능, 대상 전문가가 APPROVED 상태 |
| 입력 | expertProfileId, topic, content |
| 처리 | 요청자 행을 비관적 락으로 잠근 뒤(F-73) 두 조건을 검사한다. ① 해당 전문가와 `closed_at IS NULL`인 스레드가 이미 있으면 거부 ② 요청자의 진행 중 스레드가 5개 이상이면 거부. 통과 시 `feedback` 생성(status=PENDING) |
| 출력(성공) | 201 Created |
| 예외 | 동일 전문가와 진행 중 스레드 존재 → 409(FEEDBACK_ALREADY_OPEN) / 진행 중 5개 초과 → 409(FEEDBACK_OPEN_LIMIT_EXCEEDED) / 비구독자 → 403(SUBSCRIPTION_REQUIRED) / 미승인 전문가 → 403(FEEDBACK_EXPERT_NOT_APPROVED) / 탈퇴·정지 전문가 → 409(FEEDBACK_USER_INACTIVE) |
| 관련 화면 | S-11(전문가 상담) |
| 관련 API | POST /api/feedbacks |

#### F-58 스레드 강제 종료

| 항목 | 내용 |
| --- | --- |
| 액터 | USER(요청자 본인), 시스템(전문가 박탈 시) |
| 사전조건 | 유효한 JWT, 스레드가 종료되지 않은 상태 |
| 입력 | feedbackId |
| 처리 | 요청자 본인 확인 후 `closed_at`·`closed_by`(REQUESTER_CLOSED) 기록. 전문가 자격 박탈(F-27) 시에는 해당 전문가의 진행 중 스레드가 EXPERT_REVOKED 사유로 일괄 종료된다. 종료된 스레드는 삭제되지 않고 조회만 가능 |
| 출력(성공) | 200 |
| 예외 | 요청자 아님 → 403(FEEDBACK_ACCESS_DENIED) / 이미 종료됨 → 409(FEEDBACK_CLOSED) / 없는 id → 404(FEEDBACK_NOT_FOUND) |
| 관련 화면 | S-11(전문가 상담) |
| 관련 API | PATCH /api/feedbacks/{id}/close |

#### F-59 구독 만료 시 스레드 접근 제한

| 항목 | 내용 |
| --- | --- |
| 액터 | USER(구독 만료·결제 실패 차단 상태) |
| 사전조건 | 스레드가 이미 존재 |
| 입력 | feedbackId, content(메시지 전송 시) |
| 처리 | 메시지 작성 시점에 요청자의 구독 이용 가능 여부를 확인해 차단. 조회 계열은 제한하지 않는다. 재결제 성공 시 별도 처리 없이 즉시 복구됨 |
| 출력(성공) | (차단 해제 상태에서) 201 |
| 예외 | 구독 만료 상태에서 메시지 작성 → 409(FEEDBACK_SUBSCRIPTION_EXPIRED) / 신규 개설 시도 → 403(SUBSCRIPTION_REQUIRED) |
| 관련 화면 | S-11(전문가 상담) |
| 관련 API | POST /api/feedbacks/{id}/messages · POST /api/feedbacks |

#### F-60 결제수단(카드) 등록

| 항목 | 내용 |
| --- | --- |
| 액터 | USER |
| 사전조건 | 유효한 JWT, 계정 상태 ACTIVE |
| 입력 | (준비) 없음 / (완료) billingKey, billingIssueToken(수동 승인형인 경우) |
| 처리 | ① 준비 단계에서 `issueId`·`customerId`를 발급하고 공개 설정값(storeId, channelKey)을 내려준다. ② 프론트가 PortOne 카드 등록창에서 발급받은 빌링키를 전달하면, 서버가 PortOne에 **단건 재조회**해 발급 상태(ISSUED)와 상점을 검증한 뒤 저장한다. ③ 빌링키는 `BillingKeyConverter`가 **AES/GCM**으로 암호화해 DB에 넣는다(IV 12바이트, 태그 128비트). ④ 채널이 수동 승인형이면 빌링키가 `NEEDS_CONFIRMATION` 자리표시자로 오므로, `billingIssueToken`으로 서버가 발급을 확정한 뒤 진짜 키를 받는다. ⑤ 유저당 활성 카드는 1장이며 기존 카드가 있으면 폐기 후 교체 |
| 출력(성공) | 200 + { storeId, channelKey, issueId, customerId } / 200 |
| 예외 | 토큰 없음 → 401 / 탈퇴·정지 계정 → 409(USER_INACTIVE) / 발급 검증 실패 → 409(BILLING_KEY_VERIFICATION_FAILED) / PortOne 통신 오류 → 502(PORTONE_API_ERROR) |
| 관련 화면 | S-09(구독 결제), S-14(마이페이지-결제수단) |
| 관련 API | POST /api/billing-keys/prepare · POST /api/billing-keys/complete |
| 비고 | 카드번호·유효기간·CVC는 PortOne 창에서만 입력되며 서버·프론트를 거치지 않는다 |

#### F-61 정기결제 자동 갱신

| 항목 | 내용 |
| --- | --- |
| 액터 | 시스템(스케줄러) |
| 사전조건 | 구독이 ACTIVE이고 `auto_renew`=true, 등록된 카드 존재 |
| 입력 | 없음(스케줄 트리거) |
| 처리 | **PortOne 예약 API를 쓰지 않는다.** 매일 04:00에 만료 1일 전 이내인 ACTIVE 구독과 PAST_DUE 구독을 조회해 빌링키로 직접 청구한다. 성공 시 `renew()`로 기간을 연장하는데, 기준일은 만료 전이면 `기존 만료일`, 이미 지났으면 `현재 시각`으로 잡아 결제일이 매달 앞당겨지지 않게 한다. 스케줄러 메서드에는 `@Transactional`을 붙이지 않고 건별로 트랜잭션을 열어, 한 사용자의 실패가 다른 사용자 처리를 되돌리지 않게 한다 |
| 출력(성공) | 없음(배치) — `expired_at` +1개월, `retry_count`=0 |
| 예외 | 카드 없음 → 실패로 기록 후 다음 건 진행 / 청구 실패 → F-62로 이관 |
| 관련 화면 | 없음(서버 로직만) |
| 관련 API | 없음 |
| 비고 | 만료 1일 전부터 시도하는 이유는 스케줄러가 하루 한 번만 돌기 때문. 최소 1일 앞서야 어떤 만료 시각이든 만료 전에 한 번은 청구할 수 있다 |

#### F-62 결제 실패 유예·재시도

| 항목 | 내용 |
| --- | --- |
| 액터 | 시스템(스케줄러), USER(수동 재시도) |
| 사전조건 | (수동) 유효한 JWT, 구독이 PAST_DUE 상태 |
| 입력 | 없음 |
| 처리 | 청구 실패 시 `status`=PAST_DUE, `retry_count` 증가. 카드사 거절처럼 정상적으로 실패할 수 있는 경우는 예외를 던지지 않고 boolean으로만 알린다 — 예외를 던지면 트랜잭션이 롤백되어 방금 늘어난 실패 횟수까지 사라지기 때문이다. 자동 재시도는 하루 1회, 연속 3회(`MAX_RETRY`) 실패 시 구독을 만료시킨다. 사용자는 즉시 재시도를 요청할 수 있으며, 이 경로는 `MANUAL_RETRY` 모드로 구분되어 **자동 재시도 횟수를 소진하지 않는다**. 60초 쿨다운이 적용된다 |
| 출력(성공) | 200 + 구독 정보 |
| 예외 | PAST_DUE 아닌 상태에서 재시도 → SUBSCRIPTION_NOT_PAST_DUE / 60초 이내 재호출 → 429(PAYMENT_RETRY_TOO_SOON) / 카드 없음 → 404(BILLING_KEY_NOT_FOUND) |
| 관련 화면 | S-09(구독 결제 - 결제 실패 화면) |
| 관련 API | POST /api/payments/subscriptions/retry |
| 비고 | 결제 성공 시 `retry_count`가 0으로 초기화되어 유예 상태가 즉시 해제된다 |

#### F-63 자동 갱신 재개

| 항목 | 내용 |
| --- | --- |
| 액터 | USER |
| 사전조건 | 유효한 JWT, 구독이 이용 가능 상태(ACTIVE/PAST_DUE), `auto_renew`=false, **등록된 카드 존재** |
| 입력 | 없음 |
| 처리 | 해지 예약 상태에서 자동 갱신을 다시 켠다. 카드 유무를 먼저 확인하는데, 카드 없이 자동 갱신만 켜면 스케줄러가 매일 청구에 실패하고 3회 만에 아직 남아 있던 이용 기간까지 만료시켜 버리기 때문이다. 카드 유효성 자체는 실제 결제 시점에 확인된다 |
| 출력(성공) | 200 + 구독 정보 |
| 예외 | 카드 없음 → 404(BILLING_KEY_NOT_FOUND) / 이미 자동 갱신 중 → SUBSCRIPTION_ALREADY_AUTO_RENEW / 구독 없음 → 404(SUBSCRIPTION_NOT_FOUND) |
| 관련 화면 | S-09(구독 결제 - 해지 예약 화면) |
| 관련 API | POST /api/payments/subscriptions/resume |

#### F-64 결제수단 관리

| 항목 | 내용 |
| --- | --- |
| 액터 | USER |
| 사전조건 | 유효한 JWT |
| 입력 | (조회·삭제) 없음 / (변경) F-60과 동일 |
| 처리 | 조회 시 카드사·마스킹 번호·등록일만 반환하고 빌링키 원문은 응답에 포함하지 않는다. 변경은 F-60의 등록 흐름을 그대로 태우며 기존 카드를 교체하되 구독 상태와 결제일은 유지된다. 삭제 시 PortOne 쪽 빌링키와 서버 저장분을 함께 폐기하고, 남아 있는 구독의 자동 갱신도 함께 해지한다(`disableAutoRenewIfUsable`). 이미 결제된 기간은 만료일까지 유지된다 |
| 출력(성공) | 200 + { cardCompany, cardNumberMasked, issuedAt } / 200 |
| 예외 | 카드 없음 → 404(BILLING_KEY_NOT_FOUND) / 토큰 없음 → 401 |
| 관련 화면 | S-14(마이페이지-결제수단) |
| 관련 API | GET /api/billing-keys/me · DELETE /api/billing-keys · POST /api/billing-keys/prepare·complete |

#### F-65 미사용 결제수단 정리

| 항목 | 내용 |
| --- | --- |
| 액터 | 시스템(스케줄러) |
| 사전조건 | 마지막 청구 성공 이후 90일 경과 |
| 입력 | 없음(스케줄 트리거) |
| 처리 | 매일 05:30에 90일 이상 미사용 카드를 조회해 정리한다. 갱신 청구(04:00) 이후에 도는 이유는 방금 청구에 성공한 카드가 정리 대상으로 잡히지 않게 하기 위해서다. 삭제 경로는 사용자가 직접 삭제할 때(F-64)와 동일한 메서드를 태워 PortOne 빌링키까지 함께 지우고 자동 갱신도 해제한다. 한 건이 실패해도 나머지 정리는 계속된다 |
| 출력(성공) | 없음(배치) |
| 예외 | 개별 건 실패 시 로그만 남기고 다음 건 진행 |
| 관련 화면 | 없음(서버 로직만) |
| 관련 API | 없음 |
| 비고 | 정기결제가 도는 카드는 매달 사용 기록이 갱신되어 대상에서 자동 제외된다 |

#### F-66 결제 검증

| 항목 | 내용 |
| --- | --- |
| 액터 | 시스템 |
| 사전조건 | 결제 요청 후 |
| 입력 | paymentId |
| 처리 | PG 응답이 아니라 **결제 단건 재조회 결과**로 성공을 판정한다. 아래 6개 항목을 순서대로 대조하고 하나라도 어긋나면 결제를 FAILED로 기록한 뒤 구독 반영을 거부한다. ① 결제 상태가 PAID인가 ② storeId 일치 ③ channelKey 일치 ④ 채널 환경(TEST/LIVE)이 설정과 일치 ⑤ 금액 일치 ⑥ 통화 일치. 완료 API는 멱등 처리되어 이미 확정된 결제에 재호출해도 에러 대신 현재 상태를 반환한다 |
| 출력(성공) | (호출한 API의 성공 응답) |
| 예외 | 검증 실패 → 409(PAYMENT_VERIFICATION_FAILED) / 본인 결제 아님 → 403(PAYMENT_FORBIDDEN) / 결제 준비 내역 없음 → 404(PAYMENT_NOT_FOUND) |
| 관련 화면 | 없음(서버 로직만) |
| 관련 API | 결제 관련 전 API 공통 |
| 비고 | ④번 검증은 테스트 채널로 설정해뒀는데 실거래가 잡히는(혹은 그 반대) 환경 오반영을 막는다 |

#### F-67 환불 정책

| 항목 | 내용 |
| --- | --- |
| 액터 | — |
| 사전조건 | — |
| 입력 | — |
| 처리 | **결제 취소(환불) API를 두지 않는다.** 구독은 결제 즉시 유료 기능을 이용할 수 있어 일할 환불이 성립하지 않기 때문이다. 취소 요청은 해지(F-28)로 처리해 다음 회차 결제만 중단하고, 이미 결제된 기간은 만료일까지 이용 가능하게 둔다 |
| 출력(성공) | — |
| 예외 | — |
| 관련 화면 | S-09(구독 결제 - 해지 안내 문구) |
| 관련 API | 없음(의도적 부재) |
| 비고 | 결제 취소가 필요한 예외 상황은 PortOne 관리자 콘솔에서 수동 처리한다 |

#### F-69 스레드 목록 페이지네이션

| 항목 | 내용 |
| --- | --- |
| 액터 | USER(구독자), EXPERT |
| 사전조건 | 유효한 JWT |
| 입력 | page, size(기본 20), sort(기본 createdAt DESC) |
| 처리 | 내 문의 내역과 전문가 담당 목록을 전체 조회가 아닌 페이지 단위로 반환. 목록 표시에 필요한 연관 엔티티는 `JOIN FETCH`로 함께 조회해 N+1을 방지 |
| 출력(성공) | 200 + Page(content, totalElements, totalPages) |
| 예외 | 토큰 없음 → 401 |
| 관련 화면 | S-11(전문가 상담) |
| 관련 API | GET /api/feedbacks/me · GET /api/feedbacks/expert |

#### F-70 스레드 이미지 첨부

| 항목 | 내용 |
| --- | --- |
| 액터 | USER(요청자), EXPERT(담당 전문가) |
| 사전조건 | 유효한 JWT, 스레드 참여자 |
| 입력 | (multipart/form-data) 요청 JSON + images(파일, 다건) |
| 처리 | 이미지 형식·용량을 검증한 뒤 스토리지에 업로드하고 `feedback_message_image`에 메시지와 연결해 저장. 스레드 개설 시 첫 메시지에도 첨부 가능 |
| 출력(성공) | 201 Created — 메시지 조회 시 imageUrls 포함 |
| 예외 | 허용되지 않는 형식·용량 초과 → 400 / 스레드 참여자 아님 → 403(FEEDBACK_ACCESS_DENIED) |
| 관련 화면 | S-11(전문가 상담) |
| 관련 API | POST /api/feedbacks · POST /api/feedbacks/{id}/messages |

#### F-71 스레드 신고

| 항목 | 내용 |
| --- | --- |
| 액터 | USER, EXPERT |
| 사전조건 | 유효한 JWT |
| 입력 | targetType=FEEDBACK, targetId(feedbackId), reason, detail |
| 처리 | 게시글·댓글과 동일한 신고 엔드포인트를 재사용하며 `ReportTargetType.FEEDBACK`으로 구분해 저장. 접수된 건은 관리자 신고 목록(F-56)에 노출되고, 이 신고 이력이 F-72 관리자 열람의 전제 조건이 된다 |
| 출력(성공) | 201 Created |
| 예외 | 중복 신고 → 409(REPORT_ALREADY_EXISTS) / 신고 권한 없음 → 403(REPORT_ACCESS_DENIED) / 대상 없음 → 404(REPORT_TARGET_NOT_FOUND) |
| 관련 화면 | S-11(전문가 상담) |
| 관련 API | POST /api/reports |

#### F-72 관리자 스레드 열람

| 항목 | 내용 |
| --- | --- |
| 액터 | ADMIN |
| 사전조건 | 유효한 JWT, role=ADMIN, **해당 스레드에 신고 이력이 존재** |
| 입력 | feedbackId |
| 처리 | 상담 내용은 원칙적으로 비공개이므로, 신고가 접수된 스레드에 한해서만 내용과 메시지(이미지 포함)를 열람할 수 있게 한다. 신고 이력 유무를 게이트로 검사 |
| 출력(성공) | 200 + 스레드 상세 / 200 + 메시지 목록 |
| 예외 | 신고 이력 없는 스레드 조회 → 403(FEEDBACK_NOT_REPORTED) / ADMIN 아님 → 403 / 없는 id → 404(FEEDBACK_NOT_FOUND) |
| 관련 화면 | S-13(어드민, 신고 관리 탭) |
| 관련 API | GET /api/admin/feedbacks/{id} · GET /api/admin/feedbacks/{id}/messages |

#### F-73 스레드 개설 동시성 제어

| 항목 | 내용 |
| --- | --- |
| 액터 | 시스템 |
| 사전조건 | F-57 스레드 개설 처리 중 |
| 입력 | requesterId |
| 처리 | 중복 검사와 개수 제한 검사가 "조회 후 삽입" 구조라 동시 요청 시 둘 다 통과할 수 있다. 이를 막기 위해 요청자 User 행에 비관적 쓰기 락(`PESSIMISTIC_WRITE`)을 걸고 검사와 삽입을 직렬화한다. 잠금 범위가 사용자 한 명이라 다른 사용자의 요청은 영향받지 않는다 |
| 출력(성공) | (F-57과 동일) |
| 예외 | (F-57과 동일) |
| 관련 화면 | 없음(서버 로직만) |
| 관련 API | POST /api/feedbacks |
| 비고 | `feedback` 테이블 유니크 제약으로는 해결 불가. 중복 조건이 `closed_at IS NULL` 부분 인덱스를 요구하는데 MySQL이 지원하지 않고, "전체 5개 제한"은 애초에 제약으로 표현할 수 없기 때문 |

#### F-74 박탈 후 재승인 시 스레드 재개설

| 항목 | 내용 |
| --- | --- |
| 액터 | USER(구독자) |
| 사전조건 | 대상 전문가가 박탈 후 재승인되어 APPROVED 상태 |
| 입력 | (F-57과 동일) |
| 처리 | 박탈 시 기존 스레드가 EXPERT_REVOKED 사유로 종료되므로 `closed_at`이 채워진다. F-57의 중복 검사는 `closed_at IS NULL`만 보기 때문에, 재승인 후 동일 조합으로 새 스레드를 여는 데 제약이 없다. 별도 코드 없이 구조상 성립 |
| 출력(성공) | 201 Created |
| 예외 | (F-57과 동일) |
| 관련 화면 | S-11(전문가 상담) |
| 관련 API | POST /api/feedbacks |

#### F-75 회원 탈퇴 시 연쇄 처리

| 항목 | 내용 |
| --- | --- |
| 액터 | USER |
| 사전조건 | 유효한 JWT, 비밀번호 확인 통과 |
| 입력 | password |
| 처리 | 탈퇴 처리 과정에서 ① 구독 자동 갱신 해지(`disableAutoRenewIfUsable` — 구독이 없거나 이미 해지 상태면 아무 동작 안 함) ② 요청자가 연 진행 중 스레드를 REQUESTER_WITHDRAWN 사유로 일괄 종료 ③ 방장 스터디 소프트 딜리트·멤버십 삭제 ④ 계정 마스킹 및 WITHDRAWN 전환을 수행한다 |
| 출력(성공) | 200 |
| 예외 | 비밀번호 불일치 → 401 |
| 관련 화면 | S-14(마이페이지-설정) |
| 관련 API | DELETE /api/users/me |

#### F-76 상담 스레드 삭제

| 항목 | 내용 |
| --- | --- |
| 액터 | USER(요청자 본인) |
| 사전조건 | 유효한 JWT, 본인이 개설한 스레드, 신고 이력이 없을 것 |
| 입력 | feedbackId |
| 처리 | 요청자 본인 여부를 확인하고, 신고 이력이 있으면 거부한다. 통과하면 `close(REQUESTER_CLOSED)`로 먼저 종료 처리한 뒤(이미 닫혀 있으면 무시됨) 소프트 삭제한다. `Feedback`에 `@SoftDelete`가 적용되어 있어 `deleted=true`로만 표시되고 **모든 조회에서 자동 제외**된다. 따라서 요청자·전문가 목록과 관리자 열람에서 모두 사라지며, 중복 검사(F-57)와 개수 제한 대상에서도 빠진다 |
| 출력(성공) | 200 |
| 예외 | 요청자 아님(전문가 포함) → 403(FEEDBACK_DELETE_DENIED) / 신고 접수된 스레드 → 409(FEEDBACK_REPORTED_CANNOT_DELETE) / 없는 스레드 → 404(FEEDBACK_NOT_FOUND) |
| 관련 화면 | S-11(전문가 상담 - 상담 상세) |
| 관련 API | DELETE /api/feedbacks/{id} |
| 비고 | `Feedback`에 `@SoftDelete`가 붙으면서 `FeedbackMessage`의 `Feedback` 참조를 LAZY→EAGER로 변경해야 했다(Hibernate 6.4+ 제약). `Comment`·`PostImage`가 겪은 것과 같은 사안 |
| ⚠️오픈 이슈 | 신고보다 삭제가 먼저 이뤄지면 관리자가 심사할 수 없다. 부적절한 대화 직후 요청자가 삭제하면 전문가가 신고할 대상이 사라진다. "삭제 후 일정 기간 신고 가능" 같은 보완이 필요할 수 있음 |

---

### 선택사항

#### F-OPT-01 스터디 가입 승인/거절 (선택)

| 항목 | 내용 |
| --- | --- |
| 액터 | USER(방장) |
| 사전조건 | 방장이 자유가입(F-10) 대신 승인제 스터디로 설정한 경우 |
| 입력 | studyId, applicationId, status(APPROVED/REJECTED) |
| 처리 | 신청을 대기 상태로 두었다가 방장이 승인/거절 처리 |
| 출력(성공) | 200 |
| 예외 | 방장 아님 → 403 / 없는 applicationId → 404 |
| 관련 화면 | S-07(상세) |
| 관련 API | PATCH /api/studies/{id}/applications/{appId} |
| ⚠️오픈 이슈 | 미구현. `StudyMemberService.joinStudy()`가 승인 대기 상태 없이 즉시 가입 처리하도록만 구현되어 있음 |

---

## v4 변경 이력 요약 (최신 코드 재검증)

| 구분 | 내용 |
| --- | --- |
| 상태 코드 정정 | F-11, F-15, F-17, F-18, F-20, F-21에 있던 “204” 표기를 “200”으로 정정 — 코드 전체에서 204를 반환하는 곳은 F-27(전문가 자격 박탈) 한 곳뿐 |
| F-02/F-03 | 로그인 응답에 `tokenType` 필드가 없음을 명시, 계정 정지/탈퇴 시 로그인 차단(403) 로직 추가 반영 |
| F-09 | `recruit_start`는 입력값이 아니라 개설일로 자동 설정됨을 명시 |
| F-10/F-29 | category 필터·구독자 상단고정 모두 백엔드 미구현임을 명시(오픈 이슈로 전환) |
| F-12 | 엔드포인트 경로 `/join` → `/members`로 정정 |
| F-14 | 페이징·keyword 미지원, List 응답임을 명시 |
| F-20 | 엔드포인트 경로 `/members/me` → `/leave`로 정정, 일반 멤버·방장 겸용 API임을 명시 |
| F-22 | 관리자 유저 목록에 keyword/role 필터가 없다는 오픈 이슈 추가 |
| F-24 | ⚠️ 신규 오픈 이슈: 방장 스터디 존재 시 탈퇴 차단(409), 구독 취소 처리 로직이 실제 코드에 없음을 확인·기재 |
| F-25 | 입력 구조를 실제 DTO(`careers[]`, `certifications[]`, `introduction`) 기준으로 전면 재작성 |
| F-26 | 요청 필드명 `reject_reason` → `reason`으로 정정, 관련 화면 S-12 → S-13(전문가 심사 탭)으로 정정 |
| F-27 | 관련 화면 S-12 → S-13(전문가 심사 탭)으로 정정, 오픈 이슈를 “박탈 상태값 미정”에서 “박탈 후 즉시 재신청 가능 여부 정책 결정 필요”로 갱신(상태값 자체는 이미 REJECTED 재사용으로 확정됨) |
| F-30 | 스레드 개설 입력에 `topic` 필드 추가, 승인되지 않은 전문가 지정 시 예외 코드 추가 |
| F-32 | 출력 스펙에서 `certification` 필드 제거(실제 응답에 없음), `expertId` 필드 추가 |
| F-22-1, F-33, F-34, F-35, F-36, F-37, F-38 | 요구사항정의서엔 있었으나 기능명세서에 상세 스펙이 빠져있던 7개 항목 신규 작성. F-33·F-34는 이미 구현 완료, 나머지는 미구현 상태로 명시 |
| F-39 | 신규 항목 추가(게시글/스터디 게시글 이미지 첨부, MVP2). 요구사항정의서에 먼저 반영된 내용을 기능명세서에도 동기화 |