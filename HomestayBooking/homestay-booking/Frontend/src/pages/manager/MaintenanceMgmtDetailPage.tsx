// ─── SCR-58: Maintenance Management Detail ────────────────────────────────────
import { useState, useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import ManagerLayout from '../../layouts/ManagerLayout';
import { Badge } from './_sharedAdminData';
import { maintenanceApi, MaintenanceTicket } from '../../api/maintenanceApi';
import { PhotoLightbox } from '../../components/PhotoLightbox';

const getPhotoUrl = (url: string) => {
  if (!url) return '';
  if (url.startsWith('http')) return url;
  const backendBase = import.meta.env.VITE_API_URL || 'http://localhost:8080';
  return `${backendBase}${url.startsWith('/') ? '' : '/'}${url}`;
};

export function MaintenanceMgmtDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [ticket, setTicket] = useState<MaintenanceTicket | null>(null);
  const [status, setStatus] = useState('');
  const [resolutionNote, setResolutionNote] = useState('');
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  const STATUSES = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];

  useEffect(() => {
    async function fetchTicketDetail() {
      if (!id) return;
      setLoading(true);
      setError(null);
      try {
        const res = await maintenanceApi.getTicketDetail(id);
        if (res.success && res.data) {
          setTicket(res.data);
          setStatus(res.data.status);
          setResolutionNote(res.data.resolutionNote || '');
        } else {
          setError('Failed to fetch ticket details.');
        }
      } catch (err: any) {
        console.error('Error fetching ticket detail for manager:', err);
        setError(err.response?.data?.message || 'Error loading ticket detail. Please verify connection to server.');
      } finally {
        setLoading(false);
      }
    }
    fetchTicketDetail();
  }, [id]);

  const handleUpdateStatus = async () => {
    if (!id) return;
    setUpdating(true);
    setError(null);
    setSuccessMsg(null);
    try {
      const res = await maintenanceApi.updateTicketStatus(id, {
        status,
        resolutionNote: resolutionNote.trim() || undefined,
      });
      if (res.success && res.data) {
        setTicket(res.data);
        setStatus(res.data.status);
        setResolutionNote(res.data.resolutionNote || '');
        setSuccessMsg('Ticket status updated successfully!');
        setTimeout(() => setSuccessMsg(null), 3000);
      } else {
        setError('Failed to update status.');
      }
    } catch (err: any) {
      console.error('Error updating status:', err);
      setError(err.response?.data?.message || 'Error updating status.');
    } finally {
      setUpdating(false);
    }
  };

  if (loading) {
    return (
      <ManagerLayout>
        <div style={{ padding: '40px 0', textAlign: 'center', color: 'var(--text-muted)' }}>
          <p>Loading ticket details...</p>
        </div>
      </ManagerLayout>
    );
  }

  if (error && !ticket) {
    return (
      <ManagerLayout>
        <div className="card" style={{ padding: 24, textAlign: 'center', borderColor: '#fee2e2', background: '#fef2f2' }}>
          <p style={{ color: '#dc2626', fontWeight: 600, marginBottom: 16 }}>{error}</p>
          <Link to="/manager/maintenance" className="btn-outline" style={{ display: 'inline-block' }}>← Back to List</Link>
        </div>
      </ManagerLayout>
    );
  }

  if (!ticket) {
    return (
      <ManagerLayout>
        <div style={{ padding: '40px 0', textAlign: 'center', color: 'var(--text-muted)' }}>
          <p>Ticket not found.</p>
          <Link to="/manager/maintenance" className="btn-outline" style={{ display: 'inline-block', marginTop: 16 }}>← Back to List</Link>
        </div>
      </ManagerLayout>
    );
  }

  return (
    <ManagerLayout>
      <div className="flex items-center gap-2 body-sm text-charcoal" style={{ marginBottom: 20 }}>
        <Link to="/manager/maintenance" className="text-primary" style={{ textDecoration: 'none' }}>Maintenance</Link>
        <span>›</span>
        <span style={{ fontWeight: 600 }}>#{ticket.id.substring(0, 8)}</span>
      </div>

      {successMsg && (
        <div className="card" style={{ padding: 12, marginBottom: 16, borderColor: '#dcfce7', background: '#f0fdf4', color: '#15803d', fontWeight: 600 }}>
          {successMsg}
        </div>
      )}

      {error && (
        <div className="card" style={{ padding: 12, marginBottom: 16, borderColor: '#fee2e2', background: '#fef2f2', color: '#b91c1c', fontWeight: 600 }}>
          {error}
        </div>
      )}

      <div className="flex items-start justify-between" style={{ marginBottom: 24 }}>
        <div>
          <h1 className="heading-md" style={{ marginBottom: 4 }}>{ticket.title}</h1>
          <p className="body-sm text-charcoal">Ticket UUID: <span className="code-sm">{ticket.id}</span></p>
        </div>
        <Badge s={ticket.status} />
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 320px', gap: 20 }}>
        <div className="flex flex-col gap-5">
          <div className="card" style={{ padding: 24 }}>
            <h3 className="heading-xs" style={{ marginBottom: 16 }}>Details</h3>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
              {[
                { l: 'Customer Name', v: ticket.customerName },
                { l: 'Customer Email', v: ticket.customerEmail },
                { l: 'Room Number', v: `Room ${ticket.roomNumber}` },
                { l: 'Property', v: ticket.propertyName },
                { l: 'Submitted At', v: new Date(ticket.createdAt).toLocaleString('en-US') },
                { l: 'Last Updated', v: new Date(ticket.updatedAt).toLocaleString('en-US') },
              ].map(row => (
                <div key={row.l}>
                  <p className="body-sm text-charcoal">{row.l}</p>
                  <p style={{ fontWeight: 600, marginTop: 2 }}>{row.v}</p>
                </div>
              ))}
            </div>
          </div>

          <div className="card" style={{ padding: 24 }}>
            <h3 className="heading-xs" style={{ marginBottom: 12 }}>Description</h3>
            <p className="body-md" style={{ padding: '12px 16px', background: 'var(--surface-bone)', borderRadius: 8, whiteSpace: 'pre-wrap' }}>
              {ticket.description}
            </p>
          </div>

          {ticket.photoUrls && ticket.photoUrls.length > 0 && (
            <div className="card" style={{ padding: 24 }}>
              <h3 className="heading-xs" style={{ marginBottom: 12 }}>Attached Photos</h3>
              <PhotoLightbox photoUrls={ticket.photoUrls} getPhotoUrl={getPhotoUrl} />
            </div>
          )}

          {ticket.resolutionNote && (
            <div className="card" style={{ padding: 24, borderColor: '#dbeafe', background: '#f8fafc' }}>
              <h3 className="heading-xs" style={{ marginBottom: 8, color: '#1e40af' }}>Resolution Note</h3>
              <p className="body-md" style={{ whiteSpace: 'pre-wrap', fontStyle: 'italic' }}>
                {ticket.resolutionNote}
              </p>
            </div>
          )}
        </div>

        <div className="card-lg" style={{ padding: 24, alignSelf: 'start' }}>
          <h3 className="heading-sm" style={{ marginBottom: 14 }}>Process Request</h3>
          
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginBottom: 16 }}>
            {STATUSES.map(s => (
              <label
                key={s}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 10,
                  padding: '10px 14px',
                  border: `1.5px solid ${status === s ? 'var(--primary)' : 'var(--hairline)'}`,
                  borderRadius: 10,
                  cursor: 'pointer',
                  background: status === s ? 'rgba(15,118,110,0.08)' : 'transparent',
                }}
              >
                <input
                  type="radio"
                  name="status"
                  value={s}
                  checked={status === s}
                  onChange={() => setStatus(s)}
                  style={{ accentColor: 'var(--primary)' }}
                />
                <span style={{ fontWeight: 600, fontSize: 13 }}>{s.replace('_', ' ')}</span>
              </label>
            ))}
          </div>

          <div style={{ marginBottom: 16 }}>
            <label className="body-sm text-charcoal" style={{ display: 'block', marginBottom: 6, fontWeight: 600 }}>
              Resolution / Internal Note
            </label>
            <textarea
              value={resolutionNote}
              onChange={(e) => setResolutionNote(e.target.value)}
              placeholder="e.g. Technician dispatched, AC unit replaced..."
              style={{ width: '100%', height: 100, padding: 10, borderRadius: 8, border: '1px solid var(--hairline)', resize: 'vertical' }}
            />
          </div>

          <button
            className="btn-primary"
            onClick={handleUpdateStatus}
            disabled={updating}
            style={{ width: '100%' }}
          >
            {updating ? 'Updating...' : 'Update Ticket'}
          </button>
        </div>
      </div>
    </ManagerLayout>
  );
}
