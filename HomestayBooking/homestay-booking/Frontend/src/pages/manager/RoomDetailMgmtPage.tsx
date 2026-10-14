import { useState, useEffect, useCallback } from 'react';
import { Link, useParams } from 'react-router-dom';
import ManagerLayout from '../../layouts/ManagerLayout';
import {
  fetchRoomById,
  fetchRoomBookings,
  RoomDetail,
  RoomBookingSummary,
} from '../../api/roomsApi';
import DataTable from '../../components/ui/DataTable';
import { fixAmenityLabel } from '../../utils/amenities';

// ── Constants ──────────────────────────────────────────────────────────────────

const ROOM_STATUS: Record<string, { label: string; color: string; bg: string }> = {
  AVAILABLE:            { label: 'Trống',           color: '#fff',    bg: '#2b9a66' },
  PENDING_DEPOSIT:      { label: 'Chờ cọc',         color: '#202020', bg: '#F59E0B' },
  RESERVED:             { label: 'Đã đặt',          color: '#fff',    bg: '#2563EB' },
  OCCUPIED:             { label: 'Đang ở',          color: '#fff',    bg: '#0F766E' },
  PENDING_CLEANING:     { label: 'Chờ dọn',         color: '#fff',    bg: '#D97706' },
  CLEANING_IN_PROGRESS: { label: 'Đang dọn',        color: '#fff',    bg: '#2563EB' },
  MAINTENANCE:          { label: 'Bảo trì',         color: '#fff',    bg: '#DC2626' },
  OUT_OF_SERVICE:       { label: 'Ngưng phục vụ',   color: '#fff',    bg: '#6B7280' },
};

const BOOKING_STATUS: Record<string, { label: string; color: string; bg: string }> = {
  PENDING_DEPOSIT:         { label: 'Chờ cọc',           color: '#202020', bg: '#FEF3C7' },
  CONFIRMED:               { label: 'Đã xác nhận',       color: '#fff',    bg: '#2b9a66' },
  CHECKED_IN:              { label: 'Đã check-in',       color: '#fff',    bg: '#2563EB' },
  PENDING_INSPECTION:      { label: 'Chờ kiểm tra',      color: '#202020', bg: '#FEF3C7' },
  PENDING_DAMAGE_PAYMENT:  { label: 'Chờ thanh toán hư hại', color: '#fff', bg: '#DC2626' },
  CHECKED_OUT:             { label: 'Đã check-out',      color: '#fff',    bg: '#7C3AED' },
  CANCELLED:               { label: 'Đã hủy',            color: '#fff',    bg: '#DC2626' },
  NO_SHOW:                 { label: 'Không đến',         color: '#fff',    bg: '#6B7280' },
};

const ACTIVE_BOOKING_STATUSES = new Set(['CONFIRMED', 'CHECKED_IN', 'PENDING_DEPOSIT']);

const accentBtnStyle: React.CSSProperties = {
  borderRadius: 9999,
  borderColor: 'var(--primary)',
  color: 'var(--primary)',
  background: 'rgba(15, 118, 110, 0.06)',
  flexShrink: 0,
};

function fmt(n: number) {
  return '₫' + n.toLocaleString('vi-VN');
}

function fmtDate(d: string) {
  if (!d) return '—';
  const [y, m, day] = d.split('-');
  return `${day}/${m}/${y}`;
}

// ── Sub-components ─────────────────────────────────────────────────────────────

function RoomStatusBadge({ status, large = false }: { status: string; large?: boolean }) {
  const s = ROOM_STATUS[status] || { label: status, color: '#fff', bg: '#6B7280' };
  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        background: s.bg,
        color: s.color,
        borderRadius: 9999,
        padding: large ? '8px 20px' : '4px 12px',
        fontSize: large ? 16 : 13,
        fontWeight: 600,
        letterSpacing: 0,
        lineHeight: 1,
        whiteSpace: 'nowrap',
      }}
    >
      {s.label}
    </span>
  );
}

function BookingStatusBadge({ status }: { status: string }) {
  const s = BOOKING_STATUS[status] || { label: status, color: '#fff', bg: '#6B7280' };
  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        background: s.bg,
        color: s.color,
        borderRadius: 9999,
        padding: '3px 10px',
        fontSize: 12,
        fontWeight: 600,
        whiteSpace: 'nowrap',
      }}
    >
      {s.label}
    </span>
  );
}

function InfoRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div
      style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'flex-start',
        padding: '10px 0',
        borderBottom: '1px solid var(--hairline)',
        gap: 12,
      }}
    >
      <span className="body-sm" style={{ color: 'var(--charcoal)', flexShrink: 0 }}>{label}</span>
      <span style={{ fontWeight: 600, fontSize: 14, textAlign: 'right' }}>{value}</span>
    </div>
  );
}

function CardBox({ children, style }: { children: React.ReactNode; style?: React.CSSProperties }) {
  return (
    <div className="card" style={{ padding: 24, ...style }}>
      {children}
    </div>
  );
}

function SectionTitle({ children }: { children: React.ReactNode }) {
  return (
    <h2 className="heading-sm" style={{ marginBottom: 16 }}>{children}</h2>
  );
}

// ── Loading skeleton ───────────────────────────────────────────────────────────

function PageSkeleton() {
  const pulse = {
    background: 'var(--surface-bone)',
    borderRadius: 8,
    animation: 'pulse 1.4s ease-in-out infinite',
  } as React.CSSProperties;

  return (
    <div style={{ display: 'grid', gridTemplateColumns: '1fr 280px', gap: 20 }}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        <div className="card" style={{ padding: 24 }}>
          <div style={{ ...pulse, height: 20, width: '40%', marginBottom: 16 }} />
          {[...Array(6)].map((_, i) => (
            <div key={i} style={{ display: 'flex', justifyContent: 'space-between', padding: '10px 0', borderBottom: '1px solid var(--hairline)' }}>
              <div style={{ ...pulse, height: 14, width: '30%' }} />
              <div style={{ ...pulse, height: 14, width: '25%' }} />
            </div>
          ))}
        </div>
        <div className="card" style={{ padding: 24 }}>
          <div style={{ ...pulse, height: 20, width: '30%', marginBottom: 16 }} />
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 8 }}>
            {[...Array(6)].map((_, i) => (
              <div key={i} style={{ ...pulse, height: 120, borderRadius: 10 }} />
            ))}
          </div>
        </div>
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        {[...Array(3)].map((_, i) => (
          <div key={i} className="card" style={{ padding: 24 }}>
            <div style={{ ...pulse, height: 16, width: '60%', marginBottom: 12 }} />
            <div style={{ ...pulse, height: 36, borderRadius: 9999 }} />
          </div>
        ))}
      </div>
    </div>
  );
}

// ── Gallery Card ───────────────────────────────────────────────────────────────

function GalleryCard({ room }: { room: RoomDetail }) {
  const images = (room.images ?? []).slice().sort((a, b) => a.sortOrder - b.sortOrder);

  return (
    <CardBox>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16, gap: 12 }}>
        <h2 className="heading-sm" style={{ margin: 0 }}>Thư viện ảnh</h2>
        <Link
          to={`/manager/rooms/${room.id}/edit?tab=gallery`}
          className="btn-outline btn-sm"
          style={accentBtnStyle}
        >
          Quản lý ảnh →
        </Link>
      </div>

      {images.length === 0 ? (
        <div
          style={{
            background: 'var(--surface-bone)',
            borderRadius: 10,
            padding: '32px 20px',
            textAlign: 'center',
          }}
        >
          <p className="body-sm" style={{ color: 'var(--charcoal)', marginBottom: 8 }}>
            Chưa có ảnh nào
          </p>
          <Link to={`/manager/rooms/${room.id}/edit?tab=gallery`} className="btn-outline btn-sm">
            + Tải ảnh lên
          </Link>
        </div>
      ) : (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(3, 1fr)',
            gap: 8,
          }}
        >
          {images.slice(0, 6).map(img => (
            <div
              key={img.id}
              style={{ position: 'relative', borderRadius: 10, overflow: 'hidden', aspectRatio: '1/1' }}
            >
              <img
                src={img.imageUrl}
                alt="Phòng"
                style={{ width: '100%', height: '100%', objectFit: 'cover', display: 'block' }}
                onError={e => {
                  (e.target as HTMLImageElement).src =
                    'https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=400&h=400&fit=crop';
                }}
              />
              {img.isPrimary && (
                <span
                  style={{
                    position: 'absolute',
                    top: 6,
                    left: 6,
                    background: 'var(--primary)',
                    color: '#fff',
                    fontSize: 10,
                    fontWeight: 700,
                    padding: '2px 8px',
                    borderRadius: 9999,
                    letterSpacing: 0.5,
                  }}
                >
                  Đại diện
                </span>
              )}
            </div>
          ))}
        </div>
      )}

      {images.length > 6 && (
        <p className="body-sm" style={{ color: 'var(--charcoal)', marginTop: 10, textAlign: 'right' }}>
          +{images.length - 6} ảnh nữa ·{' '}
          <Link to={`/manager/rooms/${room.id}/edit?tab=gallery`} style={{ color: 'var(--primary)', textDecoration: 'none' }}>
            Quản lý tất cả
          </Link>
        </p>
      )}
    </CardBox>
  );
}

// ── Active Booking Banner ──────────────────────────────────────────────────────

function ActiveBookingBanner({ booking }: { booking: RoomBookingSummary }) {
  return (
    <div
      style={{
        background: '#FEF3C7',
        border: '1px solid #F59E0B',
        borderRadius: 10,
        padding: '14px 18px',
        display: 'flex',
        alignItems: 'flex-start',
        gap: 12,
        marginBottom: 16,
      }}
    >
      <span style={{ fontSize: 20, flexShrink: 0 }}>⚡</span>
      <div>
        <p style={{ fontWeight: 700, color: '#92400E', fontSize: 14, marginBottom: 2 }}>
          Đang có booking — {booking.customerName}
        </p>
        <p className="body-sm" style={{ color: '#92400E' }}>
          {fmtDate(booking.checkInDate)} → {fmtDate(booking.checkOutDate)} ·{' '}
          <BookingStatusBadge status={booking.status} />
        </p>
        <Link
          to={`/manager/bookings/${booking.id}`}
          style={{ color: '#92400E', fontSize: 13, fontWeight: 600, textDecoration: 'underline', marginTop: 4, display: 'inline-block' }}
        >
          Xem booking →
        </Link>
      </div>
    </div>
  );
}

// ── Booking History Table ──────────────────────────────────────────────────────

interface BookingHistoryProps {
  roomId: string;
  bookings: RoomBookingSummary[];
  total: number;
  page: number;
  loading: boolean;
  onPageChange: (p: number) => void;
}

function BookingHistoryCard({ roomId: _roomId, bookings, total, page, loading, onPageChange }: BookingHistoryProps) {
  const totalPages = Math.ceil(total / 5);

  const activeBooking = bookings.find(b => ACTIVE_BOOKING_STATUSES.has(b.status));

  const columns = [
    { header: 'Khách', accessor: (b: RoomBookingSummary) => (
      <div>
        <p style={{ fontWeight: 600, fontSize: 14, margin: 0 }}>{b.customerName}</p>
        <p className="body-sm" style={{ color: 'var(--charcoal)', margin: 0 }}>{b.customerEmail}</p>
      </div>
    )},
    { header: 'Check-in', accessor: (b: RoomBookingSummary) => <span className="body-sm">{fmtDate(b.checkInDate)}</span> },
    { header: 'Check-out', accessor: (b: RoomBookingSummary) => <span className="body-sm">{fmtDate(b.checkOutDate)}</span> },
    { header: 'Số tiền', accessor: (b: RoomBookingSummary) => <div style={{ textAlign: 'right', fontWeight: 700 }}>{fmt(b.totalAmount)}</div> },
    { header: 'Trạng thái', accessor: (b: RoomBookingSummary) => <BookingStatusBadge status={b.status} /> },
    { header: '', accessor: (b: RoomBookingSummary) => <Link to={`/manager/bookings/${b.id}`} className="btn-ghost btn-sm">Xem</Link> },
  ];

  return (
    <CardBox>
      <SectionTitle>Lịch sử đặt phòng</SectionTitle>

      {activeBooking && <ActiveBookingBanner booking={activeBooking} />}

      {loading ? (
        <div style={{ padding: '32px 0', textAlign: 'center', color: 'var(--charcoal)' }}>
          <div className="spinner" style={{ margin: '0 auto 8px' }} />
          Đang tải booking…
        </div>
      ) : bookings.length === 0 ? (
        <div
          style={{
            background: 'var(--surface-bone)',
            borderRadius: 10,
            padding: '32px 20px',
            textAlign: 'center',
          }}
        >
          <p className="body-sm" style={{ color: 'var(--charcoal)' }}>
            Chưa có booking nào cho phòng này
          </p>
        </div>
      ) : (
        <>
          <DataTable
            columns={columns}
            data={bookings}
            keyExtractor={(b) => b.id}
          />

          {totalPages > 1 && (
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                marginTop: 16,
              }}
            >
              <p className="body-sm" style={{ color: 'var(--charcoal)' }}>
                Tổng {total} booking
              </p>
              <div style={{ display: 'flex', gap: 6 }}>
                <button
                  className="btn-ghost btn-sm"
                  disabled={page === 0}
                  onClick={() => onPageChange(page - 1)}
                >
                  ← Trước
                </button>
                <span style={{ display: 'flex', alignItems: 'center', fontSize: 13, color: 'var(--charcoal)' }}>
                  {page + 1} / {totalPages}
                </span>
                <button
                  className="btn-ghost btn-sm"
                  disabled={page >= totalPages - 1}
                  onClick={() => onPageChange(page + 1)}
                >
                  Sau →
                </button>
              </div>
            </div>
          )}
        </>
      )}
    </CardBox>
  );
}

// ── Main Page ──────────────────────────────────────────────────────────────────

export default function RoomDetailMgmtPage() {
  const { id } = useParams<{ id: string }>();

  const [room, setRoom]       = useState<RoomDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState('');

  const [bookings, setBookings]             = useState<RoomBookingSummary[]>([]);
  const [bookingsTotal, setBookingsTotal]   = useState(0);
  const [bookingPage, setBookingPage]       = useState(0);
  const [bookingsLoading, setBookingsLoading] = useState(false);

  const loadRoom = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    setError('');
    try {
      const data = await fetchRoomById(id);
      setRoom(data);
    } catch (err: unknown) {
      const axiosErr = err as { response?: { status?: number; data?: { message?: string } } };
      const status = axiosErr?.response?.status;
      if (status === 404) {
        setError('Không tìm thấy phòng. Phòng có thể đã bị xóa.');
      } else {
        setError(axiosErr?.response?.data?.message ?? 'Không tải được thông tin phòng. Vui lòng thử lại.');
      }
    } finally {
      setLoading(false);
    }
  }, [id]);

  const loadBookings = useCallback(async (page: number) => {
    if (!id) return;
    setBookingsLoading(true);
    try {
      const data = await fetchRoomBookings(id, page, 5);
      setBookings(data.content ?? []);
      setBookingsTotal(data.totalElements ?? 0);
    } catch {
      setBookings([]);
    } finally {
      setBookingsLoading(false);
    }
  }, [id]);

  useEffect(() => { loadRoom(); }, [loadRoom]);

  useEffect(() => {
    if (room) loadBookings(bookingPage);
  }, [room, bookingPage, loadBookings]);

  if (loading) {
    return (
      <ManagerLayout>
        <div style={{ height: 20, width: 200, background: 'var(--surface-bone)', borderRadius: 6, marginBottom: 24, animation: 'pulse 1.4s ease-in-out infinite' }} />
        <PageSkeleton />
      </ManagerLayout>
    );
  }

  if (error || !room) {
    return (
      <ManagerLayout>
        <div className="flex items-center gap-2 body-sm text-charcoal" style={{ marginBottom: 20 }}>
          <Link to="/manager/rooms" className="text-primary" style={{ textDecoration: 'none' }}>Danh sách phòng</Link>
          <span>›</span>
          <span>Chi tiết phòng</span>
        </div>
        <div
          className="card"
          style={{
            padding: 48,
            textAlign: 'center',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: 12,
          }}
        >
          <span style={{ fontSize: 48 }}>⚠️</span>
          <h2 className="heading-sm">{error || 'Không tìm thấy phòng'}</h2>
          <div style={{ display: 'flex', gap: 10, marginTop: 8 }}>
            <button className="btn-primary" onClick={loadRoom}>Thử lại</button>
            <Link to="/manager/rooms" className="btn-outline">Quay lại danh sách</Link>
          </div>
        </div>
      </ManagerLayout>
    );
  }

  const primaryImage = room.images?.find(img => img.isPrimary) ?? room.images?.[0];
  const isOccupied = ['OCCUPIED', 'CHECKED_IN', 'RESERVED', 'PENDING_DEPOSIT'].includes(room.status);

  return (
    <ManagerLayout>

      {/* Breadcrumb */}
      <div className="flex items-center gap-2 body-sm" style={{ marginBottom: 20, color: 'var(--charcoal)' }}>
        <Link to="/manager/rooms" className="text-primary" style={{ textDecoration: 'none' }}>Danh sách phòng</Link>
        <span style={{ color: 'var(--stone)' }}>›</span>
        {room.propertyName && (
          <>
            <span style={{ color: 'var(--charcoal)' }}>{room.propertyName}</span>
            <span style={{ color: 'var(--stone)' }}>›</span>
          </>
        )}
        <span style={{ fontWeight: 600, color: 'var(--ink)' }}>{room.roomNumber}</span>
      </div>

      {/* Header */}
      <div
        style={{
          display: 'flex',
          alignItems: 'flex-start',
          justifyContent: 'space-between',
          marginBottom: 24,
          gap: 16,
          flexWrap: 'wrap',
        }}
      >
        <div>
          <h1 className="heading-md" style={{ marginBottom: 6 }}>
            Phòng {room.roomNumber}
            <span style={{ color: 'var(--charcoal)', fontWeight: 400, marginLeft: 8 }}>— {room.roomType}</span>
          </h1>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
            <RoomStatusBadge status={room.status} />
            {room.averageRating > 0 && (
              <span className="body-sm" style={{ color: 'var(--charcoal)' }}>
                ⭐ {room.averageRating.toFixed(1)} ({room.totalReviews} đánh giá)
              </span>
            )}
          </div>
        </div>

        <Link
          to="/manager/rooms"
          className="btn-outline btn-sm shrink-0"
          style={accentBtnStyle}
        >
          ← Quay lại
        </Link>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 280px', gap: 20, alignItems: 'start' }}>

        {/* Cột trái */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>

          {primaryImage && (
            <div style={{ borderRadius: 14, overflow: 'hidden', height: 280, background: 'var(--surface-bone)' }}>
              <img
                src={primaryImage.imageUrl}
                alt={room.roomNumber}
                style={{ width: '100%', height: '100%', objectFit: 'cover', display: 'block' }}
                onError={e => {
                  (e.target as HTMLImageElement).src =
                    'https://images.unsplash.com/photo-1571896349842-33c89424de2d?w=800&h=400&fit=crop';
                }}
              />
            </div>
          )}

          <CardBox>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12, marginBottom: 16 }}>
              <h2 className="heading-sm" style={{ margin: 0 }}>Thông tin phòng</h2>
              <Link
                to={`/manager/rooms/${room.id}/edit`}
                className="btn-outline btn-sm"
                style={accentBtnStyle}
              >
                Sửa phòng
              </Link>
            </div>
            <InfoRow label="Homestay" value={room.propertyName} />
            <InfoRow label="Địa chỉ" value={room.propertyAddress || '—'} />
            <InfoRow label="Tầng" value={`Tầng ${room.floorNumber}`} />
            <InfoRow label="Loại phòng" value={room.roomType} />
            <InfoRow label="Sức chứa" value={`${room.capacity} khách`} />
            <InfoRow label="Diện tích" value={`${room.area} m²`} />
            <InfoRow label="Giá / đêm" value={
              <span style={{ color: 'var(--primary)', fontWeight: 700 }}>
                {fmt(room.pricePerNight)}
              </span>
            } />

            {room.description && (
              <div style={{ marginTop: 16 }}>
                <p className="body-sm" style={{ color: 'var(--charcoal)', marginBottom: 6 }}>Mô tả</p>
                <p className="body-md" style={{ color: 'var(--body)', lineHeight: 1.6 }}>{room.description}</p>
              </div>
            )}

            {room.amenities && room.amenities.length > 0 && (
              <div style={{ marginTop: 16 }}>
                <p className="body-sm" style={{ color: 'var(--charcoal)', marginBottom: 8 }}>Tiện nghi</p>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                  {room.amenities.map(a => {
                    const label = fixAmenityLabel(a);
                    return (
                      <span
                        key={a}
                        style={{
                          background: 'var(--surface-bone)',
                          borderRadius: 9999,
                          padding: '3px 10px',
                          fontSize: 12,
                          color: 'var(--charcoal)',
                          fontWeight: 500,
                        }}
                      >
                        {label}
                      </span>
                    );
                  })}
                </div>
              </div>
            )}
          </CardBox>

          <GalleryCard room={room} />

          <BookingHistoryCard
            roomId={room.id}
            bookings={bookings}
            total={bookingsTotal}
            page={bookingPage}
            loading={bookingsLoading}
            onPageChange={p => setBookingPage(p)}
          />
        </div>

        {/* Cột phải */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 14, position: 'sticky', top: 24 }}>

          <CardBox>
            <p style={{ color: 'var(--ink)', marginBottom: 12, fontWeight: 700, fontSize: 14 }}>
              Trạng thái hiện tại
            </p>
            <div style={{ marginBottom: 16 }}>
              <RoomStatusBadge status={room.status} large />
            </div>

            {isOccupied && (
              <div
                style={{
                  background: '#FEF3C7',
                  border: '1px solid #F59E0B',
                  borderRadius: 8,
                  padding: '8px 12px',
                  marginBottom: 14,
                }}
              >
                <p className="body-sm" style={{ color: '#92400E', margin: 0 }}>
                  ⚠️ Phòng đang được sử dụng
                </p>
              </div>
            )}

            <Link
              to={`/manager/rooms/${room.id}/edit?tab=status`}
              className="btn-outline btn-sm"
              style={{
                ...accentBtnStyle,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                width: '100%',
                textAlign: 'center',
              }}
            >
              Cập nhật trạng thái
            </Link>
            <Link
              to={`/manager/rooms/${room.id}/calendar`}
              className="btn-ghost btn-sm"
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                width: '100%',
                textAlign: 'center',
                marginTop: 8,
              }}
            >
              Xem lịch tháng
            </Link>
          </CardBox>

          <CardBox>
            <p className="body-sm" style={{ color: 'var(--charcoal)', marginBottom: 14 }}>Thống kê</p>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span className="body-sm" style={{ color: 'var(--charcoal)' }}>Tổng booking</span>
                <span style={{ fontWeight: 700 }}>{bookingsTotal}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span className="body-sm" style={{ color: 'var(--charcoal)' }}>Đánh giá</span>
                <span style={{ fontWeight: 700 }}>{room.totalReviews ?? 0}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span className="body-sm" style={{ color: 'var(--charcoal)' }}>Điểm TB</span>
                <span style={{ fontWeight: 700 }}>
                  {room.averageRating > 0 ? `⭐ ${room.averageRating.toFixed(1)}` : '—'}
                </span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span className="body-sm" style={{ color: 'var(--charcoal)' }}>Số ảnh</span>
                <span style={{ fontWeight: 700 }}>{room.images?.length ?? 0}</span>
              </div>
            </div>
          </CardBox>

          <div style={{ padding: '8px 0' }}>
            <p className="body-sm" style={{ color: 'var(--stone)' }}>
              Mã phòng: <span style={{ fontFamily: 'monospace', fontSize: 11 }}>{room.id.slice(0, 8)}…</span>
            </p>
            {room.createdAt && (
              <p className="body-sm" style={{ color: 'var(--stone)', marginTop: 4 }}>
                Tạo ngày: {new Date(room.createdAt).toLocaleDateString('vi-VN')}
              </p>
            )}
          </div>
        </div>
      </div>
    </ManagerLayout>
  );
}
