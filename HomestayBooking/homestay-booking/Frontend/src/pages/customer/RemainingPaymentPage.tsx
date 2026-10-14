// SCR-22: Remaining Balance Payment
import { useState, useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import CustomerLayout from '../../layouts/CustomerLayout';
import Alert from '../../components/ui/Alert';
import { bookingApi, BookingDetailResponse } from '../../api/bookingApi';
import { paymentApi } from '../../api/paymentApi';

function formatVnd(n: number) {
  return `₫${Number(n).toLocaleString('vi-VN')}`;
}

function PaymentForm({ type, bookingId }: { type: 'DEPOSIT' | 'REMAINING_BALANCE'; bookingId: string }) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleVNPay() {
    setLoading(true);
    setError(null);
    try {
      const res = await paymentApi.createVnpayUrl(bookingId, type);
      if (res.success && res.data?.paymentUrl) {
        window.location.href = res.data.paymentUrl;
      } else {
        setError('Không thể tạo URL thanh toán VNPay');
      }
    } catch (err) {
      console.error(err);
      setError('Lỗi kết nối khi tạo thanh toán VNPay');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div>
      {error && <div className="mb-4"><Alert variant="error" message={error} /></div>}
      <div className="mb-6 p-4 border border-[var(--hairline)] rounded-lg bg-[var(--surface-bone)]">
        <div className="flex items-center gap-3">
          <span className="text-3xl" aria-hidden="true">💳</span>
          <div>
            <p className="font-semibold">Thanh toán qua cổng VNPay</p>
            <p className="body-sm text-charcoal">Hỗ trợ quét mã QR, thẻ ATM, Visa/Mastercard</p>
          </div>
        </div>
      </div>
      <button type="button" onClick={handleVNPay} className="btn-primary w-full py-3.5 text-base" disabled={loading}>
        {loading ? 'Đang kết nối VNPay...' : 'Thanh toán qua VNPay'}
      </button>
    </div>
  );
}

export default function RemainingPaymentPage() {
  const { id } = useParams<{ id: string }>();
  const [booking, setBooking] = useState<BookingDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    bookingApi.getMyBookingDetail(id)
      .then(res => setBooking(res.data))
      .catch(() => setError('Không thể tải thông tin booking'))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) return <CustomerLayout><div style={{ padding: 40, textAlign: 'center' }}>Đang tải thông tin...</div></CustomerLayout>;
  if (error || !booking) return <CustomerLayout><div className="alert alert-error">{error || 'Booking không tồn tại'}</div></CustomerLayout>;

  return (
    <CustomerLayout>
      <div style={{ maxWidth: 620, margin: '0 auto' }}>
        <div className="flex items-center gap-2 body-sm text-charcoal" style={{ marginBottom: 20 }}>
          <Link to="/customer/bookings" className="text-primary" style={{ textDecoration: 'none' }}>Bookings</Link>
          <span>›</span>
          <Link to={`/customer/bookings/${booking.id}`} className="text-primary" style={{ textDecoration: 'none' }}>{booking.id.split('-')[0]}</Link>
          <span>›</span>
          <span style={{ fontWeight: 600 }}>Thanh toán số dư còn lại</span>
        </div>
        <h1 className="heading-md" style={{ marginBottom: 4 }}>Thanh toán số dư còn lại</h1>
        <p className="body-md text-charcoal" style={{ marginBottom: 24 }}>Thanh toán 60% còn lại trước khi nhận phòng</p>

        <div className="card" style={{ padding: 20, marginBottom: 24 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <div>
              <p style={{ fontWeight: 700 }}>{booking.roomNumber}</p>
              <p className="body-sm text-charcoal">{booking.propertyName}</p>
            </div>
            <div style={{ textAlign: 'right' }}>
              <p className="body-sm text-charcoal">Đã cọc</p>
              <p style={{ fontWeight: 600 }}>{formatVnd(booking.depositAmount)}</p>
            </div>
          </div>
          <div className="divider" style={{ margin: '14px 0' }} />
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <span className="heading-sm">Còn lại (60%)</span>
            <span className="heading-sm text-primary">{formatVnd(booking.remainingAmount)}</span>
          </div>
        </div>

        <div className="card" style={{ padding: 24 }}>
          <PaymentForm type="REMAINING_BALANCE" bookingId={booking.id} />
        </div>
      </div>
    </CustomerLayout>
  );
}