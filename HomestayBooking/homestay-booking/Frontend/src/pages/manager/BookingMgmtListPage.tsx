import { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import ManagerLayout from '../../layouts/ManagerLayout';
import Alert from '../../components/ui/Alert';
import { DataTable, StatusBadge } from '../../components/ui';
import type { StatusVariant } from '../../components/ui/StatusBadge';
import {
  fetchManagerBookingsV1,
  type BookingSummaryResponse,
} from '../../api/bookingApi';
import { managerApi } from '../../api/managerApi';
import type { AssignedProperty } from '../../api/reportApi';

const STATUS_VI: Record<string, { label: string; variant: StatusVariant }> = {
  PENDING_DEPOSIT:        { label: 'Chờ cọc',                    variant: 'warning' },
  CONFIRMED:              { label: 'Đã xác nhận',                variant: 'success' },
  CHECKED_IN:             { label: 'Đã nhận phòng',              variant: 'primary' },
  PENDING_INSPECTION:     { label: 'Chờ kiểm tra',               variant: 'warning' },
  PENDING_DAMAGE_PAYMENT: { label: 'Chờ thanh toán thiệt hại',   variant: 'danger' },
  CHECKED_OUT:            { label: 'Đã trả phòng',               variant: 'neutral' },
  CANCELLED:              { label: 'Đã hủy',                     variant: 'danger' },
  NO_SHOW:                { label: 'Không đến',                    variant: 'danger' },
};

const ALL_STATUSES = Object.keys(STATUS_VI);
const PAGE_SIZE = 10;

function formatVnd(value: number): string {
  return new Intl.NumberFormat('vi-VN').format(value) + ' ₫';
}

function formatDate(iso: string): string {
  if (!iso) return '—';
  const [y, m, d] = iso.split('T')[0].split('-');
  return `${d}/${m}/${y}`;
}

function shortBookingId(id: string): string {
  return `BK-${id.replace(/-/g, '').slice(0, 8).toUpperCase()}`;
}

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

export default function BookingMgmtListPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const initSearch = searchParams.get('search') ?? '';

  const [properties, setProperties] = useState<AssignedProperty[]>([]);
  const [propLoading, setPropLoading] = useState(true);
  const [propError, setPropError] = useState<string | null>(null);

  const [search, setSearch] = useState(initSearch);
  const [selectedPropertyId, setPropertyId] = useState('');
  const [selectedStatus, setStatus] = useState('');
  const [checkInFrom, setCheckInFrom] = useState('');
  const [checkInTo, setCheckInTo] = useState('');
  const [page, setPage] = useState(0);

  const [bookings, setBookings] = useState<BookingSummaryResponse[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const debounceRef = useRef<ReturnType<typeof setTimeout>>();

  useEffect(() => {
    setPropLoading(true);
    managerApi.getMyAssignedProperties()
      .then(res => {
        if (res.success && res.data) setProperties(res.data);
      })
      .catch(() => setPropError('Không thể tải danh sách homestay.'))
      .finally(() => setPropLoading(false));
  }, []);

  const loadBookings = useCallback((
    pg: number,
    srch: string,
    propId: string,
    st: string,
    from: string,
    to: string,
  ) => {
    setLoading(true);
    setError(null);
    fetchManagerBookingsV1({
      page: pg,
      size: PAGE_SIZE,
      search: srch || undefined,
      propertyId: propId || undefined,
      status: st || undefined,
      checkInFrom: from || undefined,
      checkInTo: to || undefined,
      sort: 'checkInDate,desc',
    })
      .then(data => {
        setBookings(data.content);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
      })
      .catch((err: unknown) => {
        const ax = err as { response?: { status?: number; data?: { message?: string } } };
        if (ax?.response?.status === 403) {
          setError('Bạn không có quyền xem homestay này.');
        } else {
          setError(ax?.response?.data?.message ?? 'Không thể tải danh sách đặt phòng.');
        }
        setBookings([]);
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      setPage(0);
      loadBookings(0, search, selectedPropertyId, selectedStatus, checkInFrom, checkInTo);
    }, 300);
    return () => clearTimeout(debounceRef.current);
  }, [search, selectedPropertyId, selectedStatus, checkInFrom, checkInTo, loadBookings]);

  function handlePageChange(newPage: number) {
    setPage(newPage);
    loadBookings(newPage, search, selectedPropertyId, selectedStatus, checkInFrom, checkInTo);
  }

  function handleClearFilters() {
    setSearch('');
    setPropertyId('');
    setStatus('');
    setCheckInFrom('');
    setCheckInTo('');
    setPage(0);
  }

  const hasFilters = !!(search || selectedPropertyId || selectedStatus || checkInFrom || checkInTo);

  const columns = [
    {
      header: 'Mã đặt phòng',
      accessor: (b: BookingSummaryResponse) => (
        <span className="font-mono text-sm text-[#334155]" title={b.id}>
          {shortBookingId(b.id)}
        </span>
      ),
    },
    {
      header: 'Khách',
      accessor: (b: BookingSummaryResponse) => (
        <div>
          <p className="font-semibold text-sm text-[#1E293B] m-0">{b.customerName}</p>
          {b.customerEmail && (
            <p className="text-xs text-[#64748B] m-0 mt-0.5">{b.customerEmail}</p>
          )}
        </div>
      ),
    },
    {
      header: 'Phòng',
      accessor: (b: BookingSummaryResponse) => (
        <div>
          <p className="font-semibold text-sm text-[#1E293B] m-0">{b.roomNumber}</p>
          <p className="text-xs text-[#64748B] m-0 mt-0.5">{b.propertyName}</p>
        </div>
      ),
    },
    {
      header: 'Nhận phòng',
      accessor: (b: BookingSummaryResponse) => (
        <span className="text-sm text-[#334155]">{formatDate(b.checkInDate)}</span>
      ),
    },
    {
      header: 'Trả phòng',
      accessor: (b: BookingSummaryResponse) => (
        <span className="text-sm text-[#334155]">{formatDate(b.checkOutDate)}</span>
      ),
    },
    {
      header: 'Tổng tiền',
      accessor: (b: BookingSummaryResponse) => (
        <span className="font-medium text-[#1E293B]">{formatVnd(b.totalAmount)}</span>
      ),
    },
    {
      header: 'Trạng thái',
      accessor: (b: BookingSummaryResponse) => {
        const cfg = STATUS_VI[b.status] ?? { label: b.status, variant: 'neutral' as StatusVariant };
        return <StatusBadge status={cfg.label} variant={cfg.variant} />;
      },
    },
  ];

  const actions = [
    { label: 'Xem chi tiết', onClick: (b: BookingSummaryResponse) => navigate(`/manager/bookings/${b.id}`) },
  ];

  const showEmptyAssigned = !propLoading && properties.length === 0;

  return (
    <ManagerLayout>
      <div className="space-y-6">
        <h1 className="font-display text-[28px] font-bold text-[#1E293B]">
          Danh sách đặt phòng
        </h1>

        {propError && <Alert variant="error" message={propError} />}
        {error && <Alert variant="error" message={error} closeable onClose={() => setError(null)} />}

        {showEmptyAssigned && (
          <div className="bg-white rounded-[16px] border border-[#E2E8F0] p-10 text-center">
            <p className="text-[#64748B]">Bạn chưa được gán homestay nào.</p>
          </div>
        )}

        {!showEmptyAssigned && (
          <>
            <div className="bg-white rounded-[16px] border border-[#E2E8F0] p-4">
              <div className="flex flex-wrap gap-3 items-end">
                <div className="flex-1 min-w-[180px]">
                  <label className="block text-xs text-[#64748B] mb-1">Tìm kiếm</label>
                  <input
                    className="w-full border border-[#E2E8F0] rounded-md px-3 py-2 text-sm"
                    placeholder="Mã đặt phòng, tên khách, số phòng…"
                    value={search}
                    onChange={e => setSearch(e.target.value)}
                  />
                </div>

                <div className="min-w-[160px]">
                  <label className="block text-xs text-[#64748B] mb-1">Homestay</label>
                  <select
                    className="w-full border border-[#E2E8F0] rounded-md px-3 py-2 text-sm bg-white"
                    value={selectedPropertyId}
                    onChange={e => setPropertyId(e.target.value)}
                    disabled={propLoading}
                  >
                    <option value="">Tất cả homestay</option>
                    {properties.map(p => (
                      <option key={p.id} value={p.id}>{p.name}</option>
                    ))}
                  </select>
                </div>

                <div className="min-w-[150px]">
                  <label className="block text-xs text-[#64748B] mb-1">Trạng thái</label>
                  <select
                    className="w-full border border-[#E2E8F0] rounded-md px-3 py-2 text-sm bg-white"
                    value={selectedStatus}
                    onChange={e => setStatus(e.target.value)}
                  >
                    <option value="">Tất cả</option>
                    {ALL_STATUSES.map(s => (
                      <option key={s} value={s}>{STATUS_VI[s]?.label ?? s}</option>
                    ))}
                  </select>
                </div>

                <div className="min-w-[140px]">
                  <label className="block text-xs text-[#64748B] mb-1">Nhận phòng từ</label>
                  <input
                    type="date"
                    className="w-full border border-[#E2E8F0] rounded-md px-3 py-2 text-sm"
                    value={checkInFrom}
                    onChange={e => setCheckInFrom(e.target.value)}
                  />
                </div>

                <div className="min-w-[140px]">
                  <label className="block text-xs text-[#64748B] mb-1">Nhận phòng đến</label>
                  <input
                    type="date"
                    className="w-full border border-[#E2E8F0] rounded-md px-3 py-2 text-sm"
                    value={checkInTo}
                    min={checkInFrom || undefined}
                    onChange={e => setCheckInTo(e.target.value)}
                  />
                </div>

                {hasFilters && (
                  <button
                    type="button"
                    className="text-sm text-[#64748B] hover:text-[#1E293B] px-2 py-2"
                    onClick={handleClearFilters}
                  >
                    Xóa bộ lọc
                  </button>
                )}
              </div>
            </div>

            <div className="bg-white rounded-[16px] border border-[#E2E8F0]">
              {loading ? (
                <TableSkeleton />
              ) : bookings.length === 0 ? (
                <div className="p-10 text-center text-[#64748B]">
                  Không có đặt phòng nào phù hợp.
                </div>
              ) : (
                <>
                  <p className="px-4 pt-3 text-xs text-[#64748B]">
                    {totalElements} đặt phòng
                  </p>
                  <DataTable
                    columns={columns}
                    data={bookings}
                    keyExtractor={b => b.id}
                    actions={actions}
                  />
                </>
              )}
            </div>

            <Pagination page={page} totalPages={totalPages} onPageChange={handlePageChange} />
          </>
        )}
      </div>
    </ManagerLayout>
  );
}
