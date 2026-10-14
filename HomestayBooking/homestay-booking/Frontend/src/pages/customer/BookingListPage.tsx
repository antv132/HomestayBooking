import { useState, useEffect, useCallback, useMemo } from 'react';
import { Link } from 'react-router-dom';
import CustomerLayout from '../../layouts/CustomerLayout';
import Alert from '../../components/ui/Alert';
import { bookingApi, type BookingSummaryResponse } from '../../api/bookingApi';

type TabKey = 'ALL' | 'UPCOMING' | 'COMPLETED' | 'CANCELLED';

const TABS: { key: TabKey; label: string }[] = [
  { key: 'ALL', label: 'Tất cả' },
  { key: 'UPCOMING', label: 'Sắp tới' },
  { key: 'COMPLETED', label: 'Hoàn tất' },
  { key: 'CANCELLED', label: 'Đã hủy' },
];

const UPCOMING_STATUSES = new Set([
  'PENDING_DEPOSIT', 'CONFIRMED', 'CHECKED_IN', 'PENDING_INSPECTION', 'PENDING_DAMAGE_PAYMENT',
]);

const STATUS_MAP: Record<string, { cls: string; label: string }> = {
  PENDING_DEPOSIT: { cls: 'badge-warning', label: 'Chờ cọc' },
  CONFIRMED:       { cls: 'badge-success', label: 'Đã xác nhận' },
  CHECKED_IN:      { cls: 'badge-info',    label: 'Đang ở' },
  PENDING_INSPECTION: { cls: 'badge-warning', label: 'Chờ kiểm tra' },
  PENDING_DAMAGE_PAYMENT: { cls: 'badge-error', label: 'Chờ phí hư hại' },
  CHECKED_OUT:     { cls: 'badge-purple',  label: 'Đã trả phòng' },
  CANCELLED:       { cls: 'badge-error',   label: 'Đã hủy' },
  NO_SHOW:         { cls: 'badge-neutral', label: 'No-show' },
};

function StatusBadge({ status }: { status: string }) {
  const s = STATUS_MAP[status] || { cls: 'badge-neutral', label: status };
  return <span className={`badge ${s.cls}`}>{s.label}</span>;
}

function formatDate(d: string) {
  return new Date(d).toLocaleDateString('vi-VN');
}

function formatVnd(amount: number) {
  return `${amount.toLocaleString('vi-VN')} ₫`;
}

function filterByTab(bookings: BookingSummaryResponse[], tab: TabKey): BookingSummaryResponse[] {
  switch (tab) {
    case 'UPCOMING':
      return bookings.filter((b) => UPCOMING_STATUSES.has(b.status));
    case 'COMPLETED':
      return bookings.filter((b) => b.status === 'CHECKED_OUT');
    case 'CANCELLED':
      return bookings.filter((b) => b.status === 'CANCELLED');
    default:
      return bookings;
  }
}

export default function BookingListPage() {
  const [tab, setTab] = useState<TabKey>('ALL');
  const [bookings, setBookings] = useState<BookingSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadBookings = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const res = await bookingApi.getMyBookings({ page: 0, size: 50, sort: 'createdAt,desc' });
      setBookings(res.data.content);
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      setError(axiosErr?.response?.data?.message ?? 'Không thể tải danh sách đặt phòng. Vui lòng thử lại.');
      setBookings([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadBookings();
  }, [loadBookings]);

  const list = useMemo(() => filterByTab(bookings, tab), [bookings, tab]);

  return (
    <CustomerLayout>
      <div className="flex items-center justify-between flex-wrap gap-3" style={{ marginBottom: 24 }}>
        <h1 className="heading-md">Đặt phòng của tôi</h1>
        <Link to="/rooms" className="btn-primary btn-sm">+ Đặt phòng mới</Link>
      </div>

      <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginBottom: 20, padding: '4px', background: 'var(--surface-bone)', borderRadius: 9999, width: 'fit-content' }}>
        {TABS.map((t) => (
          <button
            key={t.key}
            type="button"
            className={`tab-pill ${tab === t.key ? 'active' : ''}`}
            onClick={() => setTab(t.key)}
          >
            {t.label}
          </button>
        ))}
      </div>

      {error && (
        <div style={{ marginBottom: 16 }}>
          <Alert variant="error" message={error} closeable onClose={() => setError('')} />
          <button type="button" className="btn-primary btn-sm" style={{ marginTop: 12 }} onClick={loadBookings}>
            Thử lại
          </button>
        </div>
      )}

      {loading ? (
        <p className="body-sm text-charcoal">Đang tải...</p>
      ) : list.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '60px 32px' }}>
          <div style={{ fontSize: 40, marginBottom: 12 }}>📋</div>
          <h3 className="heading-sm" style={{ marginBottom: 8 }}>Chưa có đặt phòng</h3>
          <p className="body-md text-charcoal" style={{ marginBottom: 16 }}>
            {tab === 'ALL' ? 'Bạn chưa có đặt phòng nào.' : 'Không có đặt phòng trong mục này.'}
          </p>
          <Link to="/rooms" className="btn-primary">Đặt phòng mới</Link>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {list.map((b) => (
            <div key={b.id} className="card" style={{ padding: 20 }}>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 16, alignItems: 'center' }}>
                <div style={{ flex: 1, minWidth: 200 }}>
                  <div className="flex items-center gap-2 flex-wrap" style={{ marginBottom: 4 }}>
                    <span style={{ fontWeight: 700, fontSize: 15 }}>{b.roomNumber} — {b.roomType}</span>
                    <StatusBadge status={b.status} />
                  </div>
                  <p className="body-sm text-charcoal" style={{ marginBottom: 3 }}>📍 {b.propertyName}</p>
                  <p className="body-sm text-charcoal">
                    📅 {formatDate(b.checkInDate)} → {formatDate(b.checkOutDate)} · {b.guestCount} khách
                  </p>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <p style={{ fontWeight: 700, fontSize: 16, marginBottom: 8 }}>{formatVnd(b.totalAmount)}</p>
                  <div style={{ display: 'flex', gap: 6, justifyContent: 'flex-end', flexWrap: 'wrap' }}>
                    {b.status === 'PENDING_DEPOSIT' && (
                      <Link to={`/customer/payments/${b.id}/pay`} className="btn-primary btn-sm">
                        Thanh toán cọc
                      </Link>
                    )}
                    {b.status === 'PENDING_DAMAGE_PAYMENT' && (
                      <Link to={`/customer/payments/${b.id}/damage`} className="btn-primary btn-sm">
                        Thanh toán / tranh chấp phí hư hại
                      </Link>
                    )}
                    {b.status === 'CHECKED_OUT' && !b.isReviewed && (
                      <Link to={`/customer/reviews/create?bookingId=${b.id}`} className="btn-outline btn-sm">
                        Viết đánh giá
                      </Link>
                    )}
                    <Link to={`/customer/bookings/${b.id}`} className="btn-outline btn-sm">
                      Xem chi tiết
                    </Link>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </CustomerLayout>
  );
}
