import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import AdminLayout from '../../layouts/AdminLayout';
import { DataTable, StatusBadge as UIStatusBadge } from '../../components/ui';
import { fmtDate, extractApiError, Spinner, ErrorBanner, SuccessBanner, Drawer, Pagination } from './_adminShared';
import { complaintsApi, type AdminComplaintItem, type ComplaintDetail, type ComplaintStatus } from '../../api/complaintsApi';

const STATUS_FLOW: Record<string, ComplaintStatus | null> = {
  OPEN: 'INVESTIGATING',
  INVESTIGATING: 'RESOLVED',
  RESOLVED: 'CLOSED',
  CLOSED: null,
};

const STATUS_LABELS: Record<string, string> = {
  OPEN: 'Mới',
  INVESTIGATING: 'Đang xử lý',
  RESOLVED: 'Đã giải quyết',
  CLOSED: 'Đã đóng',
};

export function AdminComplaintsPage() {
  const [items, setItems] = useState<AdminComplaintItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [statusFilter, setStatusFilter] = useState('');
  const [selected, setSelected] = useState<AdminComplaintItem | null>(null);
  const [detail, setDetail] = useState<ComplaintDetail | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [resolution, setResolution] = useState('');
  const [actionLoading, setActionLoading] = useState(false);
  const [actionMsg, setActionMsg] = useState<{ type: 'success' | 'error'; msg: string } | null>(null);

  const load = useCallback(async (p = 0, status = '') => {
    setLoading(true); setError(null);
    try {
      const res = await complaintsApi.getAdminComplaints({ page: p, size: 10, status: status || undefined });
      if (res.success) { setItems(res.data.content); setTotalPages(res.data.totalPages); setPage(p); }
    } catch (err) { setError(extractApiError(err, 'Không tải được complaints.')); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { load(0, statusFilter); }, [load, statusFilter]);

  async function openDetail(c: AdminComplaintItem) {
    setSelected(c);
    setResolution(c.resolution || '');
    setActionMsg(null);
    setDrawerOpen(true);
    try {
      const res = await complaintsApi.getAdminComplaintDetail(c.id);
      if (res.success) setDetail(res.data);
    } catch { /* silent, use summary */ }
  }

  async function handleStatusUpdate(targetStatus: ComplaintStatus) {
    if (!selected) return;
    if (targetStatus === 'RESOLVED' && resolution.trim().length < 10) {
      setActionMsg({ type: 'error', msg: 'Ghi chú giải quyết phải ít nhất 10 ký tự.' });
      return;
    }
    setActionLoading(true); setActionMsg(null);
    try {
      const res = await complaintsApi.updateComplaintStatus(selected.id, {
        status: targetStatus,
        resolution: resolution.trim() || undefined,
      });
      if (res.success) {
        setActionMsg({ type: 'success', msg: `Đã chuyển sang ${STATUS_LABELS[targetStatus]}!` });
        load(page, statusFilter);
        setTimeout(() => setDrawerOpen(false), 1200);
      } else {
        setActionMsg({ type: 'error', msg: 'Cập nhật thất bại.' });
      }
    } catch (err) { setActionMsg({ type: 'error', msg: extractApiError(err, 'Cập nhật thất bại.') }); }
    finally { setActionLoading(false); }
  }

  const columns = [
    { header: 'ID', accessor: (c: AdminComplaintItem) => <span className="code-sm">{(c.id ?? '').slice(0, 8) || '—'}</span> },
    { header: 'Khách hàng', accessor: (c: AdminComplaintItem) => <span className="font-semibold">{c.customerName || '—'}</span> },
    { header: 'Chủ đề', accessor: (c: AdminComplaintItem) => <span style={{ maxWidth: 180, display: 'inline-block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', fontSize: 13 }}>{c.subject || '—'}</span> },
    { header: 'Trạng thái', accessor: (c: AdminComplaintItem) => <UIStatusBadge status={c.status} variant={c.status === 'RESOLVED' || c.status === 'CLOSED' ? 'success' : c.status === 'OPEN' ? 'warning' : 'info'} /> },
    { header: 'Ngày tạo', accessor: (c: AdminComplaintItem) => fmtDate(c.createdAt) }
  ];

  const actions = [
    { label: 'Chi tiết', onClick: (c: AdminComplaintItem) => openDetail(c) }
  ];

  const nextStatus = selected ? STATUS_FLOW[selected.status] : null;

  return (
    <AdminLayout>
      <div className="animate-fade-in">
        <div style={{ marginBottom: 24 }}>
          <h1 style={{ fontFamily: 'Outfit', fontSize: 26, fontWeight: 700, color: 'var(--ink)', marginBottom: 4 }}>Complaint Management</h1>
          <p className="body-sm text-charcoal">Xử lý khiếu nại hệ thống (OPEN → Investigate → Resolve → Close)</p>
        </div>
        {error && <ErrorBanner msg={error} />}
        <div className="card" style={{ padding: '14px 18px', marginBottom: 16 }}>
          <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
            <label className="form-label" style={{ alignSelf: 'center', margin: 0 }}>Lọc:</label>
            {['', 'OPEN', 'INVESTIGATING', 'RESOLVED', 'CLOSED'].map(s => (
              <button key={s} className={statusFilter === s ? 'btn-primary btn-sm' : 'btn-ghost btn-sm'}
                onClick={() => setStatusFilter(s)}>
                {s ? STATUS_LABELS[s] : 'Tất cả'}
              </button>
            ))}
          </div>
        </div>
        {loading ? <Spinner /> : (
          <>
            <DataTable columns={columns} data={items} keyExtractor={(c) => c.id} actions={actions} />
            <Pagination page={page} totalPages={totalPages} onPage={p => load(p, statusFilter)} />
          </>
        )}

        <Drawer open={drawerOpen} onClose={() => { setDrawerOpen(false); setSelected(null); setDetail(null); }} title="Chi tiết Complaint">
          {selected && (
            <div>
              {actionMsg && (actionMsg.type === 'success' ? <SuccessBanner msg={actionMsg.msg} /> : <ErrorBanner msg={actionMsg.msg} />)}
              <div style={{ marginBottom: 20 }}>
                {[
                  { label: 'ID', value: selected.id.slice(0, 8) },
                  { label: 'Khách hàng', value: selected.customerName || '—' },
                  { label: 'Chủ đề', value: selected.subject || '—' },
                  { label: 'Trạng thái', value: <UIStatusBadge status={selected.status} variant={selected.status === 'RESOLVED' || selected.status === 'CLOSED' ? 'success' : selected.status === 'OPEN' ? 'warning' : 'info'} /> },
                  { label: 'Ngày tạo', value: fmtDate(selected.createdAt) },
                  ...(selected.resolvedAt ? [{ label: 'Ngày giải quyết', value: fmtDate(selected.resolvedAt) }] : []),
                ].map(r => (
                  <div key={r.label} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '8px 0', borderBottom: '1px solid var(--hairline)' }}>
                    <span className="body-sm text-charcoal">{r.label}</span>
                    <span style={{ fontWeight: 600, fontSize: 14 }}>{r.value}</span>
                  </div>
                ))}
              </div>
              <div style={{ marginBottom: 16, padding: '12px', background: 'var(--surface-bone)', borderRadius: 8 }}>
                <p style={{ fontWeight: 600, fontSize: 13, marginBottom: 6 }}>Mô tả khiếu nại:</p>
                <p className="body-sm">{detail?.description || selected.description}</p>
              </div>
              {selected.resolution && (
                <div style={{ marginBottom: 16, padding: '12px', background: '#f0fdf4', borderRadius: 8 }}>
                  <p style={{ fontWeight: 600, fontSize: 13, marginBottom: 6 }}>Ghi chú giải quyết:</p>
                  <p className="body-sm">{selected.resolution}</p>
                </div>
              )}
              {nextStatus && (
                <div style={{ marginBottom: 16 }}>
                  {(nextStatus === 'RESOLVED') && (
                    <div style={{ marginBottom: 12 }}>
                      <label className="form-label form-label-required">Ghi chú giải quyết (min 10 ký tự)</label>
                      <textarea className="textarea" rows={4}
                        placeholder="Nhập hướng giải quyết..."
                        value={resolution} onChange={e => setResolution(e.target.value)} />
                    </div>
                  )}
                  {(nextStatus === 'INVESTIGATING' || nextStatus === 'CLOSED') && (
                    <div style={{ marginBottom: 12 }}>
                      <label className="form-label">Ghi chú (tùy chọn)</label>
                      <textarea className="textarea" rows={2}
                        placeholder="Ghi chú thêm..."
                        value={resolution} onChange={e => setResolution(e.target.value)} />
                    </div>
                  )}
                  <button className="btn-primary" style={{ width: '100%' }}
                    disabled={actionLoading}
                    onClick={() => handleStatusUpdate(nextStatus!)}>
                    {actionLoading ? 'Đang xử lý...' : `Chuyển sang: ${STATUS_LABELS[nextStatus]}`}
                  </button>
                </div>
              )}
            </div>
          )}
        </Drawer>
      </div>
    </AdminLayout>
  );
}
