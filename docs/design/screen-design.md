# 화면 설계

---

## 1. 메인 화면

| 화면 ID | 화면명 | 누가 보나(역할) | 설명 |
| --- | --- | --- | --- |
| S-00 | 메인(랜딩) | 전체 | 단일 스크롤 페이지: ① Hero(캐치프레이즈 + “무료로 시작하기”/“게시글 둘러보기” CTA) ② 바로가기(Quick Access) 카드 8개 + 어드민 패널(ADMIN ONLY) ③ 서비스 소개 3카드(실시간 취업 정보/스터디 매칭/전문가 멘토링) ④ 하단 가입 유도 CTA 배너. 와이어프레임과 정확히 일치 확인 |

**바로가기 카드 ↔︎ 화면 매핑**

| 카드명 | 접근 라벨 | 연결 화면 | 비고 |
| --- | --- | --- | --- |
| 게시글 | 전체 공개 | S-03 |  |
| 글쓰기 | 로그인 필요 | S-05 |  |
| 스터디 | 전체 공개 | S-06 |  |
| 스터디 개설 | 로그인 필요 | S-08 |  |
| 구독 플랜 | 유료 · PRO | S-09 |  |
| 전문가 상담 | 구독 필요 · PRO | S-11 | 비로그인·미구독자도 진입 가능(블러 처리 안내) |
| 전문가 등록 | 신청 가능 | S-10 |  |
| 마이페이지 | 로그인 필요 | S-14 |  |
| 어드민 패널 | ADMIN ONLY | S-13 |  |

---

## 2. MVP1 화면 목록

메모: 게시글 목록(S-03)은 와이어프레임상 category 필터 UI만 노출되어 있으나, 백엔드는 keyword 검색 파라미터도 함께 지원한다(추후 검색창을 화면에 추가할 수 있음). 스터디 목록(S-06)은 category 필터 + keyword 검색을 함께 지원하는 것으로 와이어프레임에서 확인됨(단, 백엔드에 category 필터는 아직 연결되어 있지 않음 — 아래 오픈 이슈 참고).

| 화면 ID | 화면명 | 누가 보나(역할) | 설명 |
| --- | --- | --- | --- |
| S-01 | 로그인 | GUEST | username(아이디)·password 일반 로그인 또는 카카오/구글/네이버 OAuth2(F-03). accessToken 응답, refreshToken은 HttpOnly 쿠키. “비밀번호 재설정” 링크 존재(⚠️ 아래 오픈 이슈 참고) |
| S-02 | 회원가입 | GUEST | 아이디(영문·숫자 4~20자, 이메일 아님)·비밀번호·비밀번호 확인·닉네임 입력(F-01). 비밀번호 BCrypt 암호화, 역할은 서버에서 기본 USER 부여. 소셜 가입 버튼 존재. 가입 진행률 UI(4단계 체크리스트)는 프론트 전용 연출로, 별도 API 불필요 |
| S-03 | 게시글 목록 | 전체(조회) | 최신순 페이징, category 필터(F-05). 비로그인 조회 가능 — 와이어프레임과 일치 |
| S-04 | 게시글 상세 | 전체(조회)/USER,ADMIN(댓글) | 본문+댓글 목록 조회(F-05), 댓글 등록·수정·삭제는 로그인 필요(F-07,F-08) — 와이어프레임과 일치 |
| S-05 | 게시글 작성/수정 | USER, ADMIN | 작성자 본인: 등록·수정·삭제(F-04,F-06) / ADMIN: 타인 글도 수정·삭제 가능 — 와이어프레임과 일치(카테고리 4종 칩, 제목 100자·본문 글자수 카운터) |
| S-06 | 스터디 목록 | 전체(조회) | keyword 검색 + category 필터(전체/IT개발/언어/자격증/취업준비/기타, F-10) UI 노출. ⚠️ 백엔드 조회 로직에 category 필터 파라미터가 아직 연결되어 있지 않아 실제로는 keyword만 동작함 — 아래 오픈 이슈 참고. 구독자 개설 스터디 상단 고정은 MVP2 F-29 연동. 카드에 “승인제”/“마감” 상태 배지 노출 확인 |
| S-07 | 스터디 상세·게시판 | 전체(조회)/USER(신청·게시판)/방장(관리) | 상세 조회, 자유가입 신청(F-12), 가입 멤버 전용 게시판(F-13~F-17, 공지 고정 지원), 멤버 강퇴(F-18) — 게시판(가입자/미가입자) 및 게시글 상세·댓글 와이어프레임 확인 완료 |
| S-08 | 스터디 개설/수정·방장위임 | USER(방장), ADMIN | 방장 본인: 개설·수정·위임·탈퇴(F-09,F-11,F-19,F-20). 미구독자는 개설+참여 합산 2개 미만 제한. 멤버가 방장 1인뿐이면 위임 없이 삭제/탈퇴 가능 |
| S-13 | 어드민 패널 | ADMIN | 단일 화면 + 3개 탭으로 구성. ① **대시보드**: 총 회원/활성 스터디/오늘 게시글/구독자 수, 전문가 신청 대기 건수, 최근 게시글(삭제 가능)·전문가 신청 현황·스터디 현황 요약 ② **회원 관리**: 닉네임/아이디 검색, 역할별 필터(전체/USER/EXPERT/ADMIN), 상태 변경(정상↔︎정지, F-22). ⚠️ 검색·필터 UI는 와이어프레임에 있으나 백엔드 API가 아직 이 파라미터를 받지 않음 — 아래 오픈 이슈 참고 ③ **전문가 심사**(뱃지로 대기 건수 표시): 심사 대기 목록(승인/반려, F-26) + 처리 완료 이력 |

※ S-07은 방장이 승인제로 전환한 경우 F-OPT-01(가입 승인/거절)도 이 화면에서 처리(선택 기능). ⚠️ 와이어프레임의 “승인제” 배지와 달리 `StudyService.joinStudy()`는 승인 대기 상태 없이 즉시 가입 처리함 — F-OPT-01 미구현 상태 재확인.
※ S-14는 아래 3장에서 마이페이지 전체 구조로 재정의(기존 “마이페이지-탈퇴” 명칭은 5탭 중 설정 탭 하위 기능 중 하나였음이 확인되어 명칭 변경).

---

## 3. S-14 마이페이지

와이어프레임 5장을 확인한 결과 마이페이지는 **상단 프로필 요약 + 5개 탭**으로 구성된 하나의 화면입니다.

| 화면 ID | 화면명 | 누가 보나(역할) | 설명 |
| --- | --- | --- | --- |
| S-14 | 마이페이지 | USER | 상단: 프로필(닉네임·아이디·가입일·역할 배지) + 활동 통계(게시글/스터디/전문가 상담/댓글 수) + 구독 CTA. 하단 5개 탭: |
| S-14-1 | ㄴ 내 활동 | USER | 최근 게시글 3개, 참여 중인 스터디 카드, 구독 유도 배너 (대시보드 역할) |
| S-14-2 | ㄴ 게시글 | USER | 내가 쓴 글 목록(카테고리·조회수·댓글수), 수정/삭제, 새 글쓰기 버튼(F-04/F-06 재사용) |
| S-14-3 | ㄴ 스터디 | USER(방장/멤버) | “내가 개설한 스터디”(관리 버튼) / “참여 중인 스터디”(탈퇴 버튼) 구분 표시. S-08의 관리·탈퇴 플로우와 연결(F-19,F-20) |
| S-14-4 | ㄴ 전문가 상담 | USER(구독 ACTIVE) | 내 상담 스레드 목록, “새 상담 시작” 버튼 — S-11 구독자 뷰와 동일 데이터(F-30) |
| S-14-5 | ㄴ 설정 | USER | 프로필 수정(닉네임), 비밀번호 변경, 알림 설정(스터디·댓글 알림 토글), 로그아웃, **회원 탈퇴**(F-24, 비밀번호 재확인 후 처리 — 방장인 스터디 있으면 위임 전까지 탈퇴 차단, 단독멤버 예외 있음) |

⚠️ **API 미정의 항목** — S-14-5(설정)에서 사용하는 기능 중 닉네임 변경·비밀번호 변경은 API가 이미 구현되어 있음(하단 5장 참고). **알림 설정 저장**만 API 정의가 아직 없어 신규로 추가해야 합니다.

---

## 4. MVP2 화면 목록

| 화면 ID | 화면명 | 누가 보나(역할) | 설명 |
| --- | --- | --- | --- |
| S-09 | 구독 결제/관리 | USER | Mock 결제로 구독 신청/취소/조회(F-28). 서버는 실제 PG 연동 없이 `subscription.status`만 ACTIVE/CANCELLED로 전이, 동시에 `users.is_subscribed` 캐시 플래그 갱신 — ⚠️ 현재 이 API들을 처리할 Controller가 없어 화면과 연결할 백엔드 구현이 필요함. 와이어프레임: FREE/PREMIUM(9,900원) 2단 요금표. ⚠️ PREMIUM 혜택 중 “합격자 자소서 자료 열람”, “기업 인사이트 리포트”는 현재 F-ID/기능 정의가 없는 마케팅 카피로 보임 — 실제 구현 범위인지 확인 필요 |
| S-10 | 전문가 가입 신청 | USER → EXPERT 후보 | 3단계 위저드: ① 경력 입력(회사명·직함·경력연차·직무분야, 다건 추가 가능) ② 자격증/수료증(자격증명·발급기관·취득연도, 다건, 선택사항) ③ 소개글 작성(500자, 실시간 카드 미리보기) → 신청(F-25). 신청 즉시 status=PENDING, 승인 전까지 role은 USER 유지 |
| S-10-2 | ㄴ 신청 현황 | USER(신청자) | 심사중/승인완료/반려 3-상태 탭. 심사중엔 “신청서 수정”·“홈으로”, 승인완료엔 “전문가 페이지로 이동”, 반려엔 반려 사유 목록(경력 기준 미달 등)과 “재신청하기” 버튼 제공. ⚠️ 신청 현황 조회·신청서 수정 API가 아직 없어 신규 정의 필요(하단 5장 참고) |
| S-11 | 전문가 상담 | 전체(조회 일부) / USER(구독 ACTIVE) / EXPERT | **구독 상태별 조건부 렌더링:** |

· 비로그인/미구독: “활동 중인 전문가” 목록(F-32, 공개 — 닉네임·대표 경력 1건 요약만 표시) 정상 노출 + 본인 문의 내역 영역은 블러 처리 + “구독하고 시작하기” CTA
· 구독자(ACTIVE): 블러 대신 본인이 개설한 문의 스레드 목록(F-30, `GET /api/feedbacks/me`) 노출 → 클릭 시 스레드 상세(질문/답변 이어가기)로 이동
· EXPERT: 담당 문의 스레드 목록(`GET /api/feedbacks/expert`) 및 답변 작성 화면(대칭 구조). 와이어프레임(공개/구독자/상세 3장) 모두 일치 확인 |
| S-13(전문가 심사 탭) | ~~S-12 어드민-전문가 관리~~ | ADMIN | 별도 화면이 아님 — S-13 어드민 패널의 “전문가 심사” 탭으로 구현됨(위 2장 참고). 심사 대기 목록에서 바로 승인/반려(F-26, reject_reason 포함), 처리 완료 이력 조회. 자격 박탈(F-27) 시 백엔드는 status를 REJECTED로 재사용하고 role을 USER로 원복하도록 확정됨 — ⚠️ 다만 이 경우 박탈된 계정이 즉시 재신청 가능한 상태가 되므로, 박탈에 재신청 제한을 둘지는 별도 정책 결정 필요. 와이어프레임상 “박탈” 전용 버튼이 있는지, 회원관리 탭의 “정지” 처리로 대체할지도 확인 필요 |

※ S-11은 화면을 A/B로 분리하지 않고, 단일 화면에서 로그인·구독 상태값에 따라 조건부로 콘텐츠를 렌더링함.

---

## 5. 화면 ↔︎ API 매핑

### 메인

| 화면 | 동작 | 호출 API |
| --- | --- | --- |
| S-00 | 바로가기 카드 클릭 | 없음 (라우팅만, 실제 데이터는 각 대상 화면의 API 호출) |

### MVP1

| 화면 | 동작 | 호출 API |
| --- | --- | --- |
| S-01 | 로그인(일반/소셜) | POST /api/auth/login · POST /api/auth/kakao · POST /api/auth/google · POST /api/auth/naver |
| S-01 | 토큰 재발급 / 로그아웃 | POST /api/auth/reissue · POST /api/auth/logout |
| S-01 | 비밀번호 재설정(화면 노출됨, 플로우 미정의) | ⚠️ TBD — 이메일/아이디 인증 방식 결정 필요 |
| S-02 | 회원가입(일반/소셜) | POST /api/auth/signup · POST /api/auth/kakao · POST /api/auth/google · POST /api/auth/naver |
| S-03 | 목록 로드/페이지 이동/검색 | GET /api/posts?page=&size=&category=&keyword= |
| S-04 | 상세 로드 / 댓글 등록·수정·삭제 | GET /api/posts/{id} · POST /api/posts/{postId}/comments · PUT/DELETE /api/posts/{postId}/comments/{commentId} |
| S-05 | 등록 / 수정 / 삭제 | POST /api/posts · PUT /api/posts/{id} · DELETE /api/posts/{id} |
| S-05 | 이미지 첨부 🆕 | ⚠️ POST /api/posts/{id}/images (경로 예시) — 신규 정의 필요(F-39, 저장방식·API형태 미확정) |
| S-06 | 목록 로드/검색 | GET /api/studies?page=&size=&keyword= (category 필터는 ⚠️ 백엔드 미지원, 아래 오픈 이슈 참고) |
| S-07 | 상세 로드 / 신청 / 멤버 목록 / 강퇴 / 게시판 CRUD | GET /api/studies/{id} · POST /api/studies/{id}/members · GET /api/studies/{id}/members · DELETE /api/studies/{id}/members/{userId} · GET·POST·PUT·DELETE /api/studies/{id}/posts[/{postId}][/comments/{commentId}] |
| S-07 (선택) | 가입 신청 승인/거절 | PATCH /api/studies/{id}/applications/{appId} · ⚠️ 현재 `joinStudy()`는 즉시 가입 처리라 승인 대기 로직 없음(F-OPT-01 미구현) |
| S-08 | 개설 / 수정 / 위임 / 탈퇴(방장 포함) | POST /api/studies · PUT /api/studies/{id} · PATCH /api/studies/{id}/leader · DELETE /api/studies/{id}/leave · ⚠️ 스터디 자체를 즉시 삭제하는 API는 없음(방장 단독 탈퇴 시에만 자동 삭제) |
| S-13 | 게시글/댓글/스터디/스터디게시글/스터디댓글 강제 삭제 | DELETE /api/admin/posts/{id} · /comments/{id} · /studies/{id} · /study-posts/{id} · /study-post-comments/{id} |
| S-13 | 유저 목록 조회 / 상태 변경 | GET /api/admin/users?page=&size= (⚠️ keyword·role 필터는 백엔드 미지원, 아래 오픈 이슈 참고) · PATCH /api/admin/users/{id}/status |
| S-13 | 대시보드 통계 | ⚠️ GET /api/admin/dashboard/summary — 신규 정의 필요(총 회원·활성 스터디·오늘 게시글·구독자·전문가 신청 대기 수) |
| S-14 | 회원 탈퇴 | DELETE /api/users/me |
| S-14 | 내 정보 조회 | GET /api/users/me |
| S-14 | 닉네임 변경 | PATCH /api/users/me/nickname |
| S-14 | 비밀번호 변경 | PATCH /api/users/me/password |
| S-14 | 알림 설정 저장 | ⚠️ TBD — 신규 정의 필요 |

### MVP2

| 화면 | 동작 | 호출 API |
| --- | --- | --- |
| S-09 | 구독 신청 / 취소 / 조회 | ⚠️ POST /api/subscriptions · DELETE /api/subscriptions · GET /api/subscriptions/me — Controller 미구현, 신규 개발 필요 |
| S-10 | 전문가 가입 신청(경력·자격증·소개글) | POST /api/experts/signup |
| S-10-2 | 신청 현황 조회 | ⚠️ GET /api/experts/me/application — 신규 정의 필요 |
| S-10-2 | 신청서 수정(심사중) / 재신청(반려 후) | ⚠️ PUT /api/experts/me/application — 신규 정의 필요(반려 후 재신청은 현재 POST /api/experts/signup 재호출로 대체 가능) |
| S-11 | 활동 중인 전문가 목록(공개) | GET /api/experts |
| S-11 | 전문가 프로필 상세 🆕 | ⚠️ GET /api/experts/{id} — 신규 정의 필요(F-35) |
| S-11 | 내 문의 스레드 목록(구독자) | GET /api/feedbacks/me |
| S-11 | 담당 문의 스레드 목록(전문가) | GET /api/feedbacks/expert |
| S-11 | 문의 스레드 개설 / 메시지 전송·조회 | POST /api/feedbacks · GET /api/feedbacks/{id} · POST /api/feedbacks/{id}/messages · GET /api/feedbacks/{id}/messages |
| S-13(전문가 심사 탭) | 전문가 승인/거절 | PATCH /api/admin/experts/{id}/approve · PATCH /api/admin/experts/{id}/reject |
| S-13(전문가 심사 탭) | 전문가 목록 / 자격 박탈 | GET /api/admin/experts · DELETE /api/admin/experts/{id} |

## 와이어프레임

### 메인화면

![메인 화면.jpg](images/screen-design/%EB%A9%94%EC%9D%B8_%ED%99%94%EB%A9%B4.jpg)

### 어드민 패널

![어드민 패널 - 1.jpg](images/screen-design/%EC%96%B4%EB%93%9C%EB%AF%BC_%ED%8C%A8%EB%84%90_-_1.jpg)

![어드민 패널 - 2.jpg](images/screen-design/%EC%96%B4%EB%93%9C%EB%AF%BC_%ED%8C%A8%EB%84%90_-_2.jpg)

![어드민 패널 - 3.jpg](images/screen-design/%EC%96%B4%EB%93%9C%EB%AF%BC_%ED%8C%A8%EB%84%90_-_3.jpg)

![어드민 패널 - 4.jpg](images/screen-design/%EC%96%B4%EB%93%9C%EB%AF%BC_%ED%8C%A8%EB%84%90_-_4.jpg)

### 회원가입 / 로그인

![회원가입.jpg](images/screen-design/%ED%9A%8C%EC%9B%90%EA%B0%80%EC%9E%85.jpg)

![로그인.jpg](images/screen-design/%EB%A1%9C%EA%B7%B8%EC%9D%B8.jpg)

### 마이페이지

![마이페이지 - 1.jpg](images/screen-design/%EB%A7%88%EC%9D%B4%ED%8E%98%EC%9D%B4%EC%A7%80_-_1.jpg)

![마이페이지 - 2.jpg](images/screen-design/%EB%A7%88%EC%9D%B4%ED%8E%98%EC%9D%B4%EC%A7%80_-_2.jpg)

![마이페이지 - 3.jpg](images/screen-design/%EB%A7%88%EC%9D%B4%ED%8E%98%EC%9D%B4%EC%A7%80_-_3.jpg)

![마이페이지 - 4.jpg](images/screen-design/%EB%A7%88%EC%9D%B4%ED%8E%98%EC%9D%B4%EC%A7%80_-_4.jpg)

![마이페이지 - 5.jpg](images/screen-design/%EB%A7%88%EC%9D%B4%ED%8E%98%EC%9D%B4%EC%A7%80_-_5.jpg)

### 게시글

![게시글.jpg](images/screen-design/%EA%B2%8C%EC%8B%9C%EA%B8%80.jpg)

![게시글 상세.jpg](images/screen-design/%EA%B2%8C%EC%8B%9C%EA%B8%80_%EC%83%81%EC%84%B8.jpg)

![게시글 쓰기.jpg](images/screen-design/%EA%B2%8C%EC%8B%9C%EA%B8%80_%EC%93%B0%EA%B8%B0.jpg)

### 스터디

![스터디.jpg](images/screen-design/%EC%8A%A4%ED%84%B0%EB%94%94.jpg)

![스터디 상세.jpg](images/screen-design/%EC%8A%A4%ED%84%B0%EB%94%94_%EC%83%81%EC%84%B8.jpg)

![스터디 상세 - 게시판 (미가입자).jpg](images/screen-design/%EC%8A%A4%ED%84%B0%EB%94%94_%EC%83%81%EC%84%B8_-_%EA%B2%8C%EC%8B%9C%ED%8C%90_(%EB%AF%B8%EA%B0%80%EC%9E%85%EC%9E%90).jpg)

![스터디 상세-1.jpg](images/screen-design/%EC%8A%A4%ED%84%B0%EB%94%94_%EC%83%81%EC%84%B8-1.jpg)

![스터디 개설.jpg](images/screen-design/%EC%8A%A4%ED%84%B0%EB%94%94_%EA%B0%9C%EC%84%A4.jpg)

아래는 스터디 가입자 화면

![스터디 상세 - 게시판 (가입자).jpg](images/screen-design/%EC%8A%A4%ED%84%B0%EB%94%94_%EC%83%81%EC%84%B8_-_%EA%B2%8C%EC%8B%9C%ED%8C%90_(%EA%B0%80%EC%9E%85%EC%9E%90).jpg)

![스터디 상세 - 게시판 상세.jpg](images/screen-design/%EC%8A%A4%ED%84%B0%EB%94%94_%EC%83%81%EC%84%B8_-_%EA%B2%8C%EC%8B%9C%ED%8C%90_%EC%83%81%EC%84%B8.jpg)

### 전문가

![전문가 상담.jpg](images/screen-design/%EC%A0%84%EB%AC%B8%EA%B0%80_%EC%83%81%EB%8B%B4.jpg)

![전문가 상담 (구독자).jpg](images/screen-design/%EC%A0%84%EB%AC%B8%EA%B0%80_%EC%83%81%EB%8B%B4_(%EA%B5%AC%EB%8F%85%EC%9E%90).jpg)

![전문가 상담 상세.jpg](images/screen-design/%EC%A0%84%EB%AC%B8%EA%B0%80_%EC%83%81%EB%8B%B4_%EC%83%81%EC%84%B8.jpg)

![새 상담 추가 - 1.jpg](images/screen-design/%EC%83%88_%EC%83%81%EB%8B%B4_%EC%B6%94%EA%B0%80_-_1.jpg)

![새 상담 추가 - 2.jpg](images/screen-design/%EC%83%88_%EC%83%81%EB%8B%B4_%EC%B6%94%EA%B0%80_-_2.jpg)

![전문가 신청 - 1.jpg](images/screen-design/%EC%A0%84%EB%AC%B8%EA%B0%80_%EC%8B%A0%EC%B2%AD_-_1.jpg)

![전문가 신청 - 2.jpg](images/screen-design/%EC%A0%84%EB%AC%B8%EA%B0%80_%EC%8B%A0%EC%B2%AD_-_2.jpg)

![전문가 신청 - 3.jpg](images/screen-design/%EC%A0%84%EB%AC%B8%EA%B0%80_%EC%8B%A0%EC%B2%AD_-_3.jpg)

![전문가 신청 - 4.jpg](images/screen-design/%EC%A0%84%EB%AC%B8%EA%B0%80_%EC%8B%A0%EC%B2%AD_-_4.jpg)

![전문가 신청 - 5.jpg](images/screen-design/%EC%A0%84%EB%AC%B8%EA%B0%80_%EC%8B%A0%EC%B2%AD_-_5.jpg)

![전문가 신청 - 6.jpg](images/screen-design/%EC%A0%84%EB%AC%B8%EA%B0%80_%EC%8B%A0%EC%B2%AD_-_6.jpg)

### 구독 플랜

![구독 플랜.jpg](images/screen-design/%EA%B5%AC%EB%8F%85_%ED%94%8C%EB%9E%9C.jpg)