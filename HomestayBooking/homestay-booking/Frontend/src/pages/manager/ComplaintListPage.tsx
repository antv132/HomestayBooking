// ─── SCR-55: Complaint List ───────────────────────────────────────────────────
import { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import ManagerLayout from '../../layouts/ManagerLayout';
import { Badge } from './_sharedAdminData';
import { useQuery } from '@tanstack/react-query';
import { complaintsApi } from '../../api/complaintsApi';
import DataTable from '../../components/ui/DataTable';

export function ComplaintListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const searchParam = searchParams.get('search') || '';
  const statusParam = searchParams.get('status') || 'ALL';
  const pageParam = parseInt(searchParams.get('page') || '0', 10);
  const size = 10;

  const [searchInput, setSearchInput] = useState(searchParam);

  // Sync search input with URL search param
  useEffect(() => {
    setSearchInput(searchParam);
  }, [searchParam]);

  // Debounced search
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
          next.set('page', '0'); // reset page to 0 on search change
          return next;
        });
      }
    }, 400);

    return () => clearTimeout(delayDebounceFn);
  }, [searchInput, searchParam, setSearchParams]);

  const { data, isLoading, isError } = useQuery({
    queryKey: ['manager_complaints', pageParam, size, statusParam, searchParam],
    queryFn: () => complaintsApi.getComplaints({
      page: pageParam,
      size,
      status: statusParam !== 'ALL' ? statusParam : undefined,
      search: searchParam.trim() || undefined
    })
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
      next.set('page', '0'); // reset page to 0 on status change
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
    { header: 'Complaint ID', accessor: (c: any) => <span className="code-sm">#{c.id.substring(0, 8).toUpperCase()}</span> },
    { header: 'Customer', accessor: (c: any) => <span style={{ fontWeight: 600 }}>{c.customerName}</span> },
    { header: 'Subject', accessor: (c: any) => <span className="text-charcoal">{c.subject}</span> },
    { header: 'Status', accessor: (c: any) => <Badge s={c.status} /> },
    { header: 'Submitted', accessor: (c: any) => <span className="text-charcoal">{new Date(c.createdAt).toLocaleDateString('en-US')}</span> },
    { header: 'Actions', accessor: (c: any) => <Link to={`/manager/complaints/${c.id}`} className="btn-ghost btn-sm">View</Link> }
  ];

  return (
    <ManagerLayout>
      <div className="flex items-center justify-between" style={{ marginBottom: 24 }}>
        <h1 className="heading-md">Complaint Management</h1>
      </div>

      <div style={{ display: 'flex', gap: 12, marginBottom: 20, flexWrap: 'wrap' }}>
        <div style={{ minWidth: 260, flex: 1, maxWidth: 340 }}>
          <input
            className="input"
            placeholder="Search complaints by subject or customer name..."
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
            <option value="OPEN">Open</option>
            <option value="INVESTIGATING">Investigating</option>
            <option value="RESOLVED">Resolved</option>
            <option value="CLOSED">Closed</option>
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
