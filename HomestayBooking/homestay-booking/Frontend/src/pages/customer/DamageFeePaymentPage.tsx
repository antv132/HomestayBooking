// SCR-22b: Damage Fee Payment (VNPay) — 012 US6 / 023 FR-012 + FR-013 dispute
import { FormEvent, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import CustomerLayout from '../../layouts/CustomerLayout';
import Alert from '../../components/ui/Alert';
import { bookingApi, BookingDetailResponse } from '../../api/bookingApi';
import { paymentApi } from '../../api/paymentApi';
import { CustomerDamageReport, damageReportApi } from '../../api/damageReportApi';

function formatVnd(n: number) {
  return `₫${Number(n).toLocaleString('vi-VN')}`;
}

function formatDeadline(iso: string | null) {
  if (!iso) return null;
  return new Date(iso).toLocaleString('vi-VN');
}

export default function DamageFeePaymentPage() {
  const { id } = useParams<{ id: string }>();
  const [booking, setBooking] = useState<BookingDetailResponse | null>(null);
  const [report, setReport] = useState<CustomerDamageReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [paying, setPaying] = useState(false);
  const [disputing, setDisputing] = useState(false);
  const [disputeNote, setDisputeNote] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    let cancelled = false;
    (async () => {
      setLoading(true);
      setError(null);
      try {
        const [bookingRes, reportsRes] = await Promise.all([
          bookingApi.getMyBookingDetail(id),
          damageReportApi.listMine(),
        ]);
        if (cancelled) return;
        setBooking(bookingRes.data);
        const match = (reportsRes.data ?? []).find((r) => r.bookingId === id) ?? null;
        setReport(match);
      } catch {
        if (!cancelled) setError('Không thể tải thông tin booking / báo cáo hư hại');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [id]);

  async function handleVNPay() {
    if (!booking) return;
    setPaying(true);
    setError(null);
    try {
      const res = await paymentApi.createVnpayUrl(booking.id, 'DAMAGE_FEE');
      if (res.success && res.data?.paymentUrl) {
        window.location.href = res.data.paymentUrl;
      } else {
        setError('Không thể tạo URL thanh toán VNPay');
      }
    } catch (err: unknown) {
      const msg =
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : undefined;
      setError(msg ?? 'Lỗi kết nối khi tạo thanh toán VNPay');
    } finally {
      setPaying(false);
    }
  }

  async function handleDispute(e: FormEvent) {
    e.preventDefault();
    if (!report) return;
    const note = disputeNote.trim();
    if (note.length < 10) {
      setError('Ghi chú tranh chấp phải có ít nhất 10 ký tự');
      return;
    }
    setDisputing(true);
    setError(null);
    setInfo(null);
    try {
      const res = await damageReportApi.dispute(report.id, note);
      if (res.success && res.data) {
        setReport(res.data);
        setInfo(res.message ?? 'Đã gửi tranh chấp — thanh toán tạm khóa đến khi Admin xử lý');
        setDisputeNote('');
      } else {
        setError(res.message ?? 'Không gửi được tranh chấp');
      }
    } catch (err: unknown) {
      const msg =
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : undefined;
      setError(msg ?? 'Lỗi khi gửi tranh chấp');
    } finally {
      setDisputing(false);
    }
  }

  if (loading) {
    return <CustomerLayout><div style={{ padding: 40, textAlign: 'center' }}>Đang tải thông tin...</div></CustomerLayout>;
  }
  if (error && !booking) {
    return <CustomerLayout><div className="alert alert-error">{error}</div></CustomerLayout>;
  }
  if (!booking) {
    return <CustomerLayout><div className="alert alert-error">Booking không tồn tại</div></CustomerLayout>;
  }

  const fee = Number(booking.damageFeeAmount ?? report?.approvedAmount ?? 0);
  const isDisputed = report?.status === 'DISPUTED';
  const canPay = booking.status === 'PENDING_DAMAGE_PAYMENT' && fee > 0 && !isDisputed;
  const canDispute = Boolean(report?.disputeEligible);

  return (
    <CustomerLayout>
      <div style={{ maxWidth: 620, margin: '0 auto' }}>
        <div className="flex items-center gap-2 body-sm text-charcoal" style={{ marginBottom: 20 }}>
          <Link to="/customer/bookings" className="text-primary" style={{ textDecoration: 'none' }}>Bookings</Link>
          <span>›</span>
          <Link to={`/customer/bookings/${booking.id}`} className="text-primary" style={{ textDecoration: 'none' }}>
            {booking.id.split('-')[0]}
          </Link>
          <span>›</span>
          <span style={{ fontWeight: 600 }}>Thanh toán phí hư hại</span>
        </div>
        <h1 className="heading-md" style={{ marginBottom: 4 }}>Thanh toán phí hư hại</h1>
        <p className="body-md text-charcoal" style={{ marginBottom: 24 }}>
          Thanh toán qua VNPay để hoàn tất trả phòng
        </p>

        <div className="card" style={{ padding: 20, marginBottom: 24 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <div>
              <p style={{ fontWeight: 700 }}>{booking.roomNumber}</p>
              <p className="body-sm text-charcoal">{booking.propertyName}</p>
              {report && (
                <p className="body-sm text-charcoal" style={{ marginTop: 8 }}>
                  Báo cáo: <strong>{report.status}</strong>
                  {report.disputeDeadline && report.status === 'APPROVED' && (
                    <> · Hạn tranh chấp: {formatDeadline(report.disputeDeadline)}</>
                  )}
                </p>
              )}
            </div>
            <div style={{ textAlign: 'right' }}>
              <p className="body-sm text-charcoal">Phí bồi thường</p>
              <p style={{ fontWeight: 700, color: 'var(--error)' }}>{formatVnd(fee)}</p>
            </div>
          </div>
        </div>

        <div className="card" style={{ padding: 24, marginBottom: 24 }}>
          {error && <div className="mb-4"><Alert variant="error" message={error} /></div>}
          {info && <div className="mb-4"><Alert variant="success" message={info} /></div>}
          {isDisputed && (
            <Alert
              variant="warning"
              message="Báo cáo đang tranh chấp — thanh toán tạm khóa đến khi Admin xử lý."
            />
          )}
          {!canPay && !isDisputed && (
            <Alert
              variant="warning"
              message="Booking không ở trạng thái chờ thanh toán phí hư hại hoặc số tiền không hợp lệ."
            />
          )}
          {canPay && (
            <>
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
                disabled={paying}
              >
                {paying ? 'Đang kết nối VNPay...' : `Thanh toán ${formatVnd(fee)} qua VNPay`}
              </button>
            </>
          )}
        </div>

        {canDispute && report && (
          <div className="card" style={{ padding: 24 }}>
            <h2 className="heading-sm" style={{ marginBottom: 8 }}>Tranh chấp phí hư hại</h2>
            <p className="body-sm text-charcoal" style={{ marginBottom: 16 }}>
              Bạn có 24 giờ kể từ lúc duyệt để phản đối. Hạn:{' '}
              <strong>{formatDeadline(report.disputeDeadline)}</strong>
            </p>
            <form onSubmit={handleDispute}>
              <label className="body-sm" htmlFor="dispute-note" style={{ display: 'block', marginBottom: 6 }}>
                Lý do tranh chấp (10–2000 ký tự)
              </label>
              <textarea
                id="dispute-note"
                className="input"
                rows={4}
                value={disputeNote}
                onChange={(e) => setDisputeNote(e.target.value)}
                maxLength={2000}
                required
                style={{ width: '100%', marginBottom: 12 }}
              />
              <button type="submit" className="btn-outline" disabled={disputing}>
                {disputing ? 'Đang gửi...' : 'Gửi tranh chấp'}
              </button>
            </form>
          </div>
        )}

        {report && !canDispute && report.status === 'APPROVED' && (
          <Alert
            variant="info"
            message="Đã hết thời hạn tranh chấp 24 giờ — vui lòng thanh toán phí hư hại."
          />
        )}
      </div>
    </CustomerLayout>
  );
}
