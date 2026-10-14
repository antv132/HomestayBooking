import { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faPlus, faPen, faImage, faTag, faBed, faSearch, faEye, faTrash } from '@fortawesome/free-solid-svg-icons';
import ManagerLayout from '../../layouts/ManagerLayout';
import Alert from '../../components/ui/Alert';
import { DataTable, StatusBadge } from '../../components/ui';
import type { StatusVariant } from '../../components/ui/StatusBadge';
import {
  fetchManagerRoomsV1,
  deleteRoomV1,
  ROOM_TYPES,
  type RoomListItem,
} from '../../api/roomsApi';
import { managerApi } from '../../api/managerApi';
import { floorApi, type FloorSummary } from '../../api/floorApi';
import type { AssignedProperty } from '../../api/reportApi';

// ── Status mapping (tiếng Việt) ───────────────────────────────────────────────

const STATUS_VI: Record<string, { label: string; variant: StatusVariant }> = {
  AVAILABLE:            { label: 'Trống',           variant: 'success' },
  PENDING_DEPOSIT:      { label: 'Chờ cọc',         variant: 'warning' },
  RESERVED:             { label: 'Đã đặt',          variant: 'info' },
  OCCUPIED:             { label: 'Đang ở',          variant: 'primary' },
  PENDING_CLEANING:     { label: 'Chờ dọn',         variant: 'danger' },
  CLEANING_IN_PROGRESS: { label: 'Đang dọn',        variant: 'danger' },
  MAINTENANCE:          { label: 'Bảo trì',         variant: 'warning' },
  OUT_OF_SERVICE:       { label: 'Ngưng phục vụ',   variant: 'neutral' },
};

const ALL_STATUSES = Object.keys(STATUS_VI);

function formatVnd(value: number): string {
  return new Intl.NumberFormat('vi-VN').format(value) + ' ₫';
}

// ── Pagination ────────────────────────────────────────────────────────────────

function Pagination({
  page,
  totalPages,
  onPageChange,
}: {
  page: number;
  totalPages: number;
  onPageChange: (p: number) => void;
}) {
  if (totalPages <= 1) return null;
  return (
    <div className="flex justify-center gap-2 mt-6">
      <button
        type="button"
        className="px-3 py-1.5 text-sm border border-[#E2E8F0] rounded-md disabled:opacity-40"
        onClick={() => onPageChange(page - 1)}
        disabled={page === 0}
      >
        ←
      </button>
      <span className="px-3 py-1.5 text-sm text-[#64748B]">
        Trang {page + 1} / {totalPages}
      </span>
      <button
        type="button"
        className="px-3 py-1.5 text-sm border border-[#E2E8F0] rounded-md disabled:opacity-40"
        onClick={() => onPageChange(page + 1)}
        disabled={page >= totalPages - 1}
      >
        →
      </button>
    </div>
  );
}

function TableSkeleton() {
  return (
    <div className="space-y-2 p-4">
      {Array.from({ length: 6 }).map((_, i) => (
        <div key={i} className="h-12 bg-[#F1F5F9] rounded animate-pulse" />
      ))}
    </div>
  );
}

// ── Main Page ─────────────────────────────────────────────────────────────────

export default function RoomListPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const initPropertyId = searchParams.get('propertyId') ?? '';
  const initFloorId = searchParams.get('floorId') ?? '';

  const PAGE_SIZE = 10;

  const [properties, setProperties] = useState<AssignedProperty[]>([]);
  const [propLoading, setPropLoading] = useState(true);
  const [propError, setPropError] = useState<string | null>(null);

  const [search, setSearch] = useState('');
  const [selectedPropertyId, setPropertyId] = useState(initPropertyId);
  const [selectedFloorId, setFloorId] = useState(initFloorId);
  const [selectedStatus, setStatus] = useState('');
  const [selectedRoomType, setRoomType] = useState('');
  const [page, setPage] = useState(0);

  const [floors, setFloors] = useState<FloorSummary[]>([]);
  const [floorLoading, setFloorLoading] = useState(false);

  const [rooms, setRooms] = useState<RoomListItem[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const debounceRef = useRef<ReturnType<typeof setTimeout>>();

  // Load assigned properties
  useEffect(() => {
    setPropLoading(true);
    managerApi.getMyAssignedProperties()
      .then(res => {
        if (res.success && res.data) {
          setProperties(res.data);
          const validInit = initPropertyId && res.data.some(p => p.id === initPropertyId);
          if (validInit) {
            setPropertyId(initPropertyId);
          } else if (!selectedPropertyId && res.data.length > 0) {
            setPropertyId(res.data[0].id);
          }
        }
      })
      .catch(() => setPropError('Không thể tải danh sách homestay.'))
      .finally(() => setPropLoading(false));
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // Load floors when property changes
  useEffect(() => {
    if (!selectedPropertyId) {
      setFloors([]);
      setFloorId('');
      return;
    }
    setFloorLoading(true);
    floorApi.getByProperty(selectedPropertyId)
      .then(res => {
        if (res.success) setFloors(res.data ?? []);
      })
      .catch(() => setFloors([]))
      .finally(() => setFloorLoading(false));
  }, [selectedPropertyId]);

  const loadRooms = useCallback((
    pg: number,
    srch: string,
    propId: string,
    flId: string,
    st: string,
    rt: string,
  ) => {
    setLoading(true);
    setError(null);
    fetchManagerRoomsV1({
      page: pg,
      size: PAGE_SIZE,
      search: srch || undefined,
      propertyId: propId || undefined,
      floorId: flId || undefined,
      status: st || undefined,
      roomType: rt || undefined,
    })
      .then(data => {
        setRooms(data.content);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
      })
      .catch((err: unknown) => {
        const ax = err as { response?: { status?: number; data?: { message?: string } } };
        if (ax?.response?.status === 403) {
          setError('Bạn không có quyền xem homestay này.');
        } else {
          setError(ax?.response?.data?.message ?? 'Không thể tải danh sách phòng.');
        }
        setRooms([]);
      })
      .finally(() => setLoading(false));
  }, []);

  // Debounced filter changes (reset page)
  useEffect(() => {
    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      setPage(0);
      loadRooms(0, search, selectedPropertyId, selectedFloorId, selectedStatus, selectedRoomType);
    }, 300);
    return () => clearTimeout(debounceRef.current);
  }, [search, selectedPropertyId, selectedFloorId, selectedStatus, selectedRoomType, loadRooms]);

  function handlePageChange(newPage: number) {
    setPage(newPage);
    loadRooms(newPage, search, selectedPropertyId, selectedFloorId, selectedStatus, selectedRoomType);
  }

  const hasFilters = !!(search || selectedFloorId || selectedStatus || selectedRoomType);

  function handleClearFilters() {
    setSearch('');
    setFloorId('');
    setStatus('');
    setRoomType('');
    setPage(0);
  }

  async function handleDelete(room: RoomListItem) {
    if (!window.confirm(`Xóa phòng ${room.roomNumber}? Phòng sẽ ẩn khỏi danh sách (soft-delete).`)) {
      return;
    }
    setDeletingId(room.id);
    setError(null);
    try {
      await deleteRoomV1(room.id);
      loadRooms(page, search, selectedPropertyId, selectedFloorId, selectedStatus, selectedRoomType);
    } catch (err: unknown) {
      const ax = err as { response?: { status?: number; data?: { message?: string } } };
      if (ax?.response?.status === 409) {
        setError(ax.response.data?.message ?? 'Không thể xóa phòng đang có booking.');
      } else {
        setError(ax?.response?.data?.message ?? 'Xóa phòng thất bại.');
      }
    } finally {
      setDeletingId(null);
    }
  }

  const columns = [
    {
      header: 'Số phòng',
      accessor: (r: RoomListItem) => (
        <div className="flex items-center gap-3 py-1">
          {r.primaryImageUrl ? (
            <img
              src={r.primaryImageUrl}
              alt={r.roomNumber}
              className="w-12 h-12 rounded-xl object-cover shadow-sm ring-1 ring-slate-200 flex-shrink-0 transition-transform hover:scale-105"
              onError={e => { (e.target as HTMLImageElement).style.display = 'none'; }}
            />
          ) : (
            <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-slate-50 to-slate-100 flex items-center justify-center text-slate-400 flex-shrink-0 ring-1 ring-slate-200 shadow-sm transition-transform hover:scale-105">
              <FontAwesomeIcon icon={faBed} className="w-6 h-6 text-slate-300" />
            </div>
          )}
          <span className="font-extrabold text-base text-primary hover:underline">
            {r.roomNumber}
          </span>
        </div>
      ),
    },
    {
      header: 'Loại',
      accessor: (r: RoomListItem) => (
        <span className="text-slate-600 font-medium bg-slate-50 px-2.5 py-1 rounded-md border border-slate-100">{r.roomType || '—'}</span>
      ),
    },
    {
      header: 'Tầng',
      accessor: (r: RoomListItem) => (
        <span className="text-slate-600 font-medium">
          {r.floorNumber != null ? `Tầng ${r.floorNumber}` : '—'}
        </span>
      ),
    },
    {
      header: 'Giá/đêm',
      accessor: (r: RoomListItem) => (
        <span className="font-bold text-teal-700 bg-teal-50 px-2.5 py-1.5 rounded-lg border border-teal-100 shadow-sm">
          {r.pricePerNight != null ? formatVnd(r.pricePerNight) : '—'}
        </span>
      ),
    },
    {
      header: 'Trạng thái',
      accessor: (r: RoomListItem) => {
        const cfg = STATUS_VI[r.status] ?? { label: r.status, variant: 'neutral' as StatusVariant };
        return <StatusBadge status={cfg.label} variant={cfg.variant} />;
      },
    },
  ];

  const actions = [
    {
      label: (
        <div className="flex items-center gap-2">
          <FontAwesomeIcon icon={faEye} className="text-slate-500 w-4" /> Xem chi tiết
        </div>
      ),
      onClick: (r: RoomListItem) => navigate(`/manager/rooms/${r.id}`),
    },
    {
      label: (
        <div className="flex items-center gap-2">
          <FontAwesomeIcon icon={faPen} className="text-slate-500 w-4" /> Sửa
        </div>
      ),
      onClick: (r: RoomListItem) => navigate(`/manager/rooms/${r.id}/edit`),
    },
    {
      label: (
        <div className="flex items-center gap-2">
          <FontAwesomeIcon icon={faImage} className="text-slate-500 w-4" /> Thư viện ảnh
        </div>
      ),
      onClick: (r: RoomListItem) => navigate(`/manager/rooms/${r.id}/edit?tab=gallery`),
    },
    {
      label: (
        <div className="flex items-center gap-2">
          <FontAwesomeIcon icon={faEye} className="text-slate-500 w-4" /> Lịch tháng
        </div>
      ),
      onClick: (r: RoomListItem) => navigate(`/manager/rooms/${r.id}/calendar`),
    },
    {
      label: (
        <div className="flex items-center gap-2">
          <FontAwesomeIcon icon={faTag} className="text-slate-500 w-4" /> Trạng thái
        </div>
      ),
      onClick: (r: RoomListItem) => navigate(`/manager/rooms/${r.id}/edit?tab=status`),
    },
    {
      label: (
        <div className="flex items-center gap-2 text-red-600">
          <FontAwesomeIcon icon={faTrash} className="w-4" />
          {deletingId ? 'Đang xóa…' : 'Xóa phòng'}
        </div>
      ),
      onClick: (r: RoomListItem) => {
        if (!deletingId) void handleDelete(r);
      },
    },
  ];

  return (
    <ManagerLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <h1 className="font-display text-[32px] font-extrabold bg-gradient-to-r from-teal-700 to-teal-600 bg-clip-text text-transparent drop-shadow-sm">
            Danh sách phòng
          </h1>
          {properties.length > 0 && (
            <button
              type="button"
              className="group relative inline-flex items-center gap-2 bg-gradient-to-r from-teal-700 to-teal-600 text-white px-6 py-2.5 rounded-xl text-sm font-semibold hover:from-teal-600 hover:to-teal-500 transition-all duration-300 shadow-[0_4px_14px_0_rgba(15,118,110,0.39)] hover:shadow-[0_6px_20px_rgba(15,118,110,0.5)] active:scale-[0.98]"
              onClick={() => navigate('/manager/rooms/add')}
            >
              <FontAwesomeIcon icon={faPlus} className="transition-transform group-hover:rotate-90" />
              Thêm phòng mới
            </button>
          )}
        </div>

        {propError && <Alert variant="error" message={propError} />}
        {error && <Alert variant="error" message={error} closeable onClose={() => setError(null)} />}

        {!propLoading && properties.length === 0 && (
          <div className="bg-white rounded-[16px] border border-[#E2E8F0] p-10 text-center">
            <p className="text-[#64748B]">Bạn chưa được gán homestay nào.</p>
          </div>
        )}

        {properties.length > 0 && (
          <>
            {/* Filters */}
            <div className="bg-white/80 backdrop-blur-xl rounded-2xl border border-slate-200/60 p-5 shadow-sm">
              <div className="flex flex-wrap gap-4 items-end">
                <div className="flex-1 min-w-[180px]">
                  <label className="block text-[13px] font-medium text-slate-600 mb-1.5">Tìm kiếm</label>
                  <input
                    className="w-full border border-slate-200 rounded-xl px-4 py-2.5 text-sm transition-all focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500 outline-none bg-white/50 focus:bg-white"
                    placeholder="Số phòng, loại phòng…"
                    value={search}
                    onChange={e => setSearch(e.target.value)}
                  />
                </div>

                <div className="min-w-[160px]">
                  <label className="block text-[13px] font-medium text-slate-600 mb-1.5">Homestay</label>
                  <select
                    className="w-full border border-slate-200 rounded-xl px-4 py-2.5 text-sm transition-all focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500 outline-none bg-white/50 focus:bg-white cursor-pointer"
                    value={selectedPropertyId}
                    onChange={e => { setPropertyId(e.target.value); setFloorId(''); }}
                    disabled={propLoading}
                  >
                    {properties.map(p => (
                      <option key={p.id} value={p.id}>{p.name}</option>
                    ))}
                  </select>
                </div>

                <div className="min-w-[130px]">
                  <label className="block text-[13px] font-medium text-slate-600 mb-1.5">Tầng</label>
                  <select
                    className="w-full border border-slate-200 rounded-xl px-4 py-2.5 text-sm transition-all focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500 outline-none bg-white/50 focus:bg-white cursor-pointer"
                    value={selectedFloorId}
                    onChange={e => setFloorId(e.target.value)}
                    disabled={floorLoading || !selectedPropertyId}
                  >
                    <option value="">Tất cả tầng</option>
                    {floors.map(f => (
                      <option key={f.id} value={f.id}>Tầng {f.floorNumber}</option>
                    ))}
                  </select>
                </div>

                <div className="min-w-[140px]">
                  <label className="block text-[13px] font-medium text-slate-600 mb-1.5">Trạng thái</label>
                  <select
                    className="w-full border border-slate-200 rounded-xl px-4 py-2.5 text-sm transition-all focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500 outline-none bg-white/50 focus:bg-white cursor-pointer"
                    value={selectedStatus}
                    onChange={e => setStatus(e.target.value)}
                  >
                    <option value="">Tất cả</option>
                    {ALL_STATUSES.map(s => (
                      <option key={s} value={s}>{STATUS_VI[s].label}</option>
                    ))}
                  </select>
                </div>

                <div className="min-w-[130px]">
                  <label className="block text-[13px] font-medium text-slate-600 mb-1.5">Loại phòng</label>
                  <select
                    className="w-full border border-slate-200 rounded-xl px-4 py-2.5 text-sm transition-all focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500 outline-none bg-white/50 focus:bg-white cursor-pointer"
                    value={selectedRoomType}
                    onChange={e => setRoomType(e.target.value)}
                  >
                    <option value="">Tất cả</option>
                    {ROOM_TYPES.map(t => (
                      <option key={t} value={t}>{t}</option>
                    ))}
                  </select>
                </div>

                {hasFilters && (
                  <button
                    type="button"
                    className="text-sm font-semibold text-teal-600 hover:text-teal-700 px-4 py-2.5 rounded-xl hover:bg-teal-50 transition-colors"
                    onClick={handleClearFilters}
                  >
                    Xóa bộ lọc
                  </button>
                )}
              </div>
            </div>

            {/* Table */}
            {loading ? (
              <TableSkeleton />
            ) : rooms.length === 0 ? (
              <div className="bg-white/80 backdrop-blur-xl rounded-3xl border border-slate-200/60 p-20 flex flex-col items-center justify-center text-center shadow-sm relative overflow-hidden">
                <div className="absolute inset-0 bg-gradient-to-b from-teal-50/50 to-transparent pointer-events-none" />
                <div className="relative w-24 h-24 bg-teal-50 rounded-full flex items-center justify-center mb-6 ring-8 ring-teal-50/50 animate-[bounce_3s_ease-in-out_infinite]">
                  <FontAwesomeIcon icon={faSearch} className="text-teal-500 text-3xl" />
                </div>
                <h3 className="relative text-2xl font-bold text-slate-800 mb-2">Không tìm thấy phòng</h3>
                <p className="relative text-slate-500 max-w-md text-base">Có vẻ như không có phòng nào khớp với bộ lọc hiện tại. Hãy thử thay đổi tiêu chí hoặc thêm một phòng mới.</p>
              </div>
            ) : (
              <>
                <p className="text-sm text-[#64748B]">
                  {totalElements} phòng
                </p>
                <DataTable
                  columns={columns}
                  data={rooms}
                  keyExtractor={r => r.id}
                  actions={actions}
                  onRowClick={(r) => navigate(`/manager/rooms/${r.id}`)}
                  getRowClassName={() => 'hover:bg-slate-50 transition-colors'}
                />
                <Pagination page={page} totalPages={totalPages} onPageChange={handlePageChange} />
              </>
            )}
          </>
        )}
      </div>
    </ManagerLayout>
  );
}
