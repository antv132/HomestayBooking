// ─── SCR-53: Customer List ────────────────────────────────────────────────────
import { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import ManagerLayout from '../../layouts/ManagerLayout';
import { Badge } from './_sharedAdminData';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { usersApi } from '../../api/usersApi';
import DataTable from '../../components/ui/DataTable';

export function CustomerListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const searchParam = searchParams.get('search') || '';
  const statusParam = searchParams.get('status') || 'ALL';
  const pageParam = parseInt(searchParams.get('page') || '0', 10);
  const size = 10;

  const [searchInput, setSearchInput] = useState(searchParam);
  const queryClient = useQueryClient();

  // Sync searchInput when URL search changes externally (e.g. browser back/forward)
  useEffect(() => {
    setSearchInput(searchParam);
  }, [searchParam]);

  // Debounced search effect
  useEffect(() => {
    const delayDebounceFn = setTimeout(() => {
      if (searchInput.trim() !== searchParam) {
        setSearchParams(prev => {
          const next = new URLSearchParams(prev);
          if (searchInput.trim()) {
            next.set('search', searchInput.trim());
          } else {
            next.delete('search');
          }
          next.set('page', '0'); // reset page to 0 on search
          return next;
        });
      }
    }, 400);

    return () => clearTimeout(delayDebounceFn);
  }, [searchInput, searchParam, setSearchParams]);

  const { data, isLoading, isError } = useQuery({
    queryKey: ['manager_customers', pageParam, size, statusParam, searchParam],
    queryFn: () => usersApi.getAllCustomers({
      page: pageParam,
      size,
      status: statusParam !== 'ALL' ? statusParam : undefined,
      search: searchParam.trim() || undefined
    })
  });

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: string, status: string }) => usersApi.updateCustomerStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['manager_customers'] });
    },
    onError: () => {
      alert("Cập nhật trạng thái thất bại");
    }
  });

  const handleStatusChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const newStatus = e.target.value;
    setSearchParams(prev => {
      const next = new URLSearchParams(prev);
      if (newStatus !== 'ALL') {
        next.set('status', newStatus);
      } else {
        next.delete('status');
      }
      next.set('page', '0'); // reset page to 0
      return next;
    });
  };

  const handlePageChange = (newPage: number) => {
    setSearchParams(prev => {
      const next = new URLSearchParams(prev);
      next.set('page', newPage.toString());
      return next;
    });
  };

  const list = data?.data?.content || [];

  const columns = [
    { header: 'Customer', accessor: (c: any) => (
      <div>
        <p style={{ fontWeight: 700, fontSize: 13, margin: 0 }}>{c.fullName}</p>
        <p style={{ fontSize: 11, color: 'var(--ash)', margin: 0 }}>{c.email}</p>
      </div>
    )},
    { header: 'Phone', accessor: (c: any) => <span className="text-charcoal">{c.phone || 'N/A'}</span> },
    { header: 'Bookings', accessor: (c: any) => <span className="badge badge-neutral">{c.bookingCount} bookings</span> },
    { header: 'Status', accessor: (c: any) => <Badge s={c.status} /> },
    { header: 'Joined', accessor: (c: any) => <span className="text-charcoal">{new Date(c.createdAt).toLocaleDateString('en-US')}</span> },
    { header: 'Actions', accessor: (c: any) => (
      <div style={{ display: 'flex', gap: 4 }}>
        <Link to={`/manager/customers/${c.id}`} className="btn-ghost btn-sm">View</Link>
        {c.status === 'ACTIVE' ? (
          <button className="btn-ghost btn-sm" style={{ color: 'var(--error)' }} onClick={() => statusMutation.mutate({ id: c.id, status: 'SUSPENDED' })} disabled={statusMutation.isPending}>Suspend</button>
        ) : (
          <button className="btn-ghost btn-sm" style={{ color: 'var(--success)' }} onClick={() => statusMutation.mutate({ id: c.id, status: 'ACTIVE' })} disabled={statusMutation.isPending}>Activate</button>
        )}
      </div>
    )}
  ];

  return (
    <ManagerLayout>
      <div className="flex items-center justify-between" style={{ marginBottom: 24 }}>
        <h1 className="heading-md">Customer Management</h1>
      </div>

      <div style={{ display: 'flex', gap: 12, marginBottom: 20, flexWrap: 'wrap' }}>
        <div style={{ minWidth: 260, flex: 1, maxWidth: 340 }}>
          <input
            className="input"
            placeholder="Search customers by name or email..."
            value={searchInput}
            onChange={e => setSearchInput(e.target.value)}
          />
        </div>
        <div style={{ width: 180 }}>
          <select
            className="select"
            value={statusParam}
            onChange={handleStatusChange}
            style={{ width: '100%', height: 42, padding: '0 12px', borderRadius: 8, border: '1.5px solid var(--hairline)', background: '#fff', fontSize: 14, fontWeight: 500 }}
          >
            <option value="ALL">Status: All</option>
            <option value="ACTIVE">Active</option>
            <option value="SUSPENDED">Suspended</option>
            <option value="INACTIVE">Inactive</option>
          </select>
        </div>
      </div>

      <div style={{ marginBottom: 20 }}>
        {isLoading ? (
          <div style={{ textAlign: 'center', padding: 48 }}>Loading...</div>
        ) : isError ? (
          <div style={{ textAlign: 'center', padding: 48, color: 'var(--error)' }}>Error loading data</div>
        ) : (
          <DataTable 
            columns={columns}
            data={list}
            keyExtractor={(c) => c.id}
          />
        )}
      </div>

      {data?.data && data.data.totalPages > 1 && (
        <div style={{ display: 'flex', gap: 8, marginTop: 20, justifyContent: 'flex-end', alignItems: 'center' }}>
          <button className="btn-outline btn-sm" disabled={pageParam === 0} onClick={() => handlePageChange(pageParam - 1)}>Prev</button>
          <span className="body-sm text-charcoal">Page {pageParam + 1} of {data.data.totalPages}</span>
          <button className="btn-outline btn-sm" disabled={pageParam >= data.data.totalPages - 1} onClick={() => handlePageChange(pageParam + 1)}>Next</button>
        </div>
      )}
    </ManagerLayout>
  );
}
