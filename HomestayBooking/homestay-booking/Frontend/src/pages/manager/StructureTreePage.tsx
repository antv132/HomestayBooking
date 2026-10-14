import { useState, useEffect, useCallback } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import ManagerLayout from '../../layouts/ManagerLayout';
import AdminLayout from '../../layouts/AdminLayout';
import Alert from '../../components/ui/Alert';
import { propertyApi, type PropertyStructure, type FloorNode } from '../../api/propertyApi';
import { floorApi, type CreateFloorPayload, type UpdateFloorPayload } from '../../api/floorApi';
import { managerApi } from '../../api/managerApi';
import { getAdminProperties, getAdminPropertyStructure } from '../../api/adminApi';
import type { AssignedProperty } from '../../api/reportApi';

export type StructureTreePageProps = {
  /** FR-07 US5 — Admin read-only (no floor CRUD). */
  readOnly?: boolean;
};

// ── Room status (tiếng Việt) ──────────────────────────────────────────────────

const ROOM_STATUS_VI: Record<string, string> = {
  AVAILABLE: 'Trống',
  PENDING_DEPOSIT: 'Chờ cọc',
  RESERVED: 'Đã đặt',
  OCCUPIED: 'Đang ở',
  PENDING_CLEANING: 'Chờ dọn',
  CLEANING_IN_PROGRESS: 'Đang dọn',
  MAINTENANCE: 'Bảo trì',
  OUT_OF_SERVICE: 'Ngưng phục vụ',
};

// ── Floor Modal ───────────────────────────────────────────────────────────────

interface FloorModalProps {
  mode: 'add' | 'edit';
  propertyId: string;
  initial?: { id: string; floorNumber: number; description: string };
  onClose: () => void;
  onSuccess: () => void;
}

function FloorModal({ mode, propertyId, initial, onClose, onSuccess }: FloorModalProps) {
  const [floorNumber, setFloorNumber] = useState(initial?.floorNumber?.toString() ?? '');
  const [description, setDescription] = useState(initial?.description ?? '');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  async function handleSubmit() {
    if (!floorNumber || isNaN(Number(floorNumber)) || Number(floorNumber) < 1) {
      setError('Số tầng là bắt buộc và phải là số nguyên dương.');
      return;
    }
    setSaving(true);
    setError('');
    try {
      if (mode === 'add') {
        const payload: CreateFloorPayload = {
          propertyId,
          floorNumber: Number(floorNumber),
          description: description.trim() || undefined,
        };
        await floorApi.create(payload);
      } else if (initial) {
        const payload: UpdateFloorPayload = {
          floorNumber: Number(floorNumber),
          description: description.trim() || undefined,
        };
        await floorApi.update(propertyId, initial.id, payload);
      }
      onSuccess();
      onClose();
    } catch (err: unknown) {
      const ax = err as { response?: { data?: { message?: string } } };
      setError(ax?.response?.data?.message ?? 'Đã xảy ra lỗi. Vui lòng thử lại.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-sm animate-in fade-in duration-300 p-4"
      onClick={onClose}
    >
      <div
        className="bg-white/95 backdrop-blur-2xl rounded-3xl shadow-[0_20px_60px_-15px_rgba(0,0,0,0.3)] border border-white/50 p-8 w-full max-w-md transform transition-all animate-in zoom-in-95 duration-300"
        onClick={e => e.stopPropagation()}
      >
        <div className="flex items-center gap-4 mb-6">
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-teal-500 to-teal-600 flex items-center justify-center shadow-lg shadow-teal-500/30">
            <svg className="w-6 h-6 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
            </svg>
          </div>
          <h2 className="text-2xl font-display font-bold text-slate-800 tracking-tight">
            {mode === 'add' ? 'Thêm tầng mới' : 'Sửa thông tin tầng'}
          </h2>
        </div>

        {error && (
          <div className="mb-6 animate-in slide-in-from-top-2">
            <Alert variant="error" message={error} />
          </div>
        )}

        <div className="space-y-5">
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1.5">
              Số tầng <span className="text-rose-500">*</span>
            </label>
            <input
              type="number"
              min={1}
              className="w-full bg-slate-50/50 border border-slate-200 rounded-xl px-4 py-3 text-slate-800 focus:outline-none focus:ring-2 focus:ring-teal-500/40 focus:border-teal-500 transition-all shadow-inner placeholder:text-slate-400 font-medium"
              placeholder="Nhập số thứ tự tầng (VD: 1)"
              value={floorNumber}
              onChange={e => setFloorNumber(e.target.value)}
              autoFocus
            />
          </div>

          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1.5">Mô tả thêm</label>
            <input
              className="w-full bg-slate-50/50 border border-slate-200 rounded-xl px-4 py-3 text-slate-800 focus:outline-none focus:ring-2 focus:ring-teal-500/40 focus:border-teal-500 transition-all shadow-inner placeholder:text-slate-400 font-medium"
              placeholder="VD: Khu VIP có ban công"
              value={description}
              maxLength={500}
              onChange={e => setDescription(e.target.value)}
            />
          </div>
        </div>

        <div className="flex gap-3 mt-8 pt-6 border-t border-slate-100">
          <button
            type="button"
            className="flex-1 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl py-3 text-sm font-bold transition-all"
            onClick={onClose}
            disabled={saving}
          >
            Hủy bỏ
          </button>
          <button
            type="button"
            className="flex-1 relative overflow-hidden group bg-gradient-to-r from-teal-600 to-teal-500 text-white rounded-xl py-3 text-sm font-bold shadow-[0_4px_14px_0_rgba(20,184,166,0.39)] hover:shadow-[0_6px_20px_rgba(20,184,166,0.23)] hover:to-teal-400 transition-all active:scale-[0.98] disabled:opacity-70 disabled:active:scale-100"
            onClick={handleSubmit}
            disabled={saving}
          >
            <span className="relative z-10">{saving ? 'Đang lưu...' : mode === 'add' ? 'Xác nhận Thêm' : 'Lưu thay đổi'}</span>
            <div className="absolute inset-0 bg-white/20 translate-y-full group-hover:translate-y-0 transition-transform duration-300 ease-out" />
          </button>
        </div>
      </div>
    </div>
  );
}

// ── Tree View ─────────────────────────────────────────────────────────────────

interface TreeViewProps {
  structure: PropertyStructure;
  expandedFloors: Set<string>;
  onToggleFloor: (id: string) => void;
  onEditFloor: (floor: FloorNode) => void;
  onDeleteFloor: (floor: FloorNode) => void;
  readOnly?: boolean;
}

const getRoomStatusColor = (status: string) => {
  switch (status) {
    case 'AVAILABLE': return 'bg-emerald-50 text-emerald-600 border-emerald-200 shadow-[0_0_10px_rgba(16,185,129,0.1)]';
    case 'OCCUPIED': return 'bg-blue-50 text-blue-600 border-blue-200 shadow-[0_0_10px_rgba(59,130,246,0.1)]';
    case 'RESERVED':
    case 'PENDING_DEPOSIT': return 'bg-amber-50 text-amber-600 border-amber-200 shadow-[0_0_10px_rgba(245,158,11,0.1)]';
    case 'CLEANING_IN_PROGRESS':
    case 'PENDING_CLEANING': return 'bg-purple-50 text-purple-600 border-purple-200 shadow-[0_0_10px_rgba(168,85,247,0.1)]';
    case 'MAINTENANCE':
    case 'OUT_OF_SERVICE': return 'bg-rose-50 text-rose-600 border-rose-200 shadow-[0_0_10px_rgba(244,63,94,0.1)]';
    default: return 'bg-slate-50 text-slate-600 border-slate-200';
  }
};

const getRoomStatusDot = (status: string) => {
  switch (status) {
    case 'AVAILABLE': return 'bg-emerald-500 shadow-[0_0_8px_rgba(16,185,129,0.5)]';
    case 'OCCUPIED': return 'bg-blue-500 shadow-[0_0_8px_rgba(59,130,246,0.5)]';
    case 'RESERVED':
    case 'PENDING_DEPOSIT': return 'bg-amber-500 shadow-[0_0_8px_rgba(245,158,11,0.5)]';
    case 'CLEANING_IN_PROGRESS':
    case 'PENDING_CLEANING': return 'bg-purple-500 shadow-[0_0_8px_rgba(168,85,247,0.5)]';
    case 'MAINTENANCE':
    case 'OUT_OF_SERVICE': return 'bg-rose-500 shadow-[0_0_8px_rgba(244,63,94,0.5)]';
    default: return 'bg-slate-400';
  }
};

function TreeView({ structure, expandedFloors, onToggleFloor, onEditFloor, onDeleteFloor, readOnly }: TreeViewProps) {
  if (structure.floors.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-16 px-4 bg-white/40 backdrop-blur-md rounded-2xl border border-dashed border-slate-300 shadow-sm relative overflow-hidden group">
        <div className="absolute inset-0 bg-gradient-to-br from-teal-500/5 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-500" />
        <div className="w-20 h-20 bg-white rounded-full flex items-center justify-center shadow-[0_8px_30px_rgb(0,0,0,0.04)] mb-6 animate-[bounce_3s_ease-in-out_infinite]">
          <svg className="w-10 h-10 text-slate-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
          </svg>
        </div>
        <p className="text-slate-800 font-bold text-xl mb-2 relative z-10">Homestay chưa có cấu trúc</p>
        <p className="text-slate-500 text-sm text-center max-w-sm relative z-10">
          {readOnly
            ? 'Property này chưa được Manager tạo tầng.'
            : 'Bắt đầu bằng cách nhấn nút "Thêm tầng mới" để xây dựng sơ đồ phòng cho cơ sở này.'}
        </p>
      </div>
    );
  }

  return (
    <div className="relative">
      {/* Property Root Header */}
      <div className="relative overflow-hidden rounded-2xl shadow-lg border border-teal-700/20 mb-6 group">
        <div className="absolute inset-0 bg-gradient-to-r from-teal-900 via-teal-700 to-teal-800 animate-[gradient-x_3s_ease-in-out_infinite]" style={{ backgroundSize: '200% 200%' }} />
        
        {/* Decor geometric shapes */}
        <div className="absolute -top-12 -right-12 w-32 h-32 bg-white/10 rounded-full blur-2xl" />
        <div className="absolute bottom-0 left-20 w-40 h-40 bg-emerald-400/10 rounded-full blur-3xl" />

        <div className="relative z-10 flex items-center gap-5 px-6 py-6 backdrop-blur-sm">
          <div className="w-14 h-14 bg-white/20 backdrop-blur-md rounded-2xl flex items-center justify-center border border-white/20 shadow-inner shrink-0">
            <svg className="w-7 h-7 text-white drop-shadow-md" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
            </svg>
          </div>
          <div>
            <h3 className="text-2xl font-display font-extrabold text-white tracking-tight drop-shadow-sm mb-1">{structure.propertyName}</h3>
            <div className="flex items-center gap-3 text-teal-50">
              <span className="flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-black/20 text-xs font-semibold backdrop-blur-md border border-white/10">
                <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 10h16M4 14h16M4 18h16" /></svg>
                {structure.floors.length} Tầng
              </span>
              <span className="flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-black/20 text-xs font-semibold backdrop-blur-md border border-white/10">
                <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" /></svg>
                {structure.floors.reduce((acc, f) => acc + (f.roomCount ?? f.rooms.length), 0)} Phòng
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Tree Structure */}
      <div className="relative ml-4 sm:ml-8 pl-4 sm:pl-8 border-l-2 border-slate-200/60 pb-8 space-y-8">
        {structure.floors.map((floor, index) => {
          const expanded = expandedFloors.has(floor.id);
          const roomCount = floor.roomCount ?? floor.rooms.length;
          const floorDesc = floor.description?.trim();
          const isRedundantDesc = floorDesc?.toLowerCase() === `tầng ${floor.floorNumber}` || floorDesc?.toLowerCase() === `floor ${floor.floorNumber}`;

          return (
            <div key={floor.id} className="relative animate-in slide-in-from-left-4 fade-in" style={{ animationDelay: `${index * 50}ms`, animationFillMode: 'both' }}>
              {/* Connector line from left */}
              <div className="absolute -left-4 sm:-left-8 top-7 w-4 sm:w-8 border-t-2 border-slate-200/60" />
              
              {/* Floor Node Card */}
              <div className="relative group/floor">
                <div 
                  className={`flex flex-col sm:flex-row sm:items-center gap-4 p-4 rounded-2xl border transition-all duration-300 cursor-pointer ${expanded ? 'bg-white shadow-[0_8px_30px_rgb(0,0,0,0.04)] border-teal-200/50' : 'bg-white/60 backdrop-blur-md border-slate-200/60 shadow-sm hover:shadow-md hover:bg-white hover:border-slate-300'}`}
                  onClick={() => onToggleFloor(floor.id)}
                >
                  {/* Left Controls & Title */}
                  <div className="flex items-center gap-4 flex-1">
                    <button
                      type="button"
                      className={`shrink-0 w-8 h-8 flex items-center justify-center rounded-xl transition-all duration-300 ${expanded ? 'bg-teal-50 text-teal-600 rotate-90 shadow-inner' : 'bg-slate-100 text-slate-500 hover:bg-slate-200 group-hover/floor:bg-teal-50 group-hover/floor:text-teal-600'}`}
                      aria-label={expanded ? 'Thu gọn' : 'Mở rộng'}
                    >
                      <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M9 5l7 7-7 7" />
                      </svg>
                    </button>
                    
                    <div>
                      <h4 className="text-lg font-bold text-slate-800 tracking-tight flex items-center gap-2">
                        Tầng {floor.floorNumber}
                        <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-slate-100 text-slate-500 border border-slate-200/60">
                          {roomCount} phòng
                        </span>
                      </h4>
                      {!isRedundantDesc && floorDesc && (
                        <p className="text-sm text-slate-500 font-medium mt-0.5 line-clamp-1">{floorDesc}</p>
                      )}
                    </div>
                  </div>

                  {/* Actions (visible on hover) — Manager only */}
                  {!readOnly && (
                  <div className="flex items-center gap-2 opacity-100 sm:opacity-0 group-hover/floor:opacity-100 transition-opacity pl-12 sm:pl-0">
                    <button
                      type="button"
                      className="px-3 py-1.5 bg-slate-100 hover:bg-teal-50 text-slate-600 hover:text-teal-700 text-sm font-semibold rounded-lg transition-colors flex items-center gap-1.5"
                      onClick={(e) => { e.stopPropagation(); onEditFloor(floor); }}
                    >
                      <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" /></svg>
                      Sửa
                    </button>
                    <button
                      type="button"
                      className="px-3 py-1.5 bg-slate-100 hover:bg-rose-50 text-slate-600 hover:text-rose-700 text-sm font-semibold rounded-lg transition-colors flex items-center gap-1.5"
                      onClick={(e) => { e.stopPropagation(); onDeleteFloor(floor); }}
                    >
                      <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>
                      Xóa
                    </button>
                  </div>
                  )}
                </div>
              </div>

              {/* Room Leaves Grid */}
              <div 
                className={`overflow-hidden transition-all duration-400 ease-[cubic-bezier(0.4,0,0.2,1)] ${expanded ? 'max-h-[2000px] opacity-100 mt-4 mb-2' : 'max-h-0 opacity-0'}`}
              >
                <div className="relative ml-4 sm:ml-8 pl-4 sm:pl-8 border-l border-dashed border-slate-300">
                  {/* Decorative L-line fading out */}
                  <div className="absolute top-0 -left-[1px] w-[1px] h-full bg-gradient-to-b from-slate-300 to-transparent" />

                  {floor.rooms.length === 0 ? (
                    <div className="py-4 px-6 bg-slate-50/50 rounded-xl border border-dashed border-slate-200 inline-flex items-center gap-2">
                      <span className="relative flex h-2 w-2">
                        <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-amber-400 opacity-75"></span>
                        <span className="relative inline-flex rounded-full h-2 w-2 bg-amber-500"></span>
                      </span>
                      <span className="text-sm text-slate-500 font-medium">Tầng này chưa có phòng.</span>
                    </div>
                  ) : (
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3 py-1">
                      {floor.rooms.map(room => {
                        const roomCardClass = `group/room relative flex items-center justify-between p-3.5 bg-white rounded-xl border transition-all duration-300 transform hover:-translate-y-1 hover:shadow-lg ${getRoomStatusColor(room.status)}`;
                        const roomInner = (
                          <>
                          <div className="flex items-center gap-3">
                            <div className={`w-2 h-2 rounded-full ${getRoomStatusDot(room.status)}`} />
                            <div>
                              <div className="font-bold text-sm tracking-tight">Phòng {room.roomNumber}</div>
                              {room.roomType && (
                                <div className="text-[11px] font-medium opacity-70 mt-0.5 uppercase tracking-wider">{room.roomType}</div>
                              )}
                            </div>
                          </div>
                          <div className="shrink-0 text-[10px] font-bold px-2 py-1 rounded-md bg-white/50 backdrop-blur-sm border border-black/5 uppercase tracking-wider">
                            {ROOM_STATUS_VI[room.status] ?? room.status}
                          </div>
                          </>
                        );
                        return readOnly ? (
                          <div key={room.id} className={`${roomCardClass} cursor-default`}>
                            {roomInner}
                          </div>
                        ) : (
                          <Link
                            key={room.id}
                            to={`/manager/rooms/${room.id}`}
                            className={`${roomCardClass} cursor-pointer no-underline text-inherit`}
                          >
                            {roomInner}
                          </Link>
                        );
                      })}
                    </div>
                  )}
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

function TreeSkeleton() {
  return (
    <div className="space-y-6">
      <div className="h-28 bg-slate-100 rounded-2xl animate-pulse" />
      <div className="ml-8 space-y-4 border-l-2 border-slate-100 pl-8">
        {Array.from({ length: 3 }).map((_, i) => (
          <div key={i} className="space-y-3">
            <div className="h-16 bg-slate-50 rounded-2xl border border-slate-100 animate-pulse relative">
              <div className="absolute -left-8 top-8 w-8 border-t-2 border-slate-100" />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

// ── Main Page ─────────────────────────────────────────────────────────────────

export default function StructureTreePage({ readOnly = false }: StructureTreePageProps) {
  const Layout = readOnly ? AdminLayout : ManagerLayout;
  const [searchParams] = useSearchParams();
  const initPropertyId = searchParams.get('propertyId') ?? '';

  const [properties, setProperties] = useState<AssignedProperty[]>([]);
  const [propLoading, setPropLoading] = useState(true);
  const [propError, setPropError] = useState<string | null>(null);

  const [selectedPropId, setSelectedPropId] = useState(initPropertyId);
  const [structure, setStructure] = useState<PropertyStructure | null>(null);
  const [treeLoading, setTreeLoading] = useState(false);
  const [treeError, setTreeError] = useState<string | null>(null);

  const [expandedFloors, setExpandedFloors] = useState<Set<string>>(new Set());

  const [floorModal, setFloorModal] = useState<{
    open: boolean;
    mode: 'add' | 'edit';
    floor?: FloorNode;
  }>({ open: false, mode: 'add' });

  const [deleteTarget, setDeleteTarget] = useState<FloorNode | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  // Load properties — Manager assigned vs Admin all
  useEffect(() => {
    setPropLoading(true);
    setPropError(null);
    const load = readOnly
      ? getAdminProperties({ page: 0, size: 200, sort: 'name,asc' }).then(res => {
          const list = (res.data?.content ?? []).map(p => ({
            id: p.id,
            name: p.name,
            address: p.location ?? null,
            status: p.status,
          }));
          return list;
        })
      : managerApi.getMyAssignedProperties().then(res =>
          res.success && res.data ? res.data : []);

    load
      .then(list => {
        setProperties(list);
        const validInit = initPropertyId && list.some(p => p.id === initPropertyId);
        if (validInit) {
          setSelectedPropId(initPropertyId);
        } else if (list.length > 0) {
          setSelectedPropId(prev => (prev && list.some(p => p.id === prev) ? prev : list[0].id));
        }
      })
      .catch(() => setPropError(readOnly
        ? 'Không thể tải danh sách property.'
        : 'Không thể tải danh sách homestay.'))
      .finally(() => setPropLoading(false));
  }, [readOnly]); // eslint-disable-line react-hooks/exhaustive-deps

  const loadStructure = useCallback((propId: string) => {
    if (!propId) return;
    setTreeLoading(true);
    setTreeError(null);
    const fetchTree = readOnly
      ? getAdminPropertyStructure(propId)
      : propertyApi.getStructureTree(propId);
    fetchTree
      .then(res => {
        if (res.success && res.data) {
          setStructure(res.data as PropertyStructure);
          const expanded = new Set<string>();
          if (res.data.floors.length <= 5) {
            res.data.floors.forEach(f => expanded.add(f.id));
          }
          setExpandedFloors(expanded);
        }
      })
      .catch((err: unknown) => {
        const ax = err as { response?: { status?: number; data?: { message?: string } } };
        if (ax?.response?.status === 403) {
          setTreeError('Bạn không có quyền xem homestay này.');
        } else if (ax?.response?.status === 404) {
          setTreeError('Không tìm thấy homestay.');
        } else {
          setTreeError(ax?.response?.data?.message ?? 'Không thể tải cây cấu trúc. Vui lòng thử lại.');
        }
        setStructure(null);
      })
      .finally(() => setTreeLoading(false));
  }, [readOnly]);

  useEffect(() => {
    if (selectedPropId) loadStructure(selectedPropId);
  }, [selectedPropId, loadStructure]);

  function toggleFloor(id: string) {
    setExpandedFloors(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  async function confirmDeleteFloor() {
    if (!deleteTarget || readOnly) return;
    setDeleting(true);
    setDeleteError(null);
    try {
      await floorApi.remove(selectedPropId, deleteTarget.id);
      setDeleteTarget(null);
      if (selectedPropId) loadStructure(selectedPropId);
    } catch (err: unknown) {
      const ax = err as { response?: { data?: { message?: string } } };
      const msg = ax?.response?.data?.message ?? 'Không thể xóa tầng.';
      setDeleteError(
        msg.includes('phòng') ? 'Không thể xóa tầng đang có phòng.' : msg
      );
    } finally {
      setDeleting(false);
    }
  }

  return (
    <Layout>
      <div className="relative min-h-[80vh]">
        {/* Abstract Background Decoration */}
        <div className="absolute top-0 left-0 w-full h-96 bg-gradient-to-b from-teal-50/50 to-transparent pointer-events-none -z-10" />
        <div className="absolute top-20 right-10 w-96 h-96 bg-teal-100/30 rounded-full blur-3xl pointer-events-none -z-10" />

        <div className="space-y-8 max-w-5xl mx-auto px-4 pb-12 animate-in fade-in slide-in-from-bottom-4 duration-700">
          {/* Header Bar */}
          <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
            <div>
              <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-teal-50 border border-teal-100 text-teal-700 text-xs font-bold uppercase tracking-wider mb-3 shadow-sm">
                <span className="w-2 h-2 rounded-full bg-teal-500 animate-pulse" />
                {readOnly ? 'Admin · Read-only' : 'Structure Management'}
              </div>
              <h1 className="font-display text-4xl md:text-5xl font-extrabold bg-gradient-to-r from-slate-900 to-slate-700 bg-clip-text text-transparent tracking-tight mb-2">
                Sơ đồ Homestay
              </h1>
              <p className="text-slate-500 font-medium text-lg max-w-xl">
                {readOnly
                  ? 'Xem cấu trúc Property → Floor → Room trên mọi cơ sở (không chỉnh sửa tầng).'
                  : 'Quản lý kiến trúc các tầng và sơ đồ phòng với giao diện trực quan, dễ dàng mở rộng và chỉnh sửa.'}
              </p>
            </div>
            
            {/* Toolbar (Select & Add) */}
            <div className="flex flex-col sm:flex-row sm:items-center gap-4 shrink-0 bg-white/60 backdrop-blur-xl p-2 rounded-2xl shadow-sm border border-slate-200/60">
              <div className="relative w-full sm:w-64">
                <div className="absolute inset-y-0 left-0 flex items-center pl-4 pointer-events-none text-teal-600">
                  <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
                  </svg>
                </div>
                <select
                  className="w-full appearance-none bg-white/80 border border-slate-200 rounded-xl pl-11 pr-10 py-3 text-sm font-bold text-slate-800 focus:outline-none focus:ring-2 focus:ring-teal-500/50 focus:border-teal-500 transition-all shadow-inner cursor-pointer"
                  value={selectedPropId}
                  onChange={e => setSelectedPropId(e.target.value)}
                  disabled={propLoading || properties.length === 0}
                >
                  {propLoading ? (
                    <option>Đang tải…</option>
                  ) : properties.length === 0 ? (
                    <option>Không có Homestay</option>
                  ) : (
                    properties.map(p => (
                      <option key={p.id} value={p.id}>
                        {p.address ? `${p.name} — ${p.address}` : p.name}
                        {p.status === 'INACTIVE' ? ' (INACTIVE)' : ''}
                      </option>
                    ))
                  )}
                </select>
                <div className="absolute inset-y-0 right-0 flex items-center pr-4 pointer-events-none text-slate-400">
                  <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M19 9l-7 7-7-7" /></svg>
                </div>
              </div>
              
              {!readOnly && (
              <button
                type="button"
                className="group relative overflow-hidden inline-flex items-center justify-center gap-2 bg-gradient-to-r from-teal-600 to-teal-500 text-white px-6 py-3 rounded-xl text-sm font-bold shadow-[0_4px_14px_0_rgba(20,184,166,0.39)] hover:shadow-[0_6px_20px_rgba(20,184,166,0.23)] hover:to-teal-400 transition-all active:scale-[0.98] disabled:opacity-50 disabled:active:scale-100 w-full sm:w-auto"
                onClick={() => setFloorModal({ open: true, mode: 'add' })}
                disabled={treeLoading || !selectedPropId}
              >
                <span className="relative z-10 flex items-center gap-2">
                  <svg className="w-5 h-5 transition-transform group-hover:rotate-90" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M12 4v16m8-8H4" />
                  </svg>
                  Thêm Tầng
                </span>
                <div className="absolute inset-0 bg-white/20 translate-y-full group-hover:translate-y-0 transition-transform duration-300 ease-out" />
              </button>
              )}
            </div>
          </div>

          {/* Alerts */}
          {propError && <Alert variant="error" message={propError} />}
          {treeError && <Alert variant="error" message={treeError} />}

          {/* Empty: no assigned properties */}
          {!propLoading && properties.length === 0 && (
            <div className="bg-white/60 backdrop-blur-2xl rounded-3xl border border-slate-200/60 p-16 text-center shadow-sm flex flex-col items-center justify-center relative overflow-hidden group">
              <div className="absolute inset-0 bg-gradient-to-br from-rose-50 to-transparent opacity-50" />
              <div className="w-24 h-24 bg-white rounded-full flex items-center justify-center mb-6 shadow-xl shadow-slate-200/50 animate-[bounce_3s_ease-in-out_infinite] relative z-10">
                 <svg className="w-12 h-12 text-slate-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                   <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
                 </svg>
              </div>
              <p className="text-slate-800 font-display font-bold text-2xl mb-2 relative z-10">
                {readOnly ? 'Chưa có property nào' : 'Bạn chưa có Homestay nào'}
              </p>
              <p className="text-slate-500 font-medium text-lg relative z-10 max-w-md">
                {readOnly
                  ? 'Tạo property trước tại mục Danh sách Property.'
                  : 'Hãy liên hệ quản trị viên (System Admin) để được cấp quyền quản lý cơ sở.'}
              </p>
            </div>
          )}

          {/* Main Content Area */}
          {properties.length > 0 && (
            <div className="relative">
              {treeLoading ? (
                <TreeSkeleton />
              ) : structure ? (
                <TreeView
                  structure={structure}
                  expandedFloors={expandedFloors}
                  onToggleFloor={toggleFloor}
                  onEditFloor={floor => setFloorModal({ open: true, mode: 'edit', floor })}
                  onDeleteFloor={floor => { setDeleteTarget(floor); setDeleteError(null); }}
                  readOnly={readOnly}
                />
              ) : null}
            </div>
          )}
        </div>

        {/* Floor modal — Manager only */}
        {!readOnly && floorModal.open && selectedPropId && (
          <FloorModal
            mode={floorModal.mode}
            propertyId={selectedPropId}
            initial={
              floorModal.floor
                ? {
                    id: floorModal.floor.id,
                    floorNumber: floorModal.floor.floorNumber,
                    description: floorModal.floor.description ?? '',
                  }
                : undefined
            }
            onClose={() => setFloorModal({ open: false, mode: 'add' })}
            onSuccess={() => selectedPropId && loadStructure(selectedPropId)}
          />
        )}

        {/* Delete confirm — Manager only */}
        {!readOnly && deleteTarget && (
          <div
            className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-md animate-in fade-in duration-300 p-4"
            onClick={() => !deleting && setDeleteTarget(null)}
          >
            <div
              className="bg-white rounded-3xl shadow-2xl p-8 w-full max-w-sm transform transition-all animate-in zoom-in-95 duration-300 relative overflow-hidden"
              onClick={e => e.stopPropagation()}
            >
              <div className="absolute top-0 left-0 w-full h-2 bg-rose-500" />
              
              <div className="w-14 h-14 bg-rose-100 text-rose-600 rounded-full flex items-center justify-center mb-6 shadow-inner border border-rose-200">
                <svg className="w-7 h-7" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                </svg>
              </div>

              <h2 className="text-2xl font-bold text-slate-800 tracking-tight mb-2">
                Xóa Tầng {deleteTarget.floorNumber}?
              </h2>
              <p className="text-slate-500 font-medium leading-relaxed mb-6">
                Hành động này sẽ xóa vĩnh viễn cấu trúc tầng. Không thể hoàn tác.
                {(deleteTarget.roomCount ?? deleteTarget.rooms.length) > 0 && (
                  <span className="block mt-3 px-3 py-2 bg-rose-50 text-rose-700 rounded-lg text-sm font-semibold border border-rose-200">
                    ⚠️ Tầng này đang có {deleteTarget.roomCount ?? deleteTarget.rooms.length} phòng. Vui lòng xóa hết phòng trước khi xóa tầng.
                  </span>
                )}
              </p>

              {deleteError && (
                <div className="mb-6 animate-in slide-in-from-top-2">
                  <Alert variant="error" message={deleteError} />
                </div>
              )}

              <div className="flex gap-3">
                <button
                  type="button"
                  className="flex-1 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl py-3 text-sm font-bold transition-colors"
                  onClick={() => setDeleteTarget(null)}
                  disabled={deleting}
                >
                  Giữ lại
                </button>
                <button
                  type="button"
                  className="flex-1 bg-rose-600 hover:bg-rose-700 text-white rounded-xl py-3 text-sm font-bold shadow-md shadow-rose-500/20 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                  onClick={confirmDeleteFloor}
                  disabled={deleting || (deleteTarget.roomCount ?? deleteTarget.rooms.length) > 0}
                >
                  {deleting ? 'Đang xóa...' : 'Xóa vĩnh viễn'}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </Layout>
  );
}
