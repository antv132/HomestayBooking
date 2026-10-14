import { useCallback, useEffect, useState } from 'react';
import AdminLayout from '../../layouts/AdminLayout';
import { getAdminProperties, type AdminProperty } from '../../api/adminApi';
import {
  fetchAdminHousekeepingTasks,
  type HousekeepingTaskSummary,
} from '../../api/housekeepingApi';
import { DataTable } from '../../components/ui';
import { extractApiError, Spinner, ErrorBanner, StatusBadge } from '../admin/_adminShared';

const STATUS_OPTS = [
  { value: '', label: 'Tất cả' },
  { value: 'PENDING', label: 'Chờ xử lý' },
  { value: 'IN_PROGRESS', label: 'Đang dọn' },
  { value: 'COMPLETED', label: 'Hoàn tất' },
  { value: 'CANCELLED', label: 'Đã hủy' },
];

function fmtDt(iso?: string | null): string {
  if (!iso) return '—';
  return new Date(iso).toLocaleString('vi-VN', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  });
}

/** FR-012 — Admin read-only housekeeping list */
export function AdminHousekeepingPage() {
  const [properties, setProperties] = useState<AdminProperty[]>([]);
  const [propertyId, setPropertyId] = useState('');
  const [status, setStatus] = useState('');
  const [tasks, setTasks] = useState<HousekeepingTaskSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getAdminProperties({ size: 100 })
      .then(res => { if (res.success) setProperties(res.data.content ?? []); })
      .catch(() => { /* non-blocking */ });
  }, []);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchAdminHousekeepingTasks({
        propertyId: propertyId || undefined,
        status: status || undefined,
        page: 0,
        size: 50,
      });
      setTasks(data.content ?? []);
    } catch (err) {
      setError(extractApiError(err, 'Không tải được danh sách housekeeping.'));
    } finally {
      setLoading(false);
    }
  }, [propertyId, status]);

  useEffect(() => { load(); }, [load]);

  return (
    <AdminLayout>
      <div className="animate-fade-in">
        <div style={{ marginBottom: 24 }}>
          <h1 style={{ fontFamily: 'Outfit', fontSize: 26, fontWeight: 700, color: 'var(--ink)', marginBottom: 4 }}>
            Housekeeping Tasks
          </h1>
        </div>

        <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', marginBottom: 20 }}>
          <select className="input" style={{ minWidth: 200 }} value={propertyId}
            onChange={e => setPropertyId(e.target.value)}>
            <option value="">Tất cả property</option>
            {properties.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
          </select>
          <select className="input" style={{ minWidth: 140 }} value={status}
            onChange={e => setStatus(e.target.value)}>
            {STATUS_OPTS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
          </select>
        </div>

        {error && <ErrorBanner msg={error} />}
        {loading ? <Spinner /> : (
          <DataTable
            columns={[
              { header: 'Property', accessor: (t: HousekeepingTaskSummary) => t.propertyName || '—' },
              { header: 'Phòng', accessor: (t) => t.roomNumber },
              {
                header: 'Trạng thái',
                accessor: (t) => <StatusBadge status={t.status} />,
              },
              { header: 'Nhân viên', accessor: (t) => t.assigneeName || '—' },
              { header: 'Tạo lúc', accessor: (t) => fmtDt(t.createdAt) },
              { header: 'Bắt đầu', accessor: (t) => fmtDt(t.startedAt) },
              { header: 'Hoàn tất', accessor: (t) => fmtDt(t.completedAt) },
            ]}
            data={tasks}
            keyExtractor={(t) => t.id}
          />
        )}
      </div>
    </AdminLayout>
  );
}
