# Prep2gether
> 취업 정보 공유 게시판과 스터디 모집 기능을 갖춘 취준생 커뮤니티 플랫폼

취준생들이 게시판에서 취업 정보를 공유하고, 스터디를 모집·운영하며, 전문가에게 1:1 피드백을 받을 수 있는 커뮤니티 플랫폼입니다.

## 📌 주요 기능
### 회원 / 인증
- 회원가입, 로그인 (JWT 기반)
- 카카오 / 네이버 / 구글 OAuth2 소셜 로그인

### 게시판
- 게시글 등록 / 목록·상세 조회 (페이징, 검색, 카테고리 정렬) /수정 / 삭제
- 댓글

### 스터디
- 스터디 개설 / 목록·상세 조회 (페이징, 검색, 카테고리 정렬)
- 스터디 신청 및 정원 관리
- 방장의 그룹원 관리
- 승인된 유저만 참여 가능한 스터디 게시판 / 댓글

### 전문가 / 구독
- 전문가 신청 및 관리자 승인/거절
- 구독자 - 전문가 1:1 피드백 문의 스레드

### 관리자 
- 전체 게시글/스터디 그룹 관리
- 유저 목록 조회 및 계정 상태 관리

## 🛠 기술 스택
**Backend**
- Java 21, Spring Boot 3.5
- Spring Security, JWT (jjwt), OAuth2
- Spring Data JPA, MySQL 8.0
- springdoc-openapi (Swagger UI)
- JUnit5, H2 (테스트)

**Frontend**
- React 19, Vite
- React Router, Axios
- oxlint

**Infra / CI-CD**
- Docker, Docker Compose
- GitHub Actions (Backend CI / Frontend CI / CD)
- GHCR (GitHub Container Registry)
- Nginx, AWS EC2

## 📁 프로젝트 구조
```
prep2gether/
├── backend/            # Spring Boot 애플리케이션
│   └── src/main/java/org/example/backend/
│       ├── auth/       # 인증/인가 (JWT, OAuth2)
│       ├── user/       # 회원
│       ├── post/       # 게시판
│       ├── comment/    # 댓글
│       ├── study/      # 스터디 (모집/멤버/게시판/댓글)
│       ├── expert/     # 전문가 프로필/피드백
│       ├── admin/      # 관리자
│       └── common/     # 공통 응답/예외 등
├── frontend/           # React (Vite) 애플리케이션
│   └── src/
│       ├── api/        # axios 기반 API 클라이언트
│       ├── components/ # 도메인별 컴포넌트
│       ├── pages/       # 라우트 페이지
│       └── context/     # 인증 컨텍스트 등
├── docs/               # 요구사항 정의서, ERD, DDL
└── docker-compose.yml  # DB + Backend + Frontend 통합 실행
```

## 📄 API 문서
Swagger UI에서 전체 API 명세를 확인할 수 있습니다. (/swagger-ui/index.html)

## 📅 향후 계획
