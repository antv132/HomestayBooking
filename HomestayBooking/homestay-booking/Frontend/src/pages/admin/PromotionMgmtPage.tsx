import { useRef, useState, useEffect, type ChangeEvent } from 'react';
import AdminLayout from '../../layouts/AdminLayout';
import { promotionApi, type PromotionItem, type PromotionPayload } from '../../api/promotionApi';
import { resolveMediaUrl } from '../../utils/mediaUrl';

// ── Màu sắc hỗ trợ (FR-18 palette) ────────────────────────────────────────

const COLOR_OPTIONS = [
  { value: 'red',    label: 'Teal (Mặc định)', gradient: 'linear-gradient(135deg, #0F766E 0%, #0D9488 100%)' },
  { value: 'blue',   label: 'Xanh dương', gradient: 'linear-gradient(135deg, #1a3c5e 0%, #2d6a9f 100%)' },
  { value: 'green',  label: 'Xanh lá', gradient: 'linear-gradient(135deg, #1a5c3a 0%, #2e9c5e 100%)' },
  { value: 'purple', label: 'Tím',    gradient: 'linear-gradient(135deg, #4c1d8f 0%, #7c3aed 100%)' },
  { value: 'orange', label: 'Cam',    gradient: 'linear-gradient(135deg, #b45309 0%, #f59e0b 100%)' },
];

function getGradient(theme: string) {
  return COLOR_OPTIONS.find((c) => c.value === theme)?.gradient ?? COLOR_OPTIONS[0].gradient;
}

/** CTA destination presets — sync URL + suggested button label. */
const CTA_PRESETS = [
  { id: 'search', label: 'Tìm phòng', url: '/search', suggestedText: 'Tìm phòng' },
  { id: 'price-asc', label: 'Giá thấp → cao', url: '/search?sort=price-asc', suggestedText: 'Xem giá tốt' },
  { id: 'custom', label: 'Tùy chỉnh', url: '', suggestedText: '' },
] as const;

type CtaPresetId = (typeof CTA_PRESETS)[number]['id'];

function detectCtaPreset(ctaUrl: string): CtaPresetId {
  const trimmed = ctaUrl.trim();
  const match = CTA_PRESETS.find((p) => p.id !== 'custom' && p.url === trimmed);
  return match?.id ?? 'custom';
}

const EMPTY: PromotionPayload = {
  subtitle: '',
  title: '',
  description: '',
  ctaText: 'Tìm phòng',
  ctaUrl: '/search',
  imageUrl: '',
  colorTheme: 'red',
  isActive: true,
  sortOrder: 0,
};

function BannerPreview({ form }: { form: PromotionPayload }) {
  const hasImage = !!form.imageUrl?.trim();
  return (
    <div
      style={{
        borderRadius: 14,
        background: hasImage ? undefined : getGradient(form.colorTheme),
        backgroundImage: hasImage ? `url(${resolveMediaUrl(form.imageUrl)})` : undefined,
        backgroundSize: 'cover',
        backgroundPosition: 'center',
        padding: '22px 22px',
        display: 'flex',
        flexDirection: 'column',
        gap: 10,
        position: 'relative',
        overflow: 'hidden',
        minHeight: 180,
      }}
    >
      {hasImage && (
        <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(90deg, rgba(0,0,0,0.55) 0%, rgba(0,0,0,0.25) 100%)', pointerEvents: 'none' }} />
      )}
      {!hasImage && (
        <div style={{ position: 'absolute', top: -16, right: -16, width: 100, height: 100, borderRadius: '50%', background: 'rgba(255,255,255,0.08)', pointerEvents: 'none' }} />
      )}
      <span style={{ position: 'relative', zIndex: 2, fontSize: 11, fontWeight: 700, letterSpacing: '0.1em', color: 'rgba(255,255,255,0.75)', textTransform: 'uppercase' }}>
        {form.subtitle || 'Subtitle'}
      </span>
      <h3 style={{ position: 'relative', zIndex: 2, fontSize: 22, fontWeight: 800, color: '#fff', lineHeight: 1.25, margin: 0, whiteSpace: 'pre-line' }}>
        {form.title || 'Tiêu đề banner'}
      </h3>
      {form.description && (
        <p style={{ position: 'relative', zIndex: 2, fontSize: 12, color: 'rgba(255,255,255,0.85)', margin: 0 }}>{form.description}</p>
      )}
      <span
        style={{
          position: 'relative',
          zIndex: 2,
          marginTop: 4,
          display: 'inline-block',
          background: '#fff',
          color: '#333',
          fontWeight: 700,
          fontSize: 12,
          padding: '6px 14px',
          borderRadius: 9999,
          width: 'fit-content',
        }}
      >
        {form.ctaText || 'CTA'}
      </span>
    </div>
  );
}

// ── Form Modal ────────────────────────────────────────────────────────────

function BannerFormModal({
  initial,
  isEdit,
  onSave,
  onClose,
  saving,
}: {
  initial: PromotionPayload;
  isEdit: boolean;
  onSave: (p: PromotionPayload) => void;
  onClose: () => void;
  saving: boolean;
}) {
  const [form, setForm] = useState<PromotionPayload>(initial);
  const [ctaPreset, setCtaPreset] = useState<CtaPresetId>(() => detectCtaPreset(initial.ctaUrl));
  const [formError, setFormError] = useState('');
  const [uploading, setUploading] = useState(false);
  const fileRef = useRef<HTMLInputElement>(null);

  const set = (k: keyof PromotionPayload, v: string | number | boolean) =>
    setForm((p) => ({ ...p, [k]: v }));

  function applyCtaPreset(id: CtaPresetId) {
    setCtaPreset(id);
    const preset = CTA_PRESETS.find((p) => p.id === id);
    if (!preset || id === 'custom') return;
    setForm((p) => ({
      ...p,
      ctaUrl: preset.url,
      ctaText: preset.suggestedText || p.ctaText,
    }));
  }

  async function handleFileChange(e: ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    setUploading(true);
    setFormError('');
    try {
      const url = await promotionApi.uploadImage(file);
      set('imageUrl', url);
    } catch {
      setFormError('Tải ảnh thất bại. Chỉ chấp nhận JPEG/PNG/WebP ≤ 10MB.');
    } finally {
      setUploading(false);
    }
  }

  function validateAndSave() {
    if (!form.subtitle.trim() || !form.title.trim() || !form.ctaText.trim() || !form.ctaUrl.trim()) {
      setFormError('Vui lòng điền đủ title, subtitle, CTA text và đích CTA.');
      return;
    }
    const url = form.ctaUrl.trim();
    if (!(url.startsWith('/') || url.toLowerCase().startsWith('https://') || url.toLowerCase().startsWith('http://'))) {
      setFormError('CTA URL phải bắt đầu bằng / hoặc http(s)://');
      return;
    }
    setFormError('');
    onSave({
      ...form,
      imageUrl: form.imageUrl?.trim() ?? '',
    });
  }

  return (
    <div
      style={{
        position: 'fixed', inset: 0, zIndex: 9000,
        background: 'rgba(0,0,0,0.5)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        padding: 16,
      }}
      onClick={(e) => e.target === e.currentTarget && onClose()}
    >
      <div
        style={{
          background: 'var(--canvas, #fff)',
          borderRadius: 16,
          width: '100%',
          maxWidth: 700,
          maxHeight: '90vh',
          overflow: 'auto',
          padding: '28px 28px 24px',
          boxShadow: '0 20px 60px rgba(0,0,0,0.2)',
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
          <h2 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>
            {isEdit ? 'Chỉnh sửa banner' : 'Tạo banner mới'}
          </h2>
          <button
            type="button"
            onClick={onClose}
            style={{ background: 'none', border: 'none', fontSize: 22, cursor: 'pointer', color: '#666' }}
          >
            ×
          </button>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24 }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            <div>
              <label className="form-label">Subtitle (dòng nhỏ phía trên) *</label>
              <input className="input" value={form.subtitle} onChange={(e) => set('subtitle', e.target.value)} placeholder="VD: Ưu đãi cuối tuần" />
            </div>
            <div>
              <label className="form-label">Tiêu đề lớn * <span style={{ fontWeight: 400, fontSize: 11, color: '#888' }}>(xuống dòng bằng Enter)</span></label>
              <textarea
                className="input"
                value={form.title}
                onChange={(e) => set('title', e.target.value)}
                placeholder={"VD: Giảm 20%\nthứ 6 – chủ nhật"}
                rows={2}
                style={{ resize: 'vertical', fontFamily: 'inherit' }}
              />
            </div>
            <div>
              <label className="form-label">Mô tả ngắn</label>
              <textarea
                className="input"
                value={form.description}
                onChange={(e) => set('description', e.target.value)}
                placeholder="Áp dụng cho tất cả phòng Deluxe & Suite..."
                rows={2}
                style={{ resize: 'vertical', fontFamily: 'inherit' }}
              />
            </div>

            {/* CTA destination */}
            <div>
              <label className="form-label" htmlFor="cta-dest">Đích nút CTA *</label>
              <select
                id="cta-dest"
                className="input"
                value={ctaPreset}
                onChange={(e) => applyCtaPreset(e.target.value as CtaPresetId)}
              >
                {CTA_PRESETS.map((p) => (
                  <option key={p.id} value={p.id}>{p.label}</option>
                ))}
              </select>
              {ctaPreset !== 'custom' && (
                <p style={{ margin: '6px 0 0', fontSize: 12, color: '#888' }}>
                  URL: <code>{CTA_PRESETS.find((p) => p.id === ctaPreset)?.url}</code>
                </p>
              )}
            </div>

            <div>
              <label className="form-label">Text nút CTA *</label>
              <input className="input" value={form.ctaText} onChange={(e) => set('ctaText', e.target.value)} placeholder="Tìm phòng" />
            </div>

            {ctaPreset === 'custom' && (
              <div>
                <label className="form-label">URL nút CTA *</label>
                <input
                  className="input"
                  value={form.ctaUrl}
                  onChange={(e) => set('ctaUrl', e.target.value)}
                  placeholder="/search hoặc https://..."
                />
              </div>
            )}

            {/* Image upload */}
            <div>
              <label className="form-label">Ảnh banner</label>
              <input
                ref={fileRef}
                type="file"
                accept="image/jpeg,image/png,image/webp"
                style={{ display: 'none' }}
                onChange={handleFileChange}
              />
              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginTop: 4 }}>
                <button
                  type="button"
                  className="btn-outline"
                  disabled={uploading || saving}
                  onClick={() => fileRef.current?.click()}
                  style={{ fontSize: 13 }}
                >
                  {uploading ? 'Đang tải...' : form.imageUrl ? 'Đổi ảnh' : 'Tải ảnh lên'}
                </button>
                {form.imageUrl && (
                  <button
                    type="button"
                    className="btn-ghost"
                    disabled={uploading || saving}
                    onClick={() => set('imageUrl', '')}
                    style={{ fontSize: 13, color: '#d32f2f' }}
                  >
                    Xóa ảnh
                  </button>
                )}
              </div>
              <p style={{ margin: '6px 0 0', fontSize: 11, color: '#888' }}>
                JPEG / PNG / WebP, tối đa 10MB. Không có ảnh → dùng màu nền.
              </p>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
              <div>
                <label className="form-label">Thứ tự hiển thị</label>
                <input type="number" className="input" value={form.sortOrder} min={0} onChange={(e) => set('sortOrder', Number(e.target.value))} />
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                <label className="form-label">Trạng thái</label>
                <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer', marginTop: 6 }}>
                  <input type="checkbox" checked={form.isActive} onChange={(e) => set('isActive', e.target.checked)} style={{ width: 16, height: 16 }} />
                  <span style={{ fontSize: 14 }}>Hiển thị</span>
                </label>
              </div>
            </div>

            <div>
              <label className="form-label">Chủ đề màu {form.imageUrl ? '(dự phòng khi không có ảnh)' : ''}</label>
              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginTop: 4 }}>
                {COLOR_OPTIONS.map((c) => (
                  <button
                    key={c.value}
                    type="button"
                    onClick={() => set('colorTheme', c.value)}
                    title={c.label}
                    style={{
                      width: 36, height: 36, borderRadius: 8,
                      background: c.gradient,
                      border: form.colorTheme === c.value ? '3px solid var(--primary, #0068c9)' : '2px solid transparent',
                      cursor: 'pointer',
                      outline: form.colorTheme === c.value ? '2px solid #fff' : 'none',
                      outlineOffset: '-4px',
                    }}
                  />
                ))}
              </div>
            </div>
            {formError && (
              <p style={{ margin: 0, fontSize: 12, color: 'var(--error, #e53)' }}>{formError}</p>
            )}
          </div>

          <div>
            <label className="form-label" style={{ marginBottom: 8 }}>Xem trước</label>
            <BannerPreview form={form} />
          </div>
        </div>

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 24 }}>
          <button className="btn-outline" onClick={onClose} disabled={saving || uploading}>Hủy</button>
          <button
            className="btn-primary"
            disabled={saving || uploading || !form.subtitle || !form.title || !form.ctaText || !form.ctaUrl}
            onClick={validateAndSave}
          >
            {saving ? 'Đang lưu...' : 'Lưu banner'}
          </button>
        </div>
      </div>
    </div>
  );
}

// ── Main Page ─────────────────────────────────────────────────────────────

export default function PromotionMgmtPage() {
  const [banners, setBanners] = useState<PromotionItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<PromotionItem | null>(null);
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const load = async () => {
    setLoading(true);
    try {
      const data = await promotionApi.getAll();
      setBanners(data);
    } catch {
      setError('Không thể tải danh sách banner.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const handleSave = async (form: PromotionPayload) => {
    setSaving(true);
    try {
      if (editing) {
        await promotionApi.update(editing.id, form);
      } else {
        await promotionApi.create(form);
      }
      setModalOpen(false);
      setEditing(null);
      load();
    } catch {
      alert('Lưu thất bại. Vui lòng thử lại.');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Bạn có chắc muốn xóa banner này?')) return;
    setDeletingId(id);
    try {
      await promotionApi.delete(id);
      setBanners((prev) => prev.filter((b) => b.id !== id));
    } catch {
      alert('Xóa thất bại.');
    } finally {
      setDeletingId(null);
    }
  };

  const handleToggleActive = async (b: PromotionItem) => {
    try {
      const updated = await promotionApi.patchActive(b.id, !b.isActive);
      setBanners((prev) => prev.map((x) => (x.id === b.id ? { ...x, ...updated } : x)));
    } catch {
      alert('Không cập nhật được trạng thái.');
    }
  };

  const openNew = () => { setEditing(null); setModalOpen(true); };
  const openEdit = (b: PromotionItem) => { setEditing(b); setModalOpen(true); };

  return (
    <AdminLayout>
      <div style={{ maxWidth: 1100, margin: '0 auto', padding: '32px 24px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 28 }}>
          <div>
            <h1 style={{ margin: 0, fontSize: 24, fontWeight: 700 }}>Quản lý Banner</h1>
            <p style={{ margin: '4px 0 0', color: '#666', fontSize: 14 }}>
              Tạo, chỉnh sửa hoặc ẩn/hiện các banner trên trang chủ
            </p>
          </div>
          <button className="btn-primary" onClick={openNew} style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            + Thêm banner
          </button>
        </div>

        {loading && <p style={{ color: '#888' }}>Đang tải...</p>}
        {error && <p style={{ color: 'var(--error, #e53)' }}>{error}</p>}

        {!loading && !error && (
          banners.length === 0 ? (
            <div style={{
              textAlign: 'center', padding: '60px 24px',
              border: '2px dashed #ddd', borderRadius: 14,
              color: '#999',
            }}>
              <p style={{ fontSize: 15, marginBottom: 12 }}>Chưa có banner nào.</p>
              <button className="btn-primary" onClick={openNew}>+ Tạo banner đầu tiên</button>
            </div>
          ) : (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: 20 }}>
              {banners.map((b) => {
                const hasImage = !!b.imageUrl?.trim();
                return (
                  <div
                    key={b.id}
                    style={{
                      borderRadius: 14,
                      border: '1px solid var(--border, #eee)',
                      overflow: 'hidden',
                      background: 'var(--canvas, #fff)',
                      boxShadow: '0 2px 8px rgba(0,0,0,0.06)',
                      opacity: b.isActive ? 1 : 0.55,
                    }}
                  >
                    <div
                      style={{
                        height: 160,
                        background: hasImage ? undefined : getGradient(b.colorTheme),
                        backgroundImage: hasImage ? `url(${resolveMediaUrl(b.imageUrl)})` : undefined,
                        backgroundSize: 'cover',
                        backgroundPosition: 'center',
                        padding: '18px 18px',
                        display: 'flex',
                        flexDirection: 'column',
                        gap: 8,
                        position: 'relative',
                        overflow: 'hidden',
                      }}
                    >
                      {hasImage && (
                        <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(90deg, rgba(0,0,0,0.55) 0%, rgba(0,0,0,0.25) 100%)', pointerEvents: 'none' }} />
                      )}
                      {!hasImage && (
                        <div style={{ position: 'absolute', top: -12, right: -12, width: 80, height: 80, borderRadius: '50%', background: 'rgba(255,255,255,0.08)', pointerEvents: 'none' }} />
                      )}
                      <span style={{ position: 'relative', zIndex: 2, fontSize: 10, fontWeight: 700, letterSpacing: '0.1em', color: 'rgba(255,255,255,0.7)', textTransform: 'uppercase' }}>{b.subtitle}</span>
                      <h3 style={{ position: 'relative', zIndex: 2, margin: 0, fontSize: 18, fontWeight: 800, color: '#fff', lineHeight: 1.3, whiteSpace: 'pre-line' }}>{b.title}</h3>
                      {b.description && <p style={{ position: 'relative', zIndex: 2, margin: 0, fontSize: 11, color: 'rgba(255,255,255,0.8)' }}>{b.description}</p>}
                      <span style={{ position: 'relative', zIndex: 2, display: 'inline-block', background: '#fff', fontWeight: 700, fontSize: 11, padding: '4px 12px', borderRadius: 9999, width: 'fit-content', color: '#333' }}>
                        {b.ctaText}
                      </span>
                    </div>

                    <div style={{ padding: '12px 16px', display: 'flex', flexDirection: 'column', gap: 8 }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <button
                          type="button"
                          onClick={() => handleToggleActive(b)}
                          style={{
                            fontSize: 11, fontWeight: 700, padding: '2px 10px', borderRadius: 9999,
                            border: 'none', cursor: 'pointer',
                            background: b.isActive ? 'rgba(16,185,129,0.12)' : 'rgba(0,0,0,0.06)',
                            color: b.isActive ? '#059669' : '#999',
                          }}
                          title="Bật/tắt hiển thị"
                        >
                          {b.isActive ? '● Đang hiển thị' : '○ Đã ẩn'}
                        </button>
                        <span style={{ fontSize: 11, color: '#999' }}>Thứ tự: {b.sortOrder}</span>
                      </div>
                      <div style={{ display: 'flex', gap: 8 }}>
                        <button
                          className="btn-outline"
                          onClick={() => openEdit(b)}
                          style={{ flex: 1, fontSize: 13, padding: '6px 0' }}
                        >
                          Chỉnh sửa
                        </button>
                        <button
                          onClick={() => handleDelete(b.id)}
                          disabled={deletingId === b.id}
                          style={{
                            flex: 1, fontSize: 13, padding: '6px 0',
                            background: 'rgba(229,57,53,0.08)',
                            border: '1px solid rgba(229,57,53,0.2)',
                            borderRadius: 8, color: '#d32f2f', cursor: 'pointer',
                            fontWeight: 600,
                          }}
                        >
                          {deletingId === b.id ? '...' : 'Xóa'}
                        </button>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )
        )}
      </div>

      {modalOpen && (
        <BannerFormModal
          isEdit={!!editing}
          initial={editing ? {
            subtitle: editing.subtitle,
            title: editing.title,
            description: editing.description ?? '',
            ctaText: editing.ctaText,
            ctaUrl: editing.ctaUrl,
            imageUrl: editing.imageUrl ?? '',
            colorTheme: editing.colorTheme,
            isActive: editing.isActive,
            sortOrder: editing.sortOrder,
          } : EMPTY}
          onSave={handleSave}
          onClose={() => { setModalOpen(false); setEditing(null); }}
          saving={saving}
        />
      )}
    </AdminLayout>
  );
}
