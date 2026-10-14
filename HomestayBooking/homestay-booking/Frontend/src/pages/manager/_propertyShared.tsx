// _propertyShared.tsx — Shared types, helpers, components dùng chung cho Property pages
import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

// ── Types ────────────────────────────────────────────────────────────────────

export interface PropertyFormData {
  name: string;
  address: string;
  description: string;
  status: 'ACTIVE' | 'INACTIVE';
}

// ── Helpers ──────────────────────────────────────────────────────────────────

export function StatusBadge({ status }: { status: string }) {
  if (status === 'ACTIVE') {
    return <span className="badge badge-success">Active</span>;
  }
  return <span className="badge badge-neutral">Inactive</span>;
}

export function formatDate(dateStr: string | null | undefined) {
  if (!dateStr) return '—';
  return new Date(dateStr).toLocaleDateString('vi-VN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
  });
}

// ── Character Counter ─────────────────────────────────────────────────────────

function CharCounter({ value, max }: { value: string; max: number }) {
  const len   = value.length;
  const pct   = len / max;
  const color = pct >= 1 ? 'var(--error)' : pct >= 0.85 ? 'var(--warning)' : 'var(--ash)';
  return (
    <span
      className="caption"
      style={{ color, transition: 'color 0.15s', marginLeft: 'auto', flexShrink: 0 }}
    >
      {len}/{max}
    </span>
  );
}

// ── Status Radio Card ─────────────────────────────────────────────────────────

interface StatusOption {
  value: 'ACTIVE' | 'INACTIVE';
  label: string;
  description: string;
  icon: string;
  badgeClass: string;
}

const STATUS_OPTIONS: StatusOption[] = [
  {
    value: 'ACTIVE',
    label: 'Hoạt động',
    description: 'Property sẵn sàng nhận đặt phòng',
    icon: '✓',
    badgeClass: 'badge-success',
  },
  {
    value: 'INACTIVE',
    label: 'Tạm đóng',
    description: 'Property chưa mở hoặc đang bảo trì',
    icon: '—',
    badgeClass: 'badge-neutral',
  },
];

function StatusRadioCards({
  value,
  onChange,
}: {
  value: 'ACTIVE' | 'INACTIVE';
  onChange: (v: 'ACTIVE' | 'INACTIVE') => void;
}) {
  return (
    <div style={{ display: 'flex', gap: 12 }}>
      {STATUS_OPTIONS.map((opt) => {
        const selected = value === opt.value;
        return (
          <button
            key={opt.value}
            type="button"
            id={`status-${opt.value.toLowerCase()}`}
            onClick={() => onChange(opt.value)}
            style={{
              flex: 1,
              display: 'flex',
              alignItems: 'center',
              gap: 12,
              padding: '14px 16px',
              borderRadius: 10,
              border: selected
                ? `2px solid var(--${opt.value === 'ACTIVE' ? 'success' : 'neutral'})`
                : '1.5px solid var(--hairline)',
              background: selected
                ? opt.value === 'ACTIVE'
                  ? '#f0fdf4'
                  : '#f9fafb'
                : 'var(--surface-card)',
              cursor: 'pointer',
              transition: 'all 0.15s',
              textAlign: 'left',
            }}
          >
            {/* Radio circle */}
            <span
              style={{
                width: 18,
                height: 18,
                borderRadius: '50%',
                border: selected
                  ? `5px solid var(--${opt.value === 'ACTIVE' ? 'success' : 'neutral'})`
                  : '2px solid var(--stone)',
                flexShrink: 0,
                transition: 'all 0.15s',
                background: 'var(--surface-card)',
              }}
            />
            <span style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              <span
                className="body-sm"
                style={{ fontWeight: 600, color: 'var(--ink)' }}
              >
                {opt.label}
              </span>
              <span className="caption" style={{ color: 'var(--charcoal)' }}>
                {opt.description}
              </span>
            </span>
          </button>
        );
      })}
    </div>
  );
}

// ── Shared PropertyForm (dùng cho Add + Edit) ─────────────────────────────────

export function PropertyForm({
  initial,
  onSubmit,
  loading,
  submitLabel  = 'Save Property',
  cancelPath   = '/manager/properties',
  onFormChange,
  onCancel,
}: {
  initial:       PropertyFormData;
  onSubmit:      (v: PropertyFormData) => void;
  loading:       boolean;
  submitLabel?:  string;
  cancelPath?:   string;
  /** Called on every form field change — useful for dirty tracking in parent */
  onFormChange?: (v: PropertyFormData) => void;
  /** Override default cancel behaviour (e.g., for dirty-check confirm) */
  onCancel?:     () => void;
}) {
  const navigate = useNavigate();
  const [form, setForm]     = useState<PropertyFormData>(initial);
  const [errors, setErrors] = useState<Record<string, string>>({});

  // Sync lại form khi initial thay đổi (sau khi data load xong ở EditPage)
  useEffect(() => { setForm(initial); }, [initial.name, initial.address]);

  function updateField<K extends keyof PropertyFormData>(key: K, value: PropertyFormData[K]) {
    const next = { ...form, [key]: value };
    setForm(next);
    onFormChange?.(next);
  }

  function validate() {
    const e: Record<string, string> = {};
    if (!form.name.trim())             e.name    = 'Property name is required';
    if (form.name.length > 200)        e.name    = 'Maximum 200 characters';
    if (!form.address.trim())          e.address = 'Address is required';
    if (form.address.length > 500)     e.address = 'Maximum 500 characters';
    if (form.description.length > 2000) e.description = 'Maximum 2000 characters';
    return e;
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length) { setErrors(errs); return; }
    setErrors({});
    onSubmit(form);
  }

  return (
    <form onSubmit={handleSubmit} className="card-lg" style={{ padding: 32 }}>

      {/* ── Property Name ─────────────────────────────── */}
      <div style={{ marginBottom: 24 }}>
        <div style={{ display: 'flex', alignItems: 'center', marginBottom: 6 }}>
          <label className="form-label form-label-required" htmlFor="propName" style={{ marginBottom: 0 }}>
            Property Name
          </label>
          <CharCounter value={form.name} max={200} />
        </div>
        <div style={{ position: 'relative' }}>
          <span
            style={{
              position: 'absolute',
              left: 16,
              top: '50%',
              transform: 'translateY(-50%)',
              fontSize: 16,
              pointerEvents: 'none',
              userSelect: 'none',
            }}
          >
            🏢
          </span>
          <input
            id="propName"
            className={`input ${errors.name ? 'input-error' : ''}`}
            placeholder="VD: Sunset Resort Đà Nẵng"
            value={form.name}
            onChange={e => updateField('name', e.target.value)}
            maxLength={200}
            style={{ paddingLeft: 44 }}
            autoFocus
          />
        </div>
        {errors.name && <p className="form-error">{errors.name}</p>}
      </div>

      {/* ── Address ───────────────────────────────────── */}
      <div style={{ marginBottom: 24 }}>
        <div style={{ display: 'flex', alignItems: 'center', marginBottom: 6 }}>
          <label className="form-label form-label-required" htmlFor="propAddr" style={{ marginBottom: 0 }}>
            Address
          </label>
          <CharCounter value={form.address} max={500} />
        </div>
        <div style={{ position: 'relative' }}>
          <span
            style={{
              position: 'absolute',
              left: 16,
              top: '50%',
              transform: 'translateY(-50%)',
              fontSize: 16,
              pointerEvents: 'none',
              userSelect: 'none',
            }}
          >
            📍
          </span>
          <input
            id="propAddr"
            className={`input ${errors.address ? 'input-error' : ''}`}
            placeholder="VD: 123 Nguyễn Tất Thành, Đà Nẵng"
            value={form.address}
            onChange={e => updateField('address', e.target.value)}
            maxLength={500}
            style={{ paddingLeft: 44 }}
          />
        </div>
        <p className="form-hint">Địa chỉ đầy đủ, bao gồm quận/huyện và tỉnh/thành phố</p>
        {errors.address && <p className="form-error">{errors.address}</p>}
      </div>

      {/* ── Description ──────────────────────────────── */}
      <div style={{ marginBottom: 24 }}>
        <div style={{ display: 'flex', alignItems: 'center', marginBottom: 6 }}>
          <label className="form-label" htmlFor="propDesc" style={{ marginBottom: 0 }}>
            Description
            <span className="caption" style={{ color: 'var(--ash)', fontWeight: 400, marginLeft: 6 }}>(optional)</span>
          </label>
          <CharCounter value={form.description} max={2000} />
        </div>
        <textarea
          id="propDesc"
          className={`textarea ${errors.description ? 'input-error' : ''}`}
          rows={5}
          placeholder="Mô tả vị trí, tiện ích, phong cách thiết kế và điểm nổi bật của property..."
          value={form.description}
          onChange={e => updateField('description', e.target.value)}
          maxLength={2000}
          style={{ resize: 'vertical' }}
        />
        {errors.description && <p className="form-error">{errors.description}</p>}
      </div>

      {/* ── Status Radio Cards ────────────────────────── */}
      <div style={{ marginBottom: 32 }}>
        <label className="form-label" style={{ marginBottom: 10 }}>Status</label>
        <StatusRadioCards
          value={form.status}
          onChange={v => updateField('status', v)}
        />
      </div>

      {/* ── Actions ──────────────────────────────────── */}
      <div
        style={{
          display: 'flex',
          gap: 12,
          paddingTop: 24,
          borderTop: '1px solid var(--hairline)',
        }}
      >
        <button
          type="submit"
          className="btn-primary"
          disabled={loading}
          id="btn-save-property"
          style={{ minWidth: 160 }}
        >
          {loading ? (
            <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <svg
                width="16" height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                style={{ animation: 'spin 1s linear infinite' }}
              >
                <path d="M21 12a9 9 0 1 1-6.219-8.56" />
              </svg>
              Saving...
            </span>
          ) : (
            <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z" />
                <polyline points="17 21 17 13 7 13 7 21" />
                <polyline points="7 3 7 8 15 8" />
              </svg>
              {submitLabel}
            </span>
          )}
        </button>
        <button
          type="button"
          className="btn-ghost"
          onClick={() => onCancel ? onCancel() : navigate(cancelPath)}
          id="btn-cancel-property"
        >
          Cancel
        </button>
      </div>
    </form>
  );
}
