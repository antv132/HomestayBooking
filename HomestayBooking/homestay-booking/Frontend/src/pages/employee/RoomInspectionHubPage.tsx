import { useState, useEffect, useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import EmployeeLayout from '../../layouts/EmployeeLayout';
import {
  getEmployeeInspections, getChecklistItems, passInspection, failInspection,
  type ChecklistItemDefinition, type InspectionSummary,
} from '../../api/employeeApi';
import { useAuthStore } from '../../store/authStore';
import { TOUCH, extractErr, Spinner, ErrBanner, StatusBadge, Drawer } from './EmployeeShared';

export default function RoomInspectionHubPage() {
  const navigate = useNavigate();
  const userId = useAuthStore(s => s.userId);

  const [inspections, setInspections] = useState<InspectionSummary[]>([]);
  const [catalog, setCatalog] = useState<ChecklistItemDefinition[]>([]);
  const [active, setActive] = useState<InspectionSummary | null>(null);
  /** itemId -> passed */
  const [checks, setChecks] = useState<Record<string, boolean>>({});
  const [notes, setNotes] = useState('');
  const [loadingList, setLoadingList] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [formErr, setFormErr] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoadingList(true);
    setError(null);
    try {
      const [inspRes, items] = await Promise.all([
        getEmployeeInspections({ size: 50 }),
        getChecklistItems().catch(() => [] as ChecklistItemDefinition[]),
      ]);
      if (inspRes.success) {
        setInspections(inspRes.data.content);
      } else {
        setError('Không tải được danh sách kiểm tra.');
      }
      setCatalog(items);
    } catch (err) {
      setError(extractErr(err, 'Không tải được danh sách kiểm tra.'));
    } finally {
      setLoadingList(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const assignedToMe = useMemo(
    () => inspections.filter(i => i.inspectorId && i.inspectorId === userId),
    [inspections, userId],
  );
  const unassigned = useMemo(
    () => inspections.filter(i => !i.inspectorId),
    [inspections],
  );

  function openChecklist(item: InspectionSummary) {
    setActive(item);
    const initial: Record<string, boolean> = {};
    catalog.forEach(c => { initial[c.id] = true; });
    setChecks(initial);
    setNotes('');
    setFormErr(null);
    setError(null);
  }

  function closeChecklist() {
    if (submitting) return;
    setActive(null);
    setFormErr(null);
  }

  function toggleItem(itemId: string) {
    setChecks(prev => ({ ...prev, [itemId]: !prev[itemId] }));
  }

  function buildAnswers() {
    return catalog.map(c => ({
      itemId: c.id,
      passed: checks[c.id] !== false,
    }));
  }

  async function submitResult(result: 'PASS' | 'FAIL') {
    if (!active) return;
    if (result === 'FAIL' && !notes.trim()) {
      setFormErr('Please describe the damage found');
      return;
    }
    if (catalog.length === 0) {
      setFormErr('Chưa có danh mục checklist. Vui lòng restart backend để seed dữ liệu.');
      return;
    }
    setFormErr(null);
    setError(null);
    setSubmitting(true);
    const body = { note: notes.trim() || undefined, answers: buildAnswers() };
    try {
      const res = result === 'PASS'
        ? await passInspection(active.id, body)
        : await failInspection(active.id, { note: notes.trim(), answers: buildAnswers() });
      if (res.success) {
        setActive(null);
        if (result === 'FAIL') {
          navigate('/employee/damage/create', {
            state: {
              roomId: active.roomId,
              inspectionId: active.id,
              fromInspection: true,
            },
          });
        } else {
          await load();
        }
      } else {
        setError(result === 'PASS' ? 'Nộp PASS thất bại.' : 'Nộp FAIL thất bại.');
      }
    } catch (err) {
      setError(extractErr(err, result === 'PASS' ? 'Nộp PASS thất bại.' : 'Nộp FAIL thất bại.'));
    } finally {
      setSubmitting(false);
    }
  }

  const passCount = catalog.filter(c => checks[c.id] !== false).length;
  const totalCount = catalog.length || 1;
  const roomLabel = (i: InspectionSummary) => i.roomName || i.roomNumber || 'Phòng';

  function InspectionCard({ item, claim }: { item: InspectionSummary; claim?: boolean }) {
    return (
      <button
        type="button"
        onClick={() => openChecklist(item)}
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: 12,
          width: '100%',
          textAlign: 'left',
          padding: '14px 16px',
          borderRadius: 12,
          border: '1px solid var(--hairline)',
          background: 'var(--surface-card)',
          cursor: 'pointer',
          ...TOUCH,
        }}
      >
        <div style={{ minWidth: 0 }}>
          <p style={{ fontWeight: 700, fontSize: 15, color: 'var(--ink)', marginBottom: 4 }}>
            {roomLabel(item)}
          </p>
          <p className="body-sm text-charcoal" style={{ margin: 0 }}>
            {claim ? 'Unassigned — Claim & inspect' : (item.inspectorName || 'Assigned to you')}
            {item.status ? ` · ${item.status}` : ''}
          </p>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexShrink: 0 }}>
          <StatusBadge status={item.status === 'IN_PROGRESS' ? 'IN_PROGRESS' : 'PENDING'} />
          <span style={{ fontWeight: 700, fontSize: 13, color: 'var(--primary)' }}>
            {claim ? 'Claim →' : 'Inspect →'}
          </span>
        </div>
      </button>
    );
  }

  return (
    <EmployeeLayout>
      <div style={{ maxWidth: 640, margin: '0 auto' }} className="animate-fade-in">
        <div style={{ marginBottom: 20 }}>
          <h1 style={{ fontFamily: 'Outfit', fontSize: 22, fontWeight: 700, color: 'var(--ink)', marginBottom: 2 }}>
            🔍 Room Inspection
          </h1>
        </div>

        {error && <ErrBanner msg={error} />}

        {loadingList ? (
          <Spinner />
        ) : inspections.length === 0 ? (
          <div className="card" style={{ padding: 40, textAlign: 'center' }}>
            <p style={{ fontSize: 32, marginBottom: 8 }}>🎉</p>
            <p style={{ fontWeight: 600, color: 'var(--ink)', marginBottom: 4 }}>Không có phòng cần kiểm tra</p>
            <p className="body-sm text-charcoal">No rooms ready for inspection.</p>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
            <section>
              <p style={{ fontWeight: 700, fontSize: 14, color: 'var(--ink)', marginBottom: 10 }}>
                Assigned to me ({assignedToMe.length})
              </p>
              {assignedToMe.length === 0 ? (
                <p className="body-sm text-charcoal" style={{ margin: 0 }}>Không có inspection được gán cho bạn.</p>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                  {assignedToMe.map(i => <InspectionCard key={i.id} item={i} />)}
                </div>
              )}
            </section>

            <section>
              <p style={{ fontWeight: 700, fontSize: 14, color: 'var(--ink)', marginBottom: 10 }}>
                Unassigned — Claim ({unassigned.length})
              </p>
              {unassigned.length === 0 ? (
                <p className="body-sm text-charcoal" style={{ margin: 0 }}>Không có inspection trống để claim.</p>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                  {unassigned.map(i => <InspectionCard key={i.id} item={i} claim />)}
                </div>
              )}
            </section>
          </div>
        )}

        <Drawer
          open={!!active}
          onClose={closeChecklist}
          title={active ? `Checklist — ${roomLabel(active)}` : 'Checklist'}
        >
          {active && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 16, paddingBottom: 24 }}>
              <p className="body-sm text-charcoal" style={{ margin: 0 }}>
                Status: <strong>{active.status}</strong>
                {active.inspectorId ? ' · Claimed by you' : ' · Claiming on Pass/Fail'}
              </p>

              <div>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
                  <p style={{ fontWeight: 700, fontSize: 15, color: 'var(--ink)', margin: 0 }}>
                    Checklist ({passCount}/{catalog.length})
                  </p>
                  <div style={{ width: 80, height: 6, background: 'var(--hairline)', borderRadius: 3, overflow: 'hidden' }}>
                    <div style={{
                      width: `${(passCount / totalCount) * 100}%`,
                      height: '100%',
                      background: passCount === catalog.length ? '#2b9a66' : 'var(--primary)',
                      borderRadius: 3,
                      transition: 'width 0.3s',
                    }} />
                  </div>
                </div>
                {catalog.length === 0 ? (
                  <p className="body-sm text-charcoal">
                    Chưa có danh mục checklist. Restart backend để seed dữ liệu mặc định.
                  </p>
                ) : catalog.map(item => {
                  const ok = checks[item.id] !== false;
                  return (
                    <label
                      key={item.id}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 14,
                        padding: '12px 0',
                        borderBottom: '1px solid var(--hairline)',
                        cursor: 'pointer',
                        ...TOUCH,
                      }}
                    >
                      <input
                        type="checkbox"
                        checked={ok}
                        onChange={() => toggleItem(item.id)}
                        style={{ width: 20, height: 20, accentColor: 'var(--primary)', cursor: 'pointer', flexShrink: 0 }}
                      />
                      <span style={{ fontSize: 20 }}>{item.icon || '•'}</span>
                      <span style={{
                        fontWeight: ok ? 500 : 400,
                        fontSize: 15,
                        color: ok ? 'var(--ink)' : 'var(--charcoal)',
                        textDecoration: ok ? 'none' : 'line-through',
                        flex: 1,
                      }}>
                        {item.label}
                      </span>
                      {ok
                        ? <span style={{ color: '#2b9a66', fontSize: 16, fontWeight: 700 }}>✓</span>
                        : <span style={{ color: '#dc2626', fontSize: 16 }}>✗</span>}
                    </label>
                  );
                })}
              </div>

              <div>
                <label className="form-label" htmlFor="inspection-notes">
                  Note {Object.values(checks).some(v => !v) ? '(required for Fail)' : '(optional)'}
                </label>
                <textarea
                  id="inspection-notes"
                  className="textarea"
                  rows={3}
                  placeholder="Please describe the damage found"
                  value={notes}
                  onChange={e => setNotes(e.target.value)}
                />
                {formErr && <p className="form-error">{formErr}</p>}
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
                <button
                  type="button"
                  className="btn-primary"
                  disabled={submitting}
                  onClick={() => submitResult('PASS')}
                  style={{
                    ...TOUCH,
                    borderRadius: 12,
                    fontWeight: 700,
                    fontSize: 16,
                    background: '#10B981',
                    borderColor: '#10B981',
                  }}
                >
                  {submitting ? '…' : 'Pass'}
                </button>
                <button
                  type="button"
                  disabled={submitting}
                  onClick={() => submitResult('FAIL')}
                  style={{
                    ...TOUCH,
                    borderRadius: 12,
                    fontWeight: 700,
                    fontSize: 16,
                    background: '#EF4444',
                    border: 'none',
                    color: '#fff',
                    cursor: submitting ? 'not-allowed' : 'pointer',
                    opacity: submitting ? 0.5 : 1,
                  }}
                >
                  {submitting ? '…' : 'Fail'}
                </button>
              </div>
              <p className="body-sm text-charcoal" style={{ margin: 0 }}>
                Fail sẽ chuyển sang tạo Damage Report (SCR-64).
              </p>
            </div>
          )}
        </Drawer>
      </div>
    </EmployeeLayout>
  );
}
