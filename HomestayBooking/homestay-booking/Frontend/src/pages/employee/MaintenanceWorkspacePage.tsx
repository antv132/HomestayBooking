import { useState, useEffect, useCallback } from 'react';
import EmployeeLayout from '../../layouts/EmployeeLayout';
import { getEmployeeMaintenanceTickets, updateMaintenanceTicketStatus, type MaintenanceTicket } from '../../api/employeeApi';
import { TOUCH, fmtDate, extractErr, Spinner, ErrBanner, StatusBadge, Drawer } from './EmployeeShared';

// ── SCR-61: Maintenance Workspace ──────────────────────────────────────────────

function mxNext(status: MaintenanceTicket['status']): 'IN_PROGRESS' | 'RESOLVED' | null {
  if (status === 'ASSIGNED')    return 'IN_PROGRESS';
  if (status === 'IN_PROGRESS') return 'RESOLVED';
  return null;
}

const ISSUE_TYPE_ICONS: Record<string, string> = {
  ELECTRICAL: '⚡', PLUMBING: '🔧', HVAC: '❄️', FURNITURE: '🪑',
  APPLIANCE: '📺', STRUCTURAL: '🏗️', OTHER: '🛠️',
};

export default function MaintenanceWorkspacePage() {
  const [tickets, setTickets] = useState<MaintenanceTicket[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [updating, setUpdating] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState('');
  const [selected, setSelected] = useState<MaintenanceTicket | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [toastMsg, setToastMsg] = useState<string | null>(null);
  const [drawerErr, setDrawerErr] = useState<string | null>(null);

  const showToast = (msg: string) => { setToastMsg(msg); setTimeout(() => setToastMsg(null), 2400); };

  const load = useCallback(async () => {
    setLoading(true); setError(null);
    try {
      const res = await getEmployeeMaintenanceTickets({ size: 50, status: statusFilter || undefined });
      if (res.success) setTickets(res.data.content);
      else setError('Không tải được danh sách.');
    } catch (err) { setError(extractErr(err, 'Không tải được danh sách.')); }
    finally { setLoading(false); }
  }, [statusFilter]);

  useEffect(() => { load(); }, [load]);

  async function handleStatusUpdate(ticket: MaintenanceTicket) {
    const next = ticket.status === 'ASSIGNED' ? 'IN_PROGRESS' : ticket.status === 'IN_PROGRESS' ? 'RESOLVED' : null;
    if (!next || updating) return;
    // Optimistic
    setTickets(prev => prev.map(t => t.id === ticket.id ? { ...t, status: next } : t));
    if (selected?.id === ticket.id) setSelected(s => s ? { ...s, status: next } : s);
    setUpdating(ticket.id); setDrawerErr(null);
    try {
      await updateMaintenanceTicketStatus(ticket.id, next);
      showToast(next === 'RESOLVED' ? '✅ Đã giải quyết!' : '▶ Đang xử lý!');
      if (next === 'RESOLVED') { setTimeout(() => setDrawerOpen(false), 600); }
    } catch (err) {
      // Rollback
      setTickets(prev => prev.map(t => t.id === ticket.id ? { ...t, status: ticket.status } : t));
      if (selected?.id === ticket.id) setSelected(s => s ? { ...s, status: ticket.status } : s);
      setDrawerErr(extractErr(err, 'Cập nhật thất bại.'));
    } finally { setUpdating(null); }
  }

  const STATUS_FILTERS = [
    { v: '', label: 'Tất cả' }, { v: 'ASSIGNED', label: '📋 Assigned' },
    { v: 'IN_PROGRESS', label: '▶ In Progress' }, { v: 'RESOLVED', label: '✅ Resolved' },
  ];

  return (
    <EmployeeLayout>
      <div className="animate-fade-in">
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
          <div>
            <h1 style={{ fontFamily: 'Outfit', fontSize: 22, fontWeight: 700, color: 'var(--ink)', marginBottom: 2 }}>🔧 Maintenance</h1>
          </div>
          <button onClick={() => load()} style={{ ...TOUCH, background: 'var(--surface-bone)', border: '1px solid var(--hairline)', borderRadius: 10, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '0 14px', gap: 6, fontSize: 13, color: 'var(--charcoal)', fontWeight: 600 }}>
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/>
            </svg>
            Refresh
          </button>
        </div>
        {error && <ErrBanner msg={error} />}

        {/* Filter chips */}
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

        {loading ? <Spinner /> : tickets.length === 0 ? (
          <div className="card" style={{ padding: 40, textAlign: 'center' }}>
            <p style={{ fontSize: 32, marginBottom: 8 }}>🎉</p>
            <p style={{ fontWeight: 600, color: 'var(--ink)' }}>Không có yêu cầu bảo trì!</p>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {tickets.map(ticket => {
              const next = mxNext(ticket.status);
              const icon = ISSUE_TYPE_ICONS[ticket.issueType] || '🛠️';
              return (
                <div key={ticket.id} className="card" style={{
                  padding: '16px 18px', cursor: 'pointer',
                  borderLeft: ticket.status === 'IN_PROGRESS' ? '4px solid #2563EB' : ticket.status === 'RESOLVED' ? '4px solid #2b9a66' : '4px solid var(--hairline)',
                  opacity: ticket.status === 'RESOLVED' ? 0.65 : 1,
                  transition: 'opacity 0.2s',
                }} onClick={() => { setSelected(ticket); setDrawerErr(null); setDrawerOpen(true); }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
                    <div style={{ width: 44, height: 44, borderRadius: 10, background: 'var(--surface-bone)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 20, flexShrink: 0 }}>
                      {icon}
                    </div>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4, flexWrap: 'wrap' }}>
                        <p style={{ fontWeight: 700, fontSize: 15, color: 'var(--ink)' }}>{ticket.roomName}</p>
                        <StatusBadge status={ticket.status} />
                      </div>
                      <p className="body-sm text-charcoal" style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {ticket.issueType} — {ticket.description}
                      </p>
                    </div>
                    {next && (
                      <button
                        className={next === 'RESOLVED' ? 'btn-primary' : 'btn-outline'}
                        onClick={e => { e.stopPropagation(); handleStatusUpdate(ticket); }}
                        disabled={updating === ticket.id}
                        style={{ ...TOUCH, padding: '0 14px', borderRadius: 10, flexShrink: 0, fontSize: 13, fontWeight: 600, whiteSpace: 'nowrap' }}
                      >
                        {updating === ticket.id ? '...' : next === 'RESOLVED' ? '✓ Done' : '▶ Start'}
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {/* Drawer: Ticket Detail */}
        <Drawer open={drawerOpen} onClose={() => { setDrawerOpen(false); setSelected(null); }} title="Chi tiết yêu cầu">
          {selected && (
            <div>
              {drawerErr && <ErrBanner msg={drawerErr} />}
              <div style={{ marginBottom: 16 }}>
                {[
                  { label: 'Phòng', value: selected.roomName },
                  { label: 'Loại sự cố', value: `${ISSUE_TYPE_ICONS[selected.issueType] || '🛠️'} ${selected.issueType}` },
                  { label: 'Trạng thái', value: <StatusBadge status={selected.status} /> },
                  { label: 'Được giao', value: selected.assignedAt ? fmtDate(selected.assignedAt) : '—' },
                  { label: 'Giải quyết', value: selected.resolvedAt ? fmtDate(selected.resolvedAt) : '—' },
                ].map(r => (
                  <div key={r.label} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 0', borderBottom: '1px solid var(--hairline)' }}>
                    <span className="body-sm text-charcoal">{r.label}</span>
                    <span style={{ fontWeight: 600, fontSize: 14 }}>{r.value}</span>
                  </div>
                ))}
              </div>
              <div style={{ background: 'var(--surface-bone)', borderRadius: 8, padding: '12px 14px', marginBottom: 20 }}>
                <p style={{ fontWeight: 600, fontSize: 13, marginBottom: 6 }}>Mô tả sự cố:</p>
                <p className="body-sm">{selected.description}</p>
              </div>
              {mxNext(selected.status) && (
                <button
                  className={mxNext(selected.status) === 'RESOLVED' ? 'btn-primary' : 'btn-outline'}
                  style={{ width: '100%', ...TOUCH, borderRadius: 12, fontWeight: 700, fontSize: 15 }}
                  disabled={updating === selected.id}
                  onClick={() => handleStatusUpdate(selected)}
                >
                  {updating === selected.id ? 'Đang cập nhật...' : mxNext(selected.status) === 'RESOLVED' ? '✅ Đánh dấu Đã xử lý' : '▶ Bắt đầu xử lý'}
                </button>
              )}
            </div>
          )}
        </Drawer>

        {/* Toast */}
        {toastMsg && (
          <div style={{ position: 'fixed', bottom: 24, left: '50%', transform: 'translateX(-50%)', background: 'var(--ink)', color: '#fff', padding: '10px 20px', borderRadius: 30, fontSize: 14, fontWeight: 600, zIndex: 2000, boxShadow: '0 4px 20px rgba(0,0,0,0.2)' }}>
            {toastMsg}
          </div>
        )}
      </div>
    </EmployeeLayout>
  );
}
