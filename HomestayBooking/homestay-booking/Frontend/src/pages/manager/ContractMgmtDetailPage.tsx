import { Link, useParams } from 'react-router-dom';
import { useState, useEffect } from 'react';
import ManagerLayout from '../../layouts/ManagerLayout';
import { useQuery, useMutation } from '@tanstack/react-query';
import { contractApi } from '../../api/contractApi';

import type { ContractDetailResponse } from '../../api/contractApi';

const STATUS_CONFIG: Record<string, { cls: string; label: string }> = {
  ACTIVE:    { cls: 'badge-success', label: 'Active' },
  COMPLETED: { cls: 'badge-neutral', label: 'Completed' },
  CANCELLED: { cls: 'badge-error',   label: 'Cancelled' },
};

function SectionCard({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="card" style={{ padding: 28, marginBottom: 16 }}>
      <p style={{
        fontSize: 11, fontWeight: 700, letterSpacing: '0.06em',
        textTransform: 'uppercase', color: 'var(--charcoal)',
        marginBottom: 18,
      }}>
        {title}
      </p>
      {children}
    </div>
  );
}

function ContractInfoCard({ contract }: { contract: ContractDetailResponse }) {
  const statusCfg = STATUS_CONFIG[contract.status] ?? { cls: 'badge-neutral', label: contract.status };
  return (
    <SectionCard title="Contract Info">
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: 16 }}>
        <div>
          <p style={{ fontSize: 22, fontWeight: 700, letterSpacing: '-0.3px', marginBottom: 4 }}>
            ROOM RENTAL CONTRACT
          </p>
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            <span style={{ fontSize: 12, color: 'var(--charcoal)' }}>
              ID: <span className="code-md">{contract.id.slice(0, 8).toUpperCase()}</span>
            </span>
            <span style={{ color: 'var(--hairline-strong)', fontSize: 12 }}>·</span>
            <span style={{ fontSize: 12, color: 'var(--charcoal)' }}>
              Booking: <span className="code-md">{contract.bookingId.slice(0, 8).toUpperCase()}</span>
            </span>
          </div>
        </div>
        <span className={`badge ${statusCfg.cls}`} style={{ flexShrink: 0 }}>{statusCfg.label}</span>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
        <div>
          <p style={{ fontSize: 11, color: 'var(--ash)', marginBottom: 3 }}>Generated At</p>
          <p style={{ fontWeight: 600, fontSize: 13 }}>
            {new Date(contract.generatedAt).toLocaleString('en-US')}
          </p>
        </div>
        <div>
          <p style={{ fontSize: 11, color: 'var(--ash)', marginBottom: 3 }}>Last Sent Email</p>
          <p style={{ fontWeight: 600, fontSize: 13 }}>
            {contract.sentAt
              ? new Date(contract.sentAt).toLocaleString('en-US')
              : <span style={{ color: 'var(--ash)', fontWeight: 400 }}>Not sent</span>
            }
          </p>
        </div>
        {contract.pdfUrl && (
          <div style={{ gridColumn: '1 / -1' }}>
            <p style={{ fontSize: 11, color: 'var(--ash)', marginBottom: 3 }}>PDF URL</p>
            <p style={{ fontWeight: 600, fontSize: 13, wordBreak: 'break-all' }}>{contract.pdfUrl}</p>
          </div>
        )}
      </div>
    </SectionCard>
  );
}

function CustomerInfoCard({ contract }: { contract: ContractDetailResponse }) {
  return (
    <SectionCard title="Customer Info">
      <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
        <div style={{
          width: 44, height: 44, borderRadius: '50%',
          background: 'var(--primary)', color: '#fff',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontWeight: 700, fontSize: 18, flexShrink: 0,
        }}>
          {contract.customerName.charAt(0).toUpperCase()}
        </div>
        <div>
          <p style={{ fontWeight: 700, fontSize: 16, marginBottom: 3 }}>{contract.customerName}</p>
          <p style={{ fontSize: 13, color: 'var(--charcoal)', marginBottom: 2 }}>{contract.customerEmail}</p>
          {contract.customerPhone && (
            <p style={{ fontSize: 13, color: 'var(--charcoal)' }}>{contract.customerPhone}</p>
          )}
        </div>
      </div>
    </SectionCard>
  );
}

function RoomInfoCard({ contract }: { contract: ContractDetailResponse }) {
  const nights = Math.max(1, Math.ceil(
    (new Date(contract.checkOutDate).getTime() - new Date(contract.checkInDate).getTime()) / 86_400_000
  ));
  const pricePerNight = Math.round(contract.totalAmount / nights);

  const rows = [
    { label: 'Room',          value: contract.roomNumber },
    { label: 'Property',      value: contract.propertyName },
    { label: 'Check-in',      value: new Date(contract.checkInDate).toLocaleDateString('en-US') },
    { label: 'Check-out',     value: new Date(contract.checkOutDate).toLocaleDateString('en-US') },
    { label: 'Duration',      value: `${nights} nights` },
    { label: 'Price/night',   value: `₫${pricePerNight.toLocaleString('en-US')}` },
  ];

  return (
    <SectionCard title="Stay Info">
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px 24px' }}>
        {rows.map(r => (
          <div key={r.label}>
            <p style={{ fontSize: 11, color: 'var(--ash)', marginBottom: 3 }}>{r.label}</p>
            <p style={{ fontWeight: 600, fontSize: 13 }}>{r.value}</p>
          </div>
        ))}
      </div>
    </SectionCard>
  );
}

function PaymentTermsCard({ contract }: { contract: ContractDetailResponse }) {
  const remaining = contract.totalAmount - contract.depositAmount;
  return (
    <SectionCard title="Payment Terms">
      {[
        { label: 'Deposit (40%)',     amount: contract.depositAmount },
        { label: 'Remaining (60%)',   amount: remaining },
      ].map(r => (
        <div
          key={r.label}
          style={{ display: 'flex', justifyContent: 'space-between', padding: '10px 0', borderBottom: '1px solid var(--hairline)' }}
        >
          <span style={{ fontSize: 14, color: 'var(--body)' }}>{r.label}</span>
          <span style={{ fontWeight: 600, fontSize: 14 }}>₫{r.amount.toLocaleString('en-US')}</span>
        </div>
      ))}
      <div style={{ display: 'flex', justifyContent: 'space-between', padding: '14px 0 0' }}>
        <span style={{ fontWeight: 700, fontSize: 16 }}>Total</span>
        <span style={{ fontWeight: 800, fontSize: 20, color: 'var(--primary)' }}>
          ₫{contract.totalAmount.toLocaleString('en-US')}
        </span>
      </div>
    </SectionCard>
  );
}

export default function ContractMgmtDetailPage() {
  const { id } = useParams();
  const [blobUrl, setBlobUrl] = useState<string | null>(null);
  const [pdfError, setPdfError] = useState<string | null>(null);

  const { data, isLoading, isError } = useQuery({
    queryKey: ['manager_contract', id],
    queryFn: () => contractApi.getContractDetail(id!),
    enabled: !!id,
  });

  useEffect(() => {
    if (!id) return;
    let cancelled = false;
    let url: string | null = null;
    setPdfError(null);
    contractApi.getContractPdfBlob(id)
      .then((blob) => {
        if (cancelled) return;
        url = URL.createObjectURL(new Blob([blob], { type: 'application/pdf' }));
        setBlobUrl(url);
      })
      .catch(() => {
        if (!cancelled) setPdfError('Unable to load PDF preview.');
      });
    return () => {
      cancelled = true;
      if (url) URL.revokeObjectURL(url);
    };
  }, [id]);

  const resendMutation = useMutation({
    mutationFn: () => contractApi.resendContractEmail(id!),
    onSuccess: () => alert('Resend queued — email will be sent shortly.'),
    onError: () => alert('Failed to queue resend email.'),
  });

  if (isLoading) return <ManagerLayout><div style={{ padding: 40 }}>Loading contract details...</div></ManagerLayout>;
  if (isError || !data?.data) return <ManagerLayout><div style={{ padding: 40, color: 'var(--error)' }}>Error loading contract or not found.</div></ManagerLayout>;

  const c = data.data;

  return (
    <ManagerLayout>
      <div style={{ maxWidth: 960, margin: '0 auto' }}>
        <div className="flex items-center gap-2 body-sm text-charcoal" style={{ marginBottom: 20 }}>
          <Link to="/manager/contracts" className="text-primary" style={{ textDecoration: 'none' }}>Contracts</Link>
          <span>›</span>
          <span style={{ fontWeight: 600 }}>#{c.id.substring(0, 8)}...</span>
        </div>
        
        <div className="flex items-center justify-between" style={{ marginBottom: 24 }}>
          <h1 className="heading-md">Contract Details</h1>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
            <button
              className="btn-outline btn-sm"
              onClick={() => contractApi.downloadContractPdf(c.id, `Contract_${c.id.slice(0, 8)}.pdf`)}
            >
              Download PDF
            </button>
            <button 
              className="btn-primary btn-sm"
              onClick={() => {
                if (window.confirm(`Resend contract to ${c.customerEmail}?`)) {
                  resendMutation.mutate();
                }
              }}
              disabled={resendMutation.isPending}
            >
              {resendMutation.isPending ? 'Sending...' : 'Resend Email'}
            </button>
          </div>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, alignItems: 'start' }}>
          <div>
            <ContractInfoCard contract={c} />
            <CustomerInfoCard contract={c} />
            <RoomInfoCard contract={c} />
            <PaymentTermsCard contract={c} />
          </div>
          <div className="card" style={{ padding: 16, minHeight: 480, position: 'sticky', top: 16 }}>
            <p style={{ fontSize: 11, fontWeight: 700, letterSpacing: '0.06em', textTransform: 'uppercase', color: 'var(--charcoal)', marginBottom: 12 }}>
              PDF Preview
            </p>
            {pdfError && <p style={{ color: 'var(--error)', fontSize: 13 }}>{pdfError}</p>}
            {!pdfError && !blobUrl && <p className="body-sm text-charcoal">Loading PDF…</p>}
            {blobUrl && (
              <iframe
                title="Contract PDF"
                src={blobUrl}
                style={{ width: '100%', height: 560, border: '1px solid var(--hairline)', borderRadius: 8 }}
              />
            )}
          </div>
        </div>
      </div>
    </ManagerLayout>
  );
}
