import styles from './LegalDocument.module.css'

// 이용약관/개인정보처리방침처럼 "제목 + 조항 목록" 구조인 법적 문서 공통 레이아웃.
// sections: [{ heading, paragraphs?: string[], list?: string[] }]
export default function LegalDocument({ title, updatedAt, sections }) {
  return (
    <article className={styles.document}>
      <p className={styles.draftNotice}>
        이 문서는 서비스 준비 과정에서 작성된 초안이며, 정식 시행 전 법률 검토를 거쳐 내용이 변경될 수 있습니다.
      </p>

      <h1 className={styles.title}>{title}</h1>
      <p className={styles.updatedAt}>시행일 {updatedAt}</p>

      {sections.map((section) => (
        <section key={section.heading} className={styles.section}>
          <h2 className={styles.heading}>{section.heading}</h2>
          {section.paragraphs?.map((paragraph, index) => (
            <p key={index} className={styles.paragraph}>
              {paragraph}
            </p>
          ))}
          {section.list && (
            <ul className={styles.list}>
              {section.list.map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ul>
          )}
        </section>
      ))}
    </article>
  )
}
