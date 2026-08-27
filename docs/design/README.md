# 설계 문서

prep2gether 프로젝트 설계 산출물 모음입니다. (Notion 문서 이관)

## 목차

| 문서 | 내용 |
|---|---|
| [요구사항정의서](../requirement.md) | 기능 요구사항(F-XX) 정의 (기존 docs/ 파일과 동일) |
| [기능명세서](feature-spec.md) | 기능별 상세 명세 |
| [API명세](api-spec.md) | REST API 엔드포인트 명세 |
| [ERD](erd.md) | 데이터베이스 ER 다이어그램 |
| [권한매트릭스](permission-matrix.md) | 역할별 접근 권한 정의 |
| [사용자흐름](user-flow.md) | 화면 단위 사용자 플로우 (UF-XX) |
| [시퀀스 다이어그램](sequence-diagram.md) | 도메인별 처리 흐름 |
| [화면 설계](screen-design.md) | 와이어프레임 / 화면 시안 |
| [컨벤션](convention.md) | 코드·커밋·브랜치 컨벤션 |

## 폴더 구조

```
docs/design/
├── README.md              # 이 파일
├── feature-spec.md
├── api-spec.md
├── erd.md
├── permission-matrix.md
├── user-flow.md
├── sequence-diagram.md
├── screen-design.md
├── convention.md
└── images/                # 문서에서 참조하는 이미지
    ├── erd/
    ├── user-flow/
    ├── sequence/
    └── screen-design/
```
