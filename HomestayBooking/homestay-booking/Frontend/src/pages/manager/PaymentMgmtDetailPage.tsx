import { Link, useParams } from 'react-router-dom';
import ManagerLayout from '../../layouts/ManagerLayout';
import { useQuery } from '@tanstack/react-query';
import { paymentApi } from '../../api/paymentApi';

function SBadge({ s }: { s: string }) {
  const m: Record<string, { cls: string; l: string }> = {
    PENDING: { cls: 'badge-warning', l: 'Pending' },
    PAID: { cls: 'badge-success', l: 'Paid' },
    FAILED: { cls: 'badge-error', l: 'Failed' },
    REFUNDED: { cls: 'badge-info', l: 'Refunded' },
    PARTIALLY_REFUNDED: { cls: 'badge-info', l: 'Partially refunded' },
  };
  const v = m[s] || { cls: 'badge-neutral', l: s };
  return <span className={`badge ${v.cls}`}>{v.l}</span>;
}

const PAYMENT_TYPE_LABEL: Record<string, string> = {
  DEPOSIT: 'Deposit (40%)',
  REMAINING_BALANCE: 'Remaining Balance (60%)',
  DAMAGE_FEE: 'Damage Fee',
  REFUND: 'Refund',
};

export default function PaymentMgmtDetailPage() {
  const { id } = useParams();

  const { data, isLoading, isError } = useQuery({
    queryKey: ['manager_payment', id],
    queryFn: () => paymentApi.getPaymentDetail(id!),
    enabled: !!id,
  });

  if (isLoading) return <ManagerLayout><div style={{ padding: 40 }}>Loading payment details...</div></ManagerLayout>;
  if (isError || !data?.data) return <ManagerLayout><div style={{ padding: 40, color: 'var(--error)' }}>Error loading payment or not found.</div></ManagerLayout>;

  const p = data.data;

  return (
    <ManagerLayout>
      <div className="flex items-center gap-2 body-sm text-charcoal" style={{ marginBottom: 20 }}>
        <Link to="/manager/payments" className="text-primary" style={{ textDecoration: 'none' }}>Payments</Link>
        <span>›</span>
        <span style={{ fontWeight: 600 }}>#{p.id.substring(0, 8)}...</span>
      </div>
      <h1 className="heading-md" style={{ marginBottom: 24 }}>Payment Detail</h1>
      
      <div style={{ maxWidth: 640 }}>
        <div className="card-lg" style={{ padding: 28 }}>
          <h2 className="heading-sm" style={{ marginBottom: 20 }}>Transaction Info</h2>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, marginBottom: 24 }}>
            {(
              [
                { l: 'Payment ID', v: p.id },
                { l: 'Booking ID', v: p.bookingId, link: `/manager/bookings/${p.bookingId}` },
                { l: 'Customer', v: p.customerName },
                { l: 'Type', v: PAYMENT_TYPE_LABEL[p.type] ?? p.type },
                { l: 'Method', v: p.method.replace('_', ' ') },
                { l: 'Amount', v: `₫${p.amount.toLocaleString()}` },
                { l: 'Status', v: null as string | null },
                { l: 'Created At', v: new Date(p.createdAt).toLocaleString('en-US') },
                ...(p.paidAt
                  ? [{ l: 'Paid At', v: new Date(p.paidAt).toLocaleString('en-US') }]
                  : []),
              ] as Array<{ l: string; v: string | null; link?: string }>
            ).map(row => (
              <div key={row.l} style={{ wordBreak: 'break-all' }}>
                <p className="body-sm text-charcoal">{row.l}</p>
                {row.v === null ? (
                  <div style={{ marginTop: 4 }}><SBadge s={p.status} /></div>
                ) : row.link ? (
                  <p style={{ fontWeight: 600, marginTop: 4 }}><Link to={row.link} className="text-primary">{row.v}</Link></p>
                ) : (
                  <p style={{ fontWeight: 600, marginTop: 4 }}>{row.v}</p>
                )}
              </div>
            ))}
          </div>

          {p.status === 'PENDING' && p.method === 'VNPAY' && (
            <div style={{ marginTop: 8, padding: '12px 16px', background: 'var(--surface-bone)', borderRadius: 8 }}>
              <p className="body-sm text-charcoal">
                Đang chờ xác nhận từ cổng VNPay (Return URL).
              </p>
            </div>
          )}
        </div>
      </div>
    </ManagerLayout>
  );
}
