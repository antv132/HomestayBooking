// ─── BookingPages.tsx — SCR-19 detail + cancel (form/list live in dedicated pages) ───
// Exports: BookingDetailPage, BookingCancellationPage, formatBookingId

import { useState, useEffect } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import CustomerLayout from '../../layouts/CustomerLayout';
import Alert from '../../components/ui/Alert';
import { bookingApi } from '../../api/bookingApi';

export const formatBookingId = (uuid: string): string => {
  if (!uuid) return '';
  if (uuid.startsWith('b00') && uuid.length === 36) {
    const match = uuid.match(/^b00([0-9])0000-/);
    if (match) return `B00${match[1]}`;
  }
  return uuid.split('-')[0].toUpperCase();
};

function extractApiError(err: unknown, fallback: string): string {
  const data = (err as { response?: { data?: Record<string, unknown>; status?: number } })?.response?.data;
  if (typeof data?.message === 'string' && data.message) return data.message;
  if (typeof data?.error === 'string' && data.error) return data.error;
  const status = (err as { response?: { status?: number } })?.response?.status;
  if (status === 401 || status === 403) return 'Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.';
  return fallback;
}

function StatusBadge({ status }: { status: string }) {
  const m: Record<string, { cls: string; l: string }> = {
    PENDING_DEPOSIT: { cls: 'badge-warning', l: 'Chờ cọc' },
    CONFIRMED:       { cls: 'badge-success', l: 'Đã xác nhận' },
    CHECKED_IN:      { cls: 'badge-info',    l: 'Đang ở' },
    PENDING_INSPECTION: { cls: 'badge-warning', l: 'Chờ kiểm tra' },
    PENDING_DAMAGE_PAYMENT: { cls: 'badge-error', l: 'Chờ phí hư hại' },
    CHECKED_OUT:     { cls: 'badge-purple',  l: 'Đã trả phòng' },
    CANCELLED:       { cls: 'badge-error',   l: 'Đã hủy' },
    NO_SHOW:         { cls: 'badge-neutral', l: 'No-show' },
  };
  const s = m[status] || { cls: 'badge-neutral', l: status };
  return <span className={`badge ${s.cls}`}>{s.l}</span>;
}

function formatDate(d: string) {
  return new Date(d + 'T00:00:00').toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

function formatVndList(n: number) {
  return `₫${Number(n).toLocaleString('vi-VN')}`;
}

// ── SCR-19: Booking Detail ────────────────────────────────────────────────────
const STATUS_BANNER: Record<string, { bg: string; border: string; icon: string; title: string }> = {
  PENDING_DEPOSIT: { bg: '#fff7ed', border: '#fed7aa', icon: '⏳', title: 'Chờ thanh toán cọc' },
  CONFIRMED:       { bg: '#f0fdf4', border: '#bbf7d0', icon: '✓',  title: 'Đã xác nhận' },
  CHECKED_IN:      { bg: '#eff6ff', border: '#bfdbfe', icon: '🏠', title: 'Đang lưu trú' },
  PENDING_INSPECTION: { bg: '#fffbeb', border: '#fde68a', icon: '🔎', title: 'Chờ kiểm tra phòng' },
  PENDING_DAMAGE_PAYMENT: { bg: '#fef2f2', border: '#fecaca', icon: '₫', title: 'Chờ thanh toán phí hư hại' },
  CHECKED_OUT:     { bg: '#f5f3ff', border: '#ddd6fe', icon: '👋', title: 'Đã trả phòng' },
  CANCELLED:       { bg: '#fef2f2', border: '#fecaca', icon: '✕',  title: 'Đã hủy' },
  NO_SHOW:         { bg: '#f8fafc', border: '#cbd5e1', icon: '⊘',  title: 'No-show' },
};

const PAYMENT_STATUS_LABEL: Record<string, string> = {
  PENDING: 'Chờ duyệt',
  PAID: 'Đã thanh toán',
  FAILED: 'Thất bại',
  REFUNDED: 'Đã hoàn',
  PARTIALLY_REFUNDED: 'Hoàn một phần',
};

function bookingCode(id: string) {
  return id.substring(0, 8).toUpperCase();
}

function paymentBadgeCls(status: string) {
  if (status === 'PAID') return 'badge-success';
  if (status === 'FAILED') return 'badge-error';
  if (status === 'REFUNDED' || status === 'PARTIALLY_REFUNDED') return 'badge-info';
  return 'badge-warning';
}

export function BookingDetailPage() {
  const { id } = useParams();
  const [booking, setBooking] = useState<import('../../api/bookingApi').BookingDetailResponse | null>(null);
  const [loadingDetail, setLoadingDetail] = useState(true);
  const [detailError, setDetailError] = useState('');

  useEffect(() => {
    if (!id) return;
    setLoadingDetail(true);
    setDetailError('');
    bookingApi.getMyBookingDetail(id)
      .then(res => setBooking(res.data))
      .catch((err) => setDetailError(extractApiError(err, 'Không tải được thông tin booking.')))
      .finally(() => setLoadingDetail(false));
  }, [id]);

  if (loadingDetail) {
    return (
      <CustomerLayout>
        <div style={{ padding: 48, textAlign: 'center' }}>
          <p className="body-md text-charcoal">Đang tải...</p>
        </div>
      </CustomerLayout>
    );
  }

  if (detailError || !booking) {
    return (
      <CustomerLayout>
        <div style={{ margin: 24 }}>
          <Alert variant="error" message={detailError || 'Không tìm thấy booking.'} />
          <Link to="/customer/bookings" className="btn-outline" style={{ marginTop: 16, display: 'inline-flex' }}>
            ← Quay lại danh sách
          </Link>
        </div>
      </CustomerLayout>
    );
  }

  const nights = Math.ceil(
    (new Date(booking.checkOutDate + 'T00:00:00').getTime() - new Date(booking.checkInDate + 'T00:00:00').getTime()) / 86400000
  );
  const depositAmount = Number(booking.depositAmount);
  const remainingAmount = Number(booking.remainingAmount);
  const payments = booking.payments ?? [];
  const invoiceBreakdown = booking.invoiceBreakdown;
  const depositInvoice = invoiceBreakdown?.invoices?.find((i) => i.type === 'DEPOSIT');
  const remainingInvoice = invoiceBreakdown?.invoices?.find((i) => i.type === 'REMAINING_BALANCE');

  const depositPayment = payments.find(p => p.type === 'DEPOSIT');
  const remainingPayment = payments.find(p => p.type === 'REMAINING_BALANCE');

  const mapInv = (status?: string) => {
    if (!status) return undefined;
    if (status === 'PAID') return 'PAID';
    if (status === 'PENDING_PAYMENT') return 'PENDING';
    if (status === 'CANCELLED') return 'FAILED';
    return 'PENDING';
  };

  const depositPaid = depositInvoice
    ? depositInvoice.status === 'PAID'
    : (depositPayment?.status === 'PAID' || booking.status !== 'PENDING_DEPOSIT');
  const remainingPaid = remainingInvoice
    ? remainingInvoice.status === 'PAID'
    : remainingPayment?.status === 'PAID';

  const depositDisplayStatus = mapInv(depositInvoice?.status)
    ?? depositPayment?.status
    ?? (depositPaid ? 'PAID' : 'PENDING');
  const remainingDisplayStatus = mapInv(remainingInvoice?.status)
    ?? remainingPayment?.status
    ?? (remainingPaid ? 'PAID' : 'PENDING');

  const canCancel = ['PENDING_DEPOSIT', 'CONFIRMED'].includes(booking.status);
  const showPayDeposit = booking.status === 'PENDING_DEPOSIT';
  const showPayRemaining = booking.status === 'CONFIRMED' && !remainingPaid;
  const showPayDamage = booking.status === 'PENDING_DAMAGE_PAYMENT';
  const damageFeeAmount = Number(booking.damageFeeAmount ?? 0);

  const banner = STATUS_BANNER[booking.status] ?? STATUS_BANNER.PENDING_DEPOSIT;

  return (
    <CustomerLayout>
      <div className="flex items-center gap-2 body-sm text-charcoal" style={{ marginBottom: 20 }}>
        <Link to="/customer/bookings" className="text-primary" style={{ textDecoration: 'none' }}>Lịch sử đặt phòng</Link>
        <span>›</span>
        <span className="text-ink" style={{ fontWeight: 600 }}>#{bookingCode(booking.id)}</span>
      </div>

      {/* Status banner */}
      <div style={{
        background: banner.bg, border: `1px solid ${banner.border}`, borderRadius: 12,
        padding: '16px 20px', marginBottom: 24, display: 'flex', alignItems: 'center', gap: 14,
      }}>
        <span style={{ fontSize: 28 }}>{banner.icon}</span>
        <div style={{ flex: 1 }}>
          <p style={{ fontWeight: 700, fontSize: 16, marginBottom: 2 }}>{banner.title}</p>
          <p className="body-sm text-charcoal">
            Booking #{bookingCode(booking.id)} · Tạo ngày {new Date(booking.createdAt).toLocaleDateString('vi-VN')}
          </p>
        </div>
        <StatusBadge status={booking.status} />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_320px] gap-6" style={{ alignItems: 'flex-start' }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          {/* Thông tin booking */}
          <div className="card" style={{ padding: 24 }}>
            <h2 className="heading-sm" style={{ marginBottom: 16 }}>Thông tin đặt phòng</h2>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14 }}>
              {[
                { label: 'Phòng', value: `${booking.roomNumber} — ${booking.roomType}` },
                { label: 'Homestay / Resort', value: booking.propertyName },
                { label: 'Check-in', value: formatDate(booking.checkInDate) },
                { label: 'Check-out', value: formatDate(booking.checkOutDate) },
                { label: 'Số đêm', value: `${nights} đêm` },
                { label: 'Số khách', value: `${booking.guestCount} khách` },
              ].map(item => (
                <div key={item.label}>
                  <p className="body-sm text-charcoal">{item.label}</p>
                  <p style={{ fontWeight: 600, marginTop: 2 }}>{item.value}</p>
                </div>
              ))}
            </div>
            {booking.specialRequests && (
              <div style={{ marginTop: 16, padding: 12, background: 'var(--surface-bone)', borderRadius: 8 }}>
                <p className="body-sm text-charcoal">Ghi chú / yêu cầu</p>
                <p className="body-md" style={{ marginTop: 2 }}>{booking.specialRequests}</p>
              </div>
            )}
          </div>

          {/* Thanh toán */}
          <div className="card" style={{ padding: 24 }}>
            <h2 className="heading-sm" style={{ marginBottom: 16 }}>Trạng thái thanh toán</h2>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              <div style={{
                display: 'flex', alignItems: 'center', gap: 12, padding: '12px 14px',
                background: depositPaid ? '#f0fdf4' : 'var(--surface-bone)', borderRadius: 10,
                border: `1px solid ${depositPaid ? '#bbf7d0' : 'var(--hairline)'}`,
              }}>
                <div style={{
                  width: 32, height: 32, borderRadius: '50%',
                  background: depositPaid ? '#2b9a66' : 'var(--stone)',
                  display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
                }}>
                  {depositPaid ? (
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5"><polyline points="20,6 9,17 4,12"/></svg>
                  ) : (
                    <span style={{ color: '#fff', fontWeight: 700, fontSize: 12 }}>1</span>
                  )}
                </div>
                <div style={{ flex: 1 }}>
                  <p style={{ fontWeight: 600, fontSize: 14 }}>Cọc (40%)</p>
                  <p className="body-sm text-charcoal">{formatVndList(depositAmount)}</p>
                  {depositPayment?.paidAt && (
                    <p className="body-sm text-charcoal" style={{ fontSize: 11 }}>
                      Thanh toán: {new Date(depositPayment.paidAt).toLocaleString('vi-VN')}
                    </p>
                  )}
                </div>
                <span className={`badge ${paymentBadgeCls(depositDisplayStatus)}`}>
                  {PAYMENT_STATUS_LABEL[depositDisplayStatus] ?? depositDisplayStatus}
                </span>
              </div>

              <div style={{
                display: 'flex', alignItems: 'center', gap: 12, padding: '12px 14px',
                background: remainingPaid ? '#f0fdf4' : 'var(--surface-bone)', borderRadius: 10,
                border: `1px solid ${remainingPaid ? '#bbf7d0' : 'var(--hairline)'}`,
              }}>
                <div style={{
                  width: 32, height: 32, borderRadius: '50%',
                  background: remainingPaid ? '#2b9a66' : 'var(--stone)',
                  display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
                }}>
                  {remainingPaid ? (
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5"><polyline points="20,6 9,17 4,12"/></svg>
                  ) : (
                    <span style={{ color: '#fff', fontWeight: 700, fontSize: 12 }}>2</span>
                  )}
                </div>
                <div style={{ flex: 1 }}>
                  <p style={{ fontWeight: 600, fontSize: 14 }}>Phần còn lại (60%)</p>
                  <p className="body-sm text-charcoal">{formatVndList(remainingAmount)}</p>
                  {remainingPayment?.paidAt && (
                    <p className="body-sm text-charcoal" style={{ fontSize: 11 }}>
                      Thanh toán: {new Date(remainingPayment.paidAt).toLocaleString('vi-VN')}
                    </p>
                  )}
                </div>
                <span className={`badge ${paymentBadgeCls(remainingDisplayStatus)}`}>
                  {PAYMENT_STATUS_LABEL[remainingDisplayStatus] ?? remainingDisplayStatus}
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Actions panel */}
        <div>
          <div className="card-lg" style={{ padding: 24, marginBottom: 16 }}>
            <h3 className="heading-sm" style={{ marginBottom: 16 }}>Tổng tiền</h3>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
              <span className="body-sm text-charcoal">Giá phòng × {nights} đêm</span>
              <span style={{ fontWeight: 600 }}>{formatVndList(booking.totalAmount)}</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
              <span className="body-sm text-charcoal">Cọc (40%)</span>
              <span style={{ fontWeight: 600, color: 'var(--primary)' }}>{formatVndList(depositAmount)}</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
              <span className="body-sm text-charcoal">Còn lại (60%)</span>
              <span style={{ fontWeight: 600 }}>{formatVndList(remainingAmount)}</span>
            </div>
            <div className="divider" style={{ margin: '12px 0' }} />
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
              <span style={{ fontWeight: 700 }}>Tổng cộng</span>
              <span style={{ fontWeight: 800, fontSize: 18 }}>{formatVndList(booking.totalAmount)}</span>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              {showPayDeposit && (
                <Link to={`/customer/payments/${booking.id}/pay`} className="btn-primary" style={{ width: '100%', justifyContent: 'center' }}>
                  Thanh toán cọc ({formatVndList(depositAmount)})
                </Link>
              )}
              {showPayRemaining && (
                <Link to={`/customer/payments/${booking.id}/remaining`} className="btn-primary" style={{ width: '100%', justifyContent: 'center' }}>
                  Thanh toán phần còn lại ({formatVndList(remainingAmount)})
                </Link>
              )}
              {showPayDamage && (
                <Link to={`/customer/payments/${booking.id}/damage`} className="btn-primary" style={{ width: '100%', justifyContent: 'center' }}>
                  Thanh toán / tranh chấp phí hư hại{damageFeeAmount > 0 ? ` (${formatVndList(damageFeeAmount)})` : ''}
                </Link>
              )}
              {booking.status === 'CHECKED_OUT' && !booking.isReviewed && (
                <Link to={`/customer/reviews/create?bookingId=${booking.id}`} className="btn-primary" style={{ width: '100%', justifyContent: 'center' }}>
                  ⭐ Viết đánh giá
                </Link>
              )}
            </div>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            <Link to={`/customer/contracts?bookingId=${booking.id}`} className="btn-outline" style={{ justifyContent: 'flex-start', gap: 10 }}>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14,2 14,8 20,8"/></svg>
              Xem hợp đồng
            </Link>
            {canCancel && (
              <Link to={`/customer/bookings/${booking.id}/cancel`} className="btn-ghost" style={{ justifyContent: 'flex-start', color: 'var(--error)', gap: 10 }}>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
                Hủy booking
              </Link>
            )}
          </div>

          {showPayDeposit && (
            <div style={{ marginTop: 16 }}>
              <Alert
                variant="warning"
                message="Vui lòng thanh toán cọc trong thời gian quy định để giữ phòng."
              />
            </div>
          )}
          {showPayDamage && (
            <div style={{ marginTop: 16 }}>
              <Alert
                variant="error"
                message="Cần thanh toán phí hư hại qua VNPay để hoàn tất trả phòng."
              />
            </div>
          )}
        </div>
      </div>
    </CustomerLayout>
  );
}

// ── SCR-19: Booking Cancellation ─────────────────────────────────────────────
const NON_CANCELLABLE_STATUSES = new Set(['CHECKED_IN', 'CHECKED_OUT', 'CANCELLED']);

export function BookingCancellationPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [booking, setBooking] = useState<import('../../api/bookingApi').BookingDetailResponse | null>(null);
  const [preview, setPreview] = useState<import('../../api/bookingApi').CancellationPreview | null>(null);
  const [loadingDetail, setLoadingDetail] = useState(true);
  const [detailError, setDetailError] = useState('');
  const [previewError, setPreviewError] = useState('');
  const [loading, setLoading] = useState(false);
  const [confirmed, setConfirmed] = useState(false);
  const [cancelError, setCancelError] = useState('');

  useEffect(() => {
    if (!id) return;
    setLoadingDetail(true);
    setDetailError('');
    setPreviewError('');
    Promise.all([
      bookingApi.getMyBookingDetail(id),
      bookingApi.getCancellationPreview(id).catch((err) => {
        setPreviewError(extractApiError(err, 'Không tải được chính sách hủy.'));
        return null;
      }),
    ])
      .then(([bookingRes, previewRes]) => {
        setBooking(bookingRes.data);
        if (previewRes) setPreview(previewRes.data);
      })
      .catch((err) => setDetailError(extractApiError(err, 'Không tải được thông tin booking.')))
      .finally(() => setLoadingDetail(false));
  }, [id]);

  async function handleCancel() {
    if (!id) return;
    setLoading(true);
    setCancelError('');
    try {
      await bookingApi.cancelMyBooking(id);
      navigate('/customer/bookings');
    } catch (err: unknown) {
      setCancelError(extractApiError(err, 'Hủy booking thất bại.'));
      setLoading(false);
    }
  }

  if (loadingDetail) {
    return (
      <CustomerLayout>
        <div style={{ padding: 48, textAlign: 'center' }}>
          <p className="body-md text-charcoal">Đang tải...</p>
        </div>
      </CustomerLayout>
    );
  }

  if (detailError || !booking) {
    return (
      <CustomerLayout>
        <div style={{ margin: 24 }}>
          <Alert variant="error" message={detailError || 'Không tìm thấy booking.'} />
          <Link to="/customer/bookings" className="btn-outline" style={{ marginTop: 16, display: 'inline-flex' }}>
            ← Quay lại danh sách
          </Link>
        </div>
      </CustomerLayout>
    );
  }

  if (NON_CANCELLABLE_STATUSES.has(booking.status)) {
    return (
      <CustomerLayout>
        <div style={{ margin: 24, maxWidth: 560 }}>
          <Alert variant="error" message="Booking không thể hủy ở trạng thái hiện tại." />
          <Link to={`/customer/bookings/${id}`} className="btn-outline" style={{ marginTop: 16, display: 'inline-flex' }}>
            ← Quay lại chi tiết booking
          </Link>
        </div>
      </CustomerLayout>
    );
  }

  const refundPercent = preview?.refundPercent ?? 0;
  const policyVariant = refundPercent === 0 ? 'error' : refundPercent < 100 ? 'warning' : 'info';

  return (
    <CustomerLayout>
      <div style={{ maxWidth: 560, margin: '0 auto' }}>
        <div className="flex items-center gap-2 body-sm text-charcoal" style={{ marginBottom: 20 }}>
          <Link to={`/customer/bookings/${id}`} className="text-primary" style={{ textDecoration: 'none' }}>
            Booking #{bookingCode(booking.id)}
          </Link>
          <span>›</span>
          <span className="text-ink" style={{ fontWeight: 600 }}>Hủy đặt phòng</span>
        </div>

        <div className="card-lg" style={{ padding: 32 }}>
          <div style={{ width: 52, height: 52, borderRadius: '50%', background: '#fee2e2', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 20 }}>
            <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="#dc2626" strokeWidth="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
          </div>

          <h1 className="heading-md" style={{ marginBottom: 8 }}>Hủy đặt phòng</h1>
          <p className="body-md text-charcoal" style={{ marginBottom: 24 }}>
            Bạn sắp hủy đặt phòng <strong>{booking.roomNumber}</strong> tại {booking.propertyName}.
          </p>

          <div style={{ background: 'var(--surface-bone)', borderRadius: 10, padding: 16, marginBottom: 20 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
              <span className="body-sm text-charcoal">Check-in / Check-out</span>
              <span style={{ fontWeight: 600, fontSize: 14 }}>
                {formatDate(booking.checkInDate)} → {formatDate(booking.checkOutDate)}
              </span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
              <span className="body-sm text-charcoal">Còn lại đến check-in</span>
              <span style={{ fontWeight: 600 }}>{preview?.daysUntilCheckIn ?? '—'} ngày</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span className="body-sm text-charcoal">Tổng tiền</span>
              <span style={{ fontWeight: 600 }}>{formatVndList(booking.totalAmount)}</span>
            </div>
          </div>

          {previewError && (
            <div style={{ marginBottom: 16 }}>
              <Alert variant="error" message={previewError} />
            </div>
          )}

          {preview && (
            <div style={{ marginBottom: 20 }}>
              <Alert variant={policyVariant} message={preview.policyText} />
              <p className="body-sm" style={{ marginTop: 12, fontWeight: 600 }}>
                Số tiền hoàn dự kiến: {formatVndList(preview.refundAmount)}
                {preview.forfeitAmount > 0 && (
                  <span className="text-charcoal" style={{ fontWeight: 400 }}>
                    {' '}(khấu trừ {formatVndList(preview.forfeitAmount)})
                  </span>
                )}
              </p>
            </div>
          )}

          {cancelError && (
            <div style={{ marginBottom: 16 }}>
              <Alert variant="error" message={cancelError} closeable onClose={() => setCancelError('')} />
            </div>
          )}

          <label style={{ display: 'flex', alignItems: 'flex-start', gap: 10, marginBottom: 24, cursor: 'pointer' }}>
            <input
              type="checkbox"
              checked={confirmed}
              onChange={(e) => setConfirmed(e.target.checked)}
              style={{ width: 16, height: 16, accentColor: 'var(--error)', cursor: 'pointer', marginTop: 2, flexShrink: 0 }}
            />
            <span className="body-sm">
              {!preview
                ? 'Tôi hiểu hành động này không thể hoàn tác.'
                : refundPercent <= 0
                  ? 'Tôi hiểu hành động này không thể hoàn tác và sẽ không được hoàn tiền cọc theo chính sách hủy.'
                  : `Tôi hiểu hành động này không thể hoàn tác và đồng ý hoàn ${refundPercent}% cọc (dự kiến ${formatVndList(preview.refundAmount)}).`}
            </span>
          </label>

          <div style={{ display: 'flex', gap: 12 }}>
            <button
              type="button"
              className="btn-danger"
              style={{ flex: 1 }}
              onClick={handleCancel}
              disabled={!confirmed || loading || !!previewError}
            >
              {loading ? 'Đang hủy...' : 'Xác nhận hủy'}
            </button>
            <Link to={`/customer/bookings/${id}`} className="btn-ghost" style={{ flex: 1, justifyContent: 'center' }}>
              Giữ lại
            </Link>
          </div>
        </div>
      </div>
    </CustomerLayout>
  );
}
