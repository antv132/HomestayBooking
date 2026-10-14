import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import ManagerLayout from '../../layouts/ManagerLayout';
import Alert from '../../components/ui/Alert';
import { StatusBadge } from '../../components/ui';
import type { StatusVariant } from '../../components/ui/StatusBadge';
import {
  fetchHousekeepingScheduleV1,
  assignHousekeepingTaskV1,
  type HousekeepingSchedule,
  type HousekeepingTaskSummary,
} from '../../api/housekeepingApi';
import { managerApi } from '../../api/managerApi';
import type { AssignedProperty } from '../../api/reportApi';

const STATUS_VI: Record<string, { label: string; variant: StatusVariant }> = {
  PENDING:     { label: 'Chờ xử lý', variant: 'warning' },
  IN_PROGRESS: { label: 'Đang dọn',  variant: 'info' },
  COMPLETED:   { label: 'Hoàn tất',  variant: 'success' },
  CANCELLED:   { label: 'Đã hủy',    variant: 'neutral' },
};

function todayStr(): string {
  return new Date().toISOString().slice(0, 10);
}

function formatTime(iso: string): string {
  if (!iso) return '—';
  const d = new Date(iso);
  return d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
}

function canDrag(task: HousekeepingTaskSummary): boolean {
  return task.status === 'PENDING' || task.status === 'IN_PROGRESS';
}

function KpiCard({ label, value, accent }: { label: string; value: number; accent?: string }) {
  return (
    <div className="bg-white rounded-[12px] border border-[#E2E8F0] p-4 shadow-sm">
      <p className="text-xs text-[#64748B]">{label}</p>
      <p className={`text-2xl font-bold mt-1 ${accent ?? 'text-[#1E293B]'}`}>{value}</p>
    </div>
  );
}

function TaskCard({
  task,
  onDragStart,
}: {
  task: HousekeepingTaskSummary;
  onDragStart: (task: HousekeepingTaskSummary) => void;
}) {
  const cfg = STATUS_VI[task.status] ?? { label: task.status, variant: 'neutral' as StatusVariant };
  const draggable = canDrag(task);

  return (
    <div
      draggable={draggable}
      onDragStart={e => {
        if (!draggable) {
          e.preventDefault();
          return;
        }
        e.dataTransfer.setData('text/task-id', task.id);
        onDragStart(task);
      }}
      className={`bg-white rounded-md border border-[#E2E8F0] p-3 shadow-sm ${
        draggable ? 'cursor-grab active:cursor-grabbing hover:border-[#0F766E]/40' : 'opacity-80'
      }`}
    >
      <div className="flex items-center justify-between gap-2 mb-1">
        <span className="font-semibold text-[#1E293B]">Phòng {task.roomNumber}</span>
        <StatusBadge status={cfg.label} variant={cfg.variant} />
      </div>
      <p className="text-xs text-[#64748B]">{formatTime(task.createdAt)}</p>
      {task.assigneeName && (
        <p className="text-xs text-[#64748B] mt-1">{task.assigneeName}</p>
      )}
    </div>
  );
}

function ScheduleColumn({
  title,
  subtitle,
  tasks,
  employeeId,
  onAssign,
  onDragStart,
  dragMessage,
}: {
  title: string;
  subtitle?: string;
  tasks: HousekeepingTaskSummary[];
  employeeId?: string;
  onAssign: (taskId: string, assigneeId: string) => void;
  onDragStart: (task: HousekeepingTaskSummary) => void;
  dragMessage?: string;
}) {
  function handleDragOver(e: React.DragEvent) {
    if (employeeId) {
      e.preventDefault();
    }
  }

  function handleDrop(e: React.DragEvent) {
    e.preventDefault();
    if (!employeeId) return;
    const taskId = e.dataTransfer.getData('text/task-id');
    if (taskId) {
      onAssign(taskId, employeeId);
    }
  }

  return (
    <div
      className="flex-shrink-0 w-64 bg-[#F8FAFC] rounded-[12px] border border-[#E2E8F0] p-3 min-h-[320px]"
      onDragOver={handleDragOver}
      onDrop={handleDrop}
    >
      <div className="mb-3">
        <h3 className="font-semibold text-sm text-[#1E293B]">{title}</h3>
        {subtitle && <p className="text-xs text-[#64748B]">{subtitle}</p>}
        {dragMessage && !employeeId && (
          <p className="text-xs text-[#B45309] mt-1">{dragMessage}</p>
        )}
      </div>
      <div className="space-y-2">
        {tasks.length === 0 ? (
          <p className="text-xs text-[#94A3B8] text-center py-6">Không có tác vụ</p>
        ) : (
          tasks.map(t => (
            <TaskCard key={t.id} task={t} onDragStart={onDragStart} />
          ))
        )}
      </div>
    </div>
  );
}

export default function HousekeepingSchedulePage() {
  const [properties, setProperties] = useState<AssignedProperty[]>([]);
  const [selectedPropertyId, setSelectedPropertyId] = useState('');
  const [selectedDate, setSelectedDate] = useState(todayStr());
  const [schedule, setSchedule] = useState<HousekeepingSchedule | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [assigning, setAssigning] = useState(false);

  useEffect(() => {
    managerApi.getMyAssignedProperties().then(res => {
      const list = res.data ?? [];
      setProperties(list);
      if (list.length > 0) {
        setSelectedPropertyId(prev => prev || list[0].id);
      }
    }).catch(() => setLoadError('Không thể tải danh sách homestay.'));
  }, []);

  const loadSchedule = useCallback(async () => {
    if (!selectedPropertyId) {
      setSchedule(null);
      setLoading(false);
      return;
    }
    setLoading(true);
    setLoadError(null);
    try {
      const data = await fetchHousekeepingScheduleV1(selectedPropertyId, selectedDate);
      setSchedule(data);
    } catch (err: unknown) {
      const ax = err as { response?: { status?: number; data?: { message?: string } } };
      if (ax?.response?.status === 403) {
        setLoadError('Bạn không có quyền xem lịch dọn phòng homestay này.');
      } else {
        setLoadError(ax?.response?.data?.message ?? 'Không thể tải lịch dọn phòng.');
      }
      setSchedule(null);
    } finally {
      setLoading(false);
    }
  }, [selectedPropertyId, selectedDate]);

  useEffect(() => { loadSchedule(); }, [loadSchedule]);

  async function handleAssign(taskId: string, assigneeId: string) {
    setAssigning(true);
    setActionError(null);
    try {
      await assignHousekeepingTaskV1(taskId, assigneeId);
      await loadSchedule();
    } catch (err: unknown) {
      const ax = err as { response?: { data?: { message?: string } } };
      setActionError(ax?.response?.data?.message ?? 'Không thể gán nhân viên.');
    } finally {
      setAssigning(false);
    }
  }

  const summary = schedule?.summary;
  const isEmpty = schedule
    && summary
    && summary.pending === 0
    && summary.inProgress === 0
    && summary.completed === 0;

  return (
    <ManagerLayout>
      <div className="space-y-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h1 className="font-display text-[28px] font-bold text-[#1E293B]">Lịch dọn phòng</h1>
            <p className="text-sm text-[#64748B] mt-1">
              Phân công dọn phòng theo nhân viên — kéo thả tác vụ để gán
            </p>
          </div>
          <Link
            to="/manager/housekeeping/tasks"
            className="text-sm text-[#0F766E] hover:underline font-medium"
          >
            Quản lý tác vụ →
          </Link>
        </div>

        {loadError && (
          <Alert variant="error" message={loadError} closeable onClose={() => setLoadError(null)} />
        )}
        {actionError && (
          <Alert variant="error" message={actionError} closeable onClose={() => setActionError(null)} />
        )}

        <div className="flex flex-wrap gap-3 items-end bg-white rounded-[16px] border border-[#E2E8F0] p-4">
          <div className="min-w-[200px]">
            <label className="form-label" htmlFor="hk-property">Homestay</label>
            <select
              id="hk-property"
              className="input w-full"
              value={selectedPropertyId}
              onChange={e => setSelectedPropertyId(e.target.value)}
            >
              {properties.length === 0 && <option value="">— Chưa có homestay —</option>}
              {properties.map(p => (
                <option key={p.id} value={p.id}>{p.name}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="form-label" htmlFor="hk-date">Ngày</label>
            <input
              id="hk-date"
              type="date"
              className="input"
              value={selectedDate}
              onChange={e => setSelectedDate(e.target.value)}
            />
          </div>
          <button
            type="button"
            className="btn-outline btn-sm"
            onClick={loadSchedule}
            disabled={loading || assigning}
          >
            Làm mới
          </button>
        </div>

        {loading ? (
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 animate-pulse">
            {Array.from({ length: 4 }).map((_, i) => (
              <div key={i} className="h-20 bg-[#E2E8F0] rounded-[12px]" />
            ))}
          </div>
        ) : schedule && summary && (
          <>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
              <KpiCard label="Chờ xử lý" value={summary.pending} accent="text-amber-600" />
              <KpiCard label="Đang dọn" value={summary.inProgress} accent="text-blue-600" />
              <KpiCard label="Hoàn tất" value={summary.completed} accent="text-emerald-600" />
              <KpiCard label="Chưa gán" value={summary.unassigned} accent="text-[#1E293B]" />
            </div>

            {isEmpty ? (
              <div className="bg-white rounded-[16px] border border-[#E2E8F0] p-12 text-center">
                <p className="text-[#64748B]">Không có tác vụ dọn phòng trong ngày này.</p>
                <p className="text-xs text-[#94A3B8] mt-2">
                  Tác vụ được tạo tự động sau khi khách trả phòng (check-out).
                </p>
              </div>
            ) : (
              <div className="overflow-x-auto pb-2">
                <div className="flex gap-4 min-w-max">
                  <ScheduleColumn
                    title="Chưa gán"
                    subtitle={`${schedule.unassigned.length} tác vụ`}
                    tasks={schedule.unassigned}
                    onAssign={handleAssign}
                    onDragStart={() => {}}
                    dragMessage="Chỉ kéo sang cột nhân viên để gán"
                  />
                  {schedule.employees.map(emp => (
                    <ScheduleColumn
                      key={emp.employeeId}
                      title={emp.employeeName}
                      subtitle={`${emp.tasks.length} tác vụ`}
                      tasks={emp.tasks}
                      employeeId={emp.employeeId}
                      onAssign={handleAssign}
                      onDragStart={() => {}}
                    />
                  ))}
                </div>
              </div>
            )}
          </>
        )}

        {assigning && (
          <p className="text-sm text-[#64748B] text-center">Đang gán nhân viên…</p>
        )}
      </div>
    </ManagerLayout>
  );
}
