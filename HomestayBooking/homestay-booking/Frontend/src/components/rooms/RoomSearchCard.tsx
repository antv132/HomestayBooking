import { Link } from 'react-router-dom';
import SafeImage from '../ui/SafeImage';
import type { RoomListItem } from '../../api/roomsApi';

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

function StarRating({ rating }: { rating: number }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 3 }}>
      {[1, 2, 3, 4, 5].map((i) => (
        <svg key={i} width="11" height="11" viewBox="0 0 24 24" fill={i <= Math.round(rating) ? '#ea2804' : '#e5e7eb'}>
          <polygon points="12,2 15.09,8.26 22,9.27 17,14.14 18.18,21.02 12,17.77 5.82,21.02 7,14.14 2,9.27 8.91,8.26" />
        </svg>
      ))}
      <span className="body-sm text-charcoal" style={{ marginLeft: 2 }}>
        {rating.toFixed(1)}
      </span>
    </div>
  );
}

interface RoomSearchCardProps {
  room: RoomListItem;
  querySuffix?: string;
}

export default function RoomSearchCard({ room, querySuffix = '' }: RoomSearchCardProps) {
  const rating = room.averageRating ?? 0;
  const reviews = room.totalReviews ?? 0;

  return (
    <Link
      to={`/rooms/${room.id}${querySuffix}`}
      className="card"
      style={{
        overflow: 'hidden',
        textDecoration: 'none',
        color: 'inherit',
        display: 'block',
        transition: 'transform 0.2s, box-shadow 0.2s',
      }}
      onMouseEnter={(e) => {
        (e.currentTarget as HTMLElement).style.transform = 'translateY(-3px)';
        (e.currentTarget as HTMLElement).style.boxShadow = '0 8px 24px rgba(32,32,32,0.12)';
      }}
      onMouseLeave={(e) => {
        (e.currentTarget as HTMLElement).style.transform = '';
        (e.currentTarget as HTMLElement).style.boxShadow = '';
      }}
    >
      <div style={{ position: 'relative', paddingBottom: '100%', overflow: 'hidden' }}>
        <SafeImage
          src={room.primaryImageUrl}
          alt={room.roomNumber}
          style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'cover' }}
        />
        <div style={{ position: 'absolute', top: 8, left: 8 }}>
          <StatusBadge status={room.status} />
        </div>
      </div>
      <div style={{ padding: 16 }}>
        <p className="body-sm text-charcoal" style={{ marginBottom: 3 }}>
          {room.propertyName}
          {room.floorNumber != null && ` · Tầng ${room.floorNumber}`}
        </p>
        <h3 className="heading-sm font-display" style={{ marginBottom: 6, fontSize: 17 }}>
          {room.roomNumber} — {room.roomType}
        </h3>
        <div className="flex gap-3 body-sm text-charcoal" style={{ marginBottom: 8 }}>
          <span>👥 {room.capacity}</span>
          <span>📐 {room.area}m²</span>
        </div>
        {rating > 0 && (
          <div className="flex items-center gap-2" style={{ marginBottom: 12 }}>
            <StarRating rating={rating} />
            <span className="body-sm text-charcoal">({reviews})</span>
          </div>
        )}
        <div className="flex items-center justify-between" style={{ marginTop: 12, gap: 8, flexWrap: 'wrap' }}>
          <div style={{ minWidth: 0 }}>
            <span className="heading-sm text-primary">₫{Number(room.pricePerNight).toLocaleString('vi-VN')}</span>
            <span className="body-sm text-charcoal">/đêm</span>
          </div>
          <span
            className="btn-outline btn-sm"
            style={{ borderRadius: 9999, pointerEvents: 'none', flexShrink: 0, whiteSpace: 'nowrap' }}
          >
            Xem chi tiết
          </span>
        </div>
      </div>
    </Link>
  );
}

export function RoomCardSkeleton() {
  return <div className="card" style={{ height: 380, background: 'var(--surface-bone)', opacity: 0.65 }} />;
}
