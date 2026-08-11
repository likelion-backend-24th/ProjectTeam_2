import styles from './Testimonial.module.css'

// AuthShowcase 하단에 들어가는 후기 카드 (로그인 페이지에서 사용!!)
export default function Testimonial({ quote, avatarInitial, name }) {
  return (
    <div className={styles.testimonial}>
      <p className={styles.quote}>
        &ldquo;
        {quote.map((line, index) => (
          <span key={line}>
            {line}
            {index < quote.length - 1 && <br />}
          </span>
        ))}
        &rdquo;
      </p>
      <div className={styles.author}>
        <span className={styles.avatar}>{avatarInitial}</span>
        <span className={styles.authorName}>{name}</span>
      </div>
    </div>
  )
}
