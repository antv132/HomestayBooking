import { useState, useEffect, useRef } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import EmployeeLayout from '../../layouts/EmployeeLayout';
import {
  createDamageReport, uploadDamagePhotos, type DamageItem,
  getEmployeeRooms, type EmployeeRoom,
} from '../../api/employeeApi';
import { TOUCH, fmtVnd, extractErr, ErrBanner } from './EmployeeShared';

interface DamageItemRow extends DamageItem {
  _key: string;
}

interface InspectionNavState {
  roomId?: string;
  inspectionId?: string;
  fromInspection?: boolean;
}

export default function CreateDamageReportPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const navState = (location.state as InspectionNavState | null) ?? null;

  const [rooms, setRooms] = useState<EmployeeRoom[]>([]);
  const [roomId, setRoomId] = useState(navState?.roomId ?? '');
  const [inspectionId] = useState(navState?.inspectionId ?? '');
  const [items, setItems] = useState<DamageItemRow[]>([
    { _key: crypto.randomUUID(), name: '', estimatedCost: 0 },
  ]);
  const [notes, setNotes] = useState('');
  /** Local File objects for multipart upload on submit */
  const [photoFiles, setPhotoFiles] = useState<File[]>([]);
  const [photoPreviews, setPhotoPreviews] = useState<string[]>([]);
  const [loadingRooms, setLoadingRooms] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});
  const [showSummary, setShowSummary] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    async function loadRooms() {
      setLoadingRooms(true);
      try {
        const res = await getEmployeeRooms({ size: 100 });
        if (res.success) {
          setRooms(res.data.content);
          if (navState?.roomId && res.data.content.some(r => r.id === navState.roomId)) {
            setRoomId(navState.roomId);
          }
        }
      } catch { /* silent */ }
      finally { setLoadingRooms(false); }
    }
    loadRooms();
  }, [navState?.roomId]);

  // Cleanup previews on unmount only
  useEffect(() => {
    return () => {
      photoPreviews.forEach(url => URL.revokeObjectURL(url));
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps -- unmount cleanup only
  }, []);

  const totalCost = items.reduce((sum, i) => sum + (Number(i.estimatedCost) || 0), 0);

  function addItem() {
    setItems(prev => [...prev, { _key: crypto.randomUUID(), name: '', estimatedCost: 0 }]);
  }

  function removeItem(key: string) {
    if (items.length === 1) return;
    setItems(prev => prev.filter(i => i._key !== key));
  }

  function updateItem(key: string, field: 'name' | 'estimatedCost', value: string | number) {
    setItems(prev => prev.map(i => i._key === key ? { ...i, [field]: value } : i));
  }

  function handlePhotoAdd(e: React.ChangeEvent<HTMLInputElement>) {
    const files = Array.from(e.target.files || []);
    if (!files.length) return;
    setPhotoFiles(prev => [...prev, ...files]);
    setPhotoPreviews(prev => [
      ...prev,
      ...files.map(f => URL.createObjectURL(f)),
    ]);
    e.target.value = '';
  }

  function removePhoto(idx: number) {
    setPhotoPreviews(prev => {
      const url = prev[idx];
      if (url) URL.revokeObjectURL(url);
      return prev.filter((_, i) => i !== idx);
    });
    setPhotoFiles(prev => prev.filter((_, i) => i !== idx));
  }

  function validate() {
    const errs: Record<string, string> = {};
    if (!roomId) errs.roomId = 'Vui lòng chọn phòng';
    const hasEmpty = items.some(i => !i.name.trim());
    if (hasEmpty) errs.items = 'Tất cả tên hư hại không được để trống';
    const hasInvalidCost = items.some(i => Number(i.estimatedCost) <= 0);
    if (hasInvalidCost) errs.costs = 'Phí ước tính phải lớn hơn 0';
    if (photoFiles.length === 0) errs.photos = 'Cần ít nhất một ảnh minh chứng';
    return errs;
  }

  function handlePreview(e: React.FormEvent) {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length) { setFormErrors(errs); return; }
    setFormErrors({}); setShowSummary(true);
  }

  async function handleSubmit() {
    setError(null); setSubmitting(true);
    try {
      if (photoFiles.length === 0) {
        setError('Cần ít nhất một ảnh minh chứng.');
        setShowSummary(false);
        return;
      }
      const uploadRes = await uploadDamagePhotos(photoFiles);
      if (!uploadRes.success || !uploadRes.data?.length) {
        setError(uploadRes.message || 'Tải ảnh lên thất bại.');
        setShowSummary(false);
        return;
      }
      const res = await createDamageReport({
        roomId,
        inspectionId: inspectionId || undefined,
        items: items.map(i => ({ name: i.name.trim(), estimatedCost: Number(i.estimatedCost) })),
        attachments: uploadRes.data.map(url => ({ url, type: 'IMAGE' })),
        notes: notes.trim() || undefined,
      });
      if (res.success) { navigate('/employee/damage'); }
      else { setError('Tạo báo cáo thất bại.'); setShowSummary(false); }
    } catch (err) { setError(extractErr(err, 'Tạo báo cáo thất bại.')); setShowSummary(false); }
    finally { setSubmitting(false); }
  }

  return (
    <EmployeeLayout>
      <div style={{ maxWidth: 640, margin: '0 auto' }} className="animate-fade-in">
        <div style={{ marginBottom: 20 }}>
          <Link to="/employee/damage" className="body-sm text-primary" style={{ textDecoration: 'none' }}>← My Reports</Link>
          <h1 style={{ fontFamily: 'Outfit', fontSize: 22, fontWeight: 700, color: 'var(--ink)', marginTop: 8, marginBottom: 2 }}>📝 Create Damage Report</h1>
          <p className="body-sm text-charcoal">SCR-64 — Ghi nhận hư hại</p>
          {navState?.fromInspection && (
            <p className="body-sm text-charcoal" style={{ marginTop: 6 }}>Từ Room Inspection (SCR-62) — Fail</p>
          )}
        </div>
        {error && <ErrBanner msg={error} />}

        {showSummary && (
          <div className="card" style={{ padding: 20, marginBottom: 16, border: '2px solid var(--primary)' }}>
            <p style={{ fontWeight: 700, fontSize: 16, marginBottom: 14, color: 'var(--ink)' }}>📄 Xác nhận nộp báo cáo</p>
            <div style={{ marginBottom: 12 }}>
              {[
                { label: 'Phòng', value: rooms.find(r => r.id === roomId)?.name || rooms.find(r => r.id === roomId)?.roomNumber || roomId },
                { label: 'Số mục hư hại', value: `${items.length} mục` },
                { label: 'Tổng phí ước tính', value: fmtVnd(totalCost) },
                { label: 'Ảnh đính kèm', value: `${photoFiles.length} ảnh` },
              ].map(r => (
                <div key={r.label} style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 0', borderBottom: '1px solid var(--hairline)' }}>
                  <span className="body-sm text-charcoal">{r.label}</span>
                  <span style={{ fontWeight: 700, fontSize: 14 }}>{r.value}</span>
                </div>
              ))}
            </div>
            {totalCost > 5_000_000 && (
              <div className="alert alert-error" style={{ marginBottom: 12, fontSize: 13 }}>
                ⚠️ Tổng phí &gt; {fmtVnd(5_000_000)} — báo cáo sẽ được escalate lên Admin sau khi nộp.
              </div>
            )}
            <div style={{ display: 'flex', gap: 10 }}>
              <button className="btn-primary" style={{ flex: 1, ...TOUCH, borderRadius: 12, fontWeight: 700 }} onClick={handleSubmit} disabled={submitting}>
                {submitting ? 'Đang nộp...' : '✓ Xác nhận Nộp'}
              </button>
              <button className="btn-ghost" style={{ ...TOUCH, borderRadius: 12 }} onClick={() => setShowSummary(false)}>
                Sửa lại
              </button>
            </div>
          </div>
        )}

        {!showSummary && (
          <form onSubmit={handlePreview}>
            <div className="card" style={{ padding: '16px 18px', marginBottom: 14 }}>
              <label className="form-label form-label-required" htmlFor="damage-room">Phòng bị hư hại</label>
              {loadingRooms ? <div style={{ height: 44, background: 'var(--surface-bone)', borderRadius: 8 }} /> : (
                <select id="damage-room" className="input" style={{ ...TOUCH }}
                  value={roomId} onChange={e => setRoomId(e.target.value)}
                  disabled={!!navState?.roomId}>
                  <option value="">— Chọn phòng —</option>
                  {rooms.map(r => (
                    <option key={r.id} value={r.id}>{r.name || r.roomNumber}</option>
                  ))}
                </select>
              )}
              {formErrors.roomId && <p className="form-error">{formErrors.roomId}</p>}
            </div>

            <div className="card" style={{ padding: '16px 18px', marginBottom: 14 }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 14 }}>
                <p style={{ fontWeight: 700, fontSize: 15, color: 'var(--ink)' }}>🔨 Danh sách hư hại</p>
                <p style={{ fontWeight: 700, fontSize: 14, color: 'var(--primary)' }}>{fmtVnd(totalCost)}</p>
              </div>
              {(formErrors.items || formErrors.costs) && (
                <ErrBanner msg={formErrors.items || formErrors.costs} />
              )}
              <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                {items.map((item, idx) => (
                  <div key={item._key} style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                    <div style={{ flex: 2 }}>
                      {idx === 0 && <label className="form-label" style={{ fontSize: 12 }}>Tên hư hại</label>}
                      <input className="input" style={{ ...TOUCH }}
                        placeholder="VD: TV bị vỡ"
                        value={item.name}
                        onChange={e => updateItem(item._key, 'name', e.target.value)} />
                    </div>
                    <div style={{ flex: 1 }}>
                      {idx === 0 && <label className="form-label" style={{ fontSize: 12 }}>Phí ước tính (VNĐ)</label>}
                      <input className="input" type="number" min={0} style={{ ...TOUCH }}
                        placeholder="0"
                        value={item.estimatedCost || ''}
                        onChange={e => updateItem(item._key, 'estimatedCost', Number(e.target.value))} />
                    </div>
                    <button type="button" onClick={() => removeItem(item._key)}
                      style={{ ...TOUCH, width: 44, background: 'none', border: '1px solid var(--hairline)', borderRadius: 8, cursor: 'pointer', color: 'var(--error)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 16, alignSelf: idx === 0 ? 'flex-end' : 'auto' }}
                      disabled={items.length === 1}
                    >
                      ✕
                    </button>
                  </div>
                ))}
              </div>
              <button type="button" onClick={addItem} className="btn-ghost"
                style={{ marginTop: 12, width: '100%', ...TOUCH, borderRadius: 10, borderStyle: 'dashed', borderWidth: 1.5, fontSize: 14 }}>
                + Thêm mục hư hại
              </button>
            </div>

            <div className="card" style={{ padding: '16px 18px', marginBottom: 14 }}>
              <p style={{ fontWeight: 700, fontSize: 15, color: 'var(--ink)', marginBottom: 12 }}>📷 Ảnh hư hại</p>
              {formErrors.photos && <ErrBanner msg={formErrors.photos} />}
              <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
                multiple
                capture="environment"
                onChange={handlePhotoAdd}
                style={{ display: 'none' }}
                id="damage-photo-input"
              />
              <button type="button"
                onClick={() => fileInputRef.current?.click()}
                style={{ width: '100%', ...TOUCH, borderRadius: 10, background: 'var(--surface-bone)', border: '1.5px dashed var(--hairline)', cursor: 'pointer', fontSize: 14, color: 'var(--charcoal)', fontWeight: 600, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}>
                📸 Chụp / Chọn ảnh
              </button>
              {photoPreviews.length > 0 && (
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 8, marginTop: 12 }}>
                  {photoPreviews.map((src, idx) => (
                    <div key={idx} style={{ position: 'relative' }}>
                      <img src={src} alt={`Ảnh ${idx + 1}`}
                        style={{ width: '100%', aspectRatio: '1', objectFit: 'cover', borderRadius: 8, border: '1px solid var(--hairline)' }} />
                      <button type="button" onClick={() => removePhoto(idx)}
                        style={{ position: 'absolute', top: -6, right: -6, width: 22, height: 22, borderRadius: '50%', background: 'var(--error)', color: '#fff', border: 'none', cursor: 'pointer', fontSize: 12, fontWeight: 700, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                        ✕
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="card" style={{ padding: '16px 18px', marginBottom: 16 }}>
              <label className="form-label" htmlFor="damage-notes">Ghi chú</label>
              <textarea id="damage-notes" className="textarea" rows={3}
                placeholder="Mô tả thêm về tình trạng hư hại..."
                value={notes} onChange={e => setNotes(e.target.value)} />
            </div>

            {totalCost > 0 && (
              <div style={{ background: totalCost > 5_000_000 ? 'rgba(220,38,38,0.08)' : 'rgba(15,118,110,0.08)', borderRadius: 12, padding: '12px 16px', marginBottom: 14, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontWeight: 600, fontSize: 14 }}>Tổng ước tính:</span>
                <span style={{ fontWeight: 800, fontSize: 18, color: totalCost > 5_000_000 ? '#dc2626' : 'var(--primary)' }}>{fmtVnd(totalCost)}</span>
              </div>
            )}
            {totalCost > 5_000_000 && (
              <div className="alert alert-error" style={{ marginBottom: 14, fontSize: 13 }}>
                ⚠️ Tổng phí &gt; {fmtVnd(5_000_000)} — sẽ được escalate lên Admin để co-approve.
              </div>
            )}

            <button type="submit" className="btn-primary"
              style={{ width: '100%', ...TOUCH, borderRadius: 12, fontWeight: 700, fontSize: 16, marginBottom: 24 }}>
              Xem trước & Xác nhận →
            </button>
          </form>
        )}
      </div>
    </EmployeeLayout>
  );
}
