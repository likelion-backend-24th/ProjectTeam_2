import styles from './SiteFooter.module.css'

export default function SiteFooter() {
  return (
    <footer className={styles.footer}>
      <div className={styles.inner}>
        <a href="#" className={styles.logo}>
          <span className={styles.logoMark}>P</span>
          <span className={styles.logoText}>prep2gether</span>
        </a>
        <p className={styles.copyright}>© 2026 prep2gether. 취준생을 위한 커뮤니티 플랫폼.</p>
      </div>
    </footer>
  )
}
