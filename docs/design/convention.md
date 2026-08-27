# 컨벤션

# 📐 컨벤션 — prep2gether

---

## 1. 폴더 & 파일 구조

### 백엔드 (Spring Boot)

도메인별 패키지 구조를 기본으로 함:

```
org.example.backend/
├── post/
│   ├── entity/
│   ├── repository/
│   ├── service/
│   ├── controller/
│   ├── dto/
│   └── exception/       # PostErrorCode 등 도메인별 예외
├── comment/
├── study/
├── expert/
├── auth/
├── user/
├── admin/
├── common/              # ApiResponse, Meta, PageMeta, BusinessException 등 공통
└── domain/              # 여러 도메인에 걸쳐 있거나 독립적인 엔티티 (Subscription 등)
```

- 도메인 하나당 `entity / repository / service / controller / dto / exception`을 그 도메인 폴더 안에 둔다(레이어별로 최상위에서 나누지 않음)
- 여러 도메인이 공유하는 것(응답 포맷, 공통 예외 등)만 `common/`에 둔다

### 프론트엔드 (React + Vite)

```
src/
├── api/            # 백엔드 엔드포인트별 axios 래퍼 (client.js, authApi.js, postApi.js, studyApi.js, ...)
├── components/
│   ├── auth/
│   ├── common/     # 헤더, 푸터, 페이지네이션, 라우트 가드 등 전 도메인 공용
│   ├── home/
│   ├── posts/
│   └── studies/
├── constants/      # postCategory, studyCategory 등 enum성 상수
├── context/        # AuthContext 등 전역 상태 (Context API 사용, 별도 상태관리 라이브러리 안 씀)
├── pages/          # 라우트 1:1 대응, 도메인별 하위 폴더(auth/, posts/, studies/)
├── utils/          # avatarColor, formatDate 등 순수 함수
└── router.jsx
```

- 컴포넌트 폴더는 **도메인 기준**으로 나눈다(atomic design 같은 계층 구조 안 씀)
- API 호출 함수는 화면 컴포넌트에 직접 axios를 쓰지 않고 `api/` 폴더의 래퍼 함수를 거친다

---

## 2. 백엔드 코드 작성 규칙

### DTO 규칙

- **응답 DTO**: `@Builder` + 정적 팩토리 메서드 `from(entity)` 패턴 사용. 빌더를 서비스/컨트롤러에서 직접 호출하지 않는다.

```java
@Getter
@Builder
public class PostResponse {
    private Long id;
    private String title;

    public static PostResponse from(Post post) {
        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .build();
    }
}
```

- **요청 DTO**: `@AllArgsConstructor`(또는 `@NoArgsConstructor` + `@Setter`) 사용, 검증은 `@Valid` + Bean Validation 애노테이션(`@NotBlank`, `@Size` 등)으로 처리

### 예외 처리

- 도메인별 `BusinessException` + `ErrorCode` enum 구조 사용(`PostErrorCode`, `CommentErrorCode`, `StudyErrorCode` 등)
- 각 ErrorCode는 `HttpStatus`와 메시지를 함께 가짐

```java
public enum PostErrorCode implements ErrorCode {
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."),
    ...
}
```

- 컨트롤러/서비스에서 직접 `ResponseEntity.status(...)`로 에러를 만들지 않고, `throw new BusinessException(XxxErrorCode.YYY)`로 던진다

### 응답 포맷

- 모든 API는 공통 `ApiResponse<T>` 래퍼 사용: `{ success, message, data, meta, errorCode }`
- 페이징 응답은 `Meta`에 `PageMeta`(page, size, totalPages, totalElements)를 포함

### 카테고리/상태값

- 별도 테이블 대신 **Java enum + VARCHAR 컬럼**으로 저장(`PostCategory`, `StudyCategory`, `AccountStatus` 등)
- enum에 한글 라벨(`label`)이 필요하면 enum 생성자로 함께 관리

```java
public enum PostCategory {
    JOB_INFO("취업정보"),
    ...
    private final String label;
}
```

### 트랜잭션

- 단순 필드 변경(조회수 증가 등)은 `@Transactional` + JPA dirty checking을 활용하고, 명시적 `save()` 호출을 남발하지 않는다

---

## 3. 프론트엔드 코드 작성 규칙

### 컴포넌트 기본 구조

- 함수형 컴포넌트, `export default function ComponentName() { ... }` 형태
- 컴포넌트 내부에서 왜 이렇게 처리했는지 애매한 로직에는 **한글 주석**을 남긴다(특히 백엔드 특이사항을 프론트에서 우회 처리한 부분 — 예: SecurityConfig 이슈 우회, 카테고리 필터 프론트 처리 등)

```jsx
// 백엔드 SecurityConfig의 CORS 허용 origin(http://localhost:3000)과 맞춤
export default function StudyListPage() {
  // 입력을 멈추고 300ms가 지나면 자동으로 검색어를 반영한다(디바운스).
  ...
}
```

### API 연동

- 도메인별로 `api/xxxApi.js` 파일을 만들고, 그 안에서만 axios 인스턴스(`client.js`)를 사용
- 환경별 주소는 `import.meta.env.VITE_API_BASE_URL`처럼 Vite 환경변수로 분리(하드코딩 금지) — 빌드 시점에 값이 박히므로 배포 환경에 맞는 값을 빌드 시 build-arg로 반드시 주입

### 전역 상태

- Redux/Zustand 등 별도 라이브러리 대신 **React Context API**로 관리(`AuthContext`)

### 라우트 가드

- 로그인 필요한 페이지는 `RequireAuth` 래퍼 컴포넌트로 감싼다

---

## 4. Git 브랜치 전략

### 브랜치 구조

```
main            → 배포 브랜치
feat/기능명     → 새 기능 개발
fix/버그명      → 버그 수정
docs/문서명     → 문서 작업 (ERD, 명세서 등)
ci/작업명       → CI/CD, 배포 자동화 관련
refactor/내용   → 기능 변경 없는 코드 개선
eval/시점명     → 평가 제출용 스냅샷 브랜치 (PR로 merge하지 않음)
```

### 브랜치 네이밍 예시

```
fix/cors-allowed-origins
docs/update-erd
ci/deploy-automation
eval/midterm
```

### 브랜치 작업 순서

```
1. main에서 git pull 후 새 브랜치 생성
2. 작업 완료 후 커밋 → push
3. GitHub에서 main으로 PR 생성
4. merge 후 로컬/원격 브랜치 모두 삭제 (git branch --merged main 으로 확인 후 삭제)
```

- `git add`는 파일 단위 선택적 스테이징을 기본으로 하여, 다른 팀원이 작업 중인 변경사항이 실수로 같이 커밋되지 않도록 한다
- 평가·제출용 스냅샷(`eval/...`)은 일반 기능 브랜치와 달리 **PR로 다시 main에 합치지 않는다** — 특정 시점 고정 용도로만 사용

---

## 5. 커밋 컨벤션

### 커밋 메시지 구조

```
타입: 작업 내용 요약
```

### 타입 목록

| 타입 | 설명 | 예시 |
| --- | --- | --- |
| `feat` | 새로운 기능 추가 | `feat: 게시글 검색 keyword 기능 추가` |
| `fix` | 버그 수정 | `fix: 방장 단독 탈퇴 시 스터디 삭제 실패 수정` |
| `refactor` | 기능 변경 없는 코드 개선 | `refactor: PublicExpertResponse에서 certification 필드 제거` |
| `docs` | 문서 수정 | `docs: ERD 최신 코드 기준 재정리, 이미지 통합` |
| `ci` | CI/CD, 배포 자동화 | `ci: 배포 자동화 워크플로우 추가` |
| `chore` | 설정, 패키지, 환경 변경 | `chore: H2 의존성 추가` |
| `test` | 테스트 코드 | `test: PostService 단위 테스트 추가` |

### 커밋 메시지 예시

```
✅ 올바른 예시
feat: 전문가 신청 시 경력/자격증 다건 입력 지원
fix: SecurityConfig permitAll 경로 수정
docs: 기능명세서 최신 코드 기준 재검증

❌ 지양하는 예시
작업함
수정
ㅇㅇ
```

---

## 6. PR(Pull Request) 규칙

### PR 제목

```
[타입] 작업 내용

예시)
[fix] 방장 단독 탈퇴 시 스터디 삭제 실패 수정
[docs] API명세 최신 코드 기준 재정리
[ci] GitHub Actions 배포 자동화 워크플로우 추가
```

### PR 본문 템플릿

```markdown
## 작업 내용
-구현하거나 수정한 내용을 bullet로 작성

## 관련 기능 ID
-F-00 기능명 (해당하는 경우)

## 확인 방법
-어떻게 테스트했는지, 재현 방법

## 스크린샷 (선택)
-UI 변경이 있을 경우 첨부
```

### PR 규칙

- 하나의 PR은 하나의 기능/이슈/문서 작업에 집중한다(무관한 변경 섞지 않기)
- 배포 자동화 워크플로우가 `main` push에 걸려있으므로, **문서만 바뀐 PR도 merge 시 전체 재배포가 트리거됨**을 인지하고 merge 타이밍을 조율한다(필요 시 `paths-ignore`로 문서 변경은 배포 스킵하도록 워크플로우 개선 검토)
- merge된 브랜치는 바로 삭제한다(로컬 + 원격)

---

## 7. 문서(Notion) 관리 규칙

- 문서 수정 시 **검증 성격의 마커**를 다르게 씀:
    - 요구사항정의서: 완료된 최종본으로 작성, 검증 마커(✅/❌/⚠️) 사용하지 않음 — “앞으로 어떻게 개발할지”를 나타내는 문서이기 때문
    - API명세/기능명세서/권한매트릭스: 코드와 대조한 검증 결과이므로 🆕(신규 발견)/✏️(변경)/❌(미구현)/✅(일치 확인)/⚠️(오픈 이슈) 마커를 적극 사용
    - 추가기능(백로그): 우선순위별로 A(필수 보완)/B(여유 시) 구분, 실제 발견된 이슈 위주로 작성하고 MVP1에서 “하지 않을 것”으로 정한 항목과 겹치지 않게 함
- 코드와 문서가 다를 경우 **코드가 기준**이며, 문서를 코드에 맞춰 갱신한다(반대로 문서에 맞춰 코드를 임의로 판단하지 않음)
- `.md` 파일로 작성 후 노션에 **가져오기(Import)** 또는 텍스트 복사-붙여넣기로 반영(모바일 앱은 Import 미지원, 복사-붙여넣기 권장)

---

## 8. 배포/환경변수 관리

- 민감한 값(`DB_PASSWORD`, `JWT_SECRET`, 소셜로그인 키 등)은 `.gitignore`에 걸린 `.env` 파일로만 관리, 절대 커밋하지 않는다
- 프론트엔드 `VITE_` 접두사 환경변수는 **빌드 시점에 고정**되므로, 배포 환경(EC2 등)에 맞는 값을 CI에서 build-arg로 정확히 주입해야 한다 — 이 값이 로컬 기본값(`localhost`)으로 잘못 빌드되면 배포 환경에서도 로컬로 리다이렉트되는 등 예측 못한 오류가 생길 수 있으니 배포 전 반드시 확인
- 로컬 포트가 팀원마다 다를 수 있는 서비스(MySQL 등)는 하드코딩 대신 `${ENV_VAR:-기본값}` 형태로 두어 개인 환경에서 충돌 없이 오버라이드 가능하게 한다