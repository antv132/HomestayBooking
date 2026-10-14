import { useState, useEffect, useCallback, useRef } from 'react';
import { Link, useNavigate, useParams, useLocation } from 'react-router-dom';
import ManagerLayout from '../../layouts/ManagerLayout';
import Alert from '../../components/ui/Alert';
import Checkbox from '../../components/ui/Checkbox';
import { StatusBadge } from '../../components/ui';
import type { StatusVariant } from '../../components/ui/StatusBadge';
import {
  fetchManagerBookingV1,
  checkInBookingV1,
  checkOutBookingV1,
  uploadBookingIdDocumentsV1,
  type ManagerBookingDetail,
} from '../../api/bookingApi';

const STATUS_VI: Record<string, { label: string; variant: StatusVariant }> = {
  PENDING_DEPOSIT:        { label: 'Chờ cọc',                  variant: 'warning' },
  CONFIRMED:              { label: 'Đã xác nhận',              variant: 'success' },
  CHECKED_IN:             { label: 'Đã nhận phòng',            variant: 'primary' },
  PENDING_INSPECTION:     { label: 'Chờ kiểm tra',             variant: 'warning' },
  PENDING_DAMAGE_PAYMENT: { label: 'Chờ thanh toán thiệt hại', variant: 'danger' },
  CHECKED_OUT:            { label: 'Đã trả phòng',             variant: 'neutral' },
  CANCELLED:              { label: 'Đã hủy',                   variant: 'danger' },
  NO_SHOW:                { label: 'Không đến',                variant: 'danger' },
};

function formatVnd(value: number | undefined): string {
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

function isRemainingUnpaid(booking: ManagerBookingDetail): boolean {
  if (!booking.remainingAmount || booking.remainingAmount <= 0) return false;
  return !(booking.payments ?? []).some(
    p => p.type === 'REMAINING_BALANCE' && p.status === 'PAID',
  );
}

function PageSkeleton() {
  return (
    <div className="space-y-4 animate-pulse">
      <div className="h-8 bg-[#E2E8F0] rounded w-1/3" />
      <div className="h-40 bg-[#E2E8F0] rounded-[16px]" />
      <div className="h-56 bg-[#E2E8F0] rounded-[16px]" />
    </div>
  );
}

export default function BookingCheckInOutPage() {
  const { id } = useParams<{ id: string }>();
  const location = useLocation();
  const navigate = useNavigate();
  const isCheckIn = location.pathname.endsWith('/check-in');

  const fileInputRef = useRef<HTMLInputElement>(null);

  const [booking, setBooking] = useState<ManagerBookingDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [uploading, setUploading] = useState(false);

  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const [previewUrls, setPreviewUrls] = useState<string[]>([]);
  const [uploadedUrls, setUploadedUrls] = useState<string[]>([]);

  const [keyHandedOver, setKeyHandedOver] = useState(false);
  const [remainingCollected, setRemainingCollected] = useState(false);
  const [keyReturned, setKeyReturned] = useState(false);
  const [note, setNote] = useState('');

  const loadBooking = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    setLoadError(null);
    try {
      const data = await fetchManagerBookingV1(id);
      setBooking(data);
    } catch (err: unknown) {
      const ax = err as { response?: { status?: number; data?: { message?: string } } };
      if (ax?.response?.status === 403) {
        setLoadError('Bạn không có quyền thao tác đặt phòng này.');
      } else {
        setLoadError(ax?.response?.data?.message ?? 'Không thể tải chi tiết đặt phòng.');
      }
      setBooking(null);
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => { loadBooking(); }, [loadBooking]);

  useEffect(() => {
    const urls = selectedFiles.map(f => URL.createObjectURL(f));
    setPreviewUrls(urls);
    return () => urls.forEach(u => URL.revokeObjectURL(u));
  }, [selectedFiles]);

  function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const files = Array.from(e.target.files ?? []);
    if (files.length === 0) return;
    const combined = [...selectedFiles, ...files].slice(0, 3);
    setSelectedFiles(combined);
    setUploadedUrls([]);
    e.target.value = '';
  }

  function removeFile(index: number) {
    setSelectedFiles(prev => prev.filter((_, i) => i !== index));
    setUploadedUrls([]);
  }

  async function handleCheckInSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!id || !booking) return;

    setSubmitError(null);
    if (selectedFiles.length === 0) {
      setSubmitError('Vui lòng tải ít nhất một ảnh CMND/CCCD.');
      return;
    }
    if (!keyHandedOver) {
      setSubmitError('Vui lòng xác nhận đã giao chìa khóa cho khách.');
      return;
    }
    if (isRemainingUnpaid(booking) && !remainingCollected) {
      setSubmitError('Vui lòng xác nhận đã thu phần còn lại.');
      return;
    }

    setSubmitting(true);
    try {
      let docUrls = uploadedUrls;
      if (docUrls.length === 0) {
        setUploading(true);
        docUrls = await uploadBookingIdDocumentsV1(id, selectedFiles);
        setUploadedUrls(docUrls);
        setUploading(false);
      }

      await checkInBookingV1(id, {
        idDocumentUrls: docUrls,
        keyHandedOver: true,
        remainingCollected: isRemainingUnpaid(booking) ? true : undefined,
        note: note.trim() || undefined,
      });

      navigate(`/manager/bookings/${id}`, {
        state: { toast: 'Nhận phòng thành công' },
      });
    } catch (err: unknown) {
      const ax = err as { response?: { data?: { message?: string } } };
      setSubmitError(ax?.response?.data?.message ?? 'Không thể hoàn tất nhận phòng.');
    } finally {
      setSubmitting(false);
      setUploading(false);
    }
  }

  async function handleCheckOutSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!id || !booking) return;

    setSubmitError(null);
    if (!keyReturned) {
      setSubmitError('Vui lòng xác nhận đã thu lại chìa khóa.');
      return;
    }

    setSubmitting(true);
    try {
      const result = await checkOutBookingV1(id, {
        keyReturned: true,
        note: note.trim() || undefined,
      });

      const message = result.status === 'CHECKED_OUT'
        ? 'Trả phòng thành công'
        : 'Đã chuyển sang chờ kiểm tra phòng';

      navigate(`/manager/bookings/${id}`, { state: { toast: message } });
    } catch (err: unknown) {
      const ax = err as { response?: { data?: { message?: string } } };
      setSubmitError(ax?.response?.data?.message ?? 'Không thể hoàn tất trả phòng.');
    } finally {
      setSubmitting(false);
    }
  }

  const statusCfg = booking
    ? STATUS_VI[booking.status] ?? { label: booking.status, variant: 'neutral' as StatusVariant }
    : null;

  const title = isCheckIn ? 'Xác nhận nhận phòng' : 'Xác nhận trả phòng';
  const breadcrumbAction = isCheckIn ? 'Nhận phòng' : 'Trả phòng';
  const showRemainingCheckbox = booking && isCheckIn && isRemainingUnpaid(booking);

  const checkInBlocked = booking && isCheckIn && !booking.canCheckIn;
  const checkOutBlocked = booking && !isCheckIn && !booking.canCheckOut;

  if (loadError) {
    return (
      <ManagerLayout>
        <div className="card p-10 text-center max-w-md mx-auto">
          <h2 className="heading-sm mb-2">Không tải được đặt phòng</h2>
          <p className="body-md text-charcoal mb-6">{loadError}</p>
          <div className="flex gap-3 justify-center">
            <button type="button" className="btn-primary" onClick={loadBooking}>Thử lại</button>
            <Link to="/manager/bookings" className="btn-ghost">Quay lại danh sách</Link>
          </div>
        </div>
      </ManagerLayout>
    );
  }

  return (
    <ManagerLayout>
      <div className="space-y-6 max-w-3xl">
        <div className="flex items-center gap-2 text-sm text-[#64748B]">
          <Link to="/manager/bookings" className="text-[#0F766E] no-underline">Đặt phòng</Link>
          <span>›</span>
          {booking && (
            <>
              <Link to={`/manager/bookings/${id}`} className="text-[#0F766E] no-underline">
                {shortBookingId(booking.id)}
              </Link>
              <span>›</span>
            </>
          )}
          <span className="font-semibold text-[#1E293B]">{breadcrumbAction}</span>
        </div>

        <h1 className="font-display text-[28px] font-bold text-[#1E293B]">{title}</h1>

        {submitError && (
          <Alert variant="error" message={submitError} closeable onClose={() => setSubmitError(null)} />
        )}

        {loading ? (
          <PageSkeleton />
        ) : booking && (
          <>
            <div className="bg-white rounded-[16px] border border-[#E2E8F0] p-6">
              <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
                <h2 className="font-semibold text-[#1E293B]">Thông tin đặt phòng</h2>
                {statusCfg && <StatusBadge status={statusCfg.label} variant={statusCfg.variant} />}
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
                <div>
                  <p className="text-xs text-[#64748B]">Khách</p>
                  <p className="font-semibold text-[#1E293B] mt-0.5">{booking.customerName}</p>
                </div>
                <div>
                  <p className="text-xs text-[#64748B]">Phòng</p>
                  <p className="font-semibold text-[#1E293B] mt-0.5">{booking.roomNumber} · {booking.propertyName}</p>
                </div>
                <div>
                  <p className="text-xs text-[#64748B]">Nhận phòng</p>
                  <p className="font-semibold text-[#1E293B] mt-0.5">{formatDate(booking.checkInDate)}</p>
                </div>
                <div>
                  <p className="text-xs text-[#64748B]">Trả phòng</p>
                  <p className="font-semibold text-[#1E293B] mt-0.5">{formatDate(booking.checkOutDate)}</p>
                </div>
                <div>
                  <p className="text-xs text-[#64748B]">Số khách</p>
                  <p className="font-semibold text-[#1E293B] mt-0.5">{booking.guestCount}</p>
                </div>
              </div>
            </div>

            {isCheckIn ? (
              <form onSubmit={handleCheckInSubmit} className="space-y-4">
                {checkInBlocked && (
                  <Alert variant="warning" message="Booking không ở trạng thái cho phép nhận phòng." />
                )}

                <div className="bg-white rounded-[16px] border border-[#E2E8F0] p-6">
                  <h2 className="font-semibold text-[#1E293B] mb-4">Thanh toán</h2>
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-2">
                    <div>
                      <p className="text-xs text-[#64748B]">Tổng tiền</p>
                      <p className="font-bold text-[#1E293B]">{formatVnd(booking.totalAmount)}</p>
                    </div>
                    <div>
                      <p className="text-xs text-[#64748B]">Cọc (40%)</p>
                      <p className="font-semibold text-[#1E293B]">{formatVnd(booking.depositAmount)}</p>
                    </div>
                    <div className={showRemainingCheckbox ? 'p-3 bg-amber-50 rounded-lg border border-amber-200' : ''}>
                      <p className="text-xs text-[#64748B]">Còn lại (60%)</p>
                      <p className={`font-semibold ${showRemainingCheckbox ? 'text-amber-800' : 'text-[#1E293B]'}`}>
                        {formatVnd(booking.remainingAmount)}
                      </p>
                    </div>
                  </div>
                </div>

                <div className="bg-white rounded-[16px] border border-[#E2E8F0] p-6">
                  <h2 className="font-semibold text-[#1E293B] mb-2">Ảnh CMND / CCCD</h2>
                  <p className="text-xs text-[#64748B] mb-4">Tải 1–3 ảnh (JPG/PNG, tối đa 5MB mỗi ảnh)</p>

                  <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/jpeg,image/png"
                    multiple
                    className="hidden"
                    onChange={handleFileChange}
                  />

                  <button
                    type="button"
                    className="btn-outline btn-sm mb-4"
                    disabled={selectedFiles.length >= 3 || submitting}
                    onClick={() => fileInputRef.current?.click()}
                  >
                    Chọn ảnh
                  </button>

                  {previewUrls.length > 0 && (
                    <div className="flex flex-wrap gap-3">
                      {previewUrls.map((url, i) => (
                        <div key={url} className="relative w-28 h-28 rounded-lg overflow-hidden border border-[#E2E8F0]">
                          <img src={url} alt={`CMND ${i + 1}`} className="w-full h-full object-cover" />
                          <button
                            type="button"
                            className="absolute top-1 right-1 w-6 h-6 rounded-full bg-black/60 text-white text-xs"
                            onClick={() => removeFile(i)}
                            disabled={submitting}
                          >
                            ×
                          </button>
                        </div>
                      ))}
                    </div>
                  )}

                  {uploadedUrls.length > 0 && (
                    <p className="text-xs text-[#0F766E] mt-2">Đã tải {uploadedUrls.length} ảnh lên hệ thống</p>
                  )}
                </div>

                <div className="bg-white rounded-[16px] border border-[#E2E8F0] p-6 space-y-4">
                  <Checkbox
                    label="Đã giao chìa khóa cho khách"
                    checked={keyHandedOver}
                    onChange={e => setKeyHandedOver(e.target.checked)}
                    disabled={checkInBlocked || submitting}
                  />
                  {showRemainingCheckbox && (
                    <Checkbox
                      label="Đã thu phần còn lại tại quầy (tiền mặt / tại chỗ)"
                      checked={remainingCollected}
                      onChange={e => setRemainingCollected(e.target.checked)}
                      disabled={checkInBlocked || submitting}
                    />
                  )}
                  <div>
                    <label className="form-label" htmlFor="checkin-note">Ghi chú (tuỳ chọn)</label>
                    <textarea
                      id="checkin-note"
                      className="input min-h-[80px]"
                      maxLength={500}
                      value={note}
                      onChange={e => setNote(e.target.value)}
                      disabled={checkInBlocked || submitting}
                    />
                  </div>
                </div>

                <div className="flex gap-3">
                  <button
                    type="submit"
                    className="btn-primary"
                    disabled={checkInBlocked || submitting || uploading}
                  >
                    {uploading ? 'Đang tải ảnh…' : submitting ? 'Đang xử lý…' : 'Hoàn tất nhận phòng'}
                  </button>
                  <Link to={`/manager/bookings/${id}`} className="btn-ghost no-underline">Hủy</Link>
                </div>
              </form>
            ) : (
              <form onSubmit={handleCheckOutSubmit} className="space-y-4">
                {checkOutBlocked && booking.checkOutBlockedReason && (
                  <Alert variant="warning" message={booking.checkOutBlockedReason} />
                )}

                {!isCheckIn && booking.status === 'PENDING_INSPECTION' && booking.canCheckOut && (
                  <Alert variant="info" message="Kiểm tra phòng đã hoàn tất. Xác nhận thu chìa khóa để hoàn tất trả phòng." />
                )}

                <div className="bg-white rounded-[16px] border border-[#E2E8F0] p-6 space-y-4">
                  <Checkbox
                    label="Đã thu lại chìa khóa"
                    checked={keyReturned}
                    onChange={e => setKeyReturned(e.target.checked)}
                    disabled={checkOutBlocked || submitting}
                  />
                  <div>
                    <label className="form-label" htmlFor="checkout-note">Ghi chú (tuỳ chọn)</label>
                    <textarea
                      id="checkout-note"
                      className="input min-h-[80px]"
                      maxLength={500}
                      value={note}
                      onChange={e => setNote(e.target.value)}
                      disabled={checkOutBlocked || submitting}
                    />
                  </div>
                </div>

                <div className="flex gap-3">
                  <button
                    type="submit"
                    className="btn-primary"
                    disabled={checkOutBlocked || submitting}
                  >
                    {submitting ? 'Đang xử lý…' : 'Hoàn tất trả phòng'}
                  </button>
                  <Link to={`/manager/bookings/${id}`} className="btn-ghost no-underline">Hủy</Link>
                </div>
              </form>
            )}
          </>
        )}
      </div>
    </ManagerLayout>
  );
}
