import { useState, useEffect, useRef } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import ManagerLayout from '../../layouts/ManagerLayout';
import Alert from '../../components/ui/Alert';
import { StatusBadge } from '../../components/ui';
import type { StatusVariant } from '../../components/ui/StatusBadge';
import {
  fetchManagerRoomV1,
  updateRoomV1,
  ROOM_TYPES,
  type RoomDetail,
} from '../../api/roomsApi';
import { floorApi, type FloorSummary } from '../../api/floorApi';
import RoomGalleryTab from './tabs/RoomGalleryTab';
import RoomStatusTab from './tabs/RoomStatusTab';
import { AMENITY_OPTIONS, fixAmenityLabels } from '../../utils/amenities';

type TabKey = 'info' | 'gallery' | 'status';

const TABS: { key: TabKey; label: string }[] = [
  { key: 'info', label: 'Thông tin' },
  { key: 'gallery', label: 'Hình ảnh' },
  { key: 'status', label: 'Trạng thái' },
];

const STATUS_BADGE: Record<string, { label: string; variant: StatusVariant }> = {
  AVAILABLE:            { label: 'Trống',         variant: 'success' },
  PENDING_DEPOSIT:      { label: 'Chờ cọc',       variant: 'warning' },
  RESERVED:             { label: 'Đã đặt',        variant: 'info' },
  OCCUPIED:             { label: 'Đang ở',        variant: 'primary' },
  PENDING_CLEANING:     { label: 'Chờ dọn',       variant: 'warning' },
  CLEANING_IN_PROGRESS: { label: 'Đang dọn',      variant: 'info' },
  MAINTENANCE:          { label: 'Bảo trì',       variant: 'danger' },
  OUT_OF_SERVICE:       { label: 'Ngưng phục vụ', variant: 'neutral' },
};

function parseTab(raw: string | null): TabKey {
  if (raw === 'gallery' || raw === 'status' || raw === 'info') return raw;
  return 'info';
}

function FieldError({ msg }: { msg: string }) {
  return <p className="text-xs text-red-600 mt-1 font-medium">{msg}</p>;
}

function Toast({ message, onClose }: { message: string; onClose: () => void }) {
  useEffect(() => {
    const t = setTimeout(onClose, 1200);
    return () => clearTimeout(t);
  }, [onClose]);

  return (
    <div
      className="fixed bottom-6 right-6 z-[9999] px-5 py-3.5 rounded-xl text-sm font-medium text-white shadow-lg"
      style={{ background: '#202020', borderLeft: '4px solid #2b9a66' }}
    >
      {message}
    </div>
  );
}

export default function EditRoomPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const activeTab = parseTab(searchParams.get('tab'));

  const [room, setRoom] = useState<RoomDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [form, setForm] = useState({
    floorId: '',
    roomNumber: '',
    roomType: 'Standard',
    pricePerNight: '',
    capacity: '',
    area: '',
    description: '',
  });
  const [amenities, setAmenities] = useState<string[]>([]);
  const [floors, setFloors] = useState<FloorSummary[]>([]);
  const [floorsLoading, setFloorsLoading] = useState(false);

  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [roomNumberError, setRoomNumberError] = useState('');
  const [toast, setToast] = useState<string | null>(null);
  const initialized = useRef(false);

  function setTab(tab: TabKey) {
    setSearchParams(tab === 'info' ? {} : { tab }, { replace: true });
  }

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    fetchManagerRoomV1(id)
      .then((data) => {
        setRoom(data);
        setLoadError(null);
      })
      .catch((err: unknown) => {
        const axiosErr = err as { response?: { data?: { message?: string } } };
        setLoadError(axiosErr?.response?.data?.message ?? 'Không tải được thông tin phòng.');
      })
      .finally(() => setLoading(false));
  }, [id]);

  useEffect(() => {
    if (!room || initialized.current) return;
    initialized.current = true;
    setForm({
      floorId: room.floorId,
      roomNumber: room.roomNumber,
      roomType: room.roomType ?? 'Standard',
      pricePerNight: String(room.pricePerNight),
      capacity: String(room.capacity),
      area: room.area ? String(room.area) : '',
      description: room.description ?? '',
    });
    setAmenities(fixAmenityLabels(room.amenities));

    if (room.propertyId) {
      setFloorsLoading(true);
      floorApi.getByProperty(room.propertyId)
        .then((res) => setFloors(res.data ?? []))
        .catch(() => setFloors([]))
        .finally(() => setFloorsLoading(false));
    }
  }, [room]);

  function handleField(field: keyof typeof form, value: string) {
    setForm((f) => ({ ...f, [field]: value }));
    if (field === 'roomNumber') setRoomNumberError('');
    setError(null);
  }

  function toggleAmenity(name: string) {
    setAmenities((prev) =>
      prev.includes(name) ? prev.filter((a) => a !== name) : [...prev, name],
    );
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!id) return;

    setError(null);
    setRoomNumberError('');

    if (!form.floorId) {
      setError('Vui lòng chọn tầng.');
      return;
    }
    if (!form.pricePerNight || Number(form.pricePerNight) <= 0) {
      setError('Giá/đêm phải lớn hơn 0.');
      return;
    }
    if (!form.capacity || Number(form.capacity) < 1) {
      setError('Sức chứa tối thiểu 1 người.');
      return;
    }

    setSaving(true);
    try {
      await updateRoomV1(id, {
        floorId: form.floorId,
        roomNumber: form.roomNumber.trim(),
        roomType: form.roomType,
        pricePerNight: Number(form.pricePerNight),
        capacity: Number(form.capacity),
        area: form.area ? Number(form.area) : null,
        description: form.description.trim() || null,
        amenities,
      });
      setToast('Đã cập nhật phòng');
      setTimeout(() => navigate('/manager/rooms'), 1200);
    } catch (err: unknown) {
      const axiosErr = err as { response?: { status?: number; data?: { message?: string } } };
      const status = axiosErr?.response?.status;
      const message = axiosErr?.response?.data?.message ?? 'Cập nhật thất bại. Vui lòng thử lại.';
      if (status === 409) setRoomNumberError(message);
      else setError(message);
      setSaving(false);
    }
  }

  if (loading) {
    return (
      <ManagerLayout>
        <div className="w-full min-w-0 space-y-3">
          <div className="h-8 w-48 bg-[#E2E8F0] rounded animate-pulse" />
          <div className="h-12 bg-[#E2E8F0] rounded-xl animate-pulse" />
          <div className="h-64 bg-[#E2E8F0] rounded-xl animate-pulse" />
        </div>
      </ManagerLayout>
    );
  }

  if (loadError || !room || !id) {
    return (
      <ManagerLayout>
        <div className="w-full max-w-lg">
          <Alert variant="error" message={loadError ?? 'Không tìm thấy phòng.'} />
          <Link to="/manager/rooms" className="btn-ghost mt-4 inline-block">
            ← Quay lại danh sách
          </Link>
        </div>
      </ManagerLayout>
    );
  }

  const statusMeta = STATUS_BADGE[room.status];

  return (
    <ManagerLayout>
      {toast && <Toast message={toast} onClose={() => setToast(null)} />}

      <div className="w-full min-w-0 flex flex-col gap-4">
        {/* Page header — compact, one row */}
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div className="min-w-0">
            <nav className="flex items-center gap-1.5 text-xs text-[#64748B] mb-1.5 flex-wrap">
              <Link to="/manager/rooms" className="text-primary no-underline hover:underline">
                Phòng
              </Link>
              <span aria-hidden>›</span>
              <span className="text-[#0F172A] font-medium truncate">Sửa phòng</span>
            </nav>
            <div className="flex items-center gap-3 flex-wrap">
              <h1 className="heading-md m-0 truncate">Phòng {room.roomNumber}</h1>
              {statusMeta ? (
                <StatusBadge status={statusMeta.label} variant={statusMeta.variant} />
              ) : (
                <StatusBadge status={room.status} variant="neutral" />
              )}
            </div>
            <p className="text-sm text-[#64748B] mt-1 truncate">
              {room.propertyName}
              {room.floorNumber != null ? ` · Tầng ${room.floorNumber}` : ''}
              {room.roomType ? ` · ${room.roomType}` : ''}
            </p>
          </div>
          <Link
            to={`/manager/rooms/${id}`}
            className="btn-outline btn-sm shrink-0"
            style={{
              borderRadius: 9999,
              borderColor: 'var(--primary)',
              color: 'var(--primary)',
              background: 'rgba(15, 118, 110, 0.06)',
            }}
          >
            ← Quay lại
          </Link>
        </div>

        {/* Single shell: tabs + panel fit main frame */}
        <div
          className="card w-full min-w-0 overflow-hidden"
          style={{ boxShadow: 'none' }}
        >
          <div
            role="tablist"
            aria-label="Chỉnh sửa phòng"
            className="flex gap-0 overflow-x-auto border-b border-[#E2E8F0] px-2 sm:px-4"
            style={{ scrollbarWidth: 'none' }}
          >
            {TABS.map((t) => {
              const active = activeTab === t.key;
              return (
                <button
                  key={t.key}
                  type="button"
                  role="tab"
                  aria-selected={active}
                  onClick={() => setTab(t.key)}
                  className="shrink-0 px-3.5 sm:px-4 py-3 text-sm font-semibold bg-transparent border-0 cursor-pointer whitespace-nowrap"
                  style={{
                    color: active ? 'var(--primary)' : '#64748B',
                    borderBottom: active ? '2px solid var(--primary)' : '2px solid transparent',
                    marginBottom: -1,
                  }}
                >
                  {t.label}
                </button>
              );
            })}
          </div>

          <div role="tabpanel" className="p-4 sm:p-5 lg:p-6">
            {activeTab === 'info' && (
              <>
                {error && (
                  <div className="mb-4">
                    <Alert variant="error" message={error} closeable onClose={() => setError(null)} />
                  </div>
                )}

                <form onSubmit={handleSubmit} className="flex flex-col gap-5">
                  <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-x-4 gap-y-3.5">
                    <div>
                      <label className="form-label form-label-required" htmlFor="roomNumber">
                        Số phòng
                      </label>
                      <input
                        id="roomNumber"
                        className="input"
                        value={form.roomNumber}
                        onChange={(e) => handleField('roomNumber', e.target.value)}
                        required
                        style={{ borderColor: roomNumberError ? '#DC2626' : undefined }}
                      />
                      {roomNumberError && <FieldError msg={roomNumberError} />}
                    </div>

                    <div>
                      <label className="form-label form-label-required" htmlFor="floorId">
                        Tầng
                      </label>
                      {floorsLoading ? (
                        <div className="h-11 bg-[#F8FAFC] rounded-lg flex items-center px-4 text-sm text-[#64748B]">
                          Đang tải…
                        </div>
                      ) : (
                        <select
                          id="floorId"
                          className="select"
                          value={form.floorId}
                          onChange={(e) => handleField('floorId', e.target.value)}
                          required
                        >
                          <option value="">— Chọn tầng —</option>
                          {floors.map((fl) => (
                            <option key={fl.id} value={fl.id}>
                              Tầng {fl.floorNumber}
                              {fl.description ? ` — ${fl.description}` : ''}
                            </option>
                          ))}
                        </select>
                      )}
                    </div>

                    <div>
                      <label className="form-label" htmlFor="roomType">Loại phòng</label>
                      <select
                        id="roomType"
                        className="select"
                        value={form.roomType}
                        onChange={(e) => handleField('roomType', e.target.value)}
                      >
                        {ROOM_TYPES.map((t) => (
                          <option key={t} value={t}>{t}</option>
                        ))}
                      </select>
                    </div>

                    <div>
                      <label className="form-label form-label-required" htmlFor="pricePerNight">
                        Giá/đêm (₫)
                      </label>
                      <input
                        id="pricePerNight"
                        type="number"
                        className="input"
                        min={1000}
                        step={1000}
                        value={form.pricePerNight}
                        onChange={(e) => handleField('pricePerNight', e.target.value)}
                        required
                      />
                    </div>

                    <div>
                      <label className="form-label form-label-required" htmlFor="capacity">
                        Sức chứa
                      </label>
                      <input
                        id="capacity"
                        type="number"
                        className="input"
                        min={1}
                        max={20}
                        value={form.capacity}
                        onChange={(e) => handleField('capacity', e.target.value)}
                        required
                      />
                    </div>

                    <div>
                      <label className="form-label" htmlFor="area">Diện tích (m²)</label>
                      <input
                        id="area"
                        type="number"
                        className="input"
                        min={0}
                        step={0.5}
                        value={form.area}
                        onChange={(e) => handleField('area', e.target.value)}
                      />
                    </div>
                  </div>

                  <div>
                    <label className="form-label mb-2 block">Tiện nghi</label>
                    <div className="flex flex-wrap gap-2">
                      {AMENITY_OPTIONS.map((name) => {
                        const on = amenities.includes(name);
                        return (
                          <button
                            key={name}
                            type="button"
                            onClick={() => toggleAmenity(name)}
                            className="text-xs font-medium px-3 py-1.5 rounded-full border cursor-pointer transition-colors"
                            style={{
                              background: on ? 'rgba(15,118,110,0.10)' : '#fff',
                              borderColor: on ? 'var(--primary)' : '#E2E8F0',
                              color: on ? 'var(--primary)' : '#475569',
                            }}
                          >
                            {name}
                          </button>
                        );
                      })}
                    </div>
                  </div>

                  <div>
                    <label className="form-label" htmlFor="description">Mô tả</label>
                    <textarea
                      id="description"
                      className="textarea"
                      rows={3}
                      maxLength={2000}
                      placeholder="Mô tả phòng, tiện nghi nổi bật…"
                      value={form.description}
                      onChange={(e) => handleField('description', e.target.value)}
                      style={{ resize: 'vertical', minHeight: 88 }}
                    />
                    <p className="text-xs text-[#94A3B8] text-right mt-1">
                      {form.description.length} / 2000
                    </p>
                  </div>

                  <div
                    className="flex flex-wrap gap-2 items-center pt-3 mt-1"
                    style={{ borderTop: '1px solid var(--hairline)' }}
                  >
                    <button type="submit" className="btn-primary" disabled={saving}>
                      {saving ? 'Đang lưu…' : 'Lưu thay đổi'}
                    </button>
                    <Link to="/manager/rooms" className="btn-ghost">Hủy</Link>
                  </div>
                </form>
              </>
            )}

            {activeTab === 'gallery' && <RoomGalleryTab roomId={id} />}

            {activeTab === 'status' && (
              <RoomStatusTab
                room={room}
                onStatusUpdated={(status) => setRoom((r) => (r ? { ...r, status } : r))}
              />
            )}
          </div>
        </div>
      </div>
    </ManagerLayout>
  );
}
