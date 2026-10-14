import { useState, useEffect, useCallback, useMemo } from 'react';
import { Link } from 'react-router-dom';
import ManagerLayout from '../../layouts/ManagerLayout';
import Alert from '../../components/ui/Alert';
import Modal from '../../components/ui/Modal';
import { Drawer } from '../../components/ui';
import { StatusBadge } from '../../components/ui';
import type { StatusVariant } from '../../components/ui/StatusBadge';
import {
  fetchManagerHousekeepingTasksV1,
  createHousekeepingTaskV1,
  assignHousekeepingTaskV1,
  cancelHousekeepingTaskV1,
  type HousekeepingTaskSummary,
} from '../../api/housekeepingApi';
import { fetchManagerEmployeesV1 } from '../../api/managerEmployeeApi';
import { fetchManagerRoomsV1 } from '../../api/roomsApi';
import { managerApi } from '../../api/managerApi';
import type { AssignedProperty } from '../../api/reportApi';
import type { EmployeeSummary } from '../../api/managerEmployeeApi';
import type { RoomListItem } from '../../api/roomsApi';

const STATUS_VI: Record<string, { label: string; variant: StatusVariant }> = {
  PENDING:     { label: 'Chờ xử lý', variant: 'warning' },
  IN_PROGRESS: { label: 'Đang dọn',  variant: 'info' },
  COMPLETED:   { label: 'Hoàn tất',  variant: 'success' },
  CANCELLED:   { label: 'Đã hủy',    variant: 'neutral' },
};

const BOARD_COLUMNS = [
  { key: 'PENDING', title: 'Chờ xử lý' },
  { key: 'IN_PROGRESS', title: 'Đang dọn' },
  { key: 'COMPLETED', title: 'Hoàn tất' },
  { key: 'CANCELLED', title: 'Đã hủy' },
] as const;

const STATUS_FILTERS = [
  { value: '', label: 'Tất cả' },
  { value: 'PENDING', label: 'Chờ xử lý' },
  { value: 'IN_PROGRESS', label: 'Đang dọn' },
  { value: 'COMPLETED', label: 'Hoàn tất' },
  { value: 'CANCELLED', label: 'Đã hủy' },
];

function todayStr(): string {
  return new Date().toISOString().slice(0, 10);
}

function daysAgoStr(days: number): string {
  const d = new Date();
  d.setDate(d.getDate() - days);
  return d.toISOString().slice(0, 10);
}

function formatDateTime(iso: string): string {
  if (!iso) return '—';
  const d = new Date(iso);
  return d.toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function TaskCard({
  task,
  onClick,
}: {
  task: HousekeepingTaskSummary;
  onClick: () => void;
}) {
  const cfg = STATUS_VI[task.status] ?? { label: task.status, variant: 'neutral' as StatusVariant };
  return (
    <button
      type="button"
      onClick={onClick}
      className="w-full text-left bg-white border border-[#E2E8F0] rounded-lg p-4 hover:border-[#0F766E] hover:shadow-sm transition-all"
    >
      <div className="flex items-start justify-between gap-2 mb-2">
        <span className="font-semibold text-[#1E293B]">Phòng {task.roomNumber}</span>
        <StatusBadge status={cfg.label} variant={cfg.variant} />
      </div>
      <p className="text-sm text-[#64748B] m-0">
        {task.assigneeName || 'Chưa gán'}
      </p>
      <p className="text-xs text-[#94A3B8] m-0 mt-2">{formatDateTime(task.createdAt)}</p>
    </button>
  );
}

function KanbanColumn({
  title,
  tasks,
  onTaskClick,
}: {
  title: string;
  tasks: HousekeepingTaskSummary[];
  onTaskClick: (task: HousekeepingTaskSummary) => void;
}) {
  return (
    <div className="flex-1 min-w-[260px] bg-[#F8FAFC] rounded-xl border border-[#E2E8F0] p-3">
      <div className="flex items-center justify-between mb-3 px-1">
        <h3 className="text-sm font-semibold text-[#334155] m-0">{title}</h3>
        <span className="text-xs text-[#64748B] bg-white border border-[#E2E8F0] rounded-full px-2 py-0.5">
          {tasks.length}
        </span>
      </div>
      <div className="space-y-3 max-h-[calc(100vh-320px)] overflow-y-auto">
        {tasks.length === 0 ? (
          <p className="text-sm text-[#94A3B8] text-center py-8 m-0">Không có tác vụ</p>
        ) : (
          tasks.map(task => (
            <TaskCard key={task.id} task={task} onClick={() => onTaskClick(task)} />
          ))
        )}
      </div>
    </div>
  );
}

export default function HousekeepingTasksPage() {
  const [properties, setProperties] = useState<AssignedProperty[]>([]);
  const [selectedPropertyId, setSelectedPropertyId] = useState('');
  const [fromDate, setFromDate] = useState(daysAgoStr(6));
  const [toDate, setToDate] = useState(todayStr());
  const [statusFilter, setStatusFilter] = useState('');

  const [tasks, setTasks] = useState<HousekeepingTaskSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  const [drawerTask, setDrawerTask] = useState<HousekeepingTaskSummary | null>(null);
  const [employees, setEmployees] = useState<EmployeeSummary[]>([]);
  const [selectedAssigneeId, setSelectedAssigneeId] = useState('');
  const [cancelNote, setCancelNote] = useState('');
  const [drawerSubmitting, setDrawerSubmitting] = useState(false);
  const [drawerError, setDrawerError] = useState<string | null>(null);

  const [createOpen, setCreateOpen] = useState(false);
  const [rooms, setRooms] = useState<RoomListItem[]>([]);
  const [createRoomId, setCreateRoomId] = useState('');
  const [createAssigneeId, setCreateAssigneeId] = useState('');
  const [createSubmitting, setCreateSubmitting] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

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

  const loadTasks = useCallback(() => {
    if (!selectedPropertyId) {
      setTasks([]);
      return;
    }
    setLoading(true);
    setError(null);
    fetchManagerHousekeepingTasksV1({
      propertyId: selectedPropertyId,
      status: statusFilter || undefined,
      fromDate,
      toDate,
      page: 0,
      size: 100,
    })
      .then(data => setTasks(data.content))
      .catch((err: unknown) => {
        const ax = err as { response?: { status?: number; data?: { message?: string } } };
        if (ax?.response?.status === 403) {
          setError('Bạn không có quyền xem homestay này.');
        } else {
          setError(ax?.response?.data?.message ?? 'Không thể tải danh sách tác vụ.');
        }
        setTasks([]);
      })
      .finally(() => setLoading(false));
  }, [selectedPropertyId, statusFilter, fromDate, toDate]);

  useEffect(() => {
    loadTasks();
  }, [loadTasks]);

  const grouped = useMemo(() => ({
    PENDING: tasks.filter(t => t.status === 'PENDING'),
    IN_PROGRESS: tasks.filter(t => t.status === 'IN_PROGRESS'),
    COMPLETED: tasks.filter(t => t.status === 'COMPLETED'),
    CANCELLED: tasks.filter(t => t.status === 'CANCELLED'),
  }), [tasks]);

  const isEmpty = !loading && tasks.length === 0;

  function openDrawer(task: HousekeepingTaskSummary) {
    setDrawerTask(task);
    setSelectedAssigneeId(task.assigneeId ?? '');
    setCancelNote('');
    setDrawerError(null);
    if (selectedPropertyId) {
      fetchManagerEmployeesV1({ propertyId: selectedPropertyId, page: 0, size: 100 })
        .then(data => setEmployees(data.content))
        .catch(() => setEmployees([]));
    }
  }

  function openCreateModal() {
    if (!selectedPropertyId) {
      setError('Vui lòng chọn homestay trước.');
      return;
    }
    setCreateRoomId('');
    setCreateAssigneeId('');
    setCreateError(null);
    setCreateOpen(true);
    Promise.all([
      fetchManagerRoomsV1({ propertyId: selectedPropertyId, page: 0, size: 200 }),
      fetchManagerEmployeesV1({ propertyId: selectedPropertyId, page: 0, size: 100 }),
    ])
      .then(([roomsData, empData]) => {
        const eligible = roomsData.content.filter(
          r => r.status === 'PENDING_CLEANING' || r.status === 'CLEANING_IN_PROGRESS',
        );
        setRooms(eligible);
        setEmployees(empData.content);
      })
      .catch(() => {
        setRooms([]);
        setEmployees([]);
      });
  }

  async function handleAssign() {
    if (!drawerTask || !selectedAssigneeId) {
      setDrawerError('Vui lòng chọn nhân viên.');
      return;
    }
    setDrawerSubmitting(true);
    setDrawerError(null);
    try {
      await assignHousekeepingTaskV1(drawerTask.id, selectedAssigneeId);
      setDrawerTask(null);
      setSuccessMsg('Gán nhân viên thành công.');
      loadTasks();
    } catch (err: unknown) {
      const ax = err as { response?: { data?: { message?: string } } };
      setDrawerError(ax?.response?.data?.message ?? 'Không thể gán nhân viên.');
    } finally {
      setDrawerSubmitting(false);
    }
  }

  async function handleCancel() {
    if (!drawerTask) return;
    if (!window.confirm('Bạn có chắc muốn hủy tác vụ này?')) return;
    setDrawerSubmitting(true);
    setDrawerError(null);
    try {
      await cancelHousekeepingTaskV1(drawerTask.id, cancelNote.trim() || undefined);
      setDrawerTask(null);
      setSuccessMsg('Đã hủy tác vụ.');
      loadTasks();
    } catch (err: unknown) {
      const ax = err as { response?: { data?: { message?: string } } };
      setDrawerError(ax?.response?.data?.message ?? 'Không thể hủy tác vụ.');
    } finally {
      setDrawerSubmitting(false);
    }
  }

  async function handleCreate() {
    if (!createRoomId) {
      setCreateError('Vui lòng chọn phòng.');
      return;
    }
    setCreateSubmitting(true);
    setCreateError(null);
    try {
      await createHousekeepingTaskV1({
        roomId: createRoomId,
        assigneeId: createAssigneeId || undefined,
      });
      setCreateOpen(false);
      setSuccessMsg('Tạo tác vụ thành công.');
      loadTasks();
    } catch (err: unknown) {
      const ax = err as { response?: { data?: { message?: string } } };
      setCreateError(ax?.response?.data?.message ?? 'Không thể tạo tác vụ.');
    } finally {
      setCreateSubmitting(false);
    }
  }

  const drawerReadOnly = drawerTask
    ? drawerTask.status === 'COMPLETED' || drawerTask.status === 'CANCELLED'
    : true;

  return (
    <ManagerLayout>
      <div className="space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h1 className="heading-md m-0">Tác vụ dọn phòng</h1>
            <p className="text-sm text-[#64748B] mt-1 m-0">
              Kanban quản lý dọn phòng —{' '}
              <Link to="/manager/housekeeping/schedule" className="text-[#0F766E] hover:underline">
                Xem lịch phân công
              </Link>
            </p>
          </div>
          <button type="button" className="btn-primary" onClick={openCreateModal}>
            Tạo tác vụ
          </button>
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
          <div>
            <label className="block text-sm font-medium text-[#334155] mb-1">Từ ngày</label>
            <input
              type="date"
              className="input-field"
              value={fromDate}
              onChange={e => setFromDate(e.target.value)}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-[#334155] mb-1">Đến ngày</label>
            <input
              type="date"
              className="input-field"
              value={toDate}
              onChange={e => setToDate(e.target.value)}
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
            Vui lòng chọn homestay để xem tác vụ dọn phòng.
          </div>
        ) : loading ? (
          <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4">
            {BOARD_COLUMNS.map(col => (
              <div key={col.key} className="h-64 bg-[#F1F5F9] rounded-xl animate-pulse" />
            ))}
          </div>
        ) : isEmpty ? (
          <div className="text-center py-16 bg-white rounded-xl border border-[#E2E8F0]">
            <p className="text-[#64748B] mb-4">Không có tác vụ dọn phòng trong khoảng thời gian này.</p>
            <button type="button" className="btn-primary" onClick={openCreateModal}>
              Tạo tác vụ
            </button>
          </div>
        ) : (
          <div className="flex flex-col lg:flex-row gap-4 overflow-x-auto">
            {BOARD_COLUMNS.map(col => (
              <KanbanColumn
                key={col.key}
                title={col.title}
                tasks={grouped[col.key]}
                onTaskClick={openDrawer}
              />
            ))}
          </div>
        )}
      </div>

      <Drawer
        isOpen={!!drawerTask}
        onClose={() => setDrawerTask(null)}
        title={drawerTask ? `Phòng ${drawerTask.roomNumber}` : ''}
        footer={
          drawerTask && !drawerReadOnly ? (
            <div className="flex flex-col gap-2 w-full">
              <button
                type="button"
                className="btn-primary w-full"
                disabled={drawerSubmitting}
                onClick={handleAssign}
              >
                {drawerSubmitting ? 'Đang lưu...' : 'Gán nhân viên'}
              </button>
              <button
                type="button"
                className="btn-outline w-full text-[#DC2626] border-[#DC2626]"
                disabled={drawerSubmitting}
                onClick={handleCancel}
              >
                Hủy tác vụ
              </button>
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
                <StatusBadge
                  status={STATUS_VI[drawerTask.status]?.label ?? drawerTask.status}
                  variant={STATUS_VI[drawerTask.status]?.variant ?? 'neutral'}
                />
              </div>
              <div className="flex justify-between">
                <span className="text-[#64748B]">Nhân viên</span>
                <span className="font-medium">{drawerTask.assigneeName || 'Chưa gán'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-[#64748B]">Tạo lúc</span>
                <span>{formatDateTime(drawerTask.createdAt)}</span>
              </div>
              {drawerTask.startedAt && (
                <div className="flex justify-between">
                  <span className="text-[#64748B]">Bắt đầu</span>
                  <span>{formatDateTime(drawerTask.startedAt)}</span>
                </div>
              )}
              {drawerTask.completedAt && (
                <div className="flex justify-between">
                  <span className="text-[#64748B]">Hoàn tất</span>
                  <span>{formatDateTime(drawerTask.completedAt)}</span>
                </div>
              )}
              {drawerTask.note && (
                <div>
                  <span className="text-[#64748B]">Ghi chú</span>
                  <p className="m-0 mt-1 text-[#334155]">{drawerTask.note}</p>
                </div>
              )}
            </div>

            {!drawerReadOnly && (
              <>
                <div>
                  <label className="block text-sm font-medium text-[#334155] mb-1">Gán nhân viên</label>
                  <select
                    className="input-field w-full"
                    value={selectedAssigneeId}
                    onChange={e => setSelectedAssigneeId(e.target.value)}
                  >
                    <option value="">— Chọn nhân viên —</option>
                    {employees.map(emp => (
                      <option key={emp.id} value={emp.id}>{emp.fullName}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-[#334155] mb-1">Lý do hủy (tuỳ chọn)</label>
                  <textarea
                    className="input-field w-full min-h-[80px]"
                    value={cancelNote}
                    onChange={e => setCancelNote(e.target.value)}
                    placeholder="Nhập lý do hủy tác vụ..."
                  />
                </div>
              </>
            )}
          </div>
        )}
      </Drawer>

      <Modal
        isOpen={createOpen}
        onClose={() => setCreateOpen(false)}
        title="Tạo tác vụ dọn phòng"
        actions={[
          { label: 'Hủy', onClick: () => setCreateOpen(false), variant: 'ghost' },
          {
            label: createSubmitting ? 'Đang tạo...' : 'Tạo',
            onClick: handleCreate,
            variant: 'primary',
          },
        ]}
      >
        {createError && <Alert variant="error" message={createError} />}
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-[#334155] mb-1">Phòng *</label>
            <select
              className="input-field w-full"
              value={createRoomId}
              onChange={e => setCreateRoomId(e.target.value)}
            >
              <option value="">— Chọn phòng —</option>
              {rooms.map(r => (
                <option key={r.id} value={r.id}>
                  {r.roomNumber} ({r.status === 'PENDING_CLEANING' ? 'Chờ dọn' : 'Đang dọn'})
                </option>
              ))}
            </select>
            {rooms.length === 0 && (
              <p className="text-xs text-[#94A3B8] mt-1">Không có phòng chờ dọn hoặc đang dọn.</p>
            )}
          </div>
          <div>
            <label className="block text-sm font-medium text-[#334155] mb-1">Nhân viên (tuỳ chọn)</label>
            <select
              className="input-field w-full"
              value={createAssigneeId}
              onChange={e => setCreateAssigneeId(e.target.value)}
            >
              <option value="">— Chưa gán —</option>
              {employees.map(emp => (
                <option key={emp.id} value={emp.id}>{emp.fullName}</option>
              ))}
            </select>
          </div>
        </div>
      </Modal>
    </ManagerLayout>
  );
}
