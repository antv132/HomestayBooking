import { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import ManagerLayout from '../../layouts/ManagerLayout';
import Alert from '../../components/ui/Alert';
import {
  createRoomV1,
  CreateRoomPayload,
  ROOM_TYPES,
} from '../../api/roomsApi';
import { managerApi } from '../../api/managerApi';
import { floorApi, FloorSummary } from '../../api/floorApi';
import type { AssignedProperty } from '../../api/reportApi';
import { AMENITY_OPTIONS } from '../../utils/amenities';

function FieldError({ msg }: { msg: string }) {
  return <p className="text-xs text-red-600 mt-1 font-medium">{msg}</p>;
}

export default function AddRoomPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const initPropertyId = searchParams.get('propertyId') ?? '';
  const initFloorId = searchParams.get('floorId') ?? '';

  const [properties, setProperties] = useState<AssignedProperty[]>([]);
  const [propsLoading, setPropsLoading] = useState(true);
  const [selectedPropertyId, setSelectedPropertyId] = useState(initPropertyId);

  const [floors, setFloors] = useState<FloorSummary[]>([]);
  const [floorsLoading, setFloorsLoading] = useState(false);

  const [form, setForm] = useState({
    floorId: initFloorId,
    roomNumber: '',
    roomType: 'Standard',
    pricePerNight: '',
    capacity: '',
    area: '',
    description: '',
  });
  const [amenities, setAmenities] = useState<string[]>([]);

  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [roomNumberError, setRoomNumberError] = useState('');

  useEffect(() => {
    setPropsLoading(true);
    managerApi.getMyAssignedProperties()
      .then(res => {
        if (res.success && res.data) {
          setProperties(res.data);
          const validInit = initPropertyId && res.data.some(p => p.id === initPropertyId);
          if (validInit) {
            setSelectedPropertyId(initPropertyId);
          } else if (!selectedPropertyId && res.data.length > 0) {
            setSelectedPropertyId(res.data[0].id);
          }
        }
      })
      .catch(() => setError('Không thể tải danh sách homestay.'))
      .finally(() => setPropsLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const loadFloors = useCallback(async (propertyId: string) => {
    if (!propertyId) {
      setFloors([]);
      return;
    }
    setFloorsLoading(true);
    try {
      const res = await floorApi.getByProperty(propertyId);
      const floorList: FloorSummary[] = res.data ?? [];
      setFloors(floorList);
      if (initFloorId && floorList.some(fl => fl.id === initFloorId)) {
        setForm(f => ({ ...f, floorId: initFloorId }));
      }
    } catch {
      setFloors([]);
    } finally {
      setFloorsLoading(false);
    }
  }, [initFloorId]);

  useEffect(() => {
    setForm(f => ({ ...f, floorId: '' }));
    loadFloors(selectedPropertyId);
  }, [selectedPropertyId, loadFloors]);

  function handleField(field: keyof typeof form, value: string) {
    setForm(f => ({ ...f, [field]: value }));
    if (field === 'roomNumber') setRoomNumberError('');
    setError(null);
  }

  function toggleAmenity(name: string) {
    setAmenities(prev =>
      prev.includes(name) ? prev.filter(a => a !== name) : [...prev, name],
    );
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setRoomNumberError('');

    if (!selectedPropertyId) {
      setError('Vui lòng chọn homestay.');
      return;
    }
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
      const payload: CreateRoomPayload = {
        propertyId: selectedPropertyId,
        floorId: form.floorId,
        roomNumber: form.roomNumber.trim(),
        roomType: form.roomType,
        pricePerNight: Number(form.pricePerNight),
        capacity: Number(form.capacity),
        ...(form.area ? { area: Number(form.area) } : {}),
        ...(form.description ? { description: form.description.trim() } : {}),
        ...(amenities.length > 0 ? { amenities } : {}),
      };

      await createRoomV1(payload);
      navigate('/manager/rooms');
    } catch (err: unknown) {
      const axiosErr = err as { response?: { status?: number; data?: { message?: string } } };
      const status = axiosErr?.response?.status;
      const message = axiosErr?.response?.data?.message ?? 'Không thể tạo phòng. Vui lòng thử lại.';

      if (status === 409) {
        setRoomNumberError(message);
      } else {
        setError(message);
      }
    } finally {
      setSaving(false);
    }
  }

  const formDisabled = !selectedPropertyId || !form.floorId;

  return (
    <ManagerLayout>
      <div className="flex items-center gap-2 text-sm text-[#64748B] mb-5">
        <Link to="/manager/rooms" className="text-primary no-underline hover:underline">
          Danh sách phòng
        </Link>
        <span>›</span>
        <span className="font-semibold text-[#0F172A]">Thêm phòng mới</span>
      </div>

      <h1 className="heading-md mb-7">Thêm phòng mới</h1>

      <div className="card p-8 max-w-3xl">
        {error && (
          <div className="mb-5">
            <Alert variant="error" message={error} closeable onClose={() => setError(null)} />
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Property selector */}
          <div>
            <label className="form-label form-label-required">Homestay</label>
            {propsLoading ? (
              <div className="h-11 bg-[#F8FAFC] rounded-lg flex items-center px-4 text-sm text-[#64748B]">
                Đang tải…
              </div>
            ) : properties.length === 0 ? (
              <p className="text-sm text-amber-800 bg-amber-50 border border-amber-200 rounded-lg px-3 py-2">
                Bạn chưa được gán homestay nào.
              </p>
            ) : (
              <select
                className="select"
                value={selectedPropertyId}
                onChange={e => setSelectedPropertyId(e.target.value)}
                required
              >
                <option value="">— Chọn homestay —</option>
                {properties.map(p => (
                  <option key={p.id} value={p.id}>{p.name}</option>
                ))}
              </select>
            )}
          </div>

          {/* Form grid 2 cols */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="form-label form-label-required" htmlFor="roomNumber">
                Số phòng
              </label>
              <input
                id="roomNumber"
                className="input"
                placeholder="VD: 101, Suite 08"
                value={form.roomNumber}
                onChange={e => handleField('roomNumber', e.target.value)}
                required
                disabled={formDisabled}
                style={{
                  borderColor: roomNumberError ? '#DC2626' : undefined,
                  opacity: formDisabled ? 0.5 : 1,
                }}
              />
              {roomNumberError && <FieldError msg={roomNumberError} />}
              <p className="text-xs text-[#94A3B8] mt-1">Phải duy nhất trong homestay</p>
            </div>

            <div>
              <label className="form-label form-label-required">Tầng</label>
              {floorsLoading ? (
                <div className="h-11 bg-[#F8FAFC] rounded-lg flex items-center px-4 text-sm text-[#64748B]">
                  Đang tải tầng…
                </div>
              ) : (
                <select
                  className="select"
                  value={form.floorId}
                  onChange={e => handleField('floorId', e.target.value)}
                  disabled={!selectedPropertyId || floorsLoading}
                  required
                  style={{ opacity: !selectedPropertyId ? 0.5 : 1 }}
                >
                  <option value="">
                    {!selectedPropertyId ? '— Chọn homestay trước —' : '— Chọn tầng —'}
                  </option>
                  {floors.map(fl => (
                    <option key={fl.id} value={fl.id}>
                      Tầng {fl.floorNumber}
                      {fl.description ? ` — ${fl.description}` : ''}
                    </option>
                  ))}
                </select>
              )}
              {selectedPropertyId && !floorsLoading && floors.length === 0 && (
                <p className="text-xs text-[#64748B] mt-1">
                  Chưa có tầng.{' '}
                  <Link to="/manager/structure" className="text-primary no-underline">
                    Thêm tầng →
                  </Link>
                </p>
              )}
            </div>

            <div>
              <label className="form-label" htmlFor="roomType">Loại phòng</label>
              <select
                id="roomType"
                className="select"
                value={form.roomType}
                onChange={e => handleField('roomType', e.target.value)}
                disabled={formDisabled}
                style={{ opacity: formDisabled ? 0.5 : 1 }}
              >
                {ROOM_TYPES.map(t => (
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
                placeholder="VD: 1500000"
                min={1000}
                step={1000}
                value={form.pricePerNight}
                onChange={e => handleField('pricePerNight', e.target.value)}
                required
                disabled={formDisabled}
                style={{ opacity: formDisabled ? 0.5 : 1 }}
              />
            </div>

            <div>
              <label className="form-label form-label-required" htmlFor="capacity">
                Sức chứa (người)
              </label>
              <input
                id="capacity"
                type="number"
                className="input"
                placeholder="VD: 2"
                min={1}
                max={20}
                value={form.capacity}
                onChange={e => handleField('capacity', e.target.value)}
                required
                disabled={formDisabled}
                style={{ opacity: formDisabled ? 0.5 : 1 }}
              />
            </div>

            <div>
              <label className="form-label" htmlFor="area">Diện tích (m²)</label>
              <input
                id="area"
                type="number"
                className="input"
                placeholder="VD: 35"
                min={0}
                step={0.5}
                value={form.area}
                onChange={e => handleField('area', e.target.value)}
                disabled={formDisabled}
                style={{ opacity: formDisabled ? 0.5 : 1 }}
              />
              <p className="text-xs text-[#94A3B8] mt-1">Tùy chọn</p>
            </div>
          </div>

          {/* Amenities */}
          <div>
            <label className="form-label mb-3 block">Tiện nghi</label>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              {AMENITY_OPTIONS.map(name => (
                <label
                  key={name}
                  className="flex items-center gap-2 text-sm cursor-pointer select-none"
                  style={{ opacity: formDisabled ? 0.5 : 1 }}
                >
                  <input
                    type="checkbox"
                    checked={amenities.includes(name)}
                    onChange={() => toggleAmenity(name)}
                    disabled={formDisabled}
                    className="rounded border-[#CBD5E1]"
                  />
                  <span>{name}</span>
                </label>
              ))}
            </div>
          </div>

          {/* Description */}
          <div>
            <label className="form-label" htmlFor="description">Mô tả</label>
            <textarea
              id="description"
              className="textarea"
              rows={4}
              maxLength={2000}
              placeholder="Mô tả phòng, tiện nghi nổi bật…"
              value={form.description}
              onChange={e => handleField('description', e.target.value)}
              disabled={formDisabled}
              style={{ opacity: formDisabled ? 0.5 : 1, resize: 'vertical' }}
            />
            <p className="text-xs text-[#94A3B8] text-right mt-1">
              {form.description.length} / 2000
            </p>
          </div>

          {/* Actions */}
          <div className="flex gap-3 items-center pt-2">
            <button
              type="submit"
              className="btn-primary"
              disabled={saving || propsLoading}
            >
              {saving ? 'Đang tạo…' : 'Tạo phòng'}
            </button>
            <Link to="/manager/rooms" className="btn-ghost">Hủy</Link>
            {formDisabled && !saving && (
              <p className="text-xs text-[#64748B]">Chọn homestay và tầng để điền form</p>
            )}
          </div>
        </form>
      </div>
    </ManagerLayout>
  );
}
