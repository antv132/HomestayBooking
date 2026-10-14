// SCR-26: Payment History
import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import CustomerLayout from '../../layouts/CustomerLayout';
import Alert from '../../components/ui/Alert';
import { paymentApi, PaymentSummaryResponse } from '../../api/paymentApi';
import { invoiceApi, InvoiceSummary } from '../../api/invoiceApi';

function formatDateTime(iso: string): string {
  const d = new Date(iso);
  const day = String(d.getDate()).padStart(2, '0');
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const year = d.getFullYear();
  const hours = String(d.getHours()).padStart(2, '0');
  const minutes = String(d.getMinutes()).padStart(2, '0');
  return `${day}/${month}/${year} ${hours}:${minutes}`;
}

function formatTxnId(id: string): string {
  return `TXN-${id.replace(/-/g, '').slice(0, 8).toUpperCase()}`;
}

function formatBookingShortId(uuid: string): string {
  return uuid.split('-')[0].toUpperCase();
}

function formatVnd(n: number) {
  return `₫${Number(n).toLocaleString('vi-VN')}`;
}

const STATUS_CONFIG: Record<string, { cls: string; label: string }> = {
  PENDING:  { cls: 'badge-warning', label: 'Chờ xử lý' },
  PAID:     { cls: 'badge-success', label: 'Đã thanh toán' },
  FAILED:   { cls: 'badge-error',   label: 'Thất bại' },
  REFUNDED: { cls: 'badge-info',    label: 'Đã hoàn' },
  PARTIALLY_REFUNDED: { cls: 'badge-info', label: 'Hoàn một phần' },
};

const TYPE_LABELS: Record<string, string> = {
  DEPOSIT: 'Đặt cọc (40%)',
  REMAINING_BALANCE: 'Còn lại (60%)',
  DAMAGE_FEE: 'Phí hư hỏng',
};

const METHOD_LABELS: Record<string, string> = {
  VNPAY: 'VNPay',
  BANK_TRANSFER: 'Chuyển khoản',
  CASH: 'Tiền mặt',
};

const FILTER_TABS = [
  { key: 'ALL', label: 'Tất cả thanh toán' },
  { key: 'PENDING', label: 'Chờ xử lý' },
  { key: 'PAID', label: 'Đã thanh toán' },
  { key: 'FAILED', label: 'Thất bại' },
  { key: 'INVOICES', label: 'Hóa đơn (FR-11)' },
];

const INVOICE_STATUS: Record<string, { cls: string; label: string }> = {
  UNPAID: { cls: 'badge-warning', label: 'Chưa thanh toán' },
  PENDING_PAYMENT: { cls: 'badge-warning', label: 'Đang thanh toán' },
  PAID: { cls: 'badge-success', label: 'Đã thanh toán' },
  CANCELLED: { cls: 'badge-neutral', label: 'Đã hủy' },
};

export default function PaymentHistoryPage() {
  const [payments, setPayments] = useState<PaymentSummaryResponse[]>([]);
  const [invoices, setInvoices] = useState<InvoiceSummary[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [filter, setFilter] = useState('ALL');
  const [loading, setLoading] = useState(true);
  const [apiError, setApiError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      setApiError(null);
      try {
        if (filter === 'INVOICES') {
          const data = await invoiceApi.getMyInvoices({ page, size: 20 });
          if (cancelled) return;
          setInvoices(data.content ?? []);
          setPayments([]);
          setTotalPages(data.totalPages ?? 0);
        } else {
          const res = await paymentApi.getMyPayments({ page, size: 20, status: filter === 'ALL' ? undefined : filter });
          if (cancelled) return;
          if (res.success && res.data) {
            setPayments(res.data.content);
            setInvoices([]);
            setTotalPages(res.data.totalPages);
          } else {
            setPayments([]); setInvoices([]); setTotalPages(0);
            setApiError(res.message || 'Không tải được lịch sử thanh toán.');
          }
        }
      } catch {
        if (!cancelled) { setPayments([]); setInvoices([]); setApiError('Không tải được lịch sử. Vui lòng thử lại.'); }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => { cancelled = true; };
  }, [page, filter]);

  const showInvoices = filter === 'INVOICES';

  return (
    <CustomerLayout>
      <h1 className="heading-md" style={{ marginBottom: 24 }}>Lịch sử thanh toán</h1>
      <div className="flex gap-1 flex-wrap p-1 mb-5 bg-[var(--surface-bone)] rounded-full w-fit">
        {FILTER_TABS.map(tab => (
          <button key={tab.key} type="button" className={`tab-pill ${filter === tab.key ? 'active' : ''}`}
            onClick={() => { setFilter(tab.key); setPage(0); }}>
            {tab.label}
          </button>
        ))}
      </div>
      {apiError && (<div style={{ marginBottom: 20 }}><Alert variant="error" message={apiError} /></div>)}
      {loading ? (
        <div style={{ textAlign: 'center', padding: 40 }}><p className="body-md text-charcoal">Đang tải...</p></div>
      ) : showInvoices ? (
        invoices.length === 0 ? (
          <div style={{ textAlign: 'center', padding: 60 }}>
            <h3 className="heading-sm" style={{ marginBottom: 8 }}>Chưa có hóa đơn.</h3>
          </div>
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Ngày phát hành</th><th>Loại</th><th>Số tiền</th>
                  <th>Hạn</th><th>Đơn đặt phòng</th><th>Trạng thái</th>
                </tr>
              </thead>
              <tbody>
                {invoices.map((inv) => {
                  const statusCfg = INVOICE_STATUS[inv.status] ?? { cls: 'badge-neutral', label: inv.status };
                  return (
                    <tr key={inv.id}>
                      <td className="text-charcoal">{inv.createdAt ? formatDateTime(inv.createdAt) : '—'}</td>
                      <td><span className="badge badge-tag">{TYPE_LABELS[inv.type] ?? inv.type}</span></td>
                      <td style={{ fontWeight: 700 }}>{formatVnd(inv.amount)}</td>
                      <td className="text-charcoal">{inv.dueDate}{inv.overdue ? ' · Quá hạn' : ''}</td>
                      <td>
                        <Link to={`/customer/bookings/${inv.bookingId}`} className="text-primary" style={{ textDecoration: 'none', fontWeight: 600 }}>
                          #{formatBookingShortId(inv.bookingId)}
                        </Link>
                      </td>
                      <td><span className={`badge ${statusCfg.cls}`}>{statusCfg.label}</span></td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )
      ) : payments.length === 0 ? (
        <div style={{ textAlign: 'center', padding: 60 }}>
          <div style={{ fontSize: 40, marginBottom: 12 }}>💳</div>
          <h3 className="heading-sm" style={{ marginBottom: 8 }}>Chưa có lịch sử thanh toán.</h3>
        </div>
      ) : (
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Ngày</th><th>Mã giao dịch</th><th>Số tiền</th>
                <th>Loại</th><th>Phương thức</th><th>Đơn đặt phòng</th><th>Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              {payments.map(p => {
                const statusCfg = STATUS_CONFIG[p.status] ?? { cls: 'badge-neutral', label: p.status };
                const displayDate = p.paidAt || p.createdAt;
                return (
                  <tr key={p.id}>
                    <td className="text-charcoal">{displayDate ? formatDateTime(displayDate) : '—'}</td>
                    <td><span className="code-md">{formatTxnId(p.id)}</span></td>
                    <td style={{ fontWeight: 700 }}>{formatVnd(p.amount)}</td>
                    <td><span className="badge badge-tag">{TYPE_LABELS[p.type] ?? p.type}</span></td>
                    <td className="text-charcoal">{METHOD_LABELS[p.method] ?? p.method}</td>
                    <td>
                      <Link to={`/customer/bookings/${p.bookingId}`} className="text-primary" style={{ textDecoration: 'none', fontWeight: 600 }}>
                        #{formatBookingShortId(p.bookingId)}
                      </Link>
                    </td>
                    <td><span className={`badge ${statusCfg.cls}`}>{statusCfg.label}</span></td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
      {totalPages > 1 && (
        <div className="flex justify-center gap-2 mt-6">
          <button type="button" className="btn-secondary" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>Trước</button>
          <span className="body-sm text-charcoal">Trang {page + 1}/{totalPages}</span>
          <button type="button" className="btn-secondary" disabled={page + 1 >= totalPages} onClick={() => setPage((p) => p + 1)}>Sau</button>
        </div>
      )}
    </CustomerLayout>
  );
}
