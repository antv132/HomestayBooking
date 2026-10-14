import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import EmployeeLayout from '../../layouts/EmployeeLayout';
import { getEmployeeRooms, type EmployeeRoom } from '../../api/employeeApi';
import { TOUCH, extractErr, Spinner, ErrBanner, StatusBadge } from './EmployeeShared';


export default function PropertyRoomListPage() {
  const [rooms, setRooms] = useState<EmployeeRoom[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  useEffect(() => {
    async function load() {
      setLoading(true);
      try {
        const res = await getEmployeeRooms({ size: 100 });
        if (res.success) setRooms(res.data.content);
        else setError('Không tải được danh sách phòng.');
      } catch (err) { setError(extractErr(err, 'Không tải được danh sách phòng.')); }
      finally { setLoading(false); }
    }
    load();
  }, []);

  const filtered = rooms.filter(r => {
    const matchSearch = !search || (r.name || r.roomNumber || '').toLowerCase().includes(search.toLowerCase());
    const matchStatus = !statusFilter || r.status === statusFilter;
    return matchSearch && matchStatus;
  });

  const STATUS_FILTERS = [
    { v: '', label: 'Tất cả' },
    { v: 'AVAILABLE', label: 'Available' },
    { v: 'OCCUPIED', label: 'Occupied' },
    { v: 'PENDING_CLEANING', label: 'Cần dọn' },
    { v: 'MAINTENANCE', label: 'Bảo trì' },
  ];

  const ROOM_STATUS_BG: Record<string, string> = {
    AVAILABLE: 'rgba(43,154,102,0.08)',
    CLEAN: 'rgba(43,154,102,0.08)',
    OCCUPIED: 'rgba(37,99,235,0.08)',
    PENDING_CLEANING: 'rgba(217,119,6,0.08)',
    MAINTENANCE: 'rgba(220,38,38,0.08)',
  };

  return (
    <EmployeeLayout>
      <div className="animate-fade-in">
        <div style={{ marginBottom: 16 }}>
          <h1 style={{ fontFamily: 'Outfit', fontSize: 22, fontWeight: 700, color: 'var(--ink)', marginBottom: 2 }}>🚪 Room List</h1>
          <p className="body-sm text-charcoal">SCR-65 — {rooms.length} phòng</p>
        </div>
        {error && <ErrBanner msg={error} />}

        {/* Search */}
        <div className="card" style={{ padding: '12px 16px', marginBottom: 12 }}>
          <input id="room-search" className="input" style={{ ...TOUCH }}
            placeholder="🔍 Tìm theo số phòng..."
            value={search} onChange={e => setSearch(e.target.value)} />
        </div>

        {/* Status filters */}
        <div style={{ display: 'flex', gap: 8, marginBottom: 14, overflowX: 'auto', paddingBottom: 4 }}>
          {STATUS_FILTERS.map(f => (
            <button key={f.v} onClick={() => setStatusFilter(f.v)} style={{
              ...TOUCH, padding: '0 14px', borderRadius: 20,
              border: `1.5px solid ${statusFilter === f.v ? 'var(--primary)' : 'var(--hairline)'}`,
              background: statusFilter === f.v ? 'rgba(15,118,110,0.10)' : 'var(--surface-card)',
              color: statusFilter === f.v ? 'var(--primary)' : 'var(--charcoal)',
              fontWeight: statusFilter === f.v ? 700 : 400, fontSize: 13, cursor: 'pointer',
              whiteSpace: 'nowrap', flexShrink: 0,
            }}>{f.label}</button>
          ))}
        </div>

        {loading ? <Spinner /> : filtered.length === 0 ? (
          <div className="card" style={{ padding: 40, textAlign: 'center' }}>
            <p style={{ fontSize: 32, marginBottom: 8 }}>🔍</p>
            <p style={{ fontWeight: 600, color: 'var(--ink)' }}>Không tìm thấy phòng</p>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {filtered.map(room => (
              <Link
                key={room.id}
                to={`/employee/rooms/${room.id}/calendar`}
                className="card"
                style={{
                  padding: '14px 16px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: 14,
                  background: ROOM_STATUS_BG[room.status] || 'var(--surface-card)',
                  textDecoration: 'none',
                  color: 'inherit',
                }}
              >
                <div style={{ width: 44, height: 44, borderRadius: 10, background: 'var(--surface-card)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 20, flexShrink: 0 }}>
                  🚪
                </div>
                <div style={{ flex: 1 }}>
                  <p style={{ fontWeight: 700, fontSize: 15, color: 'var(--ink)', marginBottom: 4 }}>
                    {room.name || room.roomNumber || room.id.slice(0, 8)}
                  </p>
                  {room.floorName && <p className="body-sm text-charcoal">{room.floorName}</p>}
                  <p className="body-sm" style={{ color: 'var(--primary)', marginTop: 4 }}>Xem lịch →</p>
                </div>
                <StatusBadge status={room.status} />
              </Link>
            ))}
          </div>
        )}
      </div>
    </EmployeeLayout>
  );
}
