// ─── NotificationPages.tsx — SCR-13, 14 ──────────────────────────────────────
// Exports: NotificationCenterPage, NotificationDetailPage

import { useState, useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import CustomerLayout from '../../layouts/CustomerLayout';
import ManagerLayout from '../../layouts/ManagerLayout';
import EmployeeLayout from '../../layouts/EmployeeLayout';
import AdminLayout from '../../layouts/AdminLayout';
import Alert from '../../components/ui/Alert';
import { useAuthStore } from '../../store/authStore';
import { notificationApi, Notification } from '../../api/notificationApi';
import { getNotificationActionPath } from '../../utils/notificationDeepLink';

function getNotifBase(role: string | null): string {
  switch (role) {
    case 'MANAGER':
      return '/manager/notifications';
    case 'EMPLOYEE':
      return '/employee/notifications';
    case 'ADMIN':
      return '/admin/notifications';
    default:
      return '/customer/notifications';
  }
}

function RoleLayout({ children }: { children: React.ReactNode }) {
  const role = useAuthStore(s => s.role);
  if (role === 'MANAGER') return <ManagerLayout>{children}</ManagerLayout>;
  if (role === 'EMPLOYEE') return <EmployeeLayout>{children}</EmployeeLayout>;
  if (role === 'ADMIN') return <AdminLayout>{children}</AdminLayout>;
  return <CustomerLayout>{children}</CustomerLayout>;
}

const NOTIF_TYPE_META: Record<string, { bg: string; color: string; label: string; badge: string }> = {
  BOOKING_CONFIRMED:   { bg: '#dcfce7', color: '#2b9a66', label: 'Đặt phòng',    badge: 'badge-success' },
  CONTRACT_GENERATED:  { bg: '#dbeafe', color: '#2563eb', label: 'Hợp đồng',     badge: 'badge-info' },
  PAYMENT_CONFIRMED:   { bg: '#ede9fe', color: '#7c3aed', label: 'Thanh toán',   badge: 'badge-purple' },
  MAINTENANCE_UPDATED: { bg: '#fef3c7', color: '#d97706', label: 'Bảo trì',      badge: 'badge-warning' },
  SYSTEM:              { bg: '#f3f4f6', color: '#4b5563', label: 'Hệ thống',     badge: 'badge-neutral' },
};

function NotifIcon({ type, size = 42 }: { type: string; size?: number }) {
  const iconSize = Math.round(size * 0.4);
  const m: Record<string, React.ReactNode> = {
    BOOKING_CONFIRMED:   <svg width={iconSize} height={iconSize} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"><polyline points="20,6 9,17 4,12"/></svg>,
    CONTRACT_GENERATED:  <svg width={iconSize} height={iconSize} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14,2 14,8 20,8"/></svg>,
    PAYMENT_CONFIRMED:   <svg width={iconSize} height={iconSize} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><rect x="1" y="4" width="22" height="16" rx="2"/><line x1="1" y1="10" x2="23" y2="10"/></svg>,
    MAINTENANCE_UPDATED: <svg width={iconSize} height={iconSize} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/></svg>,
    SYSTEM:              <svg width={iconSize} height={iconSize} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>,
  };
  const meta = NOTIF_TYPE_META[type] || { bg: 'var(--surface-bone)', color: 'var(--charcoal)' };
  return (
    <div style={{
      width: size, height: size, borderRadius: '50%', background: meta.bg,
      display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0, color: meta.color,
    }}>
      {m[type] || '🔔'}
    </div>
  );
}

function relTime(dt: string) {
  if (!dt) return '';
  const d = (Date.now() - new Date(dt).getTime()) / 1000;
  if (d < 60) return 'Vừa xong';
  if (d < 3600) return `${Math.floor(d / 60)} phút trước`;
  if (d < 86400) return `${Math.floor(d / 3600)} giờ trước`;
  return `${Math.floor(d / 86400)} ngày trước`;
}

function formatNotifDate(iso: string) {
  return new Date(iso).toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function getRelatedAction(
  role: string | null,
  type: Notification['type'],
  relatedEntityId: string,
): { to: string; label: string } | null {
  return getNotificationActionPath(type, relatedEntityId, role);
}

// ── SCR-13: Notification Center ───────────────────────────────────────────────
export function NotificationCenterPage() {
  const role = useAuthStore(s => s.role);
  const notifBase = getNotifBase(role);

  const [notifs, setNotifs] = useState<Notification[]>([]);
  const [filter, setFilter] = useState<'ALL' | 'UNREAD'>('ALL');
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Phân trang
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const size = 10;

  const fetchUnreadCount = async () => {
    try {
      const res = await notificationApi.getUnreadCount();
      if (res.success) {
        setUnreadCount(res.data.count);
      }
    } catch (err) {
      console.error("Failed to load unread count", err);
    }
  };

  const fetchNotifications = async (targetPage: number, isLoadMore: boolean = false) => {
    if (targetPage === 0) {
      setLoading(true);
    }
    setError(null);
    try {
      const res = await notificationApi.getNotifications({
        page: targetPage,
        size,
        unreadOnly: filter === 'UNREAD',
      });
      if (res.success) {
        if (isLoadMore) {
          setNotifs(prev => [...prev, ...res.data.content]);
        } else {
          setNotifs(res.data.content);
        }
        setPage(res.data.page);
        setTotalPages(res.data.totalPages);
      } else {
        setError(res.message || 'Không thể tải thông báo');
      }
    } catch {
      setError('Đã xảy ra lỗi khi tải thông báo. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  // Fetch khi mount hoặc khi thay đổi tab/filter
  useEffect(() => {
    fetchUnreadCount();
    fetchNotifications(0, false);
  }, [filter]);

  // Đánh dấu tất cả là đã đọc
  const handleMarkAllRead = async () => {
    try {
      const res = await notificationApi.markAllRead();
      if (res.success) {
        setUnreadCount(0);
        // Cập nhật state locally
        setNotifs(prev => prev.map(n => ({ ...n, isRead: true })));
        if (filter === 'UNREAD') {
          setNotifs([]);
        }
        // Gọi custom event để thông báo cho CustomerLayout update badge chuông
        window.dispatchEvent(new Event('unreadCountChanged'));
      }
    } catch (err) {
      console.error("Failed to mark all read", err);
    }
  };

  const handleMarkRead = (id: string) => {
    // Cập nhật local state mượt mà trước khi load detail
    setNotifs(prev => prev.map(n => n.id === id ? { ...n, isRead: true } : n));
    setUnreadCount(prev => Math.max(0, prev - 1));
    // Dispatch event update badge
    setTimeout(() => {
      window.dispatchEvent(new Event('unreadCountChanged'));
    }, 100);
  };


  return (
    <RoleLayout>
      <div style={{ maxWidth: 720, margin: '0 auto' }}>
        <div className="flex items-center justify-between" style={{ marginBottom: 24 }}>
          <div>
            <h1 className="heading-md" style={{ marginBottom: 4 }}>Thông báo</h1>
            {unreadCount > 0 && <p className="body-sm text-charcoal">{unreadCount} chưa đọc</p>}
          </div>
          {unreadCount > 0 && (
            <button onClick={handleMarkAllRead} className="btn-ghost btn-sm" style={{ color: 'var(--primary)' }}>
              Đánh dấu tất cả đã đọc
            </button>
          )}
        </div>

        <div style={{ display: 'flex', gap: 6, marginBottom: 20, padding: '4px', background: 'var(--surface-bone)', borderRadius: 9999, width: 'fit-content' }}>
          {(['ALL', 'UNREAD'] as const).map(tab => (
            <button key={tab} className={`tab-pill ${filter === tab ? 'active' : ''}`} onClick={() => setFilter(tab)}>
              {tab === 'ALL' ? 'Tất cả' : `Chưa đọc (${unreadCount})`}
            </button>
          ))}
        </div>

        {loading && page === 0 ? (
          <div style={{ textAlign: 'center', padding: 60 }}>
            <p className="body-md text-charcoal">Đang tải thông báo...</p>
          </div>
        ) : error ? (
          <div style={{ maxWidth: 480, margin: '0 auto' }}>
            <Alert variant="error" message={error} />
            <button
              type="button"
              onClick={() => fetchNotifications(0, false)}
              className="btn-outline btn-sm"
              style={{ marginTop: 16 }}
            >
              Thử lại
            </button>
          </div>
        ) : notifs.length === 0 ? (
          <div style={{ textAlign: 'center', padding: 60 }}>
            <div style={{ fontSize: 40, marginBottom: 12 }}>🔔</div>
            <h3 className="heading-sm" style={{ marginBottom: 8 }}>Bạn đã xem hết!</h3>
            <p className="body-md text-charcoal" style={{ marginBottom: 0 }}>
              {filter === 'UNREAD' ? 'Không có thông báo chưa đọc.' : 'Không có thông báo mới.'}
            </p>
          </div>
        ) : (
          <>
            <div className="card" style={{ overflow: 'hidden' }}>
              {notifs.map((n, i) => (
                <Link key={n.id} to={`${notifBase}/${n.id}`}
                  onClick={() => handleMarkRead(n.id)}
                  style={{
                    display: 'flex', gap: 14, padding: '16px 20px', textDecoration: 'none',
                    background: n.isRead ? 'var(--surface-card)' : 'var(--surface-bone)',
                    borderBottom: i < notifs.length - 1 ? '1px solid var(--hairline)' : 'none',
                    borderLeft: n.isRead ? 'none' : '3px solid var(--primary)',
                    transition: 'background 0.15s',
                  }}
                  onMouseEnter={e => (e.currentTarget.style.background = '#f3f0e8')}
                  onMouseLeave={e => (e.currentTarget.style.background = n.isRead ? 'var(--surface-card)' : 'var(--surface-bone)')}>
                  <NotifIcon type={n.type} />
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div className="flex items-center gap-2" style={{ marginBottom: 3 }}>
                      <p style={{ fontWeight: n.isRead ? 500 : 700, fontSize: 14, color: 'var(--ink)' }}>{n.title}</p>
                      {!n.isRead && <div style={{ width: 7, height: 7, borderRadius: '50%', background: 'var(--primary)', flexShrink: 0 }} />}
                    </div>
                    <p className="body-sm text-charcoal" style={{ marginBottom: 3, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{n.content}</p>
                    <p style={{ fontSize: 11, color: 'var(--ash)' }}>{relTime(n.createdAt)}</p>
                  </div>
                </Link>
              ))}
            </div>

            {/* Load More Button */}
            {page < totalPages - 1 && (
              <div style={{ display: 'flex', justifyContent: 'center', marginTop: 20 }}>
                <button
                  onClick={() => fetchNotifications(page + 1, true)}
                  className="btn-outline btn-sm"
                  disabled={loading}
                >
                  {loading ? 'Đang tải...' : 'Tải thêm'}
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </RoleLayout>
  );
}

// ── SCR-14: Notification Detail ───────────────────────────────────────────────
export function NotificationDetailPage() {
  const { id } = useParams();
  const role = useAuthStore(s => s.role);
  const notifBase = getNotifBase(role);

  const [notif, setNotif] = useState<Notification | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let cancelled = false;
    async function fetchDetail() {
      if (!id) return;
      setLoading(true);
      setError('');
      try {
        const res = await notificationApi.getNotificationDetail(id);
        if (cancelled) return;
        if (res.success) {
          setNotif(res.data);
          window.dispatchEvent(new Event('unreadCountChanged'));
        } else {
          setError(res.message || 'Không thể tải chi tiết thông báo');
        }
      } catch (err: unknown) {
        if (cancelled) return;
        const axiosErr = err as { response?: { data?: { message?: string } } };
        setError(axiosErr?.response?.data?.message ?? 'Đã xảy ra lỗi khi tải chi tiết thông báo');
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    fetchDetail();
    return () => { cancelled = true; };
  }, [id]);

  const breadcrumbTitle = notif
    ? (notif.title.length > 40 ? `${notif.title.slice(0, 40)}…` : notif.title)
    : 'Chi tiết';
  const relatedAction = notif?.relatedEntityId
    ? getRelatedAction(role, notif.type, notif.relatedEntityId)
    : null;
  const typeMeta = notif
    ? (NOTIF_TYPE_META[notif.type] || { label: notif.type, badge: 'badge-neutral' })
    : null;

  return (
    <RoleLayout>
      <div style={{ maxWidth: 680, margin: '0 auto', width: '100%', minWidth: 0 }}>
        <div
          className="flex items-center gap-2 body-sm text-charcoal"
          style={{ marginBottom: 20, flexWrap: 'wrap' }}
        >
          <Link to={notifBase} className="text-primary" style={{ textDecoration: 'none' }}>
            Thông báo
          </Link>
          <span>›</span>
          <span className="text-ink" style={{ fontWeight: 600, wordBreak: 'break-word' }}>
            {breadcrumbTitle}
          </span>
        </div>

        {loading ? (
          <div className="card-lg" style={{ padding: '48px 32px', textAlign: 'center' }}>
            <p className="body-md text-charcoal" style={{ margin: 0 }}>Đang tải chi tiết thông báo...</p>
          </div>
        ) : error || !notif ? (
          <div className="card-lg" style={{ padding: 32 }}>
            <Alert variant="error" message={error || 'Không tìm thấy thông báo'} />
            <Link
              to={notifBase}
              className="btn-outline btn-sm"
              style={{ marginTop: 20, display: 'inline-flex', textDecoration: 'none' }}
            >
              ← Quay lại danh sách
            </Link>
          </div>
        ) : (
          <div className="card-lg" style={{ padding: 0, overflow: 'hidden', minWidth: 0 }}>
            {/* Header */}
            <div
              style={{
                padding: '28px 28px 24px',
                background: 'linear-gradient(180deg, var(--surface-bone) 0%, var(--surface-card) 100%)',
                borderBottom: '1px solid var(--hairline)',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'flex-start', gap: 16, minWidth: 0 }}>
                <NotifIcon type={notif.type} size={56} />
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: 10 }}>
                    {typeMeta && (
                      <span className={`badge ${typeMeta.badge}`}>{typeMeta.label}</span>
                    )}
                    <span className={`badge ${notif.isRead ? 'badge-neutral' : 'badge-primary'}`}>
                      {notif.isRead ? 'Đã đọc' : 'Mới'}
                    </span>
                  </div>
                  <h1
                    className="heading-md"
                    style={{ marginBottom: 8, wordBreak: 'break-word', lineHeight: 1.35 }}
                  >
                    {notif.title}
                  </h1>
                  <p className="body-sm text-charcoal" style={{ margin: 0, display: 'flex', flexWrap: 'wrap', gap: '4px 12px' }}>
                    <span>{relTime(notif.createdAt)}</span>
                    <span style={{ color: 'var(--ash)' }}>·</span>
                    <span>{formatNotifDate(notif.createdAt)}</span>
                  </p>
                </div>
              </div>
            </div>

            {/* Body */}
            <div style={{ padding: '28px' }}>
              <p
                className="form-label"
                style={{ marginBottom: 10, color: 'var(--mute)', fontSize: 11, letterSpacing: '0.06em', textTransform: 'uppercase' }}
              >
                Nội dung
              </p>
              <div
                style={{
                  padding: '18px 20px',
                  background: 'var(--surface-bone)',
                  borderRadius: 12,
                  border: '1px solid var(--hairline)',
                }}
              >
                <p
                  className="body-lg text-body"
                  style={{
                    lineHeight: 1.8,
                    margin: 0,
                    whiteSpace: 'pre-wrap',
                    wordBreak: 'break-word',
                    overflowWrap: 'anywhere',
                  }}
                >
                  {notif.content}
                </p>
              </div>
            </div>

            {/* Actions */}
            <div
              style={{
                padding: '16px 28px 24px',
                borderTop: '1px solid var(--hairline)',
                display: 'flex',
                flexWrap: 'wrap',
                gap: 10,
                alignItems: 'center',
                justifyContent: 'space-between',
              }}
            >
              <Link
                to={notifBase}
                className="btn-ghost btn-sm"
                style={{ textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: 6 }}
              >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <line x1="19" y1="12" x2="5" y2="12" />
                  <polyline points="12,19 5,12 12,5" />
                </svg>
                Quay lại danh sách
              </Link>
              {relatedAction && (
                <Link
                  to={relatedAction.to}
                  className="btn-primary btn-sm"
                  style={{ textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: 6 }}
                >
                  {relatedAction.label}
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <line x1="5" y1="12" x2="19" y2="12" />
                    <polyline points="12,5 19,12 12,19" />
                  </svg>
                </Link>
              )}
            </div>
          </div>
        )}
      </div>
    </RoleLayout>
  );
}
