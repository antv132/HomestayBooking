// ─── SCR-62: Activity Log ─────────────────────────────────────────────────────
import ManagerLayout from '../../layouts/ManagerLayout';
import { ACTIVITY_LOGS, ACTION_ICON } from './_sharedAdminData';

export function ActivityLogPage() {
  return (
    <ManagerLayout>
      <h1 className="heading-md" style={{ marginBottom: 24 }}>Activity Log</h1>
      <div className="card" style={{ overflow: 'hidden' }}>
        {ACTIVITY_LOGS.map((log, i) => {
          const meta = ACTION_ICON[log.action] || { bg: 'var(--surface-bone)', icon: '📝' };
          return (
            <div key={log.id} style={{ display: 'flex', gap: 14, padding: '14px 20px', borderBottom: i < ACTIVITY_LOGS.length - 1 ? '1px solid var(--hairline)' : 'none' }}>
              <div style={{ width: 36, height: 36, borderRadius: '50%', background: meta.bg, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 14, flexShrink: 0 }}>{meta.icon}</div>
              <div style={{ flex: 1 }}>
                <div className="flex items-center gap-2" style={{ marginBottom: 3 }}>
                  <span className="badge badge-tag" style={{ fontSize: 10 }}>{log.action.replace('_',' ')}</span>
                  <span style={{ fontWeight: 600, fontSize: 13 }}>{log.description}</span>
                </div>
                <p style={{ fontSize: 12, color: 'var(--ash)' }}>{log.userEmail} · {new Date(log.createdAt).toLocaleString('en-US')}</p>
              </div>
            </div>
          );
        })}
      </div>
    </ManagerLayout>
  );
}
