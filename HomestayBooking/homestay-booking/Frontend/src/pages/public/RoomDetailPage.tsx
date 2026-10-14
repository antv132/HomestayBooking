import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import PublicLayout from '../../layouts/PublicLayout';
import Alert from '../../components/ui/Alert';
import ImageGallerySlider from '../../components/ui/ImageGallerySlider';
import RoomMiniCalendar, { isRangeAvailable, isRoomBookingBlocked } from '../../components/ui/RoomMiniCalendar';
import {
  fetchRoomById,
  fetchRoomCalendar,
  fetchRoomReviews,
  type BookedRange,
  type RoomDetail,
  type RoomReviewInfo,
} from '../../api/roomsApi';
import { useAuthStore } from '../../store/authStore';
import { useCartStore } from '../../store/cartStore';
import { fixAmenityLabel } from '../../utils/amenities';
import type { DayStatus } from '../../utils/roomCalendar';

const AMENITY_ICONS: Record<string, string> = {
  WiFi: '📶',
  'Điều hòa': '❄️',
  'Hồ bơi riêng': '🏊',
  Bếp: '🍳',
  'Bếp nhỏ': '🍳',
  'View biển': '🌊',
  'Bãi đỗ xe': '🅿️',
  Minibar: '🥤',
  'Ban công': '🌅',
  'Smart TV': '📺',
  TV: '📺',
  'Room service': '🛎️',
  'Tủ lạnh': '🧊',
  'Bàn làm việc': '💼',
  'Máy giặt': '🧺',
  'Nước nóng': '🚿',
  'Tủ quần áo': '👔',
};

function StatusBadge({ status }: { status: string }) {
  const map: Record<string, { cls: string; label: string }> = {
    AVAILABLE: { cls: 'badge-success', label: 'Còn phòng' },
    PENDING_DEPOSIT: { cls: 'badge-warning', label: 'Chờ cọc' },
    RESERVED: { cls: 'badge-info', label: 'Đã đặt' },
    OCCUPIED: { cls: 'badge-neutral', label: 'Đang ở' },
    MAINTENANCE: { cls: 'badge-neutral', label: 'Bảo trì' },
  };
  const s = map[status] || { cls: 'badge-neutral', label: status };
  return <span className={`badge ${s.cls}`}>{s.label}</span>;
}

function StarRating({ rating, size = 14 }: { rating: number; size?: number }) {
  return (
    <div style={{ display: 'flex', gap: 2 }}>
      {[1, 2, 3, 4, 5].map((i) => (
        <svg key={i} width={size} height={size} viewBox="0 0 24 24" fill={i <= Math.round(rating) ? 'var(--primary)' : '#e5e7eb'}>
          <polygon points="12,2 15.09,8.26 22,9.27 17,14.14 18.18,21.02 12,17.77 5.82,21.02 7,14.14 2,9.27 8.91,8.26" />
        </svg>
      ))}
    </div>
  );
}

function ReviewCard({ review }: { review: RoomReviewInfo }) {
  const initial = review.customerName?.charAt(0)?.toUpperCase() ?? '?';
  return (
    <div className="card" style={{ padding: 16 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 8 }}>
        {review.customerName ? (
          <div
            style={{
              width: 36,
              height: 36,
              borderRadius: '50%',
              background: 'var(--primary)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: 14,
              fontWeight: 700,
              color: '#fff',
              flexShrink: 0,
            }}
          >
            {initial}
          </div>
        ) : null}
        <div style={{ flex: 1 }}>
          <p style={{ fontWeight: 600, fontSize: 14, color: 'var(--ink)' }}>{review.customerName}</p>
          <p className="body-sm text-charcoal">
            {new Date(review.createdAt).toLocaleDateString('vi-VN', { year: 'numeric', month: 'long', day: 'numeric' })}
          </p>
        </div>
        <StarRating rating={review.rating} />
      </div>
      <p className="body-md text-body">{review.comment}</p>
    </div>
  );
}

export default function RoomDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { isAuthenticated, role } = useAuthStore();

  const [room, setRoom] = useState<RoomDetail | null>(null);
  const [bookedRanges, setBookedRanges] = useState<BookedRange[]>([]);
  const [dayStatuses, setDayStatuses] = useState<Record<string, DayStatus> | undefined>();
  const [reviews, setReviews] = useState<RoomReviewInfo[]>([]);
  const [reviewPage, setReviewPage] = useState(0);
  const [reviewTotalPages, setReviewTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [reloadKey, setReloadKey] = useState(0);

  const [checkIn, setCheckIn] = useState(searchParams.get('checkIn') || '');
  const [checkOut, setCheckOut] = useState(searchParams.get('checkOut') || '');
  const [guests, setGuests] = useState(Number(searchParams.get('guests')) || 1);
  const [dateError, setDateError] = useState('');
  const [bookError, setBookError] = useState('');
  const [cartMessage, setCartMessage] = useState('');
  const addToCart = useCartStore((s) => s.addItem);

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    setError('');

    Promise.allSettled([fetchRoomById(id), fetchRoomCalendar(id), fetchRoomReviews(id, 0, 5)]).then(
      (results) => {
        const [roomResult, calendarResult, reviewsResult] = results;

        if (roomResult.status !== 'fulfilled') {
          const err = roomResult.reason as { response?: { status?: number } };
          const status = err?.response?.status;
          setError(
            status === 404
              ? 'Không tìm thấy phòng. Hãy chọn phòng từ danh sách (/rooms).'
              : 'Không tải được chi tiết phòng. Hãy restart backend (Run HomestayApplication) rồi bấm Thử lại.',
          );
          setRoom(null);
          return;
        }

        const roomData = roomResult.value;
        setRoom(roomData);
        setGuests(Math.min(Math.max(1, Number(searchParams.get('guests')) || 1), roomData.capacity));

        if (calendarResult.status === 'fulfilled') {
          setBookedRanges(calendarResult.value.bookedRanges);
        } else {
          setBookedRanges([]);
        }

        if (reviewsResult.status === 'fulfilled') {
          setReviews(reviewsResult.value.content);
          setReviewTotalPages(reviewsResult.value.totalPages);
          setReviewPage(0);
        } else {
          setReviews(roomData.reviews ?? []);
          setReviewTotalPages(0);
          setReviewPage(0);
        }
      },
    ).finally(() => setLoading(false));
  }, [id, reloadKey]);

  useEffect(() => {
    if (!room || !checkIn || !checkOut) {
      setDateError('');
      return;
    }
    if (new Date(checkOut) <= new Date(checkIn)) {
      setDateError('Ngày check-out phải sau ngày check-in.');
      return;
    }
    if (!isRangeAvailable(checkIn, checkOut, bookedRanges, room.status, dayStatuses)) {
      setDateError('Phòng không còn trống trong khoảng ngày đã chọn.');
      return;
    }
    setDateError('');
  }, [checkIn, checkOut, bookedRanges, room, dayStatuses]);

  useEffect(() => {
    setCartMessage('');
  }, [checkIn, checkOut, guests]);

  const nights =
    checkIn && checkOut && !dateError
      ? Math.max(0, Math.ceil((new Date(checkOut).getTime() - new Date(checkIn).getTime()) / 86400000))
      : 0;
  const price = room ? Number(room.pricePerNight) : 0;
  const totalAmount = nights * price;
  const depositAmount = Math.round(totalAmount * 0.4);

  function validateDates(): boolean {
    if (!checkIn || !checkOut) {
      setBookError('Vui lòng chọn ngày check-in và check-out.');
      return false;
    }
    if (dateError) {
      setBookError(dateError);
      return false;
    }
    setBookError('');
    return true;
  }

  function handleAddToCart() {
    if (!room || !validateDates()) return;
    const primary = room.images?.find((img) => img.isPrimary) ?? room.images?.[0];
    const replaced = addToCart({
      roomId: room.id,
      roomNumber: room.roomNumber,
      roomType: room.roomType,
      propertyName: room.propertyName,
      imageUrl: primary?.imageUrl ?? '',
      pricePerNight: Number(room.pricePerNight),
      capacity: room.capacity,
      checkIn,
      checkOut,
      guests,
    });
    setCartMessage(replaced ? 'Đã cập nhật ngày cho phòng này trong giỏ.' : 'Đã thêm phòng vào giỏ.');
  }

  function handleBookNow() {
    if (!room || !validateDates()) return;
    const qs = `checkIn=${checkIn}&checkOut=${checkOut}&guests=${guests}`;
    if (!isAuthenticated || role !== 'CUSTOMER') {
      navigate(`/login?redirect=${encodeURIComponent(`/request-booking/${room.id}?${qs}`)}`);
      return;
    }
    navigate(`/request-booking/${room.id}?${qs}`);
  }

  async function loadMoreReviews() {
    if (!id || reviewPage + 1 >= reviewTotalPages) return;
    const next = reviewPage + 1;
    const data = await fetchRoomReviews(id, next, 5);
    setReviews((prev) => [...prev, ...data.content]);
    setReviewPage(next);
  }

  if (loading) {
    return (
      <PublicLayout>
        <div className="container-wide" style={{ padding: '80px 0', textAlign: 'center' }}>
          <p className="body-lg text-charcoal">Đang tải thông tin phòng...</p>
        </div>
      </PublicLayout>
    );
  }

  if (error || !room) {
    return (
      <PublicLayout>
        <div className="container-wide" style={{ padding: '80px 0', maxWidth: 560, margin: '0 auto' }}>
          <Alert variant="error" message={error || 'Không tìm thấy phòng.'} />
          {id && (
            <p className="body-sm text-charcoal" style={{ marginTop: 16, textAlign: 'center' }}>
              Room ID: <code>{id}</code>
            </p>
          )}
          <div style={{ display: 'flex', gap: 12, justifyContent: 'center', flexWrap: 'wrap', marginTop: 20 }}>
            <button type="button" className="btn-primary" onClick={() => { setError(''); setReloadKey((k) => k + 1); }}>
              Thử lại
            </button>
            <Link to="/rooms" className="btn-outline">Quay lại danh sách phòng</Link>
          </div>
        </div>
      </PublicLayout>
    );
  }

  const galleryImages =
    room.images?.length > 0
      ? room.images
      : [{ id: 'fallback', imageUrl: '', sortOrder: 0, isPrimary: true }];

  const amenities = room.amenities?.length
    ? room.amenities.map((a) => fixAmenityLabel(a))
    : ['WiFi', 'Điều hòa', 'Smart TV', 'Nước nóng'];

  const canBook = !isRoomBookingBlocked(room.status);
  const displayReviews = reviews.length > 0 ? reviews : room.reviews ?? [];

  return (
    <PublicLayout>
      <div className="container-wide" style={{ paddingTop: 32, paddingBottom: 64 }}>
        <div className="flex items-center gap-2 body-sm text-charcoal" style={{ marginBottom: 24, flexWrap: 'wrap' }}>
          <Link to="/" className="text-primary" style={{ textDecoration: 'none' }}>Trang chủ</Link>
          <span>›</span>
          <Link to="/rooms" className="text-primary" style={{ textDecoration: 'none' }}>Phòng</Link>
          <span>›</span>
          <span className="text-ink" style={{ fontWeight: 600 }}>{room.propertyName}</span>
          <span>›</span>
          <span className="text-ink" style={{ fontWeight: 600 }}>{room.roomNumber}</span>
        </div>

        <div style={{ marginBottom: 36 }}>
          <ImageGallerySlider images={galleryImages} alt={room.roomNumber} />
        </div>

        <div className="room-detail-grid">
          <div>
            <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 12, marginBottom: 8 }}>
              <h1 className="display-md font-display">
                {room.roomNumber} — {room.roomType}
              </h1>
              <StatusBadge status={room.status} />
            </div>

            <p className="body-md text-charcoal" style={{ marginBottom: 4 }}>📍 {room.propertyAddress}</p>
            <p className="body-sm text-charcoal" style={{ marginBottom: 16 }}>
              {room.propertyName} · Tầng {room.floorNumber}
            </p>

            {(room.totalReviews > 0 || room.averageRating > 0) && (
              <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 20 }}>
                <span className="display-md text-primary font-display">{room.averageRating.toFixed(1)}</span>
                <div>
                  <StarRating rating={room.averageRating} size={16} />
                  <p className="body-sm text-charcoal">{room.totalReviews} đánh giá</p>
                </div>
              </div>
            )}

            <div
              style={{
                display: 'flex',
                gap: 20,
                flexWrap: 'wrap',
                padding: '16px 0',
                borderTop: '1px solid var(--hairline)',
                borderBottom: '1px solid var(--hairline)',
                marginBottom: 28,
              }}
            >
              {[
                { label: 'Sức chứa', value: `${room.capacity} khách` },
                { label: 'Diện tích', value: `${room.area} m²` },
                { label: 'Tầng', value: `Tầng ${room.floorNumber}` },
                { label: 'Loại phòng', value: room.roomType },
              ].map((stat) => (
                <div key={stat.label}>
                  <p className="body-sm text-charcoal">{stat.label}</p>
                  <p style={{ fontWeight: 600, fontSize: 15, color: 'var(--ink)', marginTop: 2 }}>{stat.value}</p>
                </div>
              ))}
            </div>

            <h2 className="heading-sm font-display" style={{ marginBottom: 10 }}>Mô tả phòng</h2>
            <p className="body-lg text-body" style={{ marginBottom: 32, lineHeight: 1.7 }}>
              {room.description || 'Phòng nghỉ tiện nghi, phù hợp cho chuyến du lịch hoặc công tác.'}
            </p>

            <h2 className="heading-sm font-display" style={{ marginBottom: 14 }}>Tiện ích</h2>
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fill, minmax(160px, 1fr))',
                gap: 12,
                marginBottom: 36,
              }}
            >
              {amenities.map((amenity) => (
                <div
                  key={amenity}
                  className="card"
                  style={{
                    padding: '14px 16px',
                    display: 'flex',
                    alignItems: 'center',
                    gap: 10,
                    border: '1px solid var(--hairline)',
                  }}
                >
                  <span style={{ fontSize: 20 }}>{AMENITY_ICONS[amenity] ?? '✓'}</span>
                  <span className="body-md" style={{ fontWeight: 500, color: 'var(--ink)' }}>{amenity}</span>
                </div>
              ))}
            </div>

            <h2 className="heading-sm font-display" style={{ marginBottom: 14 }}>Lịch trống</h2>
            <div style={{ marginBottom: 12 }}>
              <RoomMiniCalendar
                roomId={room.id}
                roomStatus={room.status}
                bookedRanges={bookedRanges}
                dayStatuses={dayStatuses}
                onDayStatusesLoaded={setDayStatuses}
              />
            </div>
            <Link to={`/rooms/${room.id}/calendar`} className="btn-ghost btn-sm" style={{ marginBottom: 36, display: 'inline-block' }}>
              Xem lịch chi tiết →
            </Link>

            <h2 className="heading-sm font-display" style={{ marginBottom: 16 }}>Đánh giá từ khách</h2>
            {displayReviews.length === 0 ? (
              <p className="body-md text-charcoal" style={{ marginBottom: 24 }}>
                Chưa có đánh giá nào cho phòng này.
              </p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 16, marginBottom: 16 }}>
                {displayReviews.map((r) => (
                  <ReviewCard key={r.id} review={r} />
                ))}
              </div>
            )}
            {reviewPage + 1 < reviewTotalPages && (
              <button type="button" className="btn-outline btn-sm" onClick={loadMoreReviews}>
                Xem thêm đánh giá
              </button>
            )}
          </div>

          {/* Booking panel — luôn hiển thị bên phải (desktop) / dưới nội dung (mobile) */}
          <div className="room-detail-booking">
            <div className="card-lg" style={{ padding: 28, boxShadow: '0 8px 24px rgba(32,32,32,0.08)' }}>
              <div style={{ display: 'flex', alignItems: 'baseline', gap: 6, marginBottom: 20 }}>
                <span className="display-md text-primary font-display">₫{price.toLocaleString('vi-VN')}</span>
                <span className="body-md text-charcoal">/đêm</span>
              </div>

              <div className="room-booking-dates" style={{ marginBottom: 12 }}>
                <div className="room-booking-date-field">
                  <label className="form-label" style={{ fontSize: 12 }}>Check-in</label>
                  <input
                    type="date"
                    className="input room-booking-date-input"
                    value={checkIn}
                    min={new Date().toISOString().slice(0, 10)}
                    onChange={(e) => { setCheckIn(e.target.value); setBookError(''); }}
                  />
                </div>
                <div className="room-booking-date-field">
                  <label className="form-label" style={{ fontSize: 12 }}>Check-out</label>
                  <input
                    type="date"
                    className="input room-booking-date-input"
                    value={checkOut}
                    min={checkIn || new Date().toISOString().slice(0, 10)}
                    onChange={(e) => { setCheckOut(e.target.value); setBookError(''); }}
                  />
                </div>
              </div>

              {dateError && (
                <div style={{ marginBottom: 12 }}>
                  <Alert variant="warning" message={dateError} />
                </div>
              )}

              {bookError && (
                <div style={{ marginBottom: 12 }}>
                  <Alert variant="warning" message={bookError} closeable onClose={() => setBookError('')} />
                </div>
              )}

              <div style={{ marginBottom: 20 }}>
                <label className="form-label" style={{ fontSize: 12 }}>Số khách</label>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <button
                    type="button"
                    className="btn-outline"
                    style={{ width: 40, height: 40, padding: 0, borderRadius: 10 }}
                    onClick={() => setGuests((g) => Math.max(1, g - 1))}
                    disabled={guests <= 1}
                  >
                    −
                  </button>
                  <input
                    type="number"
                    min={1}
                    max={room.capacity}
                    className="input"
                    value={guests}
                    readOnly
                    style={{ borderRadius: 10, height: 40, fontSize: 14, textAlign: 'center', flex: 1 }}
                  />
                  <button
                    type="button"
                    className="btn-outline"
                    style={{ width: 40, height: 40, padding: 0, borderRadius: 10 }}
                    onClick={() => setGuests((g) => Math.min(room.capacity, g + 1))}
                    disabled={guests >= room.capacity}
                  >
                    +
                  </button>
                </div>
                <p className="body-sm text-charcoal" style={{ marginTop: 6 }}>Tối đa {room.capacity} khách</p>
              </div>

              {nights > 0 && (
                <div style={{ background: 'var(--surface-bone)', borderRadius: 10, padding: 16, marginBottom: 20 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                    <span className="body-sm text-charcoal">₫{price.toLocaleString('vi-VN')} × {nights} đêm</span>
                    <span className="body-sm" style={{ fontWeight: 600 }}>₫{totalAmount.toLocaleString('vi-VN')}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', paddingTop: 8, borderTop: '1px solid var(--hairline)' }}>
                    <span className="body-sm" style={{ fontWeight: 600 }}>Tổng cộng</span>
                    <span style={{ fontWeight: 700, fontSize: 16 }}>₫{totalAmount.toLocaleString('vi-VN')}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 6 }}>
                    <span className="body-sm text-charcoal">Cọc yêu cầu (40%)</span>
                    <span className="body-sm text-primary" style={{ fontWeight: 600 }}>₫{depositAmount.toLocaleString('vi-VN')}</span>
                  </div>
                </div>
              )}

              {!canBook ? (
                <Alert variant="warning" message="Phòng hiện không khả dụng để đặt." />
              ) : !isAuthenticated || role !== 'CUSTOMER' ? (
                <>
                  <button type="button" className="btn-primary" style={{ width: '100%', height: 44 }} onClick={handleBookNow}>
                    Book Now — Đăng nhập
                  </button>
                  <p className="body-sm text-charcoal" style={{ textAlign: 'center', marginTop: 12 }}>
                    Bạn cần tài khoản khách hàng để hoàn tất đặt phòng
                  </p>
                </>
              ) : (
                <>
                  <button
                    type="button"
                    className="btn-primary"
                    style={{ width: '100%', height: 44 }}
                    onClick={handleBookNow}
                    disabled={!!dateError}
                  >
                    Book Now
                  </button>
                  <p className="body-sm text-charcoal" style={{ textAlign: 'center', marginTop: 12 }}>
                    Cọc 40% để xác nhận đặt phòng
                  </p>
                </>
              )}

              {canBook && (!isAuthenticated || role === 'CUSTOMER') && (
                <>
                  <button
                    type="button"
                    className="btn-outline"
                    style={{ width: '100%', height: 44, marginTop: 10, justifyContent: 'center' }}
                    onClick={handleAddToCart}
                    disabled={!!dateError}
                  >
                    Thêm vào giỏ
                  </button>
                  {cartMessage && (
                    <p className="body-sm" style={{ textAlign: 'center', marginTop: 10, color: 'var(--primary)', fontWeight: 600 }}>
                      {cartMessage}{' '}
                      <Link to="/cart" className="text-primary" style={{ textDecoration: 'underline' }}>Xem giỏ phòng →</Link>
                    </p>
                  )}
                </>
              )}
            </div>
          </div>
        </div>
      </div>
    </PublicLayout>
  );
}
