import { useState, useEffect, useCallback } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import ManagerLayout from '../../layouts/ManagerLayout';
import { propertyApi, PropertySummary } from '../../api/propertyApi';
import { floorApi, FloorSummary, CreateFloorPayload, UpdateFloorPayload } from '../../api/floorApi';
import DataTable from '../../components/ui/DataTable';

// ── Floor Modal (Add / Edit) ──────────────────────────────────────────────────

interface FloorModalProps {
  mode: 'add' | 'edit';
  propertyId: string;
  propertyName: string;
  initial?: FloorSummary;
  onClose: () => void;
  onSuccess: () => void;
}

function FloorModal({ mode, propertyId, propertyName, initial, onClose, onSuccess }: FloorModalProps) {
  const [floorNumber, setFloorNumber] = useState(initial?.floorNumber?.toString() ?? '');
  const [description, setDescription] = useState(initial?.description ?? '');
  const [saving, setSaving]           = useState(false);
  const [error, setError]             = useState('');

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const num = Number(floorNumber);
    if (!floorNumber || isNaN(num) || num < 1 || !Number.isInteger(num)) {
      setError('Floor number is required and must be a positive integer.');
      return;
    }
    setSaving(true);
    setError('');
    try {
      if (mode === 'add') {
        const payload: CreateFloorPayload = {
          propertyId,
          floorNumber: num,
          description: description.trim() || undefined,
        };
        await floorApi.create(payload);
      } else if (initial) {
        const payload: UpdateFloorPayload = {
          floorNumber: num,
          description: description.trim() || undefined,
        };
        await floorApi.update(propertyId, initial.id, payload);
      }
      onSuccess();
      onClose();
    } catch (err: any) {
      const msg = err?.response?.data?.message ?? 'An error occurred. Please try again.';
      setError(msg);
    } finally {
      setSaving(false);
    }
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center"
      style={{ background: 'rgba(0,0,0,0.45)', backdropFilter: 'blur(4px)' }}
      onClick={onClose}
    >
      <div
        className="card-lg"
        style={{ padding: 28, width: 460, maxWidth: '90vw' }}
        onClick={e => e.stopPropagation()}
      >
        <h2 className="heading-sm" style={{ marginBottom: 4 }}>
          {mode === 'add' ? '+ Add New Floor' : `Edit Floor ${initial?.floorNumber}`}
        </h2>
        <p className="body-sm text-charcoal" style={{ marginBottom: 20 }}>
          Property: <strong>{propertyName}</strong>
        </p>

        {error && (
          <div
            style={{
              background: '#fee2e2',
              border: '1px solid #fca5a5',
              borderRadius: 8,
              padding: '10px 14px',
              marginBottom: 16,
              color: '#dc2626',
              fontSize: 14,
            }}
          >
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: 14 }}>
            <label className="form-label form-label-required" htmlFor="fm-floor-number">
              Floor Number
            </label>
            <input
              id="fm-floor-number"
              type="number"
              min={1}
              step={1}
              className="input"
              placeholder="e.g. 1"
              value={floorNumber}
              onChange={e => setFloorNumber(e.target.value)}
              autoFocus
              required
            />
          </div>

          <div style={{ marginBottom: 24 }}>
            <label className="form-label" htmlFor="fm-description">
              Description
            </label>
            <input
              id="fm-description"
              className="input"
              placeholder="e.g. Sea View Floor"
              value={description}
              onChange={e => setDescription(e.target.value)}
            />
            <p className="form-hint">Optional — helps identify this floor at a glance.</p>
          </div>

          <div style={{ display: 'flex', gap: 10 }}>
            <button type="submit" className="btn-primary" style={{ flex: 1 }} disabled={saving}>
              {saving ? 'Saving…' : mode === 'add' ? 'Add Floor' : 'Save Changes'}
            </button>
            <button
              type="button"
              className="btn-ghost"
              style={{ flex: 1 }}
              onClick={onClose}
              disabled={saving}
            >
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ── Delete Confirm Modal ──────────────────────────────────────────────────────

interface DeleteModalProps {
  floor: FloorSummary;
  deleting: boolean;
  error: string;
  onConfirm: () => void;
  onClose: () => void;
}

function DeleteModal({ floor, deleting, error, onConfirm, onClose }: DeleteModalProps) {
  const hasRooms = floor.roomCount > 0;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center"
      style={{ background: 'rgba(0,0,0,0.45)', backdropFilter: 'blur(4px)' }}
      onClick={() => !deleting && onClose()}
    >
      <div
        className="card-lg"
        style={{ padding: 28, width: 440, maxWidth: '90vw' }}
        onClick={e => e.stopPropagation()}
      >
        <h2 className="heading-sm" style={{ marginBottom: 12 }}>
          Delete Floor {floor.floorNumber}?
        </h2>

        <p className="body-md text-charcoal" style={{ marginBottom: 16 }}>
          This will permanently delete{' '}
          <strong>
            Floor {floor.floorNumber}
            {floor.description ? ` — ${floor.description}` : ''}
          </strong>
          . This action cannot be undone.
        </p>

        {hasRooms && (
          <div
            style={{
              background: '#FEF3C7',
              border: '1px solid #F59E0B',
              borderRadius: 8,
              padding: '12px 14px',
              marginBottom: 16,
              display: 'flex',
              gap: 10,
              alignItems: 'flex-start',
            }}
          >
            <span style={{ fontSize: 16, flexShrink: 0 }}>⚠️</span>
            <p className="body-sm" style={{ color: '#92400E' }}>
              This floor has <strong>{floor.roomCount} room{floor.roomCount !== 1 ? 's' : ''}</strong>.
              You must remove all rooms before deleting this floor.
            </p>
          </div>
        )}

        {error && (
          <div
            style={{
              background: '#fee2e2',
              border: '1px solid #fca5a5',
              borderRadius: 8,
              padding: '10px 14px',
              marginBottom: 14,
              color: '#dc2626',
              fontSize: 14,
            }}
          >
            {error}
          </div>
        )}

        <div style={{ display: 'flex', gap: 10 }}>
          <button
            className="btn-primary"
            style={{ flex: 1, background: hasRooms ? 'var(--charcoal)' : 'var(--error)' }}
            onClick={onConfirm}
            disabled={deleting || hasRooms}
          >
            {deleting ? 'Deleting…' : 'Delete Floor'}
          </button>
          <button
            className="btn-ghost"
            style={{ flex: 1 }}
            onClick={onClose}
            disabled={deleting}
          >
            {hasRooms ? 'Close' : 'Cancel'}
          </button>
        </div>
      </div>
    </div>
  );
}

// ── Table skeleton ────────────────────────────────────────────────────────────

function TableSkeleton() {
  return (
    <tbody>
      {[...Array(4)].map((_, i) => (
        <tr key={i}>
          {[...Array(5)].map((__, j) => (
            <td key={j}>
              <div
                style={{
                  height: 16,
                  borderRadius: 4,
                  background: 'var(--surface-bone)',
                  width: j === 4 ? 80 : '70%',
                  animation: 'pulse 1.4s ease-in-out infinite',
                  opacity: 1 - i * 0.18,
                }}
              />
            </td>
          ))}
        </tr>
      ))}
    </tbody>
  );
}

// ── Main Page ─────────────────────────────────────────────────────────────────

export default function FloorManagementPage() {
  const [searchParams] = useSearchParams();
  const initPropertyId = searchParams.get('propertyId') ?? '';

  // ── State
  const [properties, setProperties]         = useState<PropertySummary[]>([]);
  const [propLoading, setPropLoading]        = useState(true);
  const [propError, setPropError]            = useState('');

  const [selectedPropId, setSelectedPropId]  = useState(initPropertyId);

  const [floors, setFloors]                  = useState<FloorSummary[]>([]);
  const [floorsLoading, setFloorsLoading]    = useState(false);
  const [floorsError, setFloorsError]        = useState('');

  const [modal, setModal] = useState<{
    open: boolean;
    mode: 'add' | 'edit';
    floor?: FloorSummary;
  }>({ open: false, mode: 'add' });

  const [deleteTarget, setDeleteTarget]      = useState<FloorSummary | null>(null);
  const [deleting, setDeleting]              = useState(false);
  const [deleteError, setDeleteError]        = useState('');

  // ── Property list ────────────────────────────────────────────────────────────
  useEffect(() => {
    setPropLoading(true);
    propertyApi
      .getAll({ page: 0, size: 100 })
      .then(res => {
        const list = res.data.content ?? [];
        setProperties(list);
        if (!selectedPropId && list.length > 0) {
          setSelectedPropId(list[0].id);
        }
      })
      .catch(() => setPropError('Failed to load properties. Please refresh.'))
      .finally(() => setPropLoading(false));
  }, []);

  // ── Floors list ───────────────────────────────────────────────────────────────
  const loadFloors = useCallback((propId: string) => {
    if (!propId) return;
    setFloorsLoading(true);
    setFloorsError('');
    floorApi
      .getByProperty(propId)
      .then(res => setFloors(res.data ?? []))
      .catch(() => setFloorsError('Failed to load floors. Please try again.'))
      .finally(() => setFloorsLoading(false));
  }, []);

  useEffect(() => {
    if (selectedPropId) loadFloors(selectedPropId);
    else setFloors([]);
  }, [selectedPropId, loadFloors]);

  // ── Handlers ──────────────────────────────────────────────────────────────────
  async function handleDelete() {
    if (!deleteTarget) return;
    setDeleting(true);
    setDeleteError('');
    try {
      await floorApi.remove(selectedPropId, deleteTarget.id);
      setDeleteTarget(null);
      loadFloors(selectedPropId);
    } catch (err: any) {
      const msg = err?.response?.data?.message ?? 'Failed to delete floor. Please try again.';
      setDeleteError(msg);
    } finally {
      setDeleting(false);
    }
  }

  const currentProperty = properties.find(p => p.id === selectedPropId);

  const columns = [
    { header: 'Floor', accessor: (floor: any) => (
      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        <div style={{ width: 36, height: 36, background: 'var(--surface-bone)', borderRadius: 8, display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 700, fontSize: 13, color: 'var(--ink)', flexShrink: 0 }}>
          F{floor.floorNumber}
        </div>
        <span style={{ fontWeight: 700 }}>Floor {floor.floorNumber}</span>
      </div>
    )},
    { header: 'Description', accessor: (floor: any) => (
      <span className="text-charcoal">
        {floor.description || <span style={{ color: 'var(--stone)', fontStyle: 'italic' }}>No description</span>}
      </span>
    )},
    { header: 'Rooms', accessor: (floor: any) => (
      floor.roomCount > 0 ? (
        <Link to={`/manager/rooms?floorId=${floor.id}`} style={{ textDecoration: 'none' }}>
          <span className="badge badge-neutral" style={{ cursor: 'pointer' }}>{floor.roomCount} room{floor.roomCount !== 1 ? 's' : ''}</span>
        </Link>
      ) : <span className="badge badge-neutral">0 rooms</span>
    )},
    { header: 'Actions', accessor: (floor: any) => (
      <div style={{ display: 'flex', gap: 6 }}>
        <Link to={`/manager/rooms/add?floorId=${floor.id}&propertyId=${selectedPropId}`} className="btn-ghost btn-sm">+ Room</Link>
        <button className="btn-ghost btn-sm" onClick={() => setModal({ open: true, mode: 'edit', floor })}>Edit</button>
        <button className="btn-ghost btn-sm" style={{ color: 'var(--error)' }} onClick={() => { setDeleteTarget(floor); setDeleteError(''); }}>Delete</button>
      </div>
    )}
  ];

  // ── Render ────────────────────────────────────────────────────────────────────
  return (
    <ManagerLayout>
      {/* ── Page Header ── */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          marginBottom: 8,
        }}
      >
        <div>
          <h1 className="display-md" style={{ marginBottom: 4 }}>
            Floor Management
          </h1>
          <p className="body-sm text-charcoal">
            Add, edit and delete floors within a property
          </p>
        </div>
        <div style={{ display: 'flex', gap: 10 }}>
          <Link to="/manager/structure" className="btn-ghost btn-sm">
            Structure Tree
          </Link>
          <button
            className="btn-primary btn-sm"
            onClick={() => setModal({ open: true, mode: 'add' })}
            disabled={!selectedPropId}
          >
            + Add Floor
          </button>
        </div>
      </div>

      {/* Breadcrumb */}
      <div
        className="flex items-center gap-2 body-sm"
        style={{ marginBottom: 24, color: 'var(--charcoal)' }}
      >
        <Link to="/manager/properties" className="text-primary" style={{ textDecoration: 'none' }}>
          My Property
        </Link>
        <span style={{ color: 'var(--stone)' }}>/</span>
        <span>Floor Management</span>
        {currentProperty && (
          <>
            <span style={{ color: 'var(--stone)' }}>/</span>
            <Link
              to={`/manager/properties/${currentProperty.id}`}
              className="text-primary"
              style={{ textDecoration: 'none' }}
            >
              {currentProperty.name}
            </Link>
          </>
        )}
      </div>

      {/* ── Property Selector ── */}
      {propError ? (
        <div
          style={{
            background: '#fee2e2',
            border: '1px solid #fca5a5',
            borderRadius: 10,
            padding: '10px 16px',
            marginBottom: 20,
            color: '#dc2626',
          }}
        >
          {propError}
        </div>
      ) : (
        <div
          className="card"
          style={{ padding: '16px 20px', marginBottom: 20 }}
        >
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 16,
              flexWrap: 'wrap',
            }}
          >
            <div style={{ flex: '0 0 auto' }}>
              <label
                className="form-label"
                htmlFor="fm-property-select"
                style={{ marginBottom: 4, display: 'block' }}
              >
                Select Property
              </label>
              <select
                id="fm-property-select"
                className="select"
                style={{ minWidth: 280 }}
                value={selectedPropId}
                onChange={e => setSelectedPropId(e.target.value)}
                disabled={propLoading}
              >
                {propLoading ? (
                  <option>Loading…</option>
                ) : properties.length === 0 ? (
                  <option value="">No properties available</option>
                ) : (
                  <>
                    <option value="">— Select a property —</option>
                    {properties.map(p => (
                      <option key={p.id} value={p.id}>
                        {p.name}
                      </option>
                    ))}
                  </>
                )}
              </select>
            </div>

            {/* Property stats */}
            {currentProperty && !floorsLoading && floors.length > 0 && (
              <div
                style={{
                  display: 'flex',
                  gap: 20,
                  marginLeft: 'auto',
                  flexWrap: 'wrap',
                }}
              >
                <div style={{ textAlign: 'center' }}>
                  <p style={{ fontSize: 22, fontWeight: 700, color: 'var(--ink)', lineHeight: 1.2 }}>
                    {floors.length}
                  </p>
                  <p className="body-sm text-charcoal">Total Floors</p>
                </div>
                <div style={{ textAlign: 'center' }}>
                  <p style={{ fontSize: 22, fontWeight: 700, color: 'var(--ink)', lineHeight: 1.2 }}>
                    {floors.reduce((acc, f) => acc + f.roomCount, 0)}
                  </p>
                  <p className="body-sm text-charcoal">Total Rooms</p>
                </div>
                <div style={{ textAlign: 'center' }}>
                  <p style={{ fontSize: 22, fontWeight: 700, color: '#2b9a66', lineHeight: 1.2 }}>
                    {currentProperty.status === 'ACTIVE' ? 'Active' : 'Inactive'}
                  </p>
                  <p className="body-sm text-charcoal">Status</p>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* ── Floors Table ── */}
      {!selectedPropId ? (
        <div
          className="card"
          style={{
            padding: 60,
            textAlign: 'center',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: 12,
          }}
        >
          <span style={{ fontSize: 48 }}>🏢</span>
          <p className="heading-sm" style={{ color: 'var(--charcoal)' }}>
            Select a property to view its floors
          </p>
          <p className="body-sm text-mute">Choose a property from the dropdown above.</p>
        </div>
      ) : floorsError ? (
        <div
          style={{
            background: '#fee2e2',
            border: '1px solid #fca5a5',
            borderRadius: 10,
            padding: '16px 20px',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
          }}
        >
          <p style={{ color: '#dc2626' }}>{floorsError}</p>
          <button
            className="btn-outline btn-sm"
            onClick={() => loadFloors(selectedPropId)}
          >
            Retry
          </button>
        </div>
      ) : floorsLoading ? (
        <div style={{ padding: 48, textAlign: 'center', color: 'var(--text-muted)' }}>Loading floors...</div>
      ) : floors.length === 0 ? (
        <div
          style={{
            padding: '40px 20px',
            textAlign: 'center',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: 10,
          }}
        >
          <span style={{ fontSize: 36 }}>🏗</span>
          <p className="heading-sm" style={{ color: 'var(--charcoal)' }}>
            No floors yet
          </p>
          <p className="body-sm text-mute" style={{ marginBottom: 8 }}>
            This property has no floors. Add the first floor to get started.
          </p>
          <button
            className="btn-primary btn-sm"
            onClick={() => setModal({ open: true, mode: 'add' })}
          >
            + Add First Floor
          </button>
        </div>
      ) : (
        <DataTable 
          columns={columns}
          data={floors.slice().sort((a, b) => a.floorNumber - b.floorNumber)}
          keyExtractor={(f) => f.id}
        />
      )}

      {/* ── Quick link to Structure Tree ── */}
      {selectedPropId && !floorsLoading && floors.length > 0 && (
        <div style={{ marginTop: 16, textAlign: 'right' }}>
          <Link
            to={`/manager/structure?propertyId=${selectedPropId}`}
            className="btn-ghost btn-sm"
          >
            View in Structure Tree →
          </Link>
        </div>
      )}

      {/* ── Add/Edit Modal ── */}
      {modal.open && currentProperty && (
        <FloorModal
          mode={modal.mode}
          propertyId={selectedPropId}
          propertyName={currentProperty.name}
          initial={modal.floor}
          onClose={() => setModal({ open: false, mode: 'add' })}
          onSuccess={() => loadFloors(selectedPropId)}
        />
      )}

      {/* ── Delete Modal ── */}
      {deleteTarget && (
        <DeleteModal
          floor={deleteTarget}
          deleting={deleting}
          error={deleteError}
          onConfirm={handleDelete}
          onClose={() => {
            if (!deleting) {
              setDeleteTarget(null);
              setDeleteError('');
            }
          }}
        />
      )}
    </ManagerLayout>
  );
}
