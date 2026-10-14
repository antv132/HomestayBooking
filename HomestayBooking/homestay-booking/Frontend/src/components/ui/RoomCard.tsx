import { useState } from 'react';
import { Link } from 'react-router-dom';
import SafeImage from './SafeImage';
import { resolveMediaUrl } from '../../utils/mediaUrl';

export interface Room {
  id: string;
  roomNumber: string;
  roomType: string;
  pricePerNight: number;
  capacity: number;
  area: number;
  status: string;
  propertyName: string;
  address: string;
  imageUrl: string;
  amenities?: string[];
}

interface RoomCardProps {
  room: Room;
  onSave?: (id: string) => void;
  saved?: boolean;
  layout?: 'grid' | 'list';
}

function formatPrice(price: number) {
  return '₫' + price.toLocaleString('vi-VN');
}

export default function RoomCard({ room, onSave, saved = false, layout = 'grid' }: RoomCardProps) {
  const [hovered, setHovered] = useState(false);
  const [isSaved, setIsSaved] = useState(saved);
  const imageSrc = resolveMediaUrl(room.imageUrl);

  function handleSave(e: React.MouseEvent) {
    e.preventDefault();
    e.stopPropagation();
    setIsSaved((prev) => !prev);
    onSave?.(room.id);
  }

  const statusLabel = room.status === 'AVAILABLE' ? 'Trống'
                    : room.status === 'OCCUPIED' ? 'Đã ở'
                    : room.status === 'RESERVED' ? 'Đã cọc'
                    : room.status === 'PENDING_DEPOSIT' ? 'Chờ cọc'
                    : 'Bảo trì';

  const statusClass = room.status === 'AVAILABLE' ? 'badge-success'
                    : room.status === 'OCCUPIED' ? 'badge-error'
                    : room.status === 'RESERVED' ? 'badge-warning'
                    : 'badge-neutral';

  if (layout === 'list') {
    return (
      <div
        className="card overflow-hidden flex flex-col sm:flex-row transition-all duration-200 h-full sm:h-auto group"
        style={{
          boxShadow: hovered ? '0 12px 32px rgba(14,116,144,0.1)' : '0 2px 8px rgba(15,23,42,0.06)',
          transform: hovered ? 'translateY(-2px)' : 'none',
        }}
        onMouseEnter={() => setHovered(true)}
        onMouseLeave={() => setHovered(false)}
      >
        <div className="relative w-full sm:w-64 flex-shrink-0" style={{ height: 200 }}>
          <Link to={`/rooms/${room.id}`}>
            <SafeImage
              src={imageSrc}
              alt={room.roomNumber}
              className="w-full h-full object-cover transition-transform duration-500"
              style={{ transform: hovered ? 'scale(1.05)' : 'scale(1)' }}
            />
          </Link>
          <div className="absolute top-3 left-3">
            <span className={`badge ${statusClass}`}>{statusLabel}</span>
          </div>
          <button
            className="absolute top-3 right-3 flex items-center justify-center rounded-full transition-all"
            style={{ width: 32, height: 32, background: 'rgba(255,255,255,0.9)', border: 'none', cursor: 'pointer', color: isSaved ? 'var(--error)' : 'var(--ash)' }}
            onClick={handleSave}
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill={isSaved ? 'currentColor' : 'none'} stroke="currentColor" strokeWidth="2">
              <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
            </svg>
          </button>
        </div>

        <div className="p-5 flex flex-col flex-1">
          <div className="flex justify-between items-start gap-4">
            <div>
              <p className="label-sm mb-1" style={{ color: 'var(--primary)', letterSpacing: '0.05em', textTransform: 'uppercase' }}>{room.propertyName}</p>
              <Link to={`/rooms/${room.id}`} style={{ textDecoration: 'none' }}>
                <h3 className="heading-sm group-hover:text-primary transition-colors" style={{ color: 'var(--ink)' }}>
                  Phòng {room.roomNumber} — {room.roomType}
                </h3>
              </Link>
              <p className="body-sm mt-1" style={{ color: 'var(--charcoal)' }}>📍 {room.address}</p>
            </div>
            <div className="text-right flex-shrink-0">
              <div className="font-bold text-primary" style={{ fontSize: 20 }}>{formatPrice(room.pricePerNight)}</div>
              <div className="caption text-ash">/đêm</div>
            </div>
          </div>

          <div className="flex flex-wrap gap-4 mt-4 text-sm text-charcoal">
            <span className="flex items-center gap-1.5"><span className="text-ash">👥</span> Tối đa {room.capacity} người</span>
            <span className="flex items-center gap-1.5"><span className="text-ash">📐</span> {room.area}m²</span>
          </div>

          <div className="flex items-center justify-between mt-auto pt-4 border-t border-hairline border-dashed">
            <div className="flex flex-wrap gap-2">
              {(room.amenities || []).slice(0, 3).map((a) => (
                <span key={a} className="badge badge-neutral bg-bone text-charcoal font-normal">{a}</span>
              ))}
              {(room.amenities || []).length > 3 && (
                <span className="badge badge-neutral bg-bone text-charcoal font-normal">+{(room.amenities || []).length - 3}</span>
              )}
            </div>
            <Link to={`/rooms/${room.id}`} className="btn-primary" style={{ height: 36, padding: '0 16px', fontSize: 13, borderRadius: 10 }}>
              Xem chi tiết
            </Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div
      className="card overflow-hidden flex flex-col transition-all duration-300 h-full group"
      style={{
        boxShadow: hovered ? '0 16px 40px rgba(14,116,144,0.12)' : '0 2px 10px rgba(15,23,42,0.06)',
        transform: hovered ? 'translateY(-4px)' : 'none',
      }}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
    >
      <div className="relative w-full" style={{ height: 220 }}>
        <Link to={`/rooms/${room.id}`}>
          <SafeImage
            src={imageSrc}
            alt={room.roomNumber}
            className="w-full h-full object-cover transition-transform duration-500"
            style={{ transform: hovered ? 'scale(1.06)' : 'scale(1)' }}
          />
        </Link>
        <div className="absolute top-3 left-3">
          <span className={`badge ${statusClass}`}>{statusLabel}</span>
        </div>
        <button
          className="absolute top-3 right-3 flex items-center justify-center rounded-full transition-all"
          style={{ width: 34, height: 34, background: 'rgba(255,255,255,0.95)', border: 'none', cursor: 'pointer', color: isSaved ? 'var(--error)' : 'var(--charcoal)' }}
          onClick={handleSave}
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill={isSaved ? 'currentColor' : 'none'} stroke="currentColor" strokeWidth="2">
            <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
          </svg>
        </button>
      </div>

      <div className="p-5 flex flex-col flex-1">
        <p className="label-xs mb-1.5" style={{ color: 'var(--primary)', letterSpacing: '0.06em', textTransform: 'uppercase' }}>
          {room.propertyName}
        </p>
        <Link to={`/rooms/${room.id}`} style={{ textDecoration: 'none' }}>
          <h3 className="heading-sm group-hover:text-primary transition-colors mb-1" style={{ color: 'var(--ink)' }}>
            Phòng {room.roomNumber} — {room.roomType}
          </h3>
        </Link>
        <p className="body-sm text-charcoal mb-4 line-clamp-1">📍 {room.address}</p>

        <div className="flex gap-4 text-sm text-charcoal mb-4">
          <span className="flex items-center gap-1"><span className="text-ash">👥</span> {room.capacity} khách</span>
          <span className="flex items-center gap-1"><span className="text-ash">📐</span> {room.area}m²</span>
        </div>

        <div className="mt-auto pt-4 border-t border-hairline border-dashed flex items-end justify-between gap-2 flex-wrap">
          <div style={{ minWidth: 0 }}>
            <span className="caption block text-ash mb-0.5">Chỉ từ</span>
            <span className="font-bold text-primary" style={{ fontSize: 20 }}>{formatPrice(room.pricePerNight)}</span>
            <span className="caption text-ash">/đêm</span>
          </div>
          <Link to={`/rooms/${room.id}`} className="btn-outline" style={{ height: 36, padding: '0 16px', fontSize: 13, borderRadius: 10, flexShrink: 0 }}>
            Xem
          </Link>
        </div>
      </div>
    </div>
  );
}
