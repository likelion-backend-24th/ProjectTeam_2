import { ImagePlus, X } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import styles from './ImagePicker.module.css'

const ALLOWED_EXTENSIONS = ['jpg', 'jpeg', 'png', 'gif']
const MAX_FILE_SIZE = 5 * 1024 * 1024 // 5MB, 백엔드 ImageValidator와 동일한 기준

function getExtension(fileName) {
  return fileName.includes('.') ? fileName.split('.').pop().toLowerCase() : ''
}

// 여러 장의 이미지를 선택 -> 썸네일 미리보기 -> 개별 제거할 수 있는 공용 첨부 위젯.
// 실제 업로드는 하지 않고 File 배열만 부모에게 넘긴다(부모가 FormData로 감싸서 전송).
export default function ImagePicker({ images, onChange, maxCount = 5, disabled = false }) {
  const inputRef = useRef(null)
  const [error, setError] = useState('')

  // File -> object URL 미리보기. 언마운트/변경 시 반드시 해제해서 메모리 누수를 막는다.
  const [previews, setPreviews] = useState([])
  useEffect(() => {
    const urls = images.map((file) => URL.createObjectURL(file))
    setPreviews(urls)
    return () => urls.forEach((url) => URL.revokeObjectURL(url))
  }, [images])

  function handleFilesSelected(event) {
    const files = Array.from(event.target.files ?? [])
    event.target.value = '' // 같은 파일을 다시 선택해도 onChange가 발생하도록 초기화
    if (files.length === 0) return

    setError('')

    if (images.length + files.length > maxCount) {
      setError(`이미지는 최대 ${maxCount}장까지 첨부할 수 있어요.`)
      return
    }

    for (const file of files) {
      const ext = getExtension(file.name)
      if (!ALLOWED_EXTENSIONS.includes(ext)) {
        setError(`지원하지 않는 파일 형식이에요: ${file.name}`)
        return
      }
      if (file.size > MAX_FILE_SIZE) {
        setError(`${file.name} 파일이 5MB를 넘어요.`)
        return
      }
    }

    onChange([...images, ...files])
  }

  function removeAt(index) {
    onChange(images.filter((_, i) => i !== index))
  }

  return (
    <div className={styles.wrapper}>
      <div className={styles.list}>
        {previews.map((url, index) => (
          <div key={url} className={styles.thumb}>
            <img src={url} alt={`첨부 이미지 ${index + 1}`} />
            {!disabled && (
              <button
                type="button"
                className={styles.removeButton}
                onClick={() => removeAt(index)}
                aria-label="이미지 제거"
              >
                <X size={13} />
              </button>
            )}
          </div>
        ))}

        {!disabled && images.length < maxCount && (
          <button type="button" className={styles.addButton} onClick={() => inputRef.current?.click()}>
            <ImagePlus size={18} />
            <span>사진 추가</span>
          </button>
        )}
      </div>

      <input
        ref={inputRef}
        type="file"
        accept="image/jpeg,image/png,image/gif"
        multiple
        hidden
        onChange={handleFilesSelected}
      />

      <p className={styles.helperText}>
        JPG · PNG · GIF, 장당 5MB 이하 (최대 {maxCount}장)
      </p>
      {error && <p className={styles.error}>{error}</p>}
    </div>
  )
}
