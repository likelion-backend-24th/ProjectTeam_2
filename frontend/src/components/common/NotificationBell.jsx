import { Bell } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { notificationApi } from "../../api";
import { formatDateTime } from "../../utils/formatDate";
import styles from "./NotificationBell.module.css";

export default function NotificationBell() {
  const navigate = useNavigate();
  const wrapperRef = useRef(null);

  const [unreadCount, setUnreadCount] = useState(0);
  const [notifications, setNotifications] = useState([]);
  const [isOpen, setIsOpen] = useState(false);

  // 페이지 진입 시 안 읽은 알림 개수만 가볍게 먼저 조회(뱃지 숫자용)
  useEffect(() => {
    notificationApi
      .getUnreadCount()
      .then(({ data }) => setUnreadCount(data.data.unreadCount))
      .catch(() => {});
  }, []);

  // 드롭다운 바깥을 클릭하면 닫는다.
  useEffect(() => {
    function handleClickOutside(event) {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  async function handleToggle() {
    const nextOpen = !isOpen;
    setIsOpen(nextOpen);

    if (nextOpen) {
      // 목록 조회 자체가 서버에서 읽음 처리까지 함께 해준다(NotificationService.getNotifications).
      const { data } = await notificationApi.getNotifications({
        page: 0,
        size: 10,
      });
      setNotifications(data.data);
      setUnreadCount(0);
    }
  }

  function handleItemClick(notification) {
    setIsOpen(false);
    if (notification.targetType === "POST") {
      navigate(`/posts/${notification.targetId}`);
    } else if (notification.targetType === "FEEDBACK") {
      navigate(`/experts/consult/${notification.targetId}`);
    }
  }

  function getMessage(notification) {
  return notification.commentPreview
}

  return (
    <div className={styles.wrapper} ref={wrapperRef}>
      <button
        type="button"
        className={styles.bellButton}
        onClick={handleToggle}
        aria-label="알림"
      >
        <Bell size={18} />
        {unreadCount > 0 && (
          <span className={styles.badge}>
            {unreadCount > 9 ? "9+" : unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div className={styles.dropdown}>
          <p className={styles.dropdownTitle}>알림</p>
          {notifications.length === 0 ? (
            <p className={styles.empty}>새로운 알림이 없어요.</p>
          ) : (
            notifications.map((notification) => (
              <button
                key={notification.id}
                type="button"
                className={styles.item}
                onClick={() => handleItemClick(notification)}
              >
                <span>{getMessage(notification)}</span>
                <span className={styles.time}>
                  {formatDateTime(notification.createdAt)}
                </span>
              </button>
            ))
          )}
        </div>
      )}
    </div>
  );
}
