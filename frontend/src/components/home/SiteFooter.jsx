import styles from './SiteFooter.module.css'

export default function SiteFooter() {
  return (
    <footer className={styles.footer}>
      <div className={styles.inner}>
        <a href="#" className={styles.logo}>
          <span className={styles.logoMark}>취</span>
          <span className={styles.logoText}>JOBtogether</span>
        </a>
        <p className={styles.copyright}>© 2026 JOBtogether. 취준생을 위한 커뮤니티 플랫폼.</p>
      </div>
    </footer>
  )
}
