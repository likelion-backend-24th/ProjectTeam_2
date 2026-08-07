import styles from './AuthShowcase.module.css'

// 인증 페이지(로그인/회원가입) 왼쪽에 공통으로 쓰이는 브랜드 소개 패널.
// heading/description은 줄바꿈 배열. 하단 추가 콘텐츠(후기, 가입 진행률 등)는 children으로 전달.
export default function AuthShowcase({ heading, description, children }) {
  return (
    <div className={styles.panel}>
      <span className={`${styles.ring} ${styles.ringOne}`} aria-hidden="true" />
      <span className={`${styles.ring} ${styles.ringTwo}`} aria-hidden="true" />

      <div className={styles.brand}>
        <span className={styles.brandMark}>취</span>
        <span className={styles.brandText}>JOBtogether</span>
      </div>

      <div className={styles.body}>
        <h2 className={styles.heading}>
          {heading.map((line, index) => (
            <span key={line}>
              {line}
              {index < heading.length - 1 && <br />}
            </span>
          ))}
        </h2>

        <p className={styles.description}>
          {description.map((line, index) => (
            <span key={line}>
              {line}
              {index < description.length - 1 && <br />}
            </span>
          ))}
        </p>

        {children}
      </div>
    </div>
  )
}
