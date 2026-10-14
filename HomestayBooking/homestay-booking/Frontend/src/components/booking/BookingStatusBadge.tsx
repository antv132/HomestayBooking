const STATUS_MAP: Record<string, { cls: string; label: string }> = {
  PENDING_DEPOSIT: { cls: 'badge-warning', label: 'Chờ cọc' },
  CONFIRMED: { cls: 'badge-success', label: 'Đã xác nhận' },
  CHECKED_IN: { cls: 'badge-info', label: 'Đang ở' },
  PENDING_INSPECTION: { cls: 'badge-warning', label: 'Chờ kiểm tra' },
  PENDING_DAMAGE_PAYMENT: { cls: 'badge-error', label: 'Chờ phí hư hại' },
  CHECKED_OUT: { cls: 'badge-purple', label: 'Đã trả phòng' },
  CANCELLED: { cls: 'badge-error', label: 'Đã hủy' },
  NO_SHOW: { cls: 'badge-neutral', label: 'No-show' },
};

export default function BookingStatusBadge({ status }: { status: string }) {
  const s = STATUS_MAP[status] ?? { cls: 'badge-neutral', label: status };
  return <span className={`badge ${s.cls}`}>{s.label}</span>;
}

export function formatBookingDates(checkIn: string, checkOut: string) {
  const fmt = (iso: string) =>
    new Date(iso + 'T00:00:00').toLocaleDateString('vi-VN', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
    });
  return `${fmt(checkIn)} → ${fmt(checkOut)}`;
}
