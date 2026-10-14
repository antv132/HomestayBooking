// ─── SCR-54: Customer Detail ──────────────────────────────────────────────────
import { Link, useParams } from 'react-router-dom';
import ManagerLayout from '../../layouts/ManagerLayout';
import { Badge } from './_sharedAdminData';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { usersApi } from '../../api/usersApi';
import BookingStatusBadge from '../../components/booking/BookingStatusBadge';
import DataTable from '../../components/ui/DataTable';

export function CustomerDetailPage() {
  const { id } = useParams();
  const queryClient = useQueryClient();

  const { data, isLoading, isError } = useQuery({
    queryKey: ['manager_customer', id],
    queryFn: () => usersApi.getCustomerDetail(id!),
    enabled: !!id
  });

  const statusMutation = useMutation({
    mutationFn: ({ status }: { status: string }) => usersApi.updateCustomerStatus(id!, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['manager_customer', id] });
      queryClient.invalidateQueries({ queryKey: ['manager_customers'] });
    },
    onError: () => {
      alert("Cập nhật trạng thái thất bại");
    }
  });

  if (isLoading) return <ManagerLayout><div style={{ padding: 40 }}>Loading...</div></ManagerLayout>;
  if (isError || !data?.data) return <ManagerLayout><div style={{ padding: 40, color: 'var(--error)' }}>Error or customer not found</div></ManagerLayout>;

  const c = data.data;
  const recentBookings = c.recentBookings || [];

  const formatBookingId = (uuid: string): string => {
    if (!uuid) return '';
    return uuid.substring(0, 8).toUpperCase();
  };

  const formatVnd = (amount: number) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
  };

  const columns = [
    { header: 'Booking ID', accessor: (b: any) => <span className="code-sm">#{formatBookingId(b.id)}</span> },
    { header: 'Property / Room', accessor: (b: any) => (
      <div>
        <p style={{ fontWeight: 600, fontSize: 13, margin: 0 }}>{b.roomNumber}</p>
        <p style={{ fontSize: 11, color: 'var(--ash)', margin: 0 }}>{b.propertyName}</p>
      </div>
    )},
    { header: 'Dates', accessor: (b: any) => <span className="text-charcoal" style={{ fontSize: 13 }}>{b.checkInDate} → {b.checkOutDate}</span> },
    { header: 'Total Amount', accessor: (b: any) => <span style={{ fontWeight: 600 }}>{formatVnd(b.totalAmount)}</span> },
    { header: 'Status', accessor: (b: any) => <BookingStatusBadge status={b.status} /> },
    { header: 'Action', accessor: (b: any) => <Link to={`/manager/bookings/${b.id}`} className="btn-ghost btn-sm">Details</Link> }
  ];

  return (
    <ManagerLayout>
      <div className="flex items-center gap-2 body-sm text-charcoal" style={{ marginBottom: 20 }}>
        <Link to="/manager/customers" className="text-primary" style={{ textDecoration: 'none' }}>Customers</Link>
        <span>›</span>
        <span style={{ fontWeight: 600 }}>{c.fullName}</span>
      </div>

      <div className="flex items-start justify-between" style={{ marginBottom: 24 }}>
        <h1 className="heading-md">{c.fullName}</h1>
        <div style={{ display: 'flex', gap: 8 }}>
          <Badge s={c.status} />
          {c.status === 'ACTIVE' ? (
            <button className="btn-danger btn-sm" onClick={() => statusMutation.mutate({ status: 'SUSPENDED' })} disabled={statusMutation.isPending}>Suspend Account</button>
          ) : (
            <button className="btn-outline btn-sm" style={{ color: 'var(--success)', borderColor: 'var(--success)' }} onClick={() => statusMutation.mutate({ status: 'ACTIVE' })} disabled={statusMutation.isPending}>Activate</button>
          )}
        </div>
      </div>

      {/* Grid: Profile and KPI info */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 280px', gap: 20, marginBottom: 24 }}>
        <div className="card-lg" style={{ padding: 28 }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20 }}>
            {[
              { l: 'Full Name', v: c.fullName },
              { l: 'Email', v: c.email },
              { l: 'Phone', v: c.phone || 'N/A' },
              { l: 'Joined Date', v: new Date(c.createdAt).toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' }) },
            ].map(row => (
              <div key={row.l}>
                <p className="form-label" style={{ margin: 0, color: 'var(--ash)', fontSize: 12 }}>{row.l}</p>
                <p style={{ fontSize: 15, fontWeight: 600, marginTop: 4, color: 'var(--ink)' }}>{row.v}</p>
              </div>
            ))}
          </div>
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          <div className="kpi-card" style={{ flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center', padding: 20 }}>
            <span style={{ fontSize: 32, marginBottom: 8 }}>📋</span>
            <div className="kpi-value" style={{ fontSize: 28, fontWeight: 800 }}>{c.bookingCount}</div>
            <div className="kpi-label" style={{ color: 'var(--ash)', fontSize: 13, marginTop: 4 }}>Total Bookings</div>
          </div>
        </div>
      </div>

      {/* Booking History section */}
      <div className="card-lg" style={{ padding: 28 }}>
        <div className="flex items-center justify-between" style={{ marginBottom: 20 }}>
          <h2 className="heading-sm" style={{ margin: 0 }}>Booking History (Recent {recentBookings.length})</h2>
          {c.bookingCount > 0 && (
            <Link to={`/manager/bookings?search=${encodeURIComponent(c.fullName)}`} className="btn-outline btn-sm" style={{ textDecoration: 'none' }}>
              View All Bookings
            </Link>
          )}
        </div>

        {recentBookings.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '40px 0', color: 'var(--ash)' }}>
            <span style={{ fontSize: 36, display: 'block', marginBottom: 12 }}>📭</span>
            <p className="body-md">This customer has not made any bookings yet.</p>
          </div>
        ) : (
          <DataTable 
            columns={columns}
            data={recentBookings}
            keyExtractor={(b) => b.id}
          />
        )}
      </div>
    </ManagerLayout>
  );
}
