// SCR-24: VNPay Payment Result
import { useSearchParams } from 'react-router-dom';
import { Link } from 'react-router-dom';
import CustomerLayout from '../../layouts/CustomerLayout';

export default function VNPayResultPage() {
  const [searchParams] = useSearchParams();
  const status = searchParams.get('status');
  const bookingId = searchParams.get('bookingId');
  const message = searchParams.get('message');
  const isSuccess = status === 'success';

  return (
    <CustomerLayout>
      <div style={{ maxWidth: 480, margin: '60px auto', textAlign: 'center' }}>
        <div style={{ width: 64, height: 64, borderRadius: '50%', background: isSuccess ? '#dcfce7' : '#fee2e2', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 16px' }}>
          {isSuccess ? (
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="var(--success)" strokeWidth="2.5" strokeLinecap="round"><polyline points="20,6 9,17 4,12" /></svg>
          ) : (
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="var(--error)" strokeWidth="2.5" strokeLinecap="round"><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>
          )}
        </div>
        <h2 className="heading-md" style={{ marginBottom: 8 }}>
          {isSuccess ? 'Thanh toán VNPay thành công!' : 'Thanh toán thất bại'}
        </h2>
        <p className="body-md text-charcoal" style={{ marginBottom: 24 }}>
          {isSuccess
            ? 'Cảm ơn bạn đã thanh toán. Giao dịch đã được ghi nhận vào hệ thống.'
            : message === 'Payment_Failed' ? 'Bạn đã hủy giao dịch hoặc thẻ không đủ số dư.' : 'Đã có lỗi xảy ra trong quá trình xử lý giao dịch (' + message + ').'}
        </p>
        {isSuccess && bookingId && (
          <Link to={`/customer/bookings/${bookingId}`} className="btn-primary" style={{ marginBottom: 12, display: 'block' }}>
            Xem chi tiết Booking
          </Link>
        )}
        <Link to="/customer/payments" className={isSuccess ? 'btn-ghost' : 'btn-primary'} style={{ display: 'block' }}>
          {isSuccess ? 'Lịch sử thanh toán' : 'Quay lại Lịch sử thanh toán'}
        </Link>
      </div>
    </CustomerLayout>
  );
}