import { useState, useEffect, useCallback } from 'react';
import { Link, useParams, useNavigate, useLocation } from 'react-router-dom';
import ManagerLayout from '../../layouts/ManagerLayout';
import { StatusBadge } from '../../components/ui';
import type { StatusVariant } from '../../components/ui/StatusBadge';
import {
  fetchManagerBookingV1,
  modifyManagerBookingV1,
  type ManagerBookingDetail,
  type BookingPaymentInfo,
} from '../../api/bookingApi';
import {
  fetchManagerRoomV1,
  fetchManagerRoomsV1,
  type RoomListItem,
} from '../../api/roomsApi';

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

const PAYMENT_TYPE_VI: Record<string, string> = {
  DEPOSIT: 'Đặt cọc',
  REMAINING_BALANCE: 'Phần còn lại',
  DAMAGE_FEE: 'Phí thiệt hại',
  REFUND: 'Hoàn tiền',
};

const PAYMENT_STATUS_VI: Record<string, string> = {
  PENDING: 'Chờ xử lý',
  PAID: 'Đã thanh toán',
  FAILED: 'Thất bại',
  REFUNDED: 'Đã hoàn',
  PARTIALLY_REFUNDED: 'Hoàn một phần',
};

function formatVnd(value: number | undefined): string {
  if (value == null) return '—';
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

function countNights(checkIn: string, checkOut: string): number {
  const ms = new Date(checkOut).getTime() - new Date(checkIn).getTime();
  return Math.max(1, Math.ceil(ms / 86400000));
}

function Toast({ message, onClose }: { message: string; onClose: () => void }) {
  useEffect(() => {
    const t = setTimeout(onClose, 2000);
    return () => clearTimeout(t);
  }, [onClose]);

  return (
    <div
      style={{
        position: 'fixed',
        bottom: 32,
        right: 32,
        zIndex: 9999,
        padding: '14px 20px',
        background: '#202020',
        color: '#fcfcfc',
        borderRadius: 12,
        boxShadow: '0 8px 32px rgba(32,32,32,0.18)',
        borderLeft: '4px solid #2b9a66',
      }}
    >
      {message}
    </div>
  );
}

function DetailSkeleton() {
  return (
    <div className="space-y-4 animate-pulse">
      <div className="h-8 bg-[#F1F5F9] rounded w-1/3" />
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-4">
          <div className="h-40 bg-[#F1F5F9] rounded-xl" />
          <div className="h-48 bg-[#F1F5F9] rounded-xl" />
          <div className="h-56 bg-[#F1F5F9] rounded-xl" />
        </div>
        <div className="h-64 bg-[#F1F5F9] rounded-xl" />
      </div>
    </div>
  );
}

function PaymentTable({ payments }: { payments: BookingPaymentInfo[] }) {
  if (!payments.length) {
    return <p className="text-sm text-[#64748B]">Chưa có giao dịch thanh toán.</p>;
  }
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="text-left text-xs uppercase text-[#64748B] border-b border-[#E2E8F0]">
            <th className="py-2 pr-3">Loại</th>
            <th className="py-2 pr-3">Số tiền</th>
            <th className="py-2 pr-3">Phương thức</th>
            <th className="py-2">Trạng thái</th>
          </tr>
        </thead>
        <tbody>
          {payments.map(p => (
            <tr key={p.id} className="border-b border-[#F1F5F9] align-top">
              <td className="py-2.5 pr-3">{PAYMENT_TYPE_VI[p.type] ?? p.type}</td>
              <td className="py-2.5 pr-3 font-medium">{formatVnd(p.amount)}</td>
              <td className="py-2.5 pr-3">{p.method}</td>
              <td className="py-2.5">
                <span
                  className={
                    p.status === 'REFUNDED' || p.status === 'PARTIALLY_REFUNDED'
                      ? 'text-[#0369A1] font-medium'
                      : p.status === 'FAILED'
                        ? 'text-red-600 font-medium'
                        : ''
                  }
                >
                  {PAYMENT_STATUS_VI[p.status] ?? p.status}
                </span>
                {p.verificationNote && (
                  <p className="text-xs text-[#64748B] mt-1 max-w-[280px] whitespace-normal">
                    {p.verificationNote}
                  </p>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default function BookingMgmtDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const location = useLocation();

  const [booking, setBooking] = useState<ManagerBookingDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [toast, setToast] = useState('');
  const [showModify, setShowModify] = useState(false);
  const [modifyCheckIn, setModifyCheckIn] = useState('');
  const [modifyCheckOut, setModifyCheckOut] = useState('');
  const [modifyRoomId, setModifyRoomId] = useState('');
  const [modifyRooms, setModifyRooms] = useState<RoomListItem[]>([]);
  const [modifyRoomsLoading, setModifyRoomsLoading] = useState(false);
  const [modifySubmitting, setModifySubmitting] = useState(false);
  const [modifyError, setModifyError] = useState<string | null>(null);

  const loadBooking = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    setLoadError(null);
    try {
      const data = await fetchManagerBookingV1(id);
      setBooking(data);
    } catch (err: unknown) {
      const ax = err as { response?: { status?: number; data?: { message?: string } } };
      if (ax?.response?.status === 403) {
        setLoadError('Bạn không có quyền xem đặt phòng này.');
      } else {
        setLoadError(ax?.response?.data?.message ?? 'Không thể tải chi tiết đặt phòng.');
      }
      setBooking(null);
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => { loadBooking(); }, [loadBooking]);

  useEffect(() => {
    const state = location.state as { toast?: string } | null;
    if (state?.toast) {
      setToast(state.toast);
      loadBooking();
      navigate(location.pathname, { replace: true, state: {} });
    }
  }, [location, navigate, loadBooking]);

  async function openModifyForm() {
    if (!booking) return;
    setModifyCheckIn(booking.checkInDate?.slice(0, 10) ?? '');
    setModifyCheckOut(booking.checkOutDate?.slice(0, 10) ?? '');
    setModifyRoomId(booking.roomId ?? '');
    setModifyError(null);
    setShowModify(true);
    setModifyRoomsLoading(true);
    try {
      let propertyId: string | undefined;
      if (booking.roomId) {
        const currentRoom = await fetchManagerRoomV1(booking.roomId);
        propertyId = currentRoom.propertyId;
      }
      const page = await fetchManagerRoomsV1({
        propertyId,
        page: 0,
        size: 200,
      });
      setModifyRooms(page.content ?? []);
    } catch {
      setModifyRooms([]);
      setModifyError('Không tải được danh sách phòng. Vẫn có thể giữ phòng hiện tại.');
    } finally {
      setModifyRoomsLoading(false);
    }
  }

  function estimatePriceDelta(): number | null {
    if (!booking || !modifyCheckIn || !modifyCheckOut || modifyCheckOut <= modifyCheckIn) {
      return null;
    }
    const selected = modifyRooms.find(r => r.id === modifyRoomId);
    const pricePerNight = selected?.pricePerNight;
    if (pricePerNight == null) return null;
    const nights = countNights(modifyCheckIn, modifyCheckOut);
    const newTotal = nights * pricePerNight;
    return newTotal - (booking.totalAmount ?? 0);
  }

  async function submitModify() {
    if (!id || !booking) return;
    setModifySubmitting(true);
    setModifyError(null);
    try {
      const result = await modifyManagerBookingV1(id, {
        checkIn: modifyCheckIn || undefined,
        checkOut: modifyCheckOut || undefined,
        roomId: modifyRoomId && modifyRoomId !== booking.roomId ? modifyRoomId : undefined,
      });
      setBooking(result.booking);
      setShowModify(false);
      setToast(result.message || 'Đã cập nhật booking');
    } catch (err: unknown) {
      const ax = err as { response?: { data?: { message?: string } } };
      setModifyError(ax?.response?.data?.message ?? 'Không thể sửa booking.');
    } finally {
      setModifySubmitting(false);
    }
  }

  if (loadError) {
    return (
      <ManagerLayout>
        <div className="card p-10 text-center max-w-md mx-auto">
          <h2 className="heading-sm mb-2">Không tải được đặt phòng</h2>
          <p className="body-md text-charcoal mb-6">{loadError}</p>
          <div className="flex gap-3 justify-center">
            <button type="button" className="btn-primary" onClick={loadBooking}>Thử lại</button>
            <Link to="/manager/bookings" className="btn-ghost">Quay lại danh sách</Link>
          </div>
        </div>
      </ManagerLayout>
    );
  }

  const statusCfg = booking
    ? STATUS_VI[booking.status] ?? { label: booking.status, variant: 'neutral' as StatusVariant }
    : null;

  const nights = booking
    ? countNights(booking.checkInDate, booking.checkOutDate)
    : 0;

  const showCheckOutButton = booking && (
    booking.canCheckOut
    || (booking.status === 'PENDING_INSPECTION' && booking.checkOutBlockedReason)
    || booking.status === 'PENDING_DAMAGE_PAYMENT'
  );

  return (
    <ManagerLayout>
      {toast && <Toast message={toast} onClose={() => setToast('')} />}

      <div className="space-y-6">
        <div className="flex items-center gap-2 text-sm text-[#64748B]">
          <Link to="/manager/bookings" className="text-[#0F766E] no-underline">Đặt phòng</Link>
          <span>›</span>
          <span className="font-semibold text-[#1E293B]">
            {booking ? shortBookingId(booking.id) : '…'}
          </span>
        </div>

        {loading ? (
          <DetailSkeleton />
        ) : booking && (
          <>
            <div className="flex flex-wrap items-center justify-between gap-4">
              <h1 className="font-display text-[28px] font-bold text-[#1E293B]">
                {shortBookingId(booking.id)}
              </h1>
              {statusCfg && (
                <StatusBadge status={statusCfg.label} variant={statusCfg.variant} />
              )}
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
              <div className="lg:col-span-2 space-y-4">
                <div className="bg-white rounded-[16px] border border-[#E2E8F0] p-6">
                  <h2 className="font-semibold text-[#1E293B] mb-4">Thông tin khách</h2>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div>
                      <p className="text-xs text-[#64748B]">Họ tên</p>
                      <p className="font-semibold text-[#1E293B] mt-0.5">{booking.customerName}</p>
                    </div>
                    <div>
                      <p className="text-xs text-[#64748B]">Email</p>
                      <p className="font-semibold text-[#1E293B] mt-0.5">{booking.customerEmail}</p>
                    </div>
                    {booking.customerPhone && (
                      <div>
                        <p className="text-xs text-[#64748B]">Điện thoại</p>
                        <p className="font-semibold text-[#1E293B] mt-0.5">{booking.customerPhone}</p>
                      </div>
                    )}
                  </div>
                  <Link
                    to={`/manager/customers/${booking.customerId}`}
                    className="inline-block mt-4 text-sm text-[#0F766E] font-medium no-underline hover:underline"
                  >
                    Xem hồ sơ khách →
                  </Link>
                </div>

                <div className="bg-white rounded-[16px] border border-[#E2E8F0] p-6">
                  <h2 className="font-semibold text-[#1E293B] mb-4">Chi tiết đặt phòng</h2>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div>
                      <p className="text-xs text-[#64748B]">Phòng</p>
                      <p className="font-semibold text-[#1E293B] mt-0.5">{booking.roomNumber}</p>
                    </div>
                    <div>
                      <p className="text-xs text-[#64748B]">Loại phòng</p>
                      <p className="font-semibold text-[#1E293B] mt-0.5">{booking.roomType || '—'}</p>
                    </div>
                    <div>
                      <p className="text-xs text-[#64748B]">Homestay</p>
                      <p className="font-semibold text-[#1E293B] mt-0.5">{booking.propertyName}</p>
                    </div>
                    <div>
                      <p className="text-xs text-[#64748B]">Số khách</p>
                      <p className="font-semibold text-[#1E293B] mt-0.5">{booking.guestCount}</p>
                    </div>
                    <div>
                      <p className="text-xs text-[#64748B]">Nhận phòng</p>
                      <p className="font-semibold text-[#1E293B] mt-0.5">{formatDate(booking.checkInDate)}</p>
                    </div>
                    <div>
                      <p className="text-xs text-[#64748B]">Trả phòng</p>
                      <p className="font-semibold text-[#1E293B] mt-0.5">{formatDate(booking.checkOutDate)}</p>
                    </div>
                    <div>
                      <p className="text-xs text-[#64748B]">Số đêm</p>
                      <p className="font-semibold text-[#1E293B] mt-0.5">{nights} đêm</p>
                    </div>
                  </div>
                  {booking.specialRequests && (
                    <div className="mt-4 p-3 bg-[#F8FAFC] rounded-lg border border-[#E2E8F0]">
                      <p className="text-xs text-[#64748B]">Yêu cầu đặc biệt</p>
                      <p className="text-sm text-[#334155] mt-1">{booking.specialRequests}</p>
                    </div>
                  )}
                </div>

                <div className="bg-white rounded-[16px] border border-[#E2E8F0] p-6">
                  <h2 className="font-semibold text-[#1E293B] mb-4">Thanh toán</h2>
                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-6">
                    <div>
                      <p className="text-xs text-[#64748B]">Tổng tiền</p>
                      <p className="font-bold text-[#1E293B] mt-0.5">{formatVnd(booking.totalAmount)}</p>
                    </div>
                    <div>
                      <p className="text-xs text-[#64748B]">Cọc (40%)</p>
                      <p className="font-semibold text-[#1E293B] mt-0.5">{formatVnd(booking.depositAmount)}</p>
                    </div>
                    <div>
                      <p className="text-xs text-[#64748B]">Còn lại (60%)</p>
                      <p className="font-semibold text-[#1E293B] mt-0.5">{formatVnd(booking.remainingAmount)}</p>
                    </div>
                    {(booking.damageFeeAmount ?? 0) > 0 && (
                      <div>
                        <p className="text-xs text-[#64748B]">Phí thiệt hại</p>
                        <p className="font-semibold text-red-600 mt-0.5">{formatVnd(booking.damageFeeAmount)}</p>
                      </div>
                    )}
                  </div>
                  <PaymentTable payments={booking.payments ?? []} />
                  {booking.invoiceBreakdown?.invoices?.length ? (
                    <div className="mt-4 pt-4 border-t border-[#E2E8F0]">
                      <p className="text-xs font-semibold text-[#64748B] mb-2">Hóa đơn (FR-11)</p>
                      <ul className="space-y-2">
                        {booking.invoiceBreakdown.invoices.map((inv) => (
                          <li key={inv.id} className="flex justify-between text-sm">
                            <span>{inv.type === 'DEPOSIT' ? 'Cọc' : 'Còn lại'} · {inv.status}{inv.overdue ? ' · Quá hạn' : ''}</span>
                            <span className="font-semibold">{formatVnd(inv.amount)}</span>
                          </li>
                        ))}
                      </ul>
                      <p className="text-xs text-[#64748B] mt-2">
                        Đã trả {formatVnd(booking.invoiceBreakdown.paidAmount)} · Còn nợ {formatVnd(booking.invoiceBreakdown.remainingBalance)}
                      </p>
                    </div>
                  ) : null}
                </div>
              </div>

              <div className="bg-white rounded-[16px] border border-[#E2E8F0] p-6 shadow-sm">
                <h2 className="font-semibold text-[#1E293B] mb-4">Thao tác</h2>
                <div className="flex flex-col gap-3">
                  {(booking.status === 'CONFIRMED' || booking.status === 'PENDING_DEPOSIT') && (
                    <>
                      <button
                        type="button"
                        className="btn-primary w-full disabled:opacity-50"
                        disabled={!booking.canCheckIn}
                        title={booking.checkInBlockedReason ?? undefined}
                        onClick={() => booking.canCheckIn && navigate(`/manager/bookings/${id}/check-in`)}
                      >
                        Nhận phòng
                      </button>
                      {booking.checkInBlockedReason && (
                        <p className="text-xs text-[#B45309] -mt-1">{booking.checkInBlockedReason}</p>
                      )}
                    </>
                  )}

                  {showCheckOutButton && (
                    <button
                      type="button"
                      className="btn-primary w-full disabled:opacity-50"
                      onClick={() => booking.canCheckOut && navigate(`/manager/bookings/${id}/check-out`)}
                      disabled={!booking.canCheckOut}
                      title={booking.checkOutBlockedReason ?? undefined}
                    >
                      Trả phòng
                    </button>
                  )}

                  {booking.checkOutBlockedReason && !booking.canCheckOut && (
                    <p className="text-xs text-[#B45309]">{booking.checkOutBlockedReason}</p>
                  )}

                  {booking.canCancel && (
                    <button
                      type="button"
                      className="btn-danger w-full"
                      onClick={() => navigate(`/manager/bookings/${id}/cancel`)}
                    >
                      Hủy đặt phòng
                    </button>
                  )}

                  {booking.canModify && (
                    <button
                      type="button"
                      className="btn-outline w-full"
                      onClick={openModifyForm}
                    >
                      Sửa ngày / phòng
                    </button>
                  )}

                  {showModify && booking.canModify && (
                    <div className="p-3 rounded-lg border border-[#E2E8F0] bg-[#F8FAFC] space-y-3">
                      <p className="text-xs font-semibold text-[#64748B] uppercase">Sửa booking (CONFIRMED)</p>
                      <label className="block text-xs text-[#64748B]">
                        Check-in
                        <input
                          type="date"
                          className="input mt-1 w-full"
                          value={modifyCheckIn}
                          onChange={(e) => setModifyCheckIn(e.target.value)}
                        />
                      </label>
                      <label className="block text-xs text-[#64748B]">
                        Check-out
                        <input
                          type="date"
                          className="input mt-1 w-full"
                          value={modifyCheckOut}
                          onChange={(e) => setModifyCheckOut(e.target.value)}
                          min={modifyCheckIn || undefined}
                        />
                      </label>
                      <label className="block text-xs text-[#64748B]">
                        Phòng (cùng property)
                        <select
                          className="input mt-1 w-full"
                          value={modifyRoomId}
                          disabled={modifyRoomsLoading}
                          onChange={(e) => setModifyRoomId(e.target.value)}
                        >
                          {modifyRoomsLoading && (
                            <option value={modifyRoomId}>Đang tải phòng…</option>
                          )}
                          {!modifyRoomsLoading && modifyRooms.length === 0 && booking.roomId && (
                            <option value={booking.roomId}>
                              {booking.roomNumber} — {booking.roomType || 'Phòng hiện tại'}
                            </option>
                          )}
                          {modifyRooms.map(r => (
                            <option key={r.id} value={r.id}>
                              {r.roomNumber} — {r.roomType} ({formatVnd(r.pricePerNight)}/đêm)
                              {r.id === booking.roomId ? ' · hiện tại' : ''}
                            </option>
                          ))}
                        </select>
                      </label>
                      {(() => {
                        const delta = estimatePriceDelta();
                        if (delta == null) {
                          return (
                            <p className="text-xs text-[#64748B]">
                              Chọn ngày hợp lệ để xem chênh lệch giá trước khi lưu.
                            </p>
                          );
                        }
                        const nights = countNights(modifyCheckIn, modifyCheckOut);
                        return (
                          <div
                            className="rounded-lg border px-3 py-2 text-sm"
                            style={{
                              borderColor: delta === 0 ? '#E2E8F0' : delta > 0 ? '#FDE68A' : '#BBF7D0',
                              background: delta === 0 ? '#F8FAFC' : delta > 0 ? '#FFFBEB' : '#F0FDF4',
                            }}
                          >
                            <p className="text-xs text-[#64748B] mb-0.5">
                              Dự kiến ({nights} đêm) — chênh lệch trước khi lưu
                            </p>
                            <p className="font-semibold text-[#1E293B]">
                              {delta === 0
                                ? 'Tổng tiền không đổi'
                                : delta > 0
                                  ? `+${formatVnd(delta)} (khách cần thanh toán thêm)`
                                  : `−${formatVnd(Math.abs(delta))} (giảm trên phần còn lại)`}
                            </p>
                          </div>
                        );
                      })()}
                      {modifyError && (
                        <p className="text-xs text-red-600">{modifyError}</p>
                      )}
                      <div className="flex gap-2">
                        <button
                          type="button"
                          className="btn-primary flex-1 disabled:opacity-50"
                          disabled={modifySubmitting || modifyRoomsLoading}
                          onClick={submitModify}
                        >
                          {modifySubmitting ? 'Đang lưu…' : 'Lưu thay đổi'}
                        </button>
                        <button
                          type="button"
                          className="btn-ghost flex-1"
                          disabled={modifySubmitting}
                          onClick={() => setShowModify(false)}
                        >
                          Hủy
                        </button>
                      </div>
                    </div>
                  )}

                  <button
                    type="button"
                    className="btn-outline w-full opacity-50 cursor-not-allowed"
                    disabled
                    title="Sắp có"
                  >
                    Gửi lại xác nhận
                  </button>

                  <Link
                    to={`/manager/payments?bookingId=${booking.id}`}
                    className="btn-outline w-full text-center no-underline"
                  >
                    Xem thanh toán
                  </Link>
                  <Link
                    to={`/manager/contracts?bookingId=${booking.id}`}
                    className="btn-outline w-full text-center no-underline"
                  >
                    Xem hợp đồng
                  </Link>
                </div>
              </div>
            </div>
          </>
        )}
      </div>
    </ManagerLayout>
  );
}
