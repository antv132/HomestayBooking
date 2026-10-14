import { useState, useEffect, useCallback } from 'react';
import ManagerLayout from '../../layouts/ManagerLayout';
import Alert from '../../components/ui/Alert';
import { Drawer, StatusBadge } from '../../components/ui';
import type { StatusVariant } from '../../components/ui/StatusBadge';
import {
  assignInspectorV1,
  fetchInspectionChecklistAnswersV1,
  fetchManagerInspectionsV1,
  type InspectionChecklistAnswer,
  type InspectionSummary,
} from '../../api/managerInspectionApi';
import {
  fetchManagerEmployeesV1,
  type EmployeeSummary,
} from '../../api/managerEmployeeApi';
import { managerApi } from '../../api/managerApi';
import type { AssignedProperty } from '../../api/reportApi';

const STATUS_VI: Record<string, { label: string; variant: StatusVariant }> = {
  PENDING:            { label: 'Chờ kiểm tra',           variant: 'warning' },
  IN_PROGRESS:        { label: 'Đang kiểm tra',          variant: 'info' },
  PASSED:             { label: 'Đạt',                    variant: 'success' },
  FAILED_WITH_DAMAGE: { label: 'Không đạt – có hư hại',  variant: 'danger' },
};

const STATUS_FILTERS = [
  { value: '', label: 'Tất cả' },
  { value: 'PENDING', label: 'Chờ kiểm tra' },
  { value: 'IN_PROGRESS', label: 'Đang kiểm tra' },
  { value: 'PASSED', label: 'Đạt' },
  { value: 'FAILED_WITH_DAMAGE', label: 'Không đạt' },
];

const ASSIGNABLE = new Set(['PENDING', 'IN_PROGRESS']);

function formatDateTime(iso?: string | null): string {
  if (!iso) return '—';
  return new Date(iso).toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function shortId(id: string): string {
  return `BK-${id.slice(0, 8).toUpperCase()}`;
}

function badge(status: string) {
  const cfg = STATUS_VI[status] ?? { label: status, variant: 'neutral' as StatusVariant };
  return <StatusBadge status={cfg.label} variant={cfg.variant} />;
}

export default function InspectionsPage() {
  const [properties, setProperties] = useState<AssignedProperty[]>([]);
  const [selectedPropertyId, setSelectedPropertyId] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');

  const [inspections, setInspections] = useState<InspectionSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  const [drawerItem, setDrawerItem] = useState<InspectionSummary | null>(null);
  const [employees, setEmployees] = useState<EmployeeSummary[]>([]);
  const [selectedEmployeeId, setSelectedEmployeeId] = useState('');
  const [drawerError, setDrawerError] = useState<string | null>(null);
  const [drawerSubmitting, setDrawerSubmitting] = useState(false);
  const [checklistAnswers, setChecklistAnswers] = useState<InspectionChecklistAnswer[]>([]);

  useEffect(() => {
    managerApi.getMyAssignedProperties()
      .then(res => {
        if (res.success && res.data) {
          setProperties(res.data);
          if (res.data.length > 0) {
            setSelectedPropertyId(prev => prev || res.data![0].id);
          }
        }
      })
      .catch(() => setError('Không thể tải danh sách homestay.'));
  }, []);

  useEffect(() => {
    const t = setTimeout(() => setDebouncedSearch(search.trim()), 400);
    return () => clearTimeout(t);
  }, [search]);

  const loadInspections = useCallback(() => {
    if (!selectedPropertyId) {
      setInspections([]);
      return;
    }
    setLoading(true);
    setError(null);
    fetchManagerInspectionsV1({
      propertyId: selectedPropertyId,
      status: statusFilter || undefined,
      search: debouncedSearch || undefined,
      page: 0,
      size: 100,
    })
      .then(data => setInspections(data.content))
      .catch((err: unknown) => {
        const ax = err as { response?: { status?: number; data?: { message?: string } } };
        if (ax?.response?.status === 403) {
          setError('Bạn không có quyền xem homestay này.');
        } else {
          setError(ax?.response?.data?.message ?? 'Không thể tải danh sách kiểm tra phòng.');
        }
        setInspections([]);
      })
      .finally(() => setLoading(false));
  }, [selectedPropertyId, statusFilter, debouncedSearch]);

  useEffect(() => {
    loadInspections();
  }, [loadInspections]);

  function openDrawer(item: InspectionSummary) {
    setDrawerItem(item);
    setSelectedEmployeeId(item.inspectorId ?? '');
    setDrawerError(null);
    setChecklistAnswers([]);
    fetchInspectionChecklistAnswersV1(item.id)
      .then(setChecklistAnswers)
      .catch(() => setChecklistAnswers([]));
    if (selectedPropertyId) {
      fetchManagerEmployeesV1({ propertyId: selectedPropertyId, page: 0, size: 100 })
        .then(data => setEmployees(data.content.filter(e => e.status === 'ACTIVE' || !e.status)))
        .catch(() => setEmployees([]));
    }
  }

  async function handleAssign() {
    if (!drawerItem || !selectedEmployeeId) {
      setDrawerError('Vui lòng chọn nhân viên kiểm tra.');
      return;
    }
    setDrawerSubmitting(true);
    setDrawerError(null);
    try {
      const updated = await assignInspectorV1(drawerItem.id, selectedEmployeeId);
      setDrawerItem(updated);
      setSuccessMsg('Gán nhân viên kiểm tra thành công.');
      loadInspections();
    } catch (err: unknown) {
      const ax = err as { response?: { data?: { message?: string } } };
      setDrawerError(ax?.response?.data?.message ?? 'Không thể gán nhân viên.');
    } finally {
      setDrawerSubmitting(false);
    }
  }

  const isEmpty = !loading && inspections.length === 0;
  const canAssign = drawerItem && ASSIGNABLE.has(drawerItem.status);

  return (
    <ManagerLayout>
      <div className="space-y-6">
        <div>
          <h1 className="heading-md m-0">Kiểm tra phòng</h1>
          <p className="text-sm text-[#64748B] mt-1 m-0">
            Gán nhân viên kiểm tra và theo dõi kết quả trước Check-out.
          </p>
        </div>

        {error && <Alert variant="error" message={error} closeable onClose={() => setError(null)} />}
        {successMsg && (
          <Alert variant="success" message={successMsg} closeable onClose={() => setSuccessMsg(null)} />
        )}

        <div className="flex flex-wrap gap-3 items-end bg-white rounded-xl border border-[#E2E8F0] p-4">
          <div className="min-w-[180px] flex-1 sm:max-w-xs">
            <label className="block text-sm font-medium text-[#334155] mb-1">Homestay</label>
            <select
              className="input-field w-full"
              value={selectedPropertyId}
              onChange={e => setSelectedPropertyId(e.target.value)}
            >
              <option value="">— Chọn homestay —</option>
              {properties.map(p => (
                <option key={p.id} value={p.id}>{p.name}</option>
              ))}
            </select>
          </div>
          <div className="min-w-[200px] flex-1 sm:max-w-xs">
            <label className="block text-sm font-medium text-[#334155] mb-1">Tìm theo số phòng</label>
            <input
              type="text"
              className="input-field w-full"
              value={search}
              onChange={e => setSearch(e.target.value)}
              placeholder="Nhập số phòng..."
            />
          </div>
          <div className="flex gap-2 flex-wrap">
            {STATUS_FILTERS.map(f => (
              <button
                key={f.value}
                type="button"
                className={`px-3 py-1.5 text-sm rounded-full border ${
                  statusFilter === f.value
                    ? 'border-[#0F766E] bg-[#0F766E]/10 text-[#0F766E] font-semibold'
                    : 'border-[#E2E8F0] text-[#64748B]'
                }`}
                onClick={() => setStatusFilter(f.value)}
              >
                {f.label}
              </button>
            ))}
          </div>
        </div>

        {!selectedPropertyId ? (
          <div className="text-center py-16 text-[#64748B]">
            Vui lòng chọn homestay để xem danh sách kiểm tra phòng.
          </div>
        ) : loading ? (
          <div className="h-64 bg-[#F1F5F9] rounded-xl animate-pulse" />
        ) : isEmpty ? (
          <div className="text-center py-16 bg-white rounded-xl border border-[#E2E8F0] text-[#64748B]">
            Không có bản ghi kiểm tra phòng phù hợp.
          </div>
        ) : (
          <div className="bg-white rounded-xl border border-[#E2E8F0] overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="bg-[#F8FAFC] text-[#64748B] text-left text-[12px] uppercase tracking-wide">
                    <th className="px-4 py-3 font-medium">Phòng</th>
                    <th className="px-4 py-3 font-medium">Booking</th>
                    <th className="px-4 py-3 font-medium">Người kiểm tra</th>
                    <th className="px-4 py-3 font-medium">Kết quả</th>
                    <th className="px-4 py-3 font-medium">Kiểm tra lúc</th>
                    <th className="px-4 py-3 font-medium text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  {inspections.map(item => (
                    <tr
                      key={item.id}
                      className="border-t border-[#F1F5F9] hover:bg-[#F8FAFC] cursor-pointer"
                      onClick={() => openDrawer(item)}
                    >
                      <td className="px-4 py-3 font-medium text-[#1E293B] whitespace-nowrap">
                        {item.roomNumber}
                      </td>
                      <td className="px-4 py-3 text-[#64748B] whitespace-nowrap">{shortId(item.bookingId)}</td>
                      <td className="px-4 py-3 text-[#64748B] whitespace-nowrap">
                        {item.inspectorName || (
                          <span className="text-[#B45309] font-medium">Chưa gán</span>
                        )}
                      </td>
                      <td className="px-4 py-3">{badge(item.status)}</td>
                      <td className="px-4 py-3 text-[#64748B] whitespace-nowrap">
                        {formatDateTime(item.inspectedAt)}
                      </td>
                      <td className="px-4 py-3 text-right">
                        <button
                          type="button"
                          className="text-[#0F766E] hover:underline text-sm font-medium"
                          onClick={e => { e.stopPropagation(); openDrawer(item); }}
                        >
                          {ASSIGNABLE.has(item.status) ? 'Gán / Xem' : 'Xem'}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>

      <Drawer
        isOpen={!!drawerItem}
        onClose={() => setDrawerItem(null)}
        title={drawerItem ? `Phòng ${drawerItem.roomNumber} — Kiểm tra` : ''}
      >
        {drawerItem && (
          <div className="space-y-4">
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-[#64748B]">Kết quả</span>
                {badge(drawerItem.status)}
              </div>
              <div className="flex justify-between">
                <span className="text-[#64748B]">Booking</span>
                <span className="font-medium">{shortId(drawerItem.bookingId)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-[#64748B]">Người kiểm tra</span>
                <span className="font-medium">{drawerItem.inspectorName || 'Chưa gán'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-[#64748B]">Tạo lúc</span>
                <span>{formatDateTime(drawerItem.createdAt)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-[#64748B]">Kiểm tra lúc</span>
                <span>{formatDateTime(drawerItem.inspectedAt)}</span>
              </div>
              {drawerItem.inspectionDurationMinutes != null && (
                <div className="flex justify-between">
                  <span className="text-[#64748B]">Thời lượng kiểm tra</span>
                  <span>{drawerItem.inspectionDurationMinutes} phút</span>
                </div>
              )}
            </div>

            <div>
              <span className="text-[#64748B] text-sm">Ghi chú kiểm tra</span>
              <p className="m-0 mt-1 text-[#334155] whitespace-pre-wrap">
                {drawerItem.note || 'Không có ghi chú.'}
              </p>
            </div>

            {checklistAnswers.length > 0 && (
              <div>
                <span className="text-[#64748B] text-sm">Checklist đã nộp</span>
                <ul className="m-0 mt-2 p-0 list-none space-y-1.5">
                  {checklistAnswers.map(a => (
                    <li key={a.id} className="flex justify-between items-center gap-3 text-sm border-b border-[#F1F5F9] py-1.5">
                      <span className="flex items-center gap-2 min-w-0">
                        {a.icon ? (
                          <span
                            aria-hidden
                            style={{
                              flexShrink: 0,
                              fontSize: 18,
                              lineHeight: 1,
                              fontFamily: '"Segoe UI Emoji", "Apple Color Emoji", "Noto Color Emoji", "Segoe UI Symbol", sans-serif',
                            }}
                          >
                            {a.icon}
                          </span>
                        ) : null}
                        <span className="text-[#334155] truncate" style={{ fontFamily: 'var(--font-body)' }}>
                          {a.label}
                        </span>
                      </span>
                      <span className={a.passed ? 'text-[#059669] font-semibold shrink-0' : 'text-[#DC2626] font-semibold shrink-0'}>
                        {a.passed ? 'OK' : 'FAIL'}
                      </span>
                    </li>
                  ))}
                </ul>
              </div>
            )}

            {canAssign ? (
              <div className="border-t border-[#E2E8F0] pt-4 space-y-3">
                <label className="block text-sm font-medium text-[#334155]" htmlFor="inspector-select">
                  Gán / đổi nhân viên kiểm tra
                </label>
                <select
                  id="inspector-select"
                  className="input-field w-full"
                  value={selectedEmployeeId}
                  onChange={e => setSelectedEmployeeId(e.target.value)}
                >
                  <option value="">— Chọn nhân viên —</option>
                  {employees.map(emp => (
                    <option key={emp.id} value={emp.id}>
                      {emp.fullName}{emp.email ? ` (${emp.email})` : ''}
                    </option>
                  ))}
                </select>
                {employees.length === 0 && (
                  <p className="text-xs text-[#B45309] m-0">
                    Homestay chưa có nhân viên ACTIVE. Hãy gán Employee tại mục Quản lý nhân viên.
                  </p>
                )}
                {drawerError && <p className="text-sm text-red-600 m-0">{drawerError}</p>}
                <button
                  type="button"
                  className="btn-primary w-full"
                  disabled={drawerSubmitting || !selectedEmployeeId}
                  onClick={handleAssign}
                >
                  {drawerSubmitting ? 'Đang lưu...' : 'Gán nhân viên'}
                </button>
              </div>
            ) : (
              <p className="text-xs text-[#64748B] border-t border-[#E2E8F0] pt-4 m-0">
                Kiểm tra đã hoàn tất — không thể đổi người kiểm tra.
              </p>
            )}
          </div>
        )}
      </Drawer>
    </ManagerLayout>
  );
}
