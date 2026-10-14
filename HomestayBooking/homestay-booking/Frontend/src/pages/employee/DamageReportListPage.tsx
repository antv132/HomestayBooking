import { useState, useEffect, useCallback, useRef } from 'react';
import EmployeeLayout from '../../layouts/EmployeeLayout';
import { getEmployeeDamageReports, type DamageReport } from '../../api/employeeApi';
import { fmtVnd, fmtDate, extractErr, Spinner, ErrBanner, StatusBadge, Drawer, FAB } from './EmployeeShared';

const STATUS_FILTERS = [
  { value: '', label: 'Tất cả trạng thái' },
  { value: 'DRAFT', label: 'Draft' },
  { value: 'PENDING_APPROVAL', label: 'Pending Approval' },
  { value: 'APPROVED', label: 'Approved' },
  { value: 'DISPUTED', label: 'Disputed' },
  { value: 'PAID', label: 'Paid' },
];

function formatReportId(id: string) {
  const compact = id.replace(/-/g, '').slice(0, 8).toUpperCase();
  return `DR-${compact}`;
}

function formatBookingId(id?: string) {
  if (!id) return '—';
  const compact = id.replace(/-/g, '').slice(0, 8).toUpperCase();
  return `BK-${compact}`;
}

export default function DamageReportListPage() {
  const [reports, setReports] = useState<DamageReport[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [status, setStatus] = useState('');
  const [search, setSearch] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [selected, setSelected] = useState<DamageReport | null>(null);
  const searchDebounceRef = useRef<ReturnType<typeof setTimeout>>();

  const load = useCallback(async (p = 0, st = status, srch = search) => {
    setLoading(true);
    setError(null);
    try {
      const res = await getEmployeeDamageReports({
        page: p,
        size: 15,
        status: st || undefined,
        search: srch || undefined,
      });
      if (res.success) {
        setReports(res.data.content);
        setTotalPages(res.data.totalPages);
        setPage(p);
      } else {
        setError('Không tải được danh sách.');
      }
    } catch (err) {
      const ax = err as { response?: { status?: number; data?: { message?: string } } };
      if (ax?.response?.status === 401 || ax?.response?.status === 403) {
        setError(ax.response.data?.message ?? 'Bạn không có quyền xem báo cáo hư hại.');
      } else {
        setError(extractErr(err, 'Không tải được danh sách.'));
      }
    } finally {
      setLoading(false);
    }
  }, [status, search]);

  useEffect(() => { load(0); }, [load]);

  useEffect(() => {
    return () => {
      if (searchDebounceRef.current) clearTimeout(searchDebounceRef.current);
    };
  }, []);

  function onSearchChange(value: string) {
    setSearchInput(value);
    if (searchDebounceRef.current) clearTimeout(searchDebounceRef.current);
    searchDebounceRef.current = setTimeout(() => {
      setSearch(value.trim());
      setPage(0);
    }, 350);
  }

  function onStatusChange(value: string) {
    setStatus(value);
    setPage(0);
  }

  const itemCount = (r: DamageReport) => r.itemCount ?? r.items?.length ?? 0;

  return (
    <EmployeeLayout>
      <div className="animate-fade-in">
        <div style={{ marginBottom: 20 }}>
          <h1 className="heading-md" style={{ marginBottom: 4 }}>My Reports</h1>
        </div>

        <div className="flex flex-col sm:flex-row gap-3" style={{ marginBottom: 16 }}>
          <select
            className="input-field flex-1 sm:max-w-xs"
            value={status}
            onChange={e => onStatusChange(e.target.value)}
            aria-label="Lọc theo trạng thái"
          >
            {STATUS_FILTERS.map(f => (
              <option key={f.value || 'all'} value={f.value}>{f.label}</option>
            ))}
          </select>
          <input
            type="search"
            className="input-field flex-1"
            placeholder="Tìm theo phòng, mã báo cáo, booking..."
            value={searchInput}
            onChange={e => onSearchChange(e.target.value)}
            aria-label="Tìm kiếm báo cáo hư hại"
          />
        </div>

        {error && <ErrBanner msg={error} />}

        {loading ? (
          <Spinner />
        ) : reports.length === 0 ? (
          <div className="card" style={{ padding: 40, textAlign: 'center' }}>
            <p style={{ fontWeight: 600, color: 'var(--ink)', marginBottom: 4 }}>No damage reports submitted.</p>
            <p className="body-sm text-charcoal" style={{ marginBottom: 8 }}>
              Nhấn + để tạo báo cáo hư hại mới.
            </p>
          </div>
        ) : (
          <>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginBottom: 16 }}>
              {reports.map(r => (
                <button
                  key={r.id}
                  type="button"
                  className="card"
                  onClick={() => setSelected(r)}
                  style={{
                    padding: '16px 18px',
                    display: 'flex',
                    alignItems: 'center',
                    gap: 14,
                    textAlign: 'left',
                    width: '100%',
                    cursor: 'pointer',
                    border: '1px solid var(--hairline)',
                    background: 'var(--surface-card)',
                  }}
                >
                  <div style={{
                    width: 44, height: 44, borderRadius: 10,
                    background: 'rgba(220,38,38,0.08)',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    flexShrink: 0, color: '#DC2626', fontWeight: 700, fontSize: 13,
                  }}>
                    DR
                  </div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4, flexWrap: 'wrap' }}>
                      <p style={{ fontWeight: 700, fontSize: 15, color: 'var(--ink)', margin: 0 }}>
                        Phòng {r.roomName || '—'}
                      </p>
                      <StatusBadge status={r.status} />
                      {r.requiresAdminEscalation && (
                        <span className="badge badge-error">&gt; 5M</span>
                      )}
                    </div>
                    <p className="body-sm text-charcoal" style={{ margin: '0 0 2px' }}>
                      {formatReportId(r.id)} · {formatBookingId(r.bookingId)} · {itemCount(r)} mục · {fmtVnd(Number(r.totalCost) || 0)}
                    </p>
                    <p className="body-sm text-charcoal" style={{ margin: 0 }}>{fmtDate(r.createdAt)}</p>
                  </div>
                </button>
              ))}
            </div>

            {totalPages > 1 && (
              <div style={{ display: 'flex', gap: 8, justifyContent: 'center' }}>
                <button className="btn-ghost btn-sm" disabled={page === 0} onClick={() => load(page - 1)}>‹ Trước</button>
                <span className="body-sm" style={{ alignSelf: 'center' }}>Trang {page + 1}/{totalPages}</span>
                <button className="btn-ghost btn-sm" disabled={page >= totalPages - 1} onClick={() => load(page + 1)}>Sau ›</button>
              </div>
            )}
          </>
        )}

        <FAB to="/employee/damage/create" label="Tạo báo cáo hư hại" />

        <Drawer
          open={!!selected}
          onClose={() => setSelected(null)}
          title={selected ? `Phòng ${selected.roomName || '—'}` : 'Chi tiết'}
        >
          {selected && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
                <StatusBadge status={selected.status} />
                {selected.requiresAdminEscalation && (
                  <span className="badge badge-error">Escalated (&gt; 5M)</span>
                )}
              </div>
              <div className="body-sm text-charcoal" style={{ display: 'grid', gap: 6 }}>
                <p style={{ margin: 0 }}><strong>Report:</strong> {formatReportId(selected.id)}</p>
                <p style={{ margin: 0 }}><strong>Booking:</strong> {formatBookingId(selected.bookingId)}</p>
                <p style={{ margin: 0 }}><strong>Estimated:</strong> {fmtVnd(Number(selected.totalCost) || 0)}</p>
                <p style={{ margin: 0 }}><strong>Created:</strong> {fmtDate(selected.createdAt)}</p>
                {selected.note && <p style={{ margin: 0 }}><strong>Note:</strong> {selected.note}</p>}
              </div>
              <div>
                <p style={{ fontWeight: 700, fontSize: 14, color: 'var(--ink)', marginBottom: 8 }}>
                  Damage items ({itemCount(selected)})
                </p>
                {(selected.items?.length ?? 0) === 0 ? (
                  <p className="body-sm text-charcoal">Không có chi tiết hạng mục.</p>
                ) : (
                  <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: 8 }}>
                    {selected.items.map((item, idx) => (
                      <li
                        key={`${item.name}-${idx}`}
                        className="card"
                        style={{ padding: '12px 14px', border: '1px solid var(--hairline)' }}
                      >
                        <p style={{ fontWeight: 600, margin: '0 0 4px', color: 'var(--ink)' }}>{item.name}</p>
                        <p className="body-sm text-charcoal" style={{ margin: 0 }}>
                          {fmtVnd(Number(item.estimatedCost) || 0)}
                        </p>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </div>
          )}
        </Drawer>
      </div>
    </EmployeeLayout>
  );
}
