// SCR-38 — Contract Management (Manager)
import { useState, useEffect, useRef } from 'react';
import { Link, useSearchParams, useNavigate } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import ManagerLayout from '../../layouts/ManagerLayout';
import Alert from '../../components/ui/Alert';
import { Drawer } from '../../components/ui/Drawer';
import { DataTable, StatusBadge } from '../../components/ui';
import { contractApi, ContractSummaryResponse } from '../../api/contractApi';
import { managerApi } from '../../api/managerApi';
import type { AssignedProperty } from '../../api/reportApi';

function ContractPdfDrawer({
  contract,
  onClose,
}: {
  contract: ContractSummaryResponse;
  onClose: () => void;
}) {
  const queryClient = useQueryClient();
  const [blobUrl, setBlobUrl] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const blobRef = useRef<string | null>(null);

  const shortId = contract.id.slice(0, 8).toUpperCase();
  const canResend = contract.status === 'ACTIVE';

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    setBlobUrl(null);
    setFeedback(null);

    contractApi.getContractPdfBlob(contract.id)
      .then(blob => {
        if (cancelled) return;
        const url = URL.createObjectURL(new Blob([blob], { type: 'application/pdf' }));
        blobRef.current = url;
        setBlobUrl(url);
      })
      .catch(() => {
        if (!cancelled) setError('Unable to load PDF preview. Try downloading instead.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
      if (blobRef.current) {
        URL.revokeObjectURL(blobRef.current);
        blobRef.current = null;
      }
    };
  }, [contract.id]);

  const resendMutation = useMutation({
    mutationFn: () => contractApi.resendContractEmail(contract.id),
    onSuccess: (res) => {
      setFeedback({ type: 'success', message: res.message || `Resend queued for ${contract.customerEmail}` });
      queryClient.invalidateQueries({ queryKey: ['manager_contracts'] });
    },
    onError: (err: unknown) => {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Failed to resend contract email.';
      setFeedback({ type: 'error', message: msg });
    },
  });

  const handleDownload = async () => {
    try {
      await contractApi.downloadContractPdf(contract.id, `Contract_${shortId}.pdf`);
    } catch {
      setError('Download failed. Please try again.');
    }
  };

  return (
    <Drawer
      isOpen
      onClose={onClose}
      title={`Contract ${shortId}`}
      width="max-w-3xl lg:max-w-4xl max-w-full"
      footer={
        <div className="flex gap-3 justify-end flex-wrap">
          <button type="button" className="btn-ghost" onClick={onClose}>
            Close
          </button>
          <button type="button" className="btn-outline" onClick={handleDownload}>
            Download PDF
          </button>
          {canResend && (
            <button
              type="button"
              className="btn-primary"
              disabled={resendMutation.isPending}
              onClick={() => resendMutation.mutate()}
            >
              {resendMutation.isPending ? 'Sending...' : 'Send to Guest Email'}
            </button>
          )}
        </div>
      }
    >
      <div className="space-y-4 mb-4">
        <div className="grid grid-cols-2 gap-3 body-sm">
          <div>
            <p className="text-charcoal">Guest</p>
            <p style={{ fontWeight: 600 }}>{contract.customerName}</p>
            <p className="text-charcoal">{contract.customerEmail}</p>
          </div>
          <div>
            <p className="text-charcoal">Booking</p>
            <Link to={`/manager/bookings/${contract.bookingId}`} className="text-primary no-underline font-semibold">
              {contract.bookingId.slice(0, 8).toUpperCase()}
            </Link>
          </div>
          <div>
            <p className="text-charcoal">Room</p>
            <p style={{ fontWeight: 600 }}>{contract.roomNumber} · {contract.propertyName}</p>
          </div>
          <div>
            <p className="text-charcoal">Status</p>
            <StatusBadge
              status={contract.status}
              variant={contract.status === 'ACTIVE' ? 'success' : contract.status === 'CANCELLED' ? 'danger' : 'neutral'}
            />
          </div>
        </div>

        {feedback && (
          <Alert
            variant={feedback.type === 'success' ? 'success' : 'error'}
            message={feedback.message}
            closeable
            onClose={() => setFeedback(null)}
          />
        )}
      </div>

      {loading && (
        <div className="flex flex-col items-center justify-center py-16 text-charcoal body-md">
          <div className="w-10 h-10 border-2 border-[var(--primary)] border-t-transparent rounded-full animate-spin mb-4" />
          Loading PDF...
        </div>
      )}

      {error && !loading && (
        <div className="space-y-4">
          <Alert variant="error" message={error} />
          <button type="button" className="btn-outline" onClick={handleDownload}>
            Download PDF
          </button>
        </div>
      )}

      {blobUrl && !loading && !error && (
        <iframe
          src={blobUrl}
          title={`Contract ${shortId}`}
          className="w-full border-0 rounded-lg"
          style={{ minHeight: '65vh' }}
        />
      )}
    </Drawer>
  );
}

export default function ContractMgmtListPage() {
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [propertyId, setPropertyId] = useState('');
  const [page, setPage] = useState(0);
  const [selectedContract, setSelectedContract] = useState<ContractSummaryResponse | null>(null);
  const size = 10;
  const navigate = useNavigate();

  const TABS = ['ALL', 'ACTIVE', 'COMPLETED', 'CANCELLED'];

  const [searchParams] = useSearchParams();
  const bookingIdFromUrl = searchParams.get('bookingId');

  const { data: assignedProps } = useQuery({
    queryKey: ['manager_assigned_properties'],
    queryFn: async () => {
      const res = await managerApi.getMyAssignedProperties();
      return (res.data ?? []) as AssignedProperty[];
    },
  });

  useEffect(() => {
    const t = setTimeout(() => {
      setDebouncedSearch(search.trim());
      setPage(0);
    }, 400);
    return () => clearTimeout(t);
  }, [search]);

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['manager_contracts', page, size, statusFilter, debouncedSearch, propertyId],
    queryFn: () =>
      contractApi.getManagerContracts({
        page,
        size,
        status: statusFilter === 'ALL' ? undefined : statusFilter,
        search: debouncedSearch || undefined,
        sort: 'generatedAt,desc',
        propertyId: propertyId || undefined,
      }),
    enabled: !bookingIdFromUrl,
  });

  useEffect(() => {
    if (!bookingIdFromUrl) return;
    contractApi.getContractByBookingId(bookingIdFromUrl)
      .then(res => {
        if (res.success && res.data) {
          navigate(`/manager/contracts/${res.data.id}`, { replace: true });
        } else {
          navigate('/manager/contracts', { replace: true });
        }
      })
      .catch(() => navigate('/manager/contracts', { replace: true }));
  }, [bookingIdFromUrl, navigate]);

  const columns = [
    {
      header: 'Contract ID',
      accessor: (c: ContractSummaryResponse) => (
        <span className="code-sm" title={c.id}>{c.id.substring(0, 8).toUpperCase()}</span>
      ),
    },
    {
      header: 'Booking ID',
      accessor: (c: ContractSummaryResponse) => (
        <Link
          to={`/manager/bookings/${c.bookingId}`}
          className="text-primary"
          style={{ textDecoration: 'none', fontWeight: 600 }}
          title={c.bookingId}
          onClick={e => e.stopPropagation()}
        >
          {c.bookingId.substring(0, 8).toUpperCase()}
        </Link>
      ),
    },
    {
      header: 'Guest',
      accessor: (c: ContractSummaryResponse) => (
        <div>
          <p style={{ fontWeight: 600, fontSize: 13, margin: 0 }}>{c.customerName}</p>
          <p style={{ fontSize: 11, color: 'var(--ash)', margin: 0 }}>{c.customerEmail}</p>
        </div>
      ),
    },
    {
      header: 'Date',
      accessor: (c: ContractSummaryResponse) => (
        <span className="text-charcoal body-sm">
          {new Date(c.generatedAt).toLocaleString('vi-VN')}
        </span>
      ),
    },
    {
      header: 'Status',
      accessor: (c: ContractSummaryResponse) => (
        <StatusBadge
          status={c.status}
          variant={c.status === 'ACTIVE' ? 'success' : c.status === 'CANCELLED' ? 'danger' : 'neutral'}
        />
      ),
    },
  ];

  const actions = [
    { label: 'View PDF', onClick: (c: ContractSummaryResponse) => setSelectedContract(c) },
    { label: 'Details', onClick: (c: ContractSummaryResponse) => navigate(`/manager/contracts/${c.id}`) },
  ];

  return (
    <ManagerLayout>
      <div className="flex items-center justify-between" style={{ marginBottom: 24 }}>
        <div>
          <h1 className="heading-md">Contracts</h1>
          <p className="body-sm text-charcoal" style={{ marginTop: 4 }}>
            Property-scoped accommodation contracts. View PDF or resend to guest email.
          </p>
        </div>
      </div>

      <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', marginBottom: 24, alignItems: 'center' }}>
        <input
          className="input"
          style={{ maxWidth: 320, flex: 1 }}
          placeholder="Search by guest or booking ID..."
          value={search}
          onChange={e => setSearch(e.target.value)}
        />

        <select
          className="input"
          style={{ width: 220, cursor: 'pointer', fontWeight: 500 }}
          value={propertyId}
          onChange={e => { setPropertyId(e.target.value); setPage(0); }}
          aria-label="Filter by property"
        >
          <option value="">All assigned properties</option>
          {(assignedProps ?? []).map((p) => (
            <option key={p.id} value={p.id}>{p.name}</option>
          ))}
        </select>

        <div style={{ position: 'relative' }}>
          <select
            className="input"
            style={{ width: 200, appearance: 'none', paddingRight: 36, cursor: 'pointer', fontWeight: 500 }}
            value={statusFilter}
            onChange={e => { setStatusFilter(e.target.value); setPage(0); }}
          >
            {TABS.map(tab => (
              <option key={tab} value={tab}>{tab === 'ALL' ? 'All Statuses' : tab}</option>
            ))}
          </select>
          <svg
            style={{ position: 'absolute', right: 14, top: '50%', transform: 'translateY(-50%)', pointerEvents: 'none', color: 'var(--ash)' }}
            width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
            strokeLinecap="round" strokeLinejoin="round"
          >
            <polyline points="6 9 12 15 18 9" />
          </svg>
        </div>
      </div>

      <div style={{ marginBottom: 20 }}>
        {isLoading ? (
          <div style={{ textAlign: 'center', padding: 48 }}>Loading contracts...</div>
        ) : isError ? (
          <div className="card p-8 max-w-lg mx-auto space-y-4">
            <Alert variant="error" message="Error loading contracts. Please try again." />
            <button type="button" className="btn-primary" onClick={() => refetch()}>Retry</button>
          </div>
        ) : (data?.data?.content?.length ?? 0) === 0 ? (
          <div className="card text-center py-16 px-6">
            <h3 className="heading-sm mb-2">No contracts found</h3>
            <p className="body-md text-charcoal">
              Contracts appear here after a deposit is confirmed for bookings in your properties.
            </p>
          </div>
        ) : (
          <DataTable
            columns={columns}
            data={data?.data?.content || []}
            keyExtractor={(c) => c.id}
            actions={actions}
          />
        )}
      </div>

      {data?.data && data.data.totalPages > 1 && (
        <div style={{ display: 'flex', gap: 8, marginTop: 20, justifyContent: 'flex-end', alignItems: 'center' }}>
          <button
            className="btn-outline btn-sm"
            disabled={page === 0}
            onClick={() => setPage(p => p - 1)}
          >
            Prev
          </button>
          <span className="body-sm text-charcoal">
            Page {page + 1} of {data.data.totalPages}
          </span>
          <button
            className="btn-outline btn-sm"
            disabled={page >= data.data.totalPages - 1}
            onClick={() => setPage(p => p + 1)}
          >
            Next
          </button>
        </div>
      )}

      {selectedContract && (
        <ContractPdfDrawer
          contract={selectedContract}
          onClose={() => setSelectedContract(null)}
        />
      )}
    </ManagerLayout>
  );
}
