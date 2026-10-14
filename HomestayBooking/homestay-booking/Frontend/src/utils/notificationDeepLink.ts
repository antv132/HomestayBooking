import type { NotificationType } from '../api/notificationApi';

/** FR-15 US5 — role-aware deep link from notification metadata. */
export function getNotificationActionPath(
  type: NotificationType | string,
  relatedEntityId: string | null | undefined,
  role: string | null,
): { to: string; label: string } | null {
  if (!relatedEntityId || type === 'SYSTEM') {
    return null;
  }

  const r = role ?? 'CUSTOMER';

  if (r === 'CUSTOMER') {
    switch (type) {
      case 'BOOKING_CONFIRMED':
      case 'BOOKING_CANCELLED':
        return { to: `/customer/bookings/${relatedEntityId}`, label: 'Xem booking' };
      case 'CONTRACT_GENERATED':
        return { to: `/customer/contracts/${relatedEntityId}`, label: 'Xem hợp đồng' };
      case 'PAYMENT_CONFIRMED':
        return { to: '/customer/payments', label: 'Xem thanh toán' };
      case 'MAINTENANCE_UPDATED':
        return { to: `/customer/maintenance/${relatedEntityId}`, label: 'Xem ticket bảo trì' };
      default:
        return null;
    }
  }

  if (r === 'MANAGER') {
    switch (type) {
      case 'BOOKING_CONFIRMED':
      case 'BOOKING_CANCELLED':
        return { to: `/manager/bookings/${relatedEntityId}`, label: 'Xem booking' };
      case 'PAYMENT_PENDING_VERIFICATION':
      case 'PAYMENT_CONFIRMED':
        return { to: '/manager/payments', label: 'Xem thanh toán' };
      case 'MAINTENANCE_UPDATED':
        return { to: `/manager/maintenance/${relatedEntityId}`, label: 'Xem ticket' };
      case 'AVAILABILITY_UPDATED':
        return { to: `/manager/rooms/${relatedEntityId}/calendar`, label: 'Xem lịch phòng' };
      default:
        return null;
    }
  }

  if (r === 'EMPLOYEE') {
    if (type === 'HOUSEKEEPING_TASK_UPDATED' || type === 'MAINTENANCE_UPDATED') {
      return { to: '/employee/housekeeping', label: 'Xem công việc' };
    }
  }

  if (r === 'ADMIN' && type === 'SYSTEM') {
    return null;
  }

  return null;
}
