// SCR-21 — Contract Detail (Customer Portal) — `/customer/contracts/:id`
// Layout: 2-col (Left: 4 section cards | Right: PDF preview panel)
import { useState, useEffect, useRef } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import CustomerLayout from '../../layouts/CustomerLayout';
import { contractApi } from '../../api/contractApi';
import api from '../../api/axiosInstance';
import type { ContractDetailResponse } from '../../api/contractApi';

// ── Status config ──────────────────────────────────────────────────────────────
const STATUS_CONFIG: Record<string, { cls: string; label: string; color: string }> = {
  ACTIVE:    { cls: 'badge-success', label: 'Đang hiệu lực', color: '#2b9a66' },
  COMPLETED: { cls: 'badge-purple',  label: 'Hoàn thành',    color: '#7C3AED' },
  CANCELLED: { cls: 'badge-error',   label: 'Đã huỷ',        color: '#DC2626' },
};

// ── Section Card wrapper ───────────────────────────────────────────────────────
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

// ── Card 1: Contract Info ──────────────────────────────────────────────────────
function ContractInfoCard({ contract }: { contract: ContractDetailResponse }) {
  const statusCfg = STATUS_CONFIG[contract.status] ?? { cls: 'badge-neutral', label: contract.status, color: '#6B7280' };
  return (
    <SectionCard title="Thông tin hợp đồng">
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: 16 }}>
        <div>
          <p style={{ fontSize: 22, fontWeight: 700, letterSpacing: '-0.3px', marginBottom: 4 }}>
            HỢP ĐỒNG THUÊ PHÒNG
          </p>
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            <span style={{ fontSize: 12, color: 'var(--charcoal)' }}>
              Mã HĐ: <span className="code-md">{contract.id.slice(0, 8).toUpperCase()}</span>
            </span>
            <span style={{ color: 'var(--hairline-strong)', fontSize: 12 }}>·</span>
            <span style={{ fontSize: 12, color: 'var(--charcoal)' }}>
              Mã ĐP: <span className="code-md">{contract.bookingId.slice(0, 8).toUpperCase()}</span>
            </span>
          </div>
        </div>
        <span className={`badge ${statusCfg.cls}`} style={{ flexShrink: 0 }}>{statusCfg.label}</span>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
        <div>
          <p style={{ fontSize: 11, color: 'var(--ash)', marginBottom: 3 }}>Ngày tạo hợp đồng</p>
          <p style={{ fontWeight: 600, fontSize: 13 }}>
            {new Date(contract.generatedAt).toLocaleString('vi-VN')}
          </p>
        </div>
        <div>
          <p style={{ fontSize: 11, color: 'var(--ash)', marginBottom: 3 }}>Gửi email lần cuối</p>
          <p style={{ fontWeight: 600, fontSize: 13 }}>
            {contract.sentAt
              ? new Date(contract.sentAt).toLocaleString('vi-VN')
              : <span style={{ color: 'var(--ash)', fontWeight: 400 }}>Chưa gửi</span>
            }
          </p>
        </div>
        {contract.pdfUrl && (
          <div style={{ gridColumn: '1 / -1' }}>
            <p style={{ fontSize: 11, color: 'var(--ash)', marginBottom: 3 }}>PDF</p>
            <p style={{ fontWeight: 600, fontSize: 13, wordBreak: 'break-all' }}>{contract.pdfUrl}</p>
          </div>
        )}
      </div>
    </SectionCard>
  );
}

// ── Card 2: Customer Info ──────────────────────────────────────────────────────
function CustomerInfoCard({ contract }: { contract: ContractDetailResponse }) {
  return (
    <SectionCard title="Thông tin khách thuê">
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

// ── Card 3: Room Info ──────────────────────────────────────────────────────────
function RoomInfoCard({ contract }: { contract: ContractDetailResponse }) {
  const nights = Math.max(1, Math.ceil(
    (new Date(contract.checkOutDate).getTime() - new Date(contract.checkInDate).getTime()) / 86_400_000
  ));
  const pricePerNight = Math.round(contract.totalAmount / nights);

  const rows = [
    { label: 'Phòng',          value: contract.roomNumber },
    { label: 'Cơ sở',          value: contract.propertyName },
    { label: 'Ngày nhận phòng', value: new Date(contract.checkInDate).toLocaleDateString('vi-VN') },
    { label: 'Ngày trả phòng', value: new Date(contract.checkOutDate).toLocaleDateString('vi-VN') },
    { label: 'Thời gian',      value: `${nights} đêm` },
    { label: 'Giá mỗi đêm',   value: `₫${pricePerNight.toLocaleString('vi-VN')}` },
  ];

  return (
    <SectionCard title="Thông tin lưu trú">
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

// ── Card 4: Payment Terms ──────────────────────────────────────────────────────
function PaymentTermsCard({ contract }: { contract: ContractDetailResponse }) {
  const remaining = contract.totalAmount - contract.depositAmount;
  return (
    <SectionCard title="Điều khoản thanh toán">
      {[
        { label: 'Đặt cọc (40%)',     amount: contract.depositAmount },
        { label: 'Thanh toán còn lại (60%)', amount: remaining },
      ].map(r => (
        <div
          key={r.label}
          style={{ display: 'flex', justifyContent: 'space-between', padding: '10px 0', borderBottom: '1px solid var(--hairline)' }}
        >
          <span style={{ fontSize: 14, color: 'var(--body)' }}>{r.label}</span>
          <span style={{ fontWeight: 600, fontSize: 14 }}>₫{r.amount.toLocaleString('vi-VN')}</span>
        </div>
      ))}
      <div style={{ display: 'flex', justifyContent: 'space-between', padding: '14px 0 0' }}>
        <span style={{ fontWeight: 700, fontSize: 16 }}>Tổng cộng</span>
        <span style={{ fontWeight: 800, fontSize: 20, color: 'var(--primary)' }}>
          ₫{contract.totalAmount.toLocaleString('vi-VN')}
        </span>
      </div>

      {/* Terms & Conditions */}
      <div style={{ marginTop: 20, padding: 16, background: 'var(--surface-bone)', borderRadius: 8 }}>
        <p style={{ fontWeight: 700, fontSize: 12, marginBottom: 8, color: 'var(--ink)' }}>
          Điều khoản & Điều kiện
        </p>
        {[
          '1. Tiền đặt cọc không được hoàn lại sau khi thanh toán nếu huỷ đặt phòng.',
          '2. Phần thanh toán còn lại phải được trả trước hoặc khi nhận phòng.',
          '3. Giờ nhận phòng: 14:00. Giờ trả phòng: 12:00.',
          '4. Khách hàng chịu trách nhiệm về mọi thiệt hại đối với cơ sở lưu trú.',
          '5. Hợp đồng này có giá trị pháp lý khi được cả hai bên ký kết.',
        ].map(t => (
          <p key={t} style={{ fontSize: 12, color: 'var(--charcoal)', lineHeight: 1.8 }}>{t}</p>
        ))}
      </div>

      {/* Signatures */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 48, paddingTop: 24, marginTop: 8, borderTop: '1px solid var(--hairline)' }}>
        <div style={{ textAlign: 'center' }}>
          <div style={{ height: 52, borderBottom: '1.5px solid var(--hairline)', marginBottom: 8 }} />
          <p style={{ fontSize: 12, color: 'var(--charcoal)' }}>Đại diện bên cho thuê</p>
        </div>
        <div style={{ textAlign: 'center' }}>
          <div style={{ height: 52, borderBottom: '1.5px solid var(--hairline)', marginBottom: 8 }} />
          <p style={{ fontSize: 12, color: 'var(--charcoal)' }}>Bên thuê ({contract.customerName})</p>
        </div>
      </div>
    </SectionCard>
  );
}

// ── PDF Preview Panel ──────────────────────────────────────────────────────────
function PdfPreviewPanel({
  contractId,
  onDownload,
}: {
  contractId: string;
  onDownload: () => void;
}) {
  const [blobUrl, setBlobUrl] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const blobRef = useRef<string | null>(null);

  useEffect(() => {
    setLoading(true);
    setError(false);
    setBlobUrl(null);

    api.get(`/api/v1/contracts/${contractId}/pdf`, { responseType: 'blob' })
      .then(res => {
        const url = URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }));
        blobRef.current = url;
        setBlobUrl(url);
      })
      .catch(() => setError(true))
      .finally(() => setLoading(false));

    return () => {
      if (blobRef.current) {
        URL.revokeObjectURL(blobRef.current);
        blobRef.current = null;
      }
    };
  }, [contractId]);

  return (
    <div style={{
      background: '#202020',
      borderRadius: 8,
      height: 600,
      position: 'sticky',
      top: 24,
      display: 'flex',
      flexDirection: 'column',
      overflow: 'hidden',
    }}>
      {/* Panel header */}
      <div style={{
        padding: '12px 16px',
        borderBottom: '1px solid rgba(255,255,255,0.1)',
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="rgba(252,252,252,0.7)" strokeWidth="2">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <polyline points="14,2 14,8 20,8"/>
          </svg>
          <span style={{ color: 'rgba(252,252,252,0.85)', fontSize: 12, fontWeight: 600 }}>
            PDF Preview
          </span>
        </div>
        {error && (
          <button
            onClick={onDownload}
            style={{
              fontSize: 11, color: 'rgba(252,252,252,0.6)', background: 'none',
              border: '1px solid rgba(255,255,255,0.2)', borderRadius: 4,
              padding: '3px 8px', cursor: 'pointer',
            }}
          >
            Tải xuống
          </button>
        )}
      </div>

      {/* Content area */}
      <div style={{ flex: 1, position: 'relative', overflow: 'hidden' }}>
        {/* Loading */}
        {loading && (
          <div style={{
            position: 'absolute', inset: 0,
            display: 'flex', flexDirection: 'column',
            alignItems: 'center', justifyContent: 'center', gap: 12,
          }}>
            <div style={{
              width: 32, height: 32, borderRadius: '50%',
              border: '2px solid rgba(255,255,255,0.15)',
              borderTopColor: 'rgba(255,255,255,0.6)',
              animation: 'spin 0.8s linear infinite',
            }} />
            <p style={{ fontSize: 12, color: 'rgba(252,252,252,0.5)' }}>Đang tải PDF...</p>
          </div>
        )}

        {/* Error fallback */}
        {error && !loading && (
          <div style={{
            position: 'absolute', inset: 0,
            display: 'flex', flexDirection: 'column',
            alignItems: 'center', justifyContent: 'center', gap: 12, padding: 24,
          }}>
            <div style={{ fontSize: 32 }}>📄</div>
            <p style={{ fontSize: 13, color: 'rgba(252,252,252,0.7)', textAlign: 'center' }}>
              Không thể hiển thị preview
            </p>
            <p style={{ fontSize: 11, color: 'rgba(252,252,252,0.4)', textAlign: 'center' }}>
              Nhấn "Tải PDF" ở trên để xem hợp đồng
            </p>
          </div>
        )}

        {/* iframe */}
        {blobUrl && !loading && (
          <iframe
            id="contract-pdf-iframe"
            src={blobUrl}
            title="Contract PDF Preview"
            style={{ width: '100%', height: '100%', border: 'none', display: 'block' }}
          />
        )}
      </div>
    </div>
  );
}

// ── Main Page ──────────────────────────────────────────────────────────────────
export default function ContractDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [downloading, setDownloading] = useState(false);

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['contract_detail', id],
    queryFn: () => contractApi.getMyContractDetail(id!),
    enabled: !!id,
  });

  const contract = data?.data;

  const handleDownload = async () => {
    if (!contract || downloading) return;
    setDownloading(true);
    try {
      await contractApi.downloadContractPdf(
        contract.id,
        `contract-${contract.id.slice(0, 8)}.pdf`,
      );
    } catch {
      alert('Không thể tải PDF. Vui lòng thử lại.');
    } finally {
      setDownloading(false);
    }
  };

  // ── Loading ────────────────────────────────────────────────────────────────
  if (isLoading) {
    return (
      <CustomerLayout>
        <div style={{ maxWidth: 1100, margin: '0 auto' }}>
          <div style={{ height: 16, width: 220, background: 'var(--surface-bone)', borderRadius: 8, marginBottom: 20 }} />
          <div style={{ height: 28, width: 260, background: 'var(--surface-bone)', borderRadius: 8, marginBottom: 32 }} />
          <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: 24 }}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              {[120, 90, 150, 180].map(h => (
                <div key={h} className="card" style={{ height: h }} />
              ))}
            </div>
            <div style={{ background: '#202020', borderRadius: 8, height: 600, opacity: 0.3 }} />
          </div>
        </div>
      </CustomerLayout>
    );
  }

  // ── Error ──────────────────────────────────────────────────────────────────
  if (isError || !contract) {
    return (
      <CustomerLayout>
        <div style={{ maxWidth: 560, margin: '80px auto', textAlign: 'center', padding: '0 24px' }}>
          <div style={{ fontSize: 48, marginBottom: 16 }}>⚠️</div>
          <h2 className="heading-md" style={{ marginBottom: 8, color: 'var(--error)' }}>
            Không tìm thấy hợp đồng
          </h2>
          <p className="body-md text-charcoal" style={{ marginBottom: 24 }}>
            Hợp đồng không tồn tại hoặc bạn không có quyền xem.
          </p>
          <div style={{ display: 'flex', gap: 10, justifyContent: 'center' }}>
            <button className="btn-outline" onClick={() => refetch()}>Thử lại</button>
            <button className="btn-ghost" onClick={() => navigate('/customer/contracts')}>
              ← Về danh sách
            </button>
          </div>
        </div>
      </CustomerLayout>
    );
  }

  const statusCfg = STATUS_CONFIG[contract.status] ?? { cls: 'badge-neutral', label: contract.status };

  // ── Main render ────────────────────────────────────────────────────────────
  return (
    <CustomerLayout>
      <div style={{ maxWidth: 1100, margin: '0 auto' }}>

        {/* ── Breadcrumb ── */}
        <div className="flex items-center gap-2 body-sm text-charcoal" style={{ marginBottom: 16 }}>
          <Link to="/customer/contracts" className="text-primary" style={{ textDecoration: 'none' }}>
            Hợp đồng của tôi
          </Link>
          <span>›</span>
          <span className="code-md">{contract.id.slice(0, 8).toUpperCase()}</span>
        </div>

        {/* ── Page header ── */}
        <div
          className="flex items-center justify-between"
          style={{ marginBottom: 24, flexWrap: 'wrap', gap: 12 }}
        >
          <div>
            <h1 className="heading-md" style={{ marginBottom: 4 }}>Hợp đồng thuê phòng</h1>
            <span className={`badge ${statusCfg.cls}`}>{statusCfg.label}</span>
          </div>

          <div style={{ display: 'flex', gap: 8 }}>
            <button
              id="contract-detail-download"
              className="btn-primary btn-sm"
              onClick={handleDownload}
              disabled={downloading}
              style={{ display: 'flex', alignItems: 'center', gap: 6 }}
            >
              {downloading ? 'Đang tải...' : (
                <>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                    <polyline points="7,10 12,15 17,10"/>
                    <line x1="12" y1="15" x2="12" y2="3"/>
                  </svg>
                  Tải PDF
                </>
              )}
            </button>
            <button
              id="contract-detail-print"
              className="btn-outline btn-sm"
              onClick={() => {
                const iframe = document.getElementById('contract-pdf-iframe') as HTMLIFrameElement | null;
                const win = iframe?.contentWindow;
                if (win) {
                  win.focus();
                  win.print();
                } else {
                  window.print();
                }
              }}
              style={{ display: 'flex', alignItems: 'center', gap: 6 }}
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <polyline points="6,9 6,2 18,2 18,9"/>
                <path d="M6,18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"/>
                <rect x="6" y="14" width="12" height="8"/>
              </svg>
              In
            </button>
          </div>
        </div>

        {/* ── 2-Column layout ── */}
        <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: 24, alignItems: 'start' }}>

          {/* ── LEFT: 4 Section Cards ── */}
          <div>
            <ContractInfoCard contract={contract} />
            <CustomerInfoCard contract={contract} />
            <RoomInfoCard contract={contract} />
            <PaymentTermsCard contract={contract} />

            {/* Back link */}
            <div style={{ marginTop: 8 }}>
              <Link to="/customer/contracts" className="btn-ghost btn-sm">
                ← Về danh sách hợp đồng
              </Link>
            </div>
          </div>

          {/* ── RIGHT: PDF Preview ── */}
          <div>
            <PdfPreviewPanel contractId={contract.id} onDownload={handleDownload} />
          </div>

        </div>
      </div>

      {/* Spin animation */}
      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
    </CustomerLayout>
  );
}
