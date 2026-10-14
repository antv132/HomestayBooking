import { useState, useEffect, useCallback } from 'react';
import EmployeeLayout from '../../layouts/EmployeeLayout';
import {
  fetchEmployeeHousekeepingTasks,
  startEmployeeHousekeepingTask,
  finishEmployeeHousekeepingTask,
  type EmployeeHousekeepingTask,
} from '../../api/housekeepingApi';
import { TOUCH, fmtDate, extractErr, Spinner, ErrBanner, StatusBadge } from './EmployeeShared';

function fmtTime(s?: string | null) {
  if (!s) return null;
  return new Date(s).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
}

function hkNext(status: EmployeeHousekeepingTask['status']): 'IN_PROGRESS' | 'COMPLETED' | null {
  if (status === 'PENDING') return 'IN_PROGRESS';
  if (status === 'IN_PROGRESS') return 'COMPLETED';
  return null;
}

function hkButtonLabel(status: EmployeeHousekeepingTask['status']): string | null {
  if (status === 'PENDING') return 'Start';
  if (status === 'IN_PROGRESS') return 'Finish';
  return null;
}

/** SCR-60 — Housekeeping Workspace (Employee) */
export default function HousekeepingWorkspacePage() {
  const [tasks, setTasks] = useState<EmployeeHousekeepingTask[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [updating, setUpdating] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState('');
  const [toastMsg, setToastMsg] = useState<string | null>(null);

  const showToast = (msg: string) => {
    setToastMsg(msg);
    setTimeout(() => setToastMsg(null), 2400);
  };

  const load = useCallback(async (isRefresh = false) => {
    if (isRefresh) setRefreshing(true);
    else setLoading(true);
    setError(null);
    try {
      const data = await fetchEmployeeHousekeepingTasks({
        size: 50,
        status: statusFilter || undefined,
      });
      setTasks(data.content);
    } catch (err) {
      setError(extractErr(err, 'Không tải được danh sách.'));
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [statusFilter]);

  useEffect(() => {
    load();
  }, [load]);

  async function handleAction(task: EmployeeHousekeepingTask) {
    const next = hkNext(task.status);
    if (!next || updating) return;

    setTasks((prev) => prev.map((t) => (t.id === task.id ? { ...t, status: next } : t)));
    setUpdating(task.id);
    try {
      if (next === 'IN_PROGRESS') {
        await startEmployeeHousekeepingTask(task.id);
        showToast('Started cleaning');
      } else {
        await finishEmployeeHousekeepingTask(task.id);
        showToast('Cleaning completed');
      }
      await load(true);
    } catch (err) {
      setTasks((prev) => prev.map((t) => (t.id === task.id ? { ...t, status: task.status } : t)));
      setError(extractErr(err, 'Cập nhật thất bại. Đã hoàn tác.'));
    } finally {
      setUpdating(null);
    }
  }

  const STATUS_FILTERS = [
    { v: '', label: 'Tất cả' },
    { v: 'PENDING', label: 'Pending' },
    { v: 'IN_PROGRESS', label: 'In Progress' },
    { v: 'COMPLETED', label: 'Done' },
  ];

  return (
    <EmployeeLayout>
      <div className="animate-fade-in">
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
          <div>
            <h1 style={{ fontFamily: 'Outfit', fontSize: 22, fontWeight: 700, color: 'var(--ink)', marginBottom: 2 }}>
              Housekeeping
            </h1>
          </div>
          <button
            onClick={() => load(true)}
            disabled={refreshing}
            style={{
              ...TOUCH,
              background: 'var(--surface-bone)',
              border: '1px solid var(--hairline)',
              borderRadius: 10,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              padding: '0 14px',
              gap: 6,
              fontSize: 13,
              fontWeight: 600,
              color: 'var(--charcoal)',
            }}
          >
            <svg
              width="15"
              height="15"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              style={{ animation: refreshing ? 'spin 0.8s linear infinite' : 'none' }}
            >
              <polyline points="23 4 23 10 17 10" />
              <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10" />
            </svg>
            {refreshing ? 'Loading...' : 'Refresh'}
          </button>
        </div>

        {error && <ErrBanner msg={error} />}

        <div style={{ display: 'flex', gap: 8, marginBottom: 14, overflowX: 'auto', paddingBottom: 4 }}>
          {STATUS_FILTERS.map((f) => (
            <button
              key={f.v}
              onClick={() => setStatusFilter(f.v)}
              style={{
                ...TOUCH,
                padding: '0 16px',
                borderRadius: 20,
                border: `1.5px solid ${statusFilter === f.v ? 'var(--primary)' : 'var(--hairline)'}`,
                background: statusFilter === f.v ? 'rgba(15,118,110,0.10)' : 'var(--surface-card)',
                color: statusFilter === f.v ? 'var(--primary)' : 'var(--charcoal)',
                fontWeight: statusFilter === f.v ? 700 : 400,
                fontSize: 13,
                cursor: 'pointer',
                whiteSpace: 'nowrap',
                flexShrink: 0,
              }}
            >
              {f.label}
            </button>
          ))}
        </div>

        {loading ? (
          <Spinner />
        ) : tasks.length === 0 ? (
          <div
            className="card"
            style={{
              padding: 40,
              textAlign: 'center',
              background: 'var(--surface-card)',
              border: '1px solid var(--hairline)',
            }}
          >
            <p style={{ fontWeight: 600, color: 'var(--ink)', marginBottom: 4 }}>
              No rooms assigned to clean.
            </p>
            <p className="body-sm text-charcoal">Assigned housekeeping tasks will appear here.</p>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {tasks.map((task) => {
              const nextLabel = hkButtonLabel(task.status);
              const isUpdating = updating === task.id;
              const isFinish = task.status === 'IN_PROGRESS';
              const started = fmtTime(task.startedAt);
              const completed = fmtTime(task.completedAt);

              return (
                <div
                  key={task.id}
                  className="card"
                  style={{
                    padding: 16,
                    display: 'flex',
                    alignItems: 'center',
                    gap: 14,
                    background: 'var(--surface-card)',
                    border: '1px solid var(--hairline)',
                    borderLeft:
                      task.status === 'IN_PROGRESS'
                        ? '4px solid var(--primary)'
                        : task.status === 'COMPLETED'
                          ? '4px solid var(--success)'
                          : '4px solid var(--hairline)',
                    opacity: task.status === 'COMPLETED' || task.status === 'CANCELLED' ? 0.65 : 1,
                    transition: 'opacity 0.2s, border-color 0.2s',
                  }}
                >
                  <div
                    style={{
                      width: 44,
                      height: 44,
                      borderRadius: 10,
                      background:
                        task.status === 'IN_PROGRESS' ? 'rgba(15,118,110,0.10)' : 'var(--surface-bone)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: 13,
                      fontWeight: 700,
                      color: 'var(--ink)',
                      flexShrink: 0,
                    }}
                  >
                    {task.roomNumber || '—'}
                  </div>

                  <div style={{ flex: 1, minWidth: 0 }}>
                    <p style={{ fontWeight: 700, fontSize: 15, color: 'var(--ink)', marginBottom: 4 }}>
                      Room {task.roomNumber || '—'}
                    </p>
                    {task.floorName && <p className="body-sm text-charcoal">{task.floorName}</p>}
                    {task.note && (
                      <p
                        className="body-sm text-charcoal"
                        style={{
                          marginTop: 4,
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          whiteSpace: 'nowrap',
                        }}
                      >
                        {task.note}
                      </p>
                    )}
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 6, flexWrap: 'wrap' }}>
                      <StatusBadge status={task.status} />
                      {started && <span className="body-sm text-charcoal">Started {started}</span>}
                      {completed && <span className="body-sm text-charcoal">Done {completed}</span>}
                      {!started && task.createdAt && (
                        <span className="body-sm text-charcoal">{fmtDate(task.createdAt)}</span>
                      )}
                    </div>
                  </div>

                  {nextLabel && (
                    <button
                      className={isFinish ? undefined : 'btn-primary'}
                      onClick={() => handleAction(task)}
                      disabled={isUpdating}
                      style={{
                        ...TOUCH,
                        padding: '0 16px',
                        borderRadius: 10,
                        flexShrink: 0,
                        fontSize: 13,
                        fontWeight: 600,
                        whiteSpace: 'nowrap',
                        border: 'none',
                        cursor: isUpdating ? 'not-allowed' : 'pointer',
                        ...(isFinish
                          ? {
                              background: 'var(--success)',
                              color: '#fff',
                            }
                          : {}),
                      }}
                    >
                      {isUpdating ? '...' : nextLabel}
                    </button>
                  )}
                  {task.status === 'COMPLETED' && (
                    <div style={{ color: 'var(--success)', fontSize: 22, flexShrink: 0 }}>✓</div>
                  )}
                </div>
              );
            })}
          </div>
        )}

        {toastMsg && (
          <div
            style={{
              position: 'fixed',
              bottom: 24,
              left: '50%',
              transform: 'translateX(-50%)',
              background: 'var(--ink)',
              color: '#fff',
              padding: '10px 20px',
              borderRadius: 30,
              fontSize: 14,
              fontWeight: 600,
              zIndex: 2000,
              boxShadow: '0 4px 20px rgba(0,0,0,0.2)',
            }}
          >
            {toastMsg}
          </div>
        )}
      </div>
    </EmployeeLayout>
  );
}
