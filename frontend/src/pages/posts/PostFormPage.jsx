import { ChevronLeft } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { postApi } from "../../api";
import SiteHeader from "../../components/common/SiteHeader";
import CategoryPicker from "../../components/posts/CategoryPicker";
import styles from "./PostFormPage.module.css";
import ImagePicker from "../../components/common/ImagePicker";

const TITLE_MAX_LENGTH = 100;
const DRAFT_KEY = "post-draft"; // 신규 작성 전용 임시저장 슬롯 (수정 모드에는 적용 안 함)

export default function PostFormPage() {
  const navigate = useNavigate();
  const { postId } = useParams();
  const isEditMode = Boolean(postId);

  const [category, setCategory] = useState("");
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [isLoading, setIsLoading] = useState(isEditMode);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [showDraftBanner, setShowDraftBanner] = useState(false);
  const [images, setImages] = useState([]);

  // 수정 모드일 때는 기존 게시글 내용을 불러와 폼에 미리 채운다.
  useEffect(() => {
    if (!isEditMode) return;
    let ignore = false;

    postApi
      .getPostDetail(postId)
      .then(({ data }) => {
        if (ignore) return;
        const post = data.data;
        setCategory(post.category);
        setTitle(post.title);
        setContent(post.content);
      })
      .catch(() => {
        if (!ignore) setError("게시글을 불러오지 못했습니다.");
      })
      .finally(() => {
        if (!ignore) setIsLoading(false);
      });

    return () => {
      ignore = true;
    };
  }, [postId, isEditMode]);

  // 신규 작성 모드로 페이지를 열었을 때, 이전에 저장해둔 임시저장이 있으면
  // 곧바로 덮어쓰지 않고 배너로 먼저 물어본다(실수로 쓰던 내용을 날리지 않기 위해).
  useEffect(() => {
    if (isEditMode) return;
    const saved = localStorage.getItem(DRAFT_KEY);
    if (!saved) return;

    try {
      const draft = JSON.parse(saved);
      if (draft.title || draft.content) {
        setShowDraftBanner(true);
      }
    } catch {
      localStorage.removeItem(DRAFT_KEY);
    }
  }, [isEditMode]);

  // 입력을 멈추고 500ms가 지나면 자동으로 임시저장한다(디바운스).
  // 수정 모드이거나, 아직 기존 글 로딩 중이거나, 작성 내용이 하나도 없으면 저장 안 함.
  useEffect(() => {
    if (isEditMode || isLoading) return;
    if (!category && !title && !content) return;

    const timer = setTimeout(() => {
      localStorage.setItem(
        DRAFT_KEY,
        JSON.stringify({ category, title, content, savedAt: Date.now() }),
      );
    }, 500);

    return () => clearTimeout(timer);
  }, [category, title, content, isEditMode, isLoading]);

  function handleRestoreDraft() {
    const saved = localStorage.getItem(DRAFT_KEY);
    if (saved) {
      const draft = JSON.parse(saved);
      setCategory(draft.category ?? "");
      setTitle(draft.title ?? "");
      setContent(draft.content ?? "");
    }
    setShowDraftBanner(false);
  }

  function handleDiscardDraft() {
    localStorage.removeItem(DRAFT_KEY);
    setShowDraftBanner(false);
  }

  async function handleSubmit(event) {
    event.preventDefault();

    if (!category) {
      setError("카테고리를 선택해주세요.");
      return;
    }
    if (!title.trim()) {
      setError("제목을 입력해주세요.");
      return;
    }
    if (!content.trim()) {
      setError("내용을 입력해주세요.");
      return;
    }

    setError("");
    setIsSubmitting(true);
    try {
      if (isEditMode) {
        await postApi.updatePost(postId, { title, content, category });
        navigate(`/posts/${postId}`);
      } else {
        const { data } = await postApi.createPost(
          { title, content, category },
          images,
        );
        localStorage.removeItem(DRAFT_KEY); // 등록 성공했으니 임시저장 정리
        navigate(`/posts/${data.data.id}`);
      }
    } catch (err) {
      setError(
        err.response?.data?.message ??
          (isEditMode
            ? "게시글 수정에 실패했습니다."
            : "게시글 등록에 실패했습니다."),
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <>
      <SiteHeader />
      <main className={styles.main}>
        <Link to="/posts" className={styles.breadcrumb}>
          <ChevronLeft size={16} />
          게시글 목록
        </Link>

        <p className={styles.eyebrow}>COMMUNITY</p>
        <h1 className={styles.title}>
          {isEditMode ? "게시글 수정" : "글쓰기"}
        </h1>

        {showDraftBanner && (
          <div className={styles.draftBanner}>
            <span>이전에 작성하던 임시저장 글이 있어요. 불러올까요?</span>
            <div className={styles.draftActions}>
              <button
                type="button"
                className={styles.draftLink}
                onClick={handleRestoreDraft}
              >
                불러오기
              </button>
              <button
                type="button"
                className={styles.draftLink}
                onClick={handleDiscardDraft}
              >
                삭제
              </button>
            </div>
          </div>
        )}

        {isLoading ? (
          <p className={styles.counter}>불러오는 중...</p>
        ) : (
          <form onSubmit={handleSubmit}>
            <div className={styles.field}>
              <label className={styles.label}>
                카테고리
                <span className={styles.required}>*</span>
              </label>
              <CategoryPicker value={category} onChange={setCategory} />
            </div>

            <div className={styles.field}>
              <label className={styles.label} htmlFor="title">
                제목
                <span className={styles.required}>*</span>
              </label>
              <input
                id="title"
                type="text"
                className={styles.input}
                placeholder="제목을 입력하세요"
                maxLength={TITLE_MAX_LENGTH}
                value={title}
                onChange={(event) => setTitle(event.target.value)}
              />
              <p className={styles.counter}>
                {title.length}/{TITLE_MAX_LENGTH}
              </p>
            </div>

            <div className={styles.field}>
              <label className={styles.label} htmlFor="content">
                내용
                <span className={styles.required}>*</span>
              </label>
              <textarea
                id="content"
                className={styles.textarea}
                placeholder="취업 정보, 면접 후기, 자소서 팁 등 자유롭게 작성해보세요."
                value={content}
                onChange={(event) => setContent(event.target.value)}
              />
              <p className={styles.counter}>{content.length}자</p>
            </div>
            {!isEditMode && (
              <div className={styles.field}>
                <label className={styles.label}>
                  이미지 첨부
                  <span className={styles.required}>선택</span>
                </label>
                <ImagePicker images={images} onChange={setImages} />
              </div>
            )}
            <p className={styles.notice}>
              📌 커뮤니티 이용 규칙을 위반한 게시글은 운영자에 의해 삭제될 수
              있어요. 타인을 존중하는 글 문화를 함께 만들어 나가요.
            </p>

            {error && <p className={styles.error}>{error}</p>}

            <div className={styles.actions}>
              <button
                type="button"
                className={styles.cancelButton}
                onClick={() =>
                  navigate(isEditMode ? `/posts/${postId}` : "/posts")
                }
              >
                취소
              </button>
              <button
                type="submit"
                className={styles.submitButton}
                disabled={isSubmitting}
              >
                {isSubmitting
                  ? "저장 중..."
                  : isEditMode
                    ? "수정 완료"
                    : "게시글 등록하기"}
              </button>
            </div>
          </form>
        )}
      </main>
    </>
  );
}
