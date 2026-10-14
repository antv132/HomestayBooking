import { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import CustomerLayout from '../../layouts/CustomerLayout';
import Alert from '../../components/ui/Alert';
import { bookingApi } from '../../api/bookingApi';
import { fetchRoomById, type RoomDetail } from '../../api/roomsApi';
import { isRoomBookingBlocked } from '../../utils/roomCalendar';
import { useCartStore } from '../../store/cartStore';

function formatVnd(amount: number) {
  return `${amount.toLocaleString('vi-VN')} ₫`;
}

function calcNights(checkIn: string, checkOut: string): number {
  if (!checkIn || !checkOut) return 0;
  const start = new Date(`${checkIn}T00:00:00`);
  const end = new Date(`${checkOut}T00:00:00`);
  const diff = Math.round((end.getTime() - start.getTime()) / 86400000);
  return diff > 0 ? diff : 0;
}

function roomImageUrl(room: RoomDetail): string {
  const primary = room.images?.find((img) => img.isPrimary);
  return primary?.imageUrl || room.images?.[0]?.imageUrl || '';
}

export default function BookingFormPage() {
  const navigate = useNavigate();
  const { roomId } = useParams();
  const [params] = useSearchParams();
  const [room, setRoom] = useState<RoomDetail | null>(null);
  const [roomLoading, setRoomLoading] = useState(true);
  const [roomError, setRoomError] = useState('');
  const [form, setForm] = useState({
    checkInDate: params.get('checkIn') || '',
    checkOutDate: params.get('checkOut') || '',
    guestCount: Number(params.get('guests')) || 1,
    specialRequests: '',
  });
  const [submitting, setSubmitting] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [apiError, setApiError] = useState('');

  const loadRoom = useCallback(async () => {
    if (!roomId) return;
    setRoomLoading(true);
    setRoomError('');
    try {
      const data = await fetchRoomById(roomId);
      setRoom(data);
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      setRoomError(axiosErr?.response?.data?.message ?? 'Không thể tải thông tin phòng. Vui lòng thử lại.');
      setRoom(null);
    } finally {
      setRoomLoading(false);
    }
  }, [roomId]);

  useEffect(() => {
    loadRoom();
  }, [loadRoom]);

  const nights = calcNights(form.checkInDate, form.checkOutDate);
  const pricePerNight = room?.pricePerNight ?? 0;
  const totalAmount = nights * pricePerNight;
  const depositAmount = totalAmount * 0.4;
  const canBook = !!room && !isRoomBookingBlocked(room.status);
  const imageUrl = room ? roomImageUrl(room) : '';

  function validate() {
    const e: Record<string, string> = {};
    if (!form.checkInDate) e.checkInDate = 'Vui lòng chọn ngày check-in';
    if (!form.checkOutDate) e.checkOutDate = 'Vui lòng chọn ngày check-out';
    if (form.checkInDate && form.checkOutDate && form.checkOutDate <= form.checkInDate) {
      e.checkOutDate = 'Ngày check-out phải sau ngày check-in';
    }
    if (form.guestCount < 1) e.guestCount = 'Cần ít nhất 1 khách';
    if (room && form.guestCount > room.capacity) {
      e.guestCount = `Tối đa ${room.capacity} khách`;
    }
    return e;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!roomId || !room) return;
    const errs = validate();
    if (Object.keys(errs).length) {
      setErrors(errs);
      return;
    }
    setErrors({});
    setApiError('');
    setSubmitting(true);
    try {
      const res = await bookingApi.createBooking({
        roomId,
        checkInDate: form.checkInDate,
        checkOutDate: form.checkOutDate,
        guestCount: form.guestCount,
        specialRequests: form.specialRequests || undefined,
      });
      useCartStore.getState().removeBooked(roomId, form.checkInDate, form.checkOutDate);
      // SC-001: after create → deposit VNPay path (PENDING_DEPOSIT)
      navigate(`/customer/payments/${res.data.id}/pay`);
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      setApiError(axiosErr?.response?.data?.message ?? 'Đặt phòng thất bại. Vui lòng thử lại.');
    } finally {
      setSubmitting(false);
    }
  }

  if (roomLoading) {
    return (
      <CustomerLayout>
        <div style={{ maxWidth: 960, margin: '0 auto' }}>
          <p className="body-sm text-charcoal">Đang tải thông tin phòng...</p>
        </div>
      </CustomerLayout>
    );
  }

  if (roomError || !room) {
    return (
      <CustomerLayout>
        <div style={{ maxWidth: 960, margin: '0 auto' }}>
          <Alert variant="error" message={roomError || 'Không tìm thấy phòng.'} />
          <button type="button" className="btn-primary" style={{ marginTop: 16 }} onClick={loadRoom}>
            Thử lại
          </button>
        </div>
      </CustomerLayout>
    );
  }

  return (
    <CustomerLayout>
      <div style={{ maxWidth: 960, margin: '0 auto' }}>
        <div className="flex items-center gap-2 body-sm text-charcoal" style={{ marginBottom: 20 }}>
          <Link to="/customer/bookings" className="text-primary" style={{ textDecoration: 'none' }}>
            Đặt phòng của tôi
          </Link>
          <span>›</span>
          <span style={{ fontWeight: 600 }}>Đặt phòng mới</span>
        </div>
        <h1 className="heading-md" style={{ marginBottom: 24 }}>Đặt phòng</h1>

        {!canBook && (
          <div style={{ marginBottom: 16 }}>
            <Alert variant="warning" message="Phòng hiện không khả dụng để đặt. Vui lòng chọn phòng khác." />
          </div>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-[1fr_320px] gap-6" style={{ alignItems: 'flex-start' }}>
          <form onSubmit={handleSubmit}>
            {apiError && (
              <div style={{ marginBottom: 16 }}>
                <Alert variant="error" message={apiError} closeable onClose={() => setApiError('')} />
              </div>
            )}

            <div className="card" style={{ padding: 24, marginBottom: 20 }}>
              <h2 className="heading-sm" style={{ marginBottom: 16 }}>Thông tin đặt phòng</h2>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, marginBottom: 16 }}>
                <div>
                  <label className="form-label form-label-required" htmlFor="checkInDate">Ngày check-in</label>
                  <input
                    id="checkInDate"
                    type="date"
                    className={`input ${errors.checkInDate ? 'input-error' : ''}`}
                    value={form.checkInDate}
                    onChange={(e) => setForm((p) => ({ ...p, checkInDate: e.target.value }))}
                  />
                  {errors.checkInDate && <p className="form-error">{errors.checkInDate}</p>}
                </div>
                <div>
                  <label className="form-label form-label-required" htmlFor="checkOutDate">Ngày check-out</label>
                  <input
                    id="checkOutDate"
                    type="date"
                    className={`input ${errors.checkOutDate ? 'input-error' : ''}`}
                    value={form.checkOutDate}
                    onChange={(e) => setForm((p) => ({ ...p, checkOutDate: e.target.value }))}
                  />
                  {errors.checkOutDate && <p className="form-error">{errors.checkOutDate}</p>}
                </div>
              </div>
              <div style={{ marginBottom: 16 }}>
                <label className="form-label form-label-required" htmlFor="guestCount">Số khách</label>
                <input
                  id="guestCount"
                  type="number"
                  min={1}
                  max={room.capacity}
                  className={`input ${errors.guestCount ? 'input-error' : ''}`}
                  value={form.guestCount}
                  onChange={(e) => setForm((p) => ({ ...p, guestCount: +e.target.value }))}
                />
                {errors.guestCount && <p className="form-error">{errors.guestCount}</p>}
                <p className="form-hint">Tối đa {room.capacity} khách</p>
              </div>
              <div>
                <label className="form-label" htmlFor="specialRequests">Yêu cầu đặc biệt (tùy chọn)</label>
                <textarea
                  id="specialRequests"
                  className="textarea"
                  rows={3}
                  placeholder="Ghi chú thêm cho chỗ nghỉ..."
                  value={form.specialRequests}
                  onChange={(e) => setForm((p) => ({ ...p, specialRequests: e.target.value }))}
                />
              </div>
            </div>

            <div style={{ marginBottom: 20 }}>
              <Alert
                variant="info"
                message="Sau khi xác nhận đặt phòng, bạn cần thanh toán cọc 40% để giữ chỗ. Hợp đồng sẽ được gửi qua email sau khi thanh toán thành công."
              />
            </div>

            <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
              <button
                type="submit"
                className="btn-primary"
                disabled={submitting || nights === 0 || !canBook}
              >
                {submitting ? 'Đang xử lý...' : 'Xác nhận đặt phòng'}
              </button>
              <Link to={`/rooms/${roomId}`} className="btn-ghost">Hủy</Link>
            </div>
          </form>

          <div className="card-lg lg:sticky lg:top-6" style={{ padding: 22 }}>
            {imageUrl ? (
              <img
                src={imageUrl}
                alt={room.roomNumber}
                style={{ width: '100%', height: 140, objectFit: 'cover', borderRadius: 10, marginBottom: 16 }}
              />
            ) : (
              <div
                style={{
                  width: '100%',
                  height: 140,
                  borderRadius: 10,
                  marginBottom: 16,
                  background: 'var(--surface-bone)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
                className="body-sm text-charcoal"
              >
                Không có ảnh
              </div>
            )}
            <p className="body-sm text-charcoal">{room.propertyName}</p>
            <h3 style={{ fontWeight: 700, fontSize: 16, margin: '4px 0 8px' }}>
              {room.roomNumber} — {room.roomType}
            </h3>
            <div className="flex gap-3 body-sm text-charcoal" style={{ marginBottom: 16 }}>
              <span>👥 Tối đa {room.capacity}</span>
              <span>📐 {room.area}m²</span>
            </div>
            <div className="divider" style={{ marginBottom: 14 }} />
            {nights > 0 ? (
              <>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                  <span className="body-sm text-charcoal">
                    {formatVnd(pricePerNight)} × {nights} đêm
                  </span>
                  <span style={{ fontWeight: 600 }}>{formatVnd(totalAmount)}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                  <span style={{ fontWeight: 700 }}>Tổng cộng</span>
                  <span style={{ fontWeight: 800, color: 'var(--ink)' }}>{formatVnd(totalAmount)}</span>
                </div>
                <div style={{ background: 'rgba(15,118,110,0.08)', borderRadius: 8, padding: '10px 12px', marginTop: 10 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span className="body-sm" style={{ color: 'var(--primary)', fontWeight: 600 }}>
                      Cọc yêu cầu (40%)
                    </span>
                    <span style={{ fontWeight: 800, color: 'var(--primary)' }}>{formatVnd(depositAmount)}</span>
                  </div>
                </div>
              </>
            ) : (
              <p className="body-sm text-charcoal" style={{ textAlign: 'center' }}>
                Chọn ngày để xem giá
              </p>
            )}
          </div>
        </div>
      </div>
    </CustomerLayout>
  );
}
