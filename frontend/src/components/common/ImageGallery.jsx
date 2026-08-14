import styles from './ImageGallery.module.css'

// 게시글/스터디 상세에 첨부 이미지를 그리드로 보여주는 간단한 갤러리.
export default function ImageGallery({ imageUrls }) {
  if (!imageUrls || imageUrls.length === 0) return null

  return (
    <div className={styles.gallery}>
      {imageUrls.map((url) => (
        <img key={url} src={url} alt="" className={styles.image} />
      ))}
    </div>
  )
}
