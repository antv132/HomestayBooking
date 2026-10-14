// PropertyDetailPage.tsx — Manager "My Property" (FR-06, read-only)
// Màn duy nhất: overview property được gán. Nếu Manager có nhiều property,
// hiển thị Select để chuyển; nếu chỉ 1 thì hiển thị thẳng.
import { useState, useEffect } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import ManagerLayout from '../../layouts/ManagerLayout';
import { propertyApi, PropertyDetail, PropertySummary } from '../../api/propertyApi';
import { StatusBadge, formatDate } from './_propertyShared';

function OccupancyBar({ label, count, total, color }: {
  label: string; count: number; total: number; color: string;
}) {
  const pct = total > 0 ? Math.round((count / total) * 100) : 0;
  return (
    <div style={{ marginBottom: 12 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
        <span className="body-sm" style={{ color: 'var(--charcoal)' }}>{label}</span>
        <span className="body-sm" style={{ fontWeight: 600 }}>
          {count} <span style={{ color: 'var(--ash)' }}>({pct}%)</span>
        </span>
      </div>
      <div style={{ height: 6, background: 'var(--surface-bone)', borderRadius: 999 }}>
        <div
          style={{
            height: '100%',
            borderRadius: 999,
            width: `${pct}%`,
            background: color,
            transition: 'width 0.6s ease',
          }}
        />
      </div>
    </div>
  );
}

export default function PropertyDetailPage() {
  const { id: routeId } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [assigned, setAssigned] = useState<PropertySummary[]>([]);
  const [assignedLoading, setAssignedLoading] = useState(true);
  const [property, setProperty] = useState<PropertyDetail | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Load danh sách property được gán (dùng cho selector + chọn mặc định)
  useEffect(() => {
    setAssignedLoading(true);
    propertyApi.getAll({ page: 0, size: 100 })
      .then((res) => { if (res.success) setAssigned(res.data.content); })
      .catch(() => setError('Could not load your assigned properties. Please try again.'))
      .finally(() => setAssignedLoading(false));
  }, []);

  // Property đang chọn: ưu tiên :id trên URL, fallback property đầu tiên
  const selectedId = routeId || assigned[0]?.id || null;

  useEffect(() => {
    if (!selectedId) return;
    setDetailLoading(true);
    setError(null);
    propertyApi.getDetail(selectedId)
      .then((res) => { if (res.success) setProperty(res.data); })
      .catch(() => setError('Could not load property. Please try again.'))
      .finally(() => setDetailLoading(false));
  }, [selectedId]);

  function handleSelect(nextId: string) {
    navigate(`/manager/properties/${nextId}`);
  }

  const loading = assignedLoading || (detailLoading && !property);

  if (loading) {
    return (
      <ManagerLayout>
        <div style={{ padding: '60px 0', textAlign: 'center', color: 'var(--ash)' }}>
          <p>Loading property…</p>
        </div>
      </ManagerLayout>
    );
  }

  if (!assignedLoading && assigned.length === 0) {
    return (
      <ManagerLayout>
        <div className="card-lg" style={{ padding: '48px 32px', textAlign: 'center', maxWidth: 520, margin: '40px auto' }}>
          <div
            style={{
              width: 56, height: 56, borderRadius: 16, margin: '0 auto 16px',
              background: 'rgba(15,118,110,0.10)', color: 'var(--primary)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}
          >
            <svg width="26" height="26" fill="none" stroke="currentColor" strokeWidth="1.75" viewBox="0 0 24 24">
              <path d="M3 21h18M5 21V7l7-4 7 4v14M9 21v-6h6v6" />
            </svg>
          </div>
          <h2 className="heading-sm" style={{ marginBottom: 8 }}>No property assigned</h2>
          <p className="body-sm text-charcoal">
            Ask an Admin to assign you to a property. Your property overview will appear here.
          </p>
        </div>
      </ManagerLayout>
    );
  }

  if (error || !property) {
    return (
      <ManagerLayout>
        <div className="alert alert-error" style={{ maxWidth: 480 }}>
          {error || 'Property not found.'}
        </div>
      </ManagerLayout>
    );
  }

  const { stats, floors } = property;
  const occupancyPct = stats.totalRooms > 0
    ? Math.round(((stats.occupiedRooms + stats.reservedRooms) / stats.totalRooms) * 100)
    : 0;

  const quickLinks = [
    { to: `/manager/structure?propertyId=${property.id}`, label: 'Structure tree', id: 'btn-quick-structure', primary: true },
    { to: `/manager/rooms?propertyId=${property.id}`, label: 'All rooms', id: 'btn-quick-rooms' },
    { to: `/manager/floors?propertyId=${property.id}`, label: 'Floors', id: 'btn-quick-floors' },
    { to: `/manager/bookings?propertyId=${property.id}`, label: 'Bookings', id: 'btn-quick-bookings' },
  ];

  return (
    <ManagerLayout>
      {/* Page header + property selector */}
      <div
        style={{
          display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between',
          gap: 16, flexWrap: 'wrap', marginBottom: 20,
        }}
      >
        <div>
          <h1 className="heading-md" style={{ marginBottom: 4 }}>My Property</h1>
          <p className="body-sm text-charcoal">
            {assigned.length > 1
              ? `You manage ${assigned.length} properties — pick one to view.`
              : 'Overview of your assigned property (read-only).'}
          </p>
        </div>
        {assigned.length > 1 && (
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <label htmlFor="property-selector" className="body-sm text-charcoal" style={{ whiteSpace: 'nowrap' }}>
              Property
            </label>
            <select
              id="property-selector"
              className="select"
              value={property.id}
              onChange={(e) => handleSelect(e.target.value)}
              style={{ minWidth: 220 }}
            >
              {assigned.map((p) => (
                <option key={p.id} value={p.id}>{p.name}</option>
              ))}
            </select>
          </div>
        )}
      </div>

      {/* Hero overview */}
      <div
        className="card-lg animate-fade-up"
        style={{
          padding: 28,
          marginBottom: 20,
          background: 'linear-gradient(135deg, rgba(15,118,110,0.06) 0%, #fff 55%)',
          border: '1px solid rgba(15,118,110,0.12)',
          opacity: detailLoading ? 0.6 : 1,
          transition: 'opacity 0.2s ease',
        }}
      >
        <div
          style={{
            display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between',
            gap: 16, flexWrap: 'wrap', marginBottom: 20,
          }}
        >
          <div style={{ minWidth: 0, flex: '1 1 240px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap', marginBottom: 8 }}>
              <h2 className="heading-md" style={{ margin: 0 }}>{property.name}</h2>
              <StatusBadge status={property.status} />
            </div>
            <p className="body-sm text-charcoal" style={{ lineHeight: 1.5 }}>{property.address}</p>
            {property.description && (
              <p className="body-md" style={{ marginTop: 12, color: 'var(--body)', lineHeight: 1.65, maxWidth: 720 }}>
                {property.description}
              </p>
            )}
            <p className="body-sm text-charcoal" style={{ marginTop: 12 }}>
              Created {formatDate(property.createdAt)} · Updated {formatDate(property.updatedAt)}
            </p>
          </div>
          <div style={{ display: 'flex', gap: 8, flexShrink: 0, flexWrap: 'wrap' }}>
            <Link
              to={`/manager/structure?propertyId=${property.id}`}
              className="btn-primary btn-sm"
              id="btn-view-structure"
            >
              Structure tree
            </Link>
            <Link
              to={`/manager/rooms?propertyId=${property.id}`}
              className="btn-outline btn-sm"
              id="btn-view-rooms"
            >
              Rooms
            </Link>
          </div>
        </div>

        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(120px, 1fr))',
            gap: 12,
          }}
        >
          {[
            { label: 'Floors', value: stats.totalFloors },
            { label: 'Rooms', value: stats.totalRooms },
            { label: 'Available', value: stats.availableRooms, color: 'var(--success)' },
            { label: 'Occupied', value: stats.occupiedRooms, color: 'var(--primary)' },
            { label: 'Occupancy', value: `${occupancyPct}%`, color: 'var(--primary)' },
          ].map((item) => (
            <div
              key={item.label}
              style={{
                padding: '14px 16px',
                borderRadius: 12,
                background: 'rgba(255,255,255,0.85)',
                border: '1px solid var(--border)',
              }}
            >
              <p className="body-sm text-charcoal" style={{ marginBottom: 4 }}>{item.label}</p>
              <p style={{ fontSize: 22, fontWeight: 700, color: item.color || 'var(--ink)', lineHeight: 1.2 }}>
                {item.value}
              </p>
            </div>
          ))}
        </div>
      </div>

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'minmax(0, 1fr) minmax(260px, 320px)',
          gap: 20,
          alignItems: 'flex-start',
        }}
        className="manager-property-detail-grid"
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: 20, minWidth: 0 }}>
          <div className="card-lg" style={{ padding: 28 }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 20, gap: 12, flexWrap: 'wrap' }}>
              <h2 className="heading-sm" style={{ margin: 0 }}>Floor & room summary</h2>
              <Link
                to={`/manager/structure?propertyId=${property.id}`}
                className="btn-ghost btn-sm"
                id="btn-manage-floors"
              >
                Manage floors →
              </Link>
            </div>

            {floors.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '32px 0', color: 'var(--ash)' }}>
                <p className="body-sm">No floors yet. Open Structure to add floors.</p>
                <Link
                  to={`/manager/structure?propertyId=${property.id}`}
                  className="btn-outline btn-sm"
                  style={{ marginTop: 12 }}
                >
                  Open structure
                </Link>
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                {floors.map((floor) => (
                  <div
                    key={floor.id}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      gap: 12,
                      padding: '14px 16px',
                      borderRadius: 12,
                      background: 'var(--surface-bone)',
                      flexWrap: 'wrap',
                    }}
                  >
                    <div style={{ minWidth: 0 }}>
                      <p style={{ fontWeight: 700, color: 'var(--primary)', marginBottom: 2 }}>
                        Floor {floor.floorNumber}
                      </p>
                      <p className="body-sm text-charcoal">
                        {floor.description || `Floor ${floor.floorNumber}`}
                      </p>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
                      <div style={{ textAlign: 'right' }}>
                        <p className="body-sm text-charcoal">Rooms</p>
                        <p style={{ fontWeight: 700 }}>{floor.roomCount}</p>
                      </div>
                      <div style={{ textAlign: 'right' }}>
                        <p className="body-sm text-charcoal">Available</p>
                        <p style={{ fontWeight: 700, color: 'var(--success)' }}>{floor.availableCount}</p>
                      </div>
                      <Link
                        to={`/manager/rooms?propertyId=${property.id}&floorId=${floor.id}`}
                        className="btn-ghost btn-sm"
                        id={`btn-floor-rooms-${floor.id}`}
                      >
                        View rooms
                      </Link>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div className="card-lg" style={{ padding: 24 }}>
            <h2 className="heading-sm" style={{ marginBottom: 16 }}>Operations</h2>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {quickLinks.map((link) => (
                <Link
                  key={link.id}
                  to={link.to}
                  className={link.primary ? 'btn-primary' : 'btn-outline'}
                  style={{ justifyContent: 'center', textAlign: 'center' }}
                  id={link.id}
                >
                  {link.label}
                </Link>
              ))}
            </div>
            <p className="body-sm text-charcoal" style={{ marginTop: 14, lineHeight: 1.5 }}>
              Property profile is managed by Admin. You can run structure, rooms, and bookings for assigned properties.
            </p>
          </div>

          <div className="card-lg" style={{ padding: 24 }}>
            <h2 className="heading-sm" style={{ marginBottom: 6 }}>Occupancy</h2>
            <p className="body-sm text-charcoal" style={{ marginBottom: 20 }}>
              {stats.totalRooms} rooms ·{' '}
              <strong style={{ color: 'var(--primary)' }}>{occupancyPct}%</strong> occupied / reserved
            </p>

            <OccupancyBar label="Available" count={stats.availableRooms} total={stats.totalRooms} color="var(--success)" />
            <OccupancyBar label="Occupied" count={stats.occupiedRooms} total={stats.totalRooms} color="var(--primary)" />
            <OccupancyBar label="Reserved" count={stats.reservedRooms} total={stats.totalRooms} color="var(--info, #3b82f6)" />
            <OccupancyBar label="Pending deposit" count={stats.pendingDepositRooms} total={stats.totalRooms} color="var(--warning)" />
            <OccupancyBar label="Maintenance" count={stats.maintenanceRooms} total={stats.totalRooms} color="var(--error)" />
          </div>
        </div>
      </div>

      <style>{`
        @media (max-width: 900px) {
          .manager-property-detail-grid {
            grid-template-columns: 1fr !important;
          }
        }
      `}</style>
    </ManagerLayout>
  );
}
