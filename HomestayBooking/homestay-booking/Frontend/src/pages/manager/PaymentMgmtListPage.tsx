import { useState, useEffect, useCallback, useRef, type ReactNode } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import ManagerLayout from '../../layouts/ManagerLayout';
import Alert from '../../components/ui/Alert';
import { DataTable, StatusBadge, Drawer } from '../../components/ui';
import type { StatusVariant } from '../../components/ui/StatusBadge';
import {
  fetchManagerPaymentsV1,
  paymentApi,
  type PaymentSummaryResponse,
  type PaymentDetailResponse,
} from '../../api/paymentApi';
import { managerApi } from '../../api/managerApi';
import type { AssignedProperty } from '../../api/reportApi';

type TabKey = 'ALL' | 'PENDING' | 'PAID';

const STATUS_VI: Record<string, { label: string; variant: StatusVariant }> = {
  PENDING:  { label: 'Chờ VNPay', variant: 'warning' },
  PAID:     { label: 'Đã thanh toán', variant: 'success' },
  FAILED:   { label: 'Thất bại', variant: 'danger' },
  REFUNDED: { label: 'Đã hoàn', variant: 'info' },
  PARTIALLY_REFUNDED: { label: 'Hoàn một phần', variant: 'info' },
};

const TYPE_VI: Record<string, string> = {
  DEPOSIT: 'Đặt cọc',
  REMAINING_BALANCE: 'Phần còn lại',
  DAMAGE_FEE: 'Phí thiệt hại',
};

const TABS: { key: TabKey; label: string }[] = [
  { key: 'ALL', label: 'Tất cả' },
  { key: 'PENDING', label: 'Chờ VNPay' },
  { key: 'PAID', label: 'Hoàn tất' },
];

const PAGE_SIZE = 10;

function formatVnd(value: number): string {
  return new Intl.NumberFormat('vi-VN').format(value) + ' ₫';
}

function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return '—';
  return new Date(iso).toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function shortTxnId(id: string): string {
  return `TXN-${id.replace(/-/g, '').slice(0, 8).toUpperCase()}`;
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

function DetailRow({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="flex justify-between gap-4 items-center py-3 border-b border-gray-100 border-dashed last:border-0">
      <dt className="text-sm font-medium text-gray-500 shrink-0">{label}</dt>
      <dd className="text-sm text-right m-0 text-gray-900">{children}</dd>
    </div>
  );
}

export default function PaymentMgmtListPage() {
  const [searchParams] = useSearchParams();
  const initSearch = searchParams.get('bookingId') ?? '';

  const [properties, setProperties] = useState<AssignedProperty[]>([]);
  const [propLoading, setPropLoading] = useState(true);
  const [propError, setPropError] = useState<string | null>(null);

  const [activeTab, setActiveTab] = useState<TabKey>('ALL');
  const [search, setSearch] = useState(initSearch);
  const [selectedPropertyId, setPropertyId] = useState('');
  const [page, setPage] = useState(0);

  const [payments, setPayments] = useState<PaymentSummaryResponse[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [detail, setDetail] = useState<PaymentDetailResponse | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState<string | null>(null);

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

  const loadPayments = useCallback((
    pg: number,
    tab: TabKey,
    srch: string,
    propId: string,
  ) => {
    setLoading(true);
    setError(null);
    fetchManagerPaymentsV1({
      page: pg,
      size: PAGE_SIZE,
      search: srch || undefined,
      propertyId: propId || undefined,
      status: tab === 'ALL' ? undefined : tab,
      method: 'VNPAY',
      sort: 'createdAt,desc',
    })
      .then(data => {
        setPayments(data.content);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
      })
      .catch((err: unknown) => {
        const ax = err as { response?: { status?: number; data?: { message?: string } } };
        if (ax?.response?.status === 403) {
          setError('Bạn không có quyền xem homestay này.');
        } else {
          setError(ax?.response?.data?.message ?? 'Không thể tải danh sách thanh toán.');
        }
        setPayments([]);
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      setPage(0);
      loadPayments(0, activeTab, search, selectedPropertyId);
    }, 300);
    return () => clearTimeout(debounceRef.current);
  }, [activeTab, search, selectedPropertyId, loadPayments]);

  function handlePageChange(newPage: number) {
    setPage(newPage);
    loadPayments(newPage, activeTab, search, selectedPropertyId);
  }

  async function openDrawer(paymentId: string) {
    setDrawerOpen(true);
    setDetail(null);
    setDetailError(null);
    setDetailLoading(true);
    try {
      const res = await paymentApi.getPaymentDetail(paymentId);
      setDetail(res.data);
    } catch (err: unknown) {
      const ax = err as { response?: { data?: { message?: string } } };
      setDetailError(ax?.response?.data?.message ?? 'Không thể tải chi tiết thanh toán.');
    } finally {
      setDetailLoading(false);
    }
  }

  function closeDrawer() {
    setDrawerOpen(false);
    setDetail(null);
    setDetailError(null);
  }

  const columns = [
    {
      header: 'Ngày',
      accessor: (p: PaymentSummaryResponse) => (
        <span className="text-sm text-[#334155] whitespace-nowrap">{formatDateTime(p.createdAt)}</span>
      ),
    },
    {
      header: 'Mã giao dịch',
      accessor: (p: PaymentSummaryResponse) => (
        <span className="font-mono text-sm" title={p.id}>{shortTxnId(p.id)}</span>
      ),
    },
    {
      header: 'Booking',
      accessor: (p: PaymentSummaryResponse) => (
        <span className="font-mono text-sm text-[#0F766E]">{shortBookingId(p.bookingId)}</span>
      ),
    },
    {
      header: 'Loại',
      accessor: (p: PaymentSummaryResponse) => (
        <span className="text-sm text-[#334155]">{TYPE_VI[p.type] ?? p.type}</span>
      ),
    },
    {
      header: 'Số tiền',
      accessor: (p: PaymentSummaryResponse) => (
        <span className="font-semibold text-[#1E293B] whitespace-nowrap">{formatVnd(p.amount)}</span>
      ),
      className: 'text-right',
    },
    {
      header: 'Trạng thái',
      accessor: (p: PaymentSummaryResponse) => {
        const cfg = STATUS_VI[p.status] ?? { label: p.status, variant: 'neutral' as StatusVariant };
        return <StatusBadge status={cfg.label} variant={cfg.variant} />;
      },
    },
    {
      header: 'Thao tác',
      accessor: (p: PaymentSummaryResponse) => (
        <div className="flex justify-end" onClick={e => e.stopPropagation()}>
          <button
            type="button"
            className="text-sm font-medium text-[#0F766E] hover:underline"
            onClick={() => openDrawer(p.id)}
          >
            Xem chi tiết
          </button>
        </div>
      ),
      className: 'text-right w-[110px]',
    },
  ];

  const showEmptyAssigned = !propLoading && properties.length === 0;
  const statusCfg = detail
    ? (STATUS_VI[detail.status] ?? { label: detail.status, variant: 'neutral' as StatusVariant })
    : null;

  return (
    <ManagerLayout>
      <div className="space-y-6">
        <div>
          <h1 className="heading-md m-0">Payments</h1>
          <p className="text-sm text-[#64748B] mt-1 mb-0">
            Xem chi tiết giao dịch VNPay
            {!showEmptyAssigned && !loading ? ` · ${totalElements} giao dịch` : ''}
          </p>
        </div>

        {propError && <Alert variant="error" message={propError} />}
        {error && <Alert variant="error" message={error} closeable onClose={() => setError(null)} />}

        {showEmptyAssigned ? (
          <div className="text-center py-16 bg-white rounded-xl border border-[#E2E8F0]">
            <p className="text-[#64748B] m-0">Bạn chưa được gán homestay nào.</p>
          </div>
        ) : (
          <>
            <div className="flex flex-wrap gap-1 border-b border-[#E2E8F0]">
              {TABS.map(tab => (
                <button
                  key={tab.key}
                  type="button"
                  onClick={() => {
                    setActiveTab(tab.key);
                    setPage(0);
                  }}
                  className={`px-4 py-2.5 text-sm font-medium border-b-2 -mb-px transition-colors ${
                    activeTab === tab.key
                      ? 'border-[#0F766E] text-[#0F766E]'
                      : 'border-transparent text-[#64748B] hover:text-[#1E293B]'
                  }`}
                >
                  {tab.label}
                </button>
              ))}
            </div>

            <div className="flex flex-col sm:flex-row gap-3">
              <input
                type="search"
                className="input flex-1"
                placeholder="Tìm mã giao dịch, booking, tên khách…"
                value={search}
                onChange={e => setSearch(e.target.value)}
                aria-label="Tìm kiếm"
              />
              <select
                className="input sm:w-56"
                value={selectedPropertyId}
                onChange={e => setPropertyId(e.target.value)}
                disabled={propLoading}
                aria-label="Homestay"
              >
                <option value="">Tất cả homestay</option>
                {properties.map(p => (
                  <option key={p.id} value={p.id}>{p.name}</option>
                ))}
              </select>
            </div>

            {loading ? (
              <div className="space-y-2 bg-white rounded-xl border border-[#E2E8F0] p-4">
                {Array.from({ length: 6 }).map((_, i) => (
                  <div key={i} className="h-12 bg-[#F1F5F9] rounded animate-pulse" />
                ))}
              </div>
            ) : payments.length === 0 ? (
              <div className="text-center py-16 bg-white rounded-xl border border-[#E2E8F0] text-[#64748B]">
                Không có giao dịch nào.
              </div>
            ) : (
              <>
                <DataTable
                  columns={columns}
                  data={payments}
                  keyExtractor={p => p.id}
                  onRowClick={p => openDrawer(p.id)}
                  getRowClassName={p => (
                    p.status === 'PENDING' ? 'bg-amber-50/60' : ''
                  )}
                />
                <Pagination page={page} totalPages={totalPages} onPageChange={handlePageChange} />
              </>
            )}
          </>
        )}
      </div>

      <Drawer
        isOpen={drawerOpen}
        onClose={closeDrawer}
        title="Chi tiết thanh toán"
        width="max-w-md"
      >
        {detailLoading && (
          <div className="space-y-4">
            <div className="h-28 bg-gray-100 rounded-2xl animate-pulse" />
            <div className="h-64 bg-gray-100 rounded-2xl animate-pulse" />
          </div>
        )}

        {detailError && (
          <Alert variant="error" message={detailError} closeable onClose={() => setDetailError(null)} />
        )}

        {!detailLoading && detail && statusCfg && (
          <div className="space-y-6">
            {/* Amount Card */}
            <div className="bg-gradient-to-br from-teal-50 to-teal-100/50 rounded-2xl p-6 border border-teal-100 shadow-sm relative overflow-hidden">
              <div className="absolute top-0 right-0 p-4 opacity-5">
                <svg className="w-32 h-32 text-teal-900 transform translate-x-8 -translate-y-8" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z" />
                </svg>
              </div>
              <div className="relative z-10 flex flex-col gap-4">
                <div className="flex items-start justify-between">
                  <div className="space-y-1">
                    <p className="font-mono text-sm font-medium text-teal-800/70 m-0 uppercase tracking-wider">{shortTxnId(detail.id)}</p>
                    <p className="text-4xl font-extrabold text-teal-900 m-0 tracking-tight">{formatVnd(detail.amount)}</p>
                  </div>
                  <div className="shrink-0 bg-white shadow-sm rounded-full p-1">
                    <StatusBadge status={statusCfg.label} variant={statusCfg.variant} />
                  </div>
                </div>
              </div>
            </div>

            {/* General Info */}
            <div className="bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden">
              <div className="px-5 py-4 border-b border-gray-100 bg-gray-50/50">
                <h3 className="text-sm font-semibold text-gray-900 m-0">Thông tin giao dịch</h3>
              </div>
              <div className="px-5 py-2">
                <dl className="m-0">
                  <DetailRow label="Khách hàng">
                    <span className="font-medium text-gray-900">{detail.customerName}</span>
                  </DetailRow>
                  <DetailRow label="Booking">
                    <Link
                      to={`/manager/bookings/${detail.bookingId}`}
                      className="font-mono font-medium text-teal-600 hover:text-teal-700 hover:underline transition-colors"
                      onClick={closeDrawer}
                    >
                      {shortBookingId(detail.bookingId)}
                    </Link>
                  </DetailRow>
                  <DetailRow label="Loại">
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800">
                      {TYPE_VI[detail.type] ?? detail.type}
                    </span>
                  </DetailRow>
                  <DetailRow label="Phương thức">
                    <span className="font-medium text-gray-900">VNPay</span>
                  </DetailRow>
                  <DetailRow label="Tạo lúc">
                    <span className="text-gray-600">{formatDateTime(detail.createdAt)}</span>
                  </DetailRow>
                  <DetailRow label="Thanh toán lúc">
                    <span className="text-gray-600">{formatDateTime(detail.paidAt)}</span>
                  </DetailRow>
                </dl>
              </div>
            </div>

            {/* Gateway Details */}
            {(detail.orderRef || detail.gatewayTransactionId || detail.gatewayResponseCode) && (
              <div className="bg-gray-50 rounded-2xl border border-gray-200 p-5">
                <h3 className="text-[11px] font-bold tracking-wider text-gray-500 uppercase mb-3">Chi tiết cổng thanh toán</h3>
                <dl className="m-0">
                  {detail.orderRef && (
                    <DetailRow label="Order ref">
                      <span className="font-mono text-xs text-gray-600 break-all">{detail.orderRef}</span>
                    </DetailRow>
                  )}
                  {detail.gatewayTransactionId && (
                    <DetailRow label="Mã VNPay">
                      <span className="font-mono text-xs text-gray-600 break-all">{detail.gatewayTransactionId}</span>
                    </DetailRow>
                  )}
                  {detail.gatewayResponseCode && (
                    <DetailRow label="Mã phản hồi">
                      <span className="font-mono text-xs font-medium text-gray-700 bg-white px-2 py-1 rounded border border-gray-200 shadow-sm">{detail.gatewayResponseCode}</span>
                    </DetailRow>
                  )}
                </dl>
              </div>
            )}

            {/* Status Messages */}
            {detail.status === 'PENDING' && (
              <div className="flex items-start gap-3 p-4 rounded-xl bg-amber-50 border border-amber-200 shadow-sm">
                <svg className="w-5 h-5 text-amber-500 shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <div>
                  <h4 className="text-sm font-medium text-amber-800 m-0">Đang chờ xác nhận</h4>
                  <p className="m-0 text-sm text-amber-700 mt-1">Đang chờ VNPay xác nhận. Hệ thống sẽ cập nhật tự động khi có kết quả.</p>
                </div>
              </div>
            )}

            {detail.status === 'FAILED' && (
              <div className="flex items-start gap-3 p-4 rounded-xl bg-red-50 border border-red-200 shadow-sm">
                <svg className="w-5 h-5 text-red-500 shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                </svg>
                <div>
                  <h4 className="text-sm font-medium text-red-800 m-0">Thanh toán thất bại</h4>
                  <p className="m-0 text-sm text-red-700 mt-1">Giao dịch đã thất bại hoặc bị hủy trên cổng VNPay.</p>
                </div>
              </div>
            )}
          </div>
        )}
      </Drawer>
    </ManagerLayout>
  );
}
