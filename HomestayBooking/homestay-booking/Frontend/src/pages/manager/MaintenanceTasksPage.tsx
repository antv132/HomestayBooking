import { useState, useEffect, useCallback } from 'react';
import ManagerLayout from '../../layouts/ManagerLayout';
import Alert from '../../components/ui/Alert';
import { Drawer, StatusBadge } from '../../components/ui';
import type { StatusVariant } from '../../components/ui/StatusBadge';
import {
  fetchManagerMaintenanceTasksV1,
  assignMaintenanceTaskV1,
  updateMaintenanceTaskStatusV1,
  type MaintenanceTaskSummary,
} from '../../api/managerMaintenanceApi';
import { fetchManagerEmployeesV1, type EmployeeSummary } from '../../api/managerEmployeeApi';
import { managerApi } from '../../api/managerApi';
import type { AssignedProperty } from '../../api/reportApi';

const BACKEND_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080';

function photoUrl(url: string): string {
  return url.startsWith('http') ? url : `${BACKEND_BASE}${url}`;
}

const STATUS_VI: Record<string, { label: string; variant: StatusVariant }> = {
  OPEN:        { label: 'Mở',        variant: 'warning' },
  ASSIGNED:    { label: 'Đã gán',    variant: 'info' },
  IN_PROGRESS: { label: 'Đang xử lý', variant: 'primary' },
  RESOLVED:    { label: 'Đã xử lý',  variant: 'info' },
  CLOSED:      { label: 'Đã đóng',   variant: 'success' },
};

const STATUS_FILTERS = [
  { value: '', label: 'Tất cả' },
  { value: 'OPEN', label: 'Mở' },
  { value: 'ASSIGNED', label: 'Đã gán' },
  { value: 'IN_PROGRESS', label: 'Đang xử lý' },
  { value: 'RESOLVED', label: 'Đã xử lý' },
  { value: 'CLOSED', label: 'Đã đóng' },
];

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

function badge(status: string) {
  const cfg = STATUS_VI[status] ?? { label: status, variant: 'neutral' as StatusVariant };
  return <StatusBadge status={cfg.label} variant={cfg.variant} />;
}

export default function MaintenanceTasksPage() {
  const [properties, setProperties] = useState<AssignedProperty[]>([]);
  const [selectedPropertyId, setSelectedPropertyId] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');

  const [tasks, setTasks] = useState<MaintenanceTaskSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  const [drawerTask, setDrawerTask] = useState<MaintenanceTaskSummary | null>(null);
  const [employees, setEmployees] = useState<EmployeeSummary[]>([]);
  const [selectedAssigneeId, setSelectedAssigneeId] = useState('');
  const [resolutionNote, setResolutionNote] = useState('');
  const [drawerSubmitting, setDrawerSubmitting] = useState(false);
  const [drawerError, setDrawerError] = useState<string | null>(null);

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

  const loadTasks = useCallback(() => {
    if (!selectedPropertyId) {
      setTasks([]);
      return;
    }
    setLoading(true);
    setError(null);
    fetchManagerMaintenanceTasksV1({
      propertyId: selectedPropertyId,
      status: statusFilter || undefined,
      search: debouncedSearch || undefined,
      page: 0,
      size: 100,
    })
      .then(data => setTasks(data.content))
      .catch((err: unknown) => {
        const ax = err as { response?: { status?: number; data?: { message?: string } } };
        if (ax?.response?.status === 403) {
          setError('Bạn không có quyền xem homestay này.');
        } else {
          setError(ax?.response?.data?.message ?? 'Không thể tải danh sách bảo trì.');
        }
        setTasks([]);
      })
      .finally(() => setLoading(false));
  }, [selectedPropertyId, statusFilter, debouncedSearch]);

  useEffect(() => {
    loadTasks();
  }, [loadTasks]);

  function openDrawer(task: MaintenanceTaskSummary) {
    setDrawerTask(task);
    setSelectedAssigneeId(task.assignedEmployeeId ?? '');
    setResolutionNote(task.resolutionNote ?? '');
    setDrawerError(null);
    if (selectedPropertyId) {
      fetchManagerEmployeesV1({ propertyId: selectedPropertyId, page: 0, size: 100 })
        .then(data => setEmployees(data.content))
        .catch(() => setEmployees([]));
    }
  }

  async function handleAssign() {
    if (!drawerTask || !selectedAssigneeId) {
      setDrawerError('Vui lòng chọn kỹ thuật viên.');
      return;
    }
    setDrawerSubmitting(true);
    setDrawerError(null);
    try {
      await assignMaintenanceTaskV1(drawerTask.id, selectedAssigneeId);
      setDrawerTask(null);
      setSuccessMsg('Gán kỹ thuật viên thành công.');
      loadTasks();
    } catch (err: unknown) {
      const ax = err as { response?: { data?: { message?: string } } };
      setDrawerError(ax?.response?.data?.message ?? 'Không thể gán kỹ thuật viên.');
    } finally {
      setDrawerSubmitting(false);
    }
  }

  async function handleVerifyClose() {
    if (!drawerTask) return;
    const note = resolutionNote.trim();
    if (note.length < 10) {
      setDrawerError('Ghi chú xác nhận đóng phải có ít nhất 10 ký tự.');
      return;
    }
    if (!window.confirm('Xác nhận đã kiểm tra và đóng yêu cầu bảo trì này?')) return;
    setDrawerSubmitting(true);
    setDrawerError(null);
    try {
      await updateMaintenanceTaskStatusV1(drawerTask.id, {
        status: 'CLOSED',
        resolutionNote: note,
      });
      setDrawerTask(null);
      setSuccessMsg('Đã xác nhận và đóng yêu cầu bảo trì.');
      loadTasks();
    } catch (err: unknown) {
      const ax = err as { response?: { data?: { message?: string } } };
      setDrawerError(ax?.response?.data?.message ?? 'Không thể đóng yêu cầu.');
    } finally {
      setDrawerSubmitting(false);
    }
  }

  const canAssign = drawerTask
    ? drawerTask.status === 'OPEN' || drawerTask.status === 'ASSIGNED'
    : false;
  const canVerifyClose = drawerTask ? drawerTask.status === 'RESOLVED' : false;

  const isEmpty = !loading && tasks.length === 0;

  return (
    <ManagerLayout>
      <div className="space-y-6">
        <div>
          <h1 className="heading-md m-0">Tác vụ bảo trì</h1>
          <p className="text-sm text-[#64748B] mt-1 m-0">
            Quản lý & phân công xử lý sự cố bảo trì theo homestay.
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
            <label className="block text-sm font-medium text-[#334155] mb-1">Tìm kiếm sự cố</label>
            <input
              type="text"
              className="input-field w-full"
              value={search}
              onChange={e => setSearch(e.target.value)}
              placeholder="Nhập tiêu đề sự cố..."
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
            Vui lòng chọn homestay để xem tác vụ bảo trì.
          </div>
        ) : loading ? (
          <div className="h-64 bg-[#F1F5F9] rounded-xl animate-pulse" />
        ) : isEmpty ? (
          <div className="text-center py-16 bg-white rounded-xl border border-[#E2E8F0] text-[#64748B]">
            Không có yêu cầu bảo trì phù hợp.
          </div>
        ) : (
          <div className="bg-white rounded-xl border border-[#E2E8F0] overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="bg-[#F8FAFC] text-[#64748B] text-left text-[12px] uppercase tracking-wide">
                    <th className="px-4 py-3 font-medium">Phòng</th>
                    <th className="px-4 py-3 font-medium">Sự cố</th>
                    <th className="px-4 py-3 font-medium">Người báo</th>
                    <th className="px-4 py-3 font-medium">Người được gán</th>
                    <th className="px-4 py-3 font-medium">Trạng thái</th>
                    <th className="px-4 py-3 font-medium text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  {tasks.map(task => (
                    <tr
                      key={task.id}
                      className="border-t border-[#F1F5F9] hover:bg-[#F8FAFC] cursor-pointer"
                      onClick={() => openDrawer(task)}
                    >
                      <td className="px-4 py-3 font-medium text-[#1E293B] whitespace-nowrap">
                        {task.roomNumber}
                      </td>
                      <td className="px-4 py-3 text-[#334155] max-w-[280px] truncate">{task.title}</td>
                      <td className="px-4 py-3 text-[#64748B] whitespace-nowrap">{task.customerName}</td>
                      <td className="px-4 py-3 text-[#64748B] whitespace-nowrap">
                        {task.assignedEmployeeName || 'Chưa gán'}
                      </td>
                      <td className="px-4 py-3">{badge(task.status)}</td>
                      <td className="px-4 py-3 text-right">
                        <button
                          type="button"
                          className="text-[#0F766E] hover:underline text-sm font-medium"
                          onClick={e => { e.stopPropagation(); openDrawer(task); }}
                        >
                          Xem
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
        isOpen={!!drawerTask}
        onClose={() => setDrawerTask(null)}
        title={drawerTask ? `Phòng ${drawerTask.roomNumber} — Bảo trì` : ''}
        footer={
          drawerTask && (canAssign || canVerifyClose) ? (
            <div className="flex flex-col gap-2 w-full">
              {canAssign && (
                <button
                  type="button"
                  className="btn-primary w-full"
                  disabled={drawerSubmitting}
                  onClick={handleAssign}
                >
                  {drawerSubmitting ? 'Đang lưu...' : 'Gán kỹ thuật viên'}
                </button>
              )}
              {canVerifyClose && (
                <button
                  type="button"
                  className="btn-primary w-full"
                  disabled={drawerSubmitting}
                  onClick={handleVerifyClose}
                >
                  {drawerSubmitting ? 'Đang lưu...' : 'Xác nhận & Đóng'}
                </button>
              )}
            </div>
          ) : undefined
        }
      >
        {drawerTask && (
          <div className="space-y-4">
            {drawerError && <Alert variant="error" message={drawerError} />}

            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-[#64748B]">Trạng thái</span>
                {badge(drawerTask.status)}
              </div>
              <div className="flex justify-between">
                <span className="text-[#64748B]">Người báo</span>
                <span className="font-medium">{drawerTask.customerName}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-[#64748B]">Kỹ thuật viên</span>
                <span className="font-medium">{drawerTask.assignedEmployeeName || 'Chưa gán'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-[#64748B]">Tạo lúc</span>
                <span>{formatDateTime(drawerTask.createdAt)}</span>
              </div>
              {drawerTask.assignedAt && (
                <div className="flex justify-between">
                  <span className="text-[#64748B]">Gán lúc</span>
                  <span>{formatDateTime(drawerTask.assignedAt)}</span>
                </div>
              )}
              {drawerTask.verifiedAt && (
                <div className="flex justify-between">
                  <span className="text-[#64748B]">Đóng lúc</span>
                  <span>{formatDateTime(drawerTask.verifiedAt)}</span>
                </div>
              )}
            </div>

            <div>
              <span className="text-[#64748B] text-sm">Tiêu đề</span>
              <p className="m-0 mt-1 font-medium text-[#1E293B]">{drawerTask.title}</p>
            </div>

            {drawerTask.description && (
              <div>
                <span className="text-[#64748B] text-sm">Mô tả</span>
                <p className="m-0 mt-1 text-[#334155] whitespace-pre-wrap">{drawerTask.description}</p>
              </div>
            )}

            {drawerTask.photoUrls && drawerTask.photoUrls.length > 0 && (
              <div>
                <span className="text-[#64748B] text-sm">Ảnh đính kèm</span>
                <div className="mt-2 grid grid-cols-3 gap-2">
                  {drawerTask.photoUrls.map((url, idx) => (
                    <a key={idx} href={photoUrl(url)} target="_blank" rel="noreferrer">
                      <img
                        src={photoUrl(url)}
                        alt={`Ảnh ${idx + 1}`}
                        className="w-full h-20 object-cover rounded-lg border border-[#E2E8F0]"
                      />
                    </a>
                  ))}
                </div>
              </div>
            )}

            {canAssign && (
              <div>
                <label className="block text-sm font-medium text-[#334155] mb-1">Gán kỹ thuật viên</label>
                <select
                  className="input-field w-full"
                  value={selectedAssigneeId}
                  onChange={e => setSelectedAssigneeId(e.target.value)}
                >
                  <option value="">— Chọn kỹ thuật viên —</option>
                  {employees.map(emp => (
                    <option key={emp.id} value={emp.id}>{emp.fullName}</option>
                  ))}
                </select>
              </div>
            )}

            {canVerifyClose && (
              <div>
                <label className="block text-sm font-medium text-[#334155] mb-1">
                  Ghi chú xử lý (tuỳ chọn)
                </label>
                <textarea
                  className="input-field w-full min-h-[80px]"
                  value={resolutionNote}
                  onChange={e => setResolutionNote(e.target.value)}
                  placeholder="Ghi chú khi xác nhận đóng yêu cầu..."
                />
              </div>
            )}

            {!canAssign && !canVerifyClose && drawerTask.resolutionNote && (
              <div>
                <span className="text-[#64748B] text-sm">Ghi chú xử lý</span>
                <p className="m-0 mt-1 text-[#334155] whitespace-pre-wrap">{drawerTask.resolutionNote}</p>
              </div>
            )}
          </div>
        )}
      </Drawer>
    </ManagerLayout>
  );
}
