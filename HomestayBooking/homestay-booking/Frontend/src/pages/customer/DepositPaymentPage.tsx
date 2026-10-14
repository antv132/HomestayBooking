// SCR-21: Order Review & Deposit Payment (+ SCR-20 hold countdown)
import { useState, useEffect, useMemo } from 'react';
import { Link, useParams } from 'react-router-dom';
import CustomerLayout from '../../layouts/CustomerLayout';
import Alert from '../../components/ui/Alert';
import { bookingApi, BookingDetailResponse } from '../../api/bookingApi';
import { paymentApi } from '../../api/paymentApi';

function formatDate(d: string) {
  return new Date(d + 'T00:00:00').toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

function formatVnd(n: number) {
  return `₫${Number(n).toLocaleString('vi-VN')}`;
}

function formatRemain(ms: number) {
  const totalSec = Math.max(0, Math.floor(ms / 1000));
  const m = Math.floor(totalSec / 60);
  const s = totalSec % 60;
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
}

function useHoldCountdown(holdExpiresAt: string | null | undefined) {
  const endMs = useMemo(() => {
    if (!holdExpiresAt) return null;
    const t = new Date(holdExpiresAt).getTime();
    return Number.isFinite(t) ? t : null;
  }, [holdExpiresAt]);

  const [now, setNow] = useState(() => Date.now());

  useEffect(() => {
    if (endMs == null) return;
    setNow(Date.now());
    const id = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(id);
  }, [endMs]);

  if (endMs == null) {
    return { remainingMs: null as number | null, expired: false, label: '' };
  }
  const remainingMs = Math.max(0, endMs - now);
  return {
    remainingMs,
    expired: remainingMs <= 0,
    label: formatRemain(remainingMs),
  };
}

function PaymentForm({
  type,
  bookingId,
  disabled,
}: {
  type: 'DEPOSIT' | 'REMAINING_BALANCE';
  bookingId: string;
  disabled?: boolean;
}) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleVNPay() {
    if (disabled) return;
    setLoading(true);
    setError(null);
    try {
      const res = await paymentApi.createVnpayUrl(bookingId, type);
      if (res.success && res.data?.paymentUrl) {
        window.location.href = res.data.paymentUrl;
      } else {
        setError('Không thể tạo URL thanh toán VNPay');
      }
    } catch {
      setError('Lỗi kết nối khi tạo thanh toán VNPay');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div>
      {error && (
        <div className="mb-4">
          <Alert variant="error" message={error} />
        </div>
      )}
      <div className="mb-6 p-4 border border-[var(--hairline)] rounded-lg bg-[var(--surface-bone)]">
        <div className="flex items-center gap-3">
          <span className="text-3xl" aria-hidden="true">💳</span>
          <div>
            <p className="font-semibold">Thanh toán qua cổng VNPay</p>
            <p className="body-sm text-charcoal">Hỗ trợ quét mã QR, thẻ ATM, Visa/Mastercard</p>
          </div>
        </div>
      </div>
      <button
        type="button"
        onClick={handleVNPay}
        className="btn-primary w-full py-3.5 text-base"
        disabled={loading || disabled}
      >
        {loading ? 'Đang kết nối VNPay...' : 'Thanh toán qua VNPay'}
      </button>
    </div>
  );
}

export default function DepositPaymentPage() {
  const { id } = useParams<{ id: string }>();
  const [booking, setBooking] = useState<BookingDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) { setLoading(false); setError('Không tìm thấy mã đặt phòng'); return; }
    setLoading(true); setError(null);
    bookingApi.getMyBookingDetail(id)
      .then(res => setBooking(res.data))
      .catch(() => setError('Không thể tải thông tin đặt phòng'))
      .finally(() => setLoading(false));
  }, [id]);

  const hold = useHoldCountdown(booking?.holdExpiresAt);

  if (loading) return <CustomerLayout><div className="py-10 text-center body-md text-charcoal">Đang tải thông tin...</div></CustomerLayout>;
  if (error || !booking) return <CustomerLayout><div className="max-w-2xl mx-auto py-8"><Alert variant="error" message={error || 'Đặt phòng không tồn tại'} /></div></CustomerLayout>;

  const shortId = booking.id.split('-')[0];

  if (booking.status !== 'PENDING_DEPOSIT') {
    return (
      <CustomerLayout>
        <div className="max-w-2xl mx-auto py-8 space-y-4">
          <Alert variant="warning" message="Đặt phòng này không ở trạng thái chờ thanh toán cọc." />
          <Link to={`/customer/bookings/${booking.id}`} className="btn-primary inline-flex">Xem chi tiết đặt phòng</Link>
        </div>
      </CustomerLayout>
    );
  }

  const holdExpired = hold.expired;

  return (
    <CustomerLayout>
      <div className="max-w-6xl mx-auto">
        <nav className="flex items-center gap-2 body-sm text-charcoal mb-5">
          <Link to="/customer/bookings" className="text-primary no-underline">Đặt phòng</Link>
          <span aria-hidden="true">›</span>
          <Link to={`/customer/bookings/${booking.id}`} className="text-primary no-underline">{shortId}</Link>
          <span aria-hidden="true">›</span>
          <span className="font-semibold">Thanh toán</span>
        </nav>
        <h1 className="heading-md mb-1">Xem lại &amp; Thanh toán</h1>
        <p className="body-md text-charcoal mb-8">Xác nhận thông tin đặt phòng và thanh toán 40% tiền cọc qua VNPay</p>
        <div className="grid grid-cols-1 lg:grid-cols-5 gap-6 lg:gap-8 items-start">
          <div className="lg:col-span-3 space-y-6">
            <section className="card p-5">
              <h2 className="heading-sm mb-4">Thông tin khách</h2>
              <dl className="space-y-2 body-md">
                <div className="flex justify-between gap-4"><dt className="text-charcoal">Họ tên</dt><dd className="font-medium text-right">{booking.customerName}</dd></div>
                <div className="flex justify-between gap-4"><dt className="text-charcoal">Email</dt><dd className="font-medium text-right break-all">{booking.customerEmail}</dd></div>
                <div className="flex justify-between gap-4"><dt className="text-charcoal">Số điện thoại</dt><dd className="font-medium">{booking.customerPhone || '—'}</dd></div>
              </dl>
            </section>
            <section className="card p-5">
              <h2 className="heading-sm mb-4">Chi tiết đặt phòng</h2>
              <p className="font-bold text-base mb-1">{booking.roomNumber} — {booking.roomType}</p>
              <p className="body-sm text-charcoal mb-3">{booking.propertyName}</p>
              <p className="body-md mb-1">📅 {formatDate(booking.checkInDate)} → {formatDate(booking.checkOutDate)}</p>
              <p className="body-sm text-charcoal">👥 {booking.guestCount} khách</p>
              <div className="divider my-4" />
              <p className="body-sm text-charcoal mb-1">Yêu cầu đặc biệt</p>
              <p className="body-md">{booking.specialRequests?.trim() ? booking.specialRequests : 'Không có'}</p>
            </section>
            <section className="card p-5">
              <h2 className="heading-sm mb-4">Chính sách &amp; nội quy</h2>
              <ul className="body-md text-charcoal space-y-2 list-disc pl-5">
                <li>Chính sách hủy: từ 7 ngày trước check-in hoàn 100% cọc; từ 3 đến dưới 7 ngày hoàn 50%; dưới 3 ngày không hoàn.</li>
                <li>Giờ check-in: 14:00 · check-out: 12:00.</li>
                <li>Giữ yên tĩnh sau 22:00; không tổ chức tiệc trong phòng.</li>
                <li>Cấm hút thuốc trong phòng.</li>
              </ul>
            </section>
          </div>
          <div className="lg:col-span-2">
            <div className="card p-5 lg:sticky lg:top-6 shadow-md">
              <h2 className="heading-sm mb-4">Tóm tắt đơn hàng</h2>
              <div className="space-y-3 body-md">
                <div className="flex justify-between"><span className="text-charcoal">Tổng tiền</span><span className="font-semibold">{formatVnd(booking.totalAmount)}</span></div>
                <div className="flex justify-between"><span className="text-charcoal">Tiền cọc (40%)</span><span className="font-semibold text-primary">{formatVnd(booking.depositAmount)}</span></div>
                <div className="flex justify-between"><span className="text-charcoal">Còn lại (60%)</span><span className="font-semibold">{formatVnd(booking.remainingAmount)}</span></div>
              </div>
              <div className="divider my-4" />

              {booking.holdExpiresAt && !holdExpired && (
                <div
                  className="mb-4 p-3 rounded-lg border border-[#fed7aa] bg-[#fff7ed]"
                  role="timer"
                  aria-live="polite"
                  aria-atomic="true"
                >
                  <p className="body-sm text-charcoal mb-1">Thời gian giữ chỗ còn lại</p>
                  <p className="font-bold text-lg tabular-nums text-[#c2410c]" aria-label={`${hold.label} còn lại`}>
                    {hold.label}
                  </p>
                  <p className="body-sm text-charcoal mt-1">
                    Thanh toán cọc trước khi hết giờ để giữ phòng (mặc định 10 phút).
                  </p>
                </div>
              )}

              {holdExpired && (
                <div className="mb-4">
                  <Alert
                    variant="error"
                    message="Hết thời gian giữ chỗ. Booking có thể đã bị hủy — vui lòng tạo đặt phòng mới hoặc tải lại trang."
                  />
                  <div className="flex flex-col gap-2 mt-3">
                    <button
                      type="button"
                      className="btn-outline w-full"
                      onClick={() => window.location.reload()}
                    >
                      Tải lại trang
                    </button>
                    <Link to="/search" className="btn-primary w-full justify-center inline-flex">
                      Đặt phòng mới
                    </Link>
                  </div>
                </div>
              )}

              <p className="body-sm text-charcoal mb-3">Phương thức thanh toán</p>
              <PaymentForm type="DEPOSIT" bookingId={booking.id} disabled={holdExpired} />
            </div>
          </div>
        </div>
      </div>
    </CustomerLayout>
  );
}
