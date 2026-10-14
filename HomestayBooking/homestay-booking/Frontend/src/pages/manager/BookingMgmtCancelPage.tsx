import { useState, useEffect } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import ManagerLayout from '../../layouts/ManagerLayout';
import {
  cancelManagerBookingV1,
  fetchManagerBookingV1,
  getManagerCancellationPreview,
  type CancellationPreview,
  type ManagerBookingDetail,
} from '../../api/bookingApi';

function formatVnd(value: number | undefined | null): string {
  if (value == null) return '—';
  return new Intl.NumberFormat('vi-VN').format(value) + ' ₫';
}

function formatDate(iso: string): string {
  if (!iso) return '—';
  const [y, m, d] = iso.split('T')[0].split('-');
  return `${d}/${m}/${y}`;
}

function shortBookingId(id: string): string {
  return `BK-${id.replace(/-/g, '').slice(0, 8).toUpperCase()}`;
}

function extractApiError(err: unknown, fallback: string): string {
  if (err && typeof err === 'object' && 'response' in err) {
    const data = (err as { response?: { data?: { message?: string } } }).response?.data;
    if (data?.message) return data.message;
  }
  if (err instanceof Error && err.message) return err.message;
  return fallback;
}

const CANCELLABLE = new Set(['PENDING_DEPOSIT', 'CONFIRMED']);

/** SCR-69 — Manager Booking Cancellation (100% refund). */
export default function BookingMgmtCancelPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [booking, setBooking] = useState<ManagerBookingDetail | null>(null);
  const [preview, setPreview] = useState<CancellationPreview | null>(null);
  const [reason, setReason] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [loadError, setLoadError] = useState('');
  const [previewError, setPreviewError] = useState('');
  const [submitError, setSubmitError] = useState('');

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    setLoadError('');
    setPreviewError('');
    Promise.all([
      fetchManagerBookingV1(id),
      getManagerCancellationPreview(id).catch((err) => {
        setPreviewError(extractApiError(err, 'Không tải được thông tin hoàn tiền.'));
        return null;
      }),
    ])
      .then(([detail, previewRes]) => {
        setBooking(detail);
        if (previewRes) setPreview(previewRes.data);
      })
      .catch((err) => setLoadError(extractApiError(err, 'Không tải được booking.')))
      .finally(() => setLoading(false));
  }, [id]);

  async function handleConfirm() {
    if (!id) return;
    const trimmed = reason.trim();
    if (!trimmed) {
      setSubmitError('Vui lòng nhập lý do hủy.');
      return;
    }
    setSubmitting(true);
    setSubmitError('');
    try {
      await cancelManagerBookingV1(id, trimmed);
      navigate(`/manager/bookings/${id}`, {
        state: { toast: 'Đã hủy booking — hoàn 100% số tiền đã thanh toán' },
      });
    } catch (err) {
      setSubmitError(extractApiError(err, 'Hủy booking thất bại.'));
      setSubmitting(false);
    }
  }

  return (
    <ManagerLayout>
      <div className="max-w-xl mx-auto">
        <div className="flex items-center gap-2 text-sm text-[#64748B] mb-5">
          <Link to="/manager/bookings" className="text-[#0F766E] no-underline">Đặt phòng</Link>
          <span>›</span>
          {id && (
            <>
              <Link to={`/manager/bookings/${id}`} className="text-[#0F766E] no-underline">
                {booking ? shortBookingId(booking.id) : 'Chi tiết'}
              </Link>
              <span>›</span>
            </>
          )}
          <span className="font-semibold text-[#1E293B]">Hủy đặt phòng</span>
        </div>

        {loading && (
          <div className="bg-white rounded-[16px] border border-[#E2E8F0] p-8 animate-pulse">
            <div className="h-8 bg-[#F1F5F9] rounded w-1/2 mb-4" />
            <div className="h-32 bg-[#F1F5F9] rounded" />
          </div>
        )}

        {!loading && loadError && (
          <div className="bg-white rounded-[16px] border border-red-200 p-6">
            <p className="text-red-600 mb-4">{loadError}</p>
            <Link to="/manager/bookings" className="btn-ghost">Quay lại danh sách</Link>
          </div>
        )}

        {!loading && booking && !CANCELLABLE.has(booking.status) && (
          <div className="bg-white rounded-[16px] border border-red-200 p-6">
            <p className="text-red-600 mb-4">Booking không thể hủy ở trạng thái hiện tại.</p>
            <Link to={`/manager/bookings/${id}`} className="btn-ghost">Quay lại chi tiết</Link>
          </div>
        )}

        {!loading && booking && CANCELLABLE.has(booking.status) && (
          <div className="bg-white rounded-[16px] border border-[#E2E8F0] shadow-sm overflow-hidden">
            <div className="border-t-4 border-red-500 p-8">
              <h1 className="text-xl font-semibold text-[#1E293B] mb-2">Hủy đặt phòng (Manager)</h1>
              <p className="text-sm text-[#64748B] mb-5">
                Hủy booking <strong>{shortBookingId(booking.id)}</strong> — phòng {booking.roomNumber} · {booking.customerName}
              </p>

              <div className="rounded-lg bg-[#ECFEFF] border border-[#A5F3FC] text-[#0E7490] text-sm p-3 mb-5">
                Manager-initiated cancel → khách được hoàn <strong>100%</strong> số tiền đã thanh toán.
              </div>

              <div className="rounded-lg bg-[#F8FAFC] p-4 mb-5 space-y-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-[#64748B]">Check-in / Check-out</span>
                  <span className="font-medium">{formatDate(booking.checkInDate)} → {formatDate(booking.checkOutDate)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-[#64748B]">Tổng tiền booking</span>
                  <span className="font-medium">{formatVnd(booking.totalAmount)}</span>
                </div>
                {preview && (
                  <>
                    <div className="flex justify-between">
                      <span className="text-[#64748B]">Hoàn dự kiến ({preview.refundPercent}%)</span>
                      <span className="font-semibold text-[#0F766E]">{formatVnd(preview.refundAmount)}</span>
                    </div>
                    {preview.forfeitAmount > 0 && (
                      <div className="flex justify-between">
                        <span className="text-[#64748B]">Khấu trừ</span>
                        <span className="font-medium">{formatVnd(preview.forfeitAmount)}</span>
                      </div>
                    )}
                  </>
                )}
              </div>

              {previewError && (
                <p className="text-sm text-red-600 mb-4">{previewError}</p>
              )}

              <label className="block text-sm font-medium text-[#1E293B] mb-1.5" htmlFor="cancel-reason">
                Lý do hủy <span className="text-red-500">*</span>
              </label>
              <textarea
                id="cancel-reason"
                className="input w-full min-h-[100px] mb-4"
                maxLength={500}
                placeholder="VD: Lỗi hệ thống / bất khả kháng / phòng không sử dụng được..."
                value={reason}
                onChange={(e) => setReason(e.target.value)}
              />
              <p className="text-xs text-[#94A3B8] mb-4">{reason.length}/500</p>

              {submitError && (
                <p className="text-sm text-red-600 mb-4">{submitError}</p>
              )}

              <div className="flex gap-3">
                <button
                  type="button"
                  className="btn-danger flex-1"
                  disabled={submitting || !!previewError || !reason.trim()}
                  onClick={handleConfirm}
                >
                  {submitting ? 'Đang hủy...' : 'Xác nhận hủy (hoàn 100%)'}
                </button>
                <Link to={`/manager/bookings/${id}`} className="btn-ghost flex-1 text-center no-underline">
                  Quay lại
                </Link>
              </div>
            </div>
          </div>
        )}
      </div>
    </ManagerLayout>
  );
}
