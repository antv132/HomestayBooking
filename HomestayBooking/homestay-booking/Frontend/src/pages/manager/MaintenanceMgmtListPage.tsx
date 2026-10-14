// ─── SCR-57: Maintenance Management List ─────────────────────────────────────
import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import ManagerLayout from '../../layouts/ManagerLayout';
import { Badge } from './_sharedAdminData';
import { maintenanceApi, MaintenanceTicket } from '../../api/maintenanceApi';
import DataTable from '../../components/ui/DataTable';

export function MaintenanceMgmtListPage() {
  const [filter, setFilter] = useState('ALL');
  const [tickets, setTickets] = useState<MaintenanceTicket[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const TABS = ['ALL', 'OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];

  useEffect(() => {
    async function fetchTickets() {
      setLoading(true);
      setError(null);
      try {
        const res = await maintenanceApi.getAllTickets({ status: filter });
        if (res.success && res.data) {
          setTickets(res.data.content);
        } else {
          setError('Failed to load maintenance tickets');
        }
      } catch (err: any) {
        console.error('Error fetching tickets for manager:', err);
        setError(err.response?.data?.message || 'Error loading tickets. Please verify connection to server.');
      } finally {
        setLoading(false);
      }
    }
    fetchTickets();
  }, [filter]);

  const columns = [
    { header: 'ID', accessor: (t: any) => <span className="code-sm" title={t.id}>{t.id.substring(0, 8)}</span> },
    { header: 'Customer', accessor: (t: any) => (
      <div>
        <div style={{ fontWeight: 600 }}>{t.customerName}</div>
        <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>{t.customerEmail}</div>
      </div>
    )},
    { header: 'Room', accessor: (t: any) => `Room ${t.roomNumber} · ${t.propertyName}` },
    { header: 'Title', accessor: (t: any) => <span className="text-charcoal" style={{ fontWeight: 500 }}>{t.title}</span> },
    { header: 'Status', accessor: (t: any) => <Badge s={t.status} /> },
    { header: 'Date', accessor: (t: any) => <span className="text-charcoal">{new Date(t.createdAt).toLocaleDateString('en-US')}</span> }
  ];

  return (
    <ManagerLayout>
      <h1 className="heading-md" style={{ marginBottom: 24 }}>Maintenance Management</h1>
      <div style={{ display: 'flex', gap: 4, marginBottom: 20, padding: '4px', background: 'var(--surface-bone)', borderRadius: 9999, width: 'fit-content' }}>
        {TABS.map(tab => (
          <button
            key={tab}
            className={`tab-pill ${filter === tab ? 'active' : ''}`}
            onClick={() => setFilter(tab)}
          >
            {tab.replace('_', ' ')}
          </button>
        ))}
      </div>

      {loading ? (
        <div style={{ padding: '40px 0', textAlign: 'center', color: 'var(--text-muted)' }}>
          <p>Loading tickets...</p>
        </div>
      ) : error ? (
        <div className="card" style={{ padding: 24, textAlign: 'center', borderColor: '#fee2e2', background: '#fef2f2' }}>
          <p style={{ color: '#dc2626', fontWeight: 600 }}>{error}</p>
        </div>
      ) : tickets.length === 0 ? (
        <div style={{ padding: '40px 0', textAlign: 'center', color: 'var(--text-muted)' }}>
          <p>No maintenance tickets found.</p>
        </div>
      ) : (
        <DataTable 
          columns={columns}
          data={tickets}
          keyExtractor={(t) => t.id}
          actions={[
            { label: 'View', onClick: (t) => window.location.href = `/manager/maintenance/${t.id}` }
          ]}
        />
      )}
    </ManagerLayout>
  );
}
