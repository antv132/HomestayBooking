import { describe, expect, it } from 'vitest';
import { getNotificationActionPath } from './notificationDeepLink';

describe('notificationDeepLink utils', () => {
  describe('getNotificationActionPath', () => {
    it('returns null when relatedEntityId is null', () => {
      expect(getNotificationActionPath('BOOKING_CONFIRMED', null, 'CUSTOMER')).toBeNull();
    });

    it('returns null when relatedEntityId is undefined', () => {
      expect(getNotificationActionPath('BOOKING_CONFIRMED', undefined, 'CUSTOMER')).toBeNull();
    });

    it('returns null when type is SYSTEM', () => {
      expect(getNotificationActionPath('SYSTEM', '123', 'CUSTOMER')).toBeNull();
      expect(getNotificationActionPath('SYSTEM', '123', 'ADMIN')).toBeNull();
    });

    describe('CUSTOMER role', () => {
      it('returns booking path for BOOKING_CONFIRMED', () => {
        const result = getNotificationActionPath('BOOKING_CONFIRMED', '123', 'CUSTOMER');
        expect(result).toEqual({ to: '/customer/bookings/123', label: 'Xem booking' });
      });

      it('returns booking path for BOOKING_CANCELLED', () => {
        const result = getNotificationActionPath('BOOKING_CANCELLED', '456', 'CUSTOMER');
        expect(result).toEqual({ to: '/customer/bookings/456', label: 'Xem booking' });
      });

      it('returns contract path for CONTRACT_GENERATED', () => {
        const result = getNotificationActionPath('CONTRACT_GENERATED', '789', 'CUSTOMER');
        expect(result).toEqual({ to: '/customer/contracts/789', label: 'Xem hợp đồng' });
      });

      it('returns payments path for PAYMENT_CONFIRMED', () => {
        const result = getNotificationActionPath('PAYMENT_CONFIRMED', '123', 'CUSTOMER');
        expect(result).toEqual({ to: '/customer/payments', label: 'Xem thanh toán' });
      });

      it('returns maintenance path for MAINTENANCE_UPDATED', () => {
        const result = getNotificationActionPath('MAINTENANCE_UPDATED', '999', 'CUSTOMER');
        expect(result).toEqual({ to: '/customer/maintenance/999', label: 'Xem ticket bảo trì' });
      });

      it('returns null for unhandled notification types', () => {
        expect(getNotificationActionPath('UNKNOWN_TYPE', '123', 'CUSTOMER')).toBeNull();
        expect(getNotificationActionPath('PAYMENT_PENDING_VERIFICATION', '123', 'CUSTOMER')).toBeNull();
      });
    });

    describe('MANAGER role', () => {
      it('returns booking path for BOOKING_CONFIRMED', () => {
        const result = getNotificationActionPath('BOOKING_CONFIRMED', '123', 'MANAGER');
        expect(result).toEqual({ to: '/manager/bookings/123', label: 'Xem booking' });
      });

      it('returns booking path for BOOKING_CANCELLED', () => {
        const result = getNotificationActionPath('BOOKING_CANCELLED', '456', 'MANAGER');
        expect(result).toEqual({ to: '/manager/bookings/456', label: 'Xem booking' });
      });

      it('returns payments path for PAYMENT_PENDING_VERIFICATION', () => {
        const result = getNotificationActionPath('PAYMENT_PENDING_VERIFICATION', '789', 'MANAGER');
        expect(result).toEqual({ to: '/manager/payments', label: 'Xem thanh toán' });
      });

      it('returns payments path for PAYMENT_CONFIRMED', () => {
        const result = getNotificationActionPath('PAYMENT_CONFIRMED', '123', 'MANAGER');
        expect(result).toEqual({ to: '/manager/payments', label: 'Xem thanh toán' });
      });

      it('returns maintenance path for MAINTENANCE_UPDATED', () => {
        const result = getNotificationActionPath('MAINTENANCE_UPDATED', '999', 'MANAGER');
        expect(result).toEqual({ to: '/manager/maintenance/999', label: 'Xem ticket' });
      });

      it('returns room calendar path for AVAILABILITY_UPDATED', () => {
        const result = getNotificationActionPath('AVAILABILITY_UPDATED', 'room-123', 'MANAGER');
        expect(result).toEqual({ to: '/manager/rooms/room-123/calendar', label: 'Xem lịch phòng' });
      });

      it('returns null for unhandled notification types', () => {
        expect(getNotificationActionPath('UNKNOWN_TYPE', '123', 'MANAGER')).toBeNull();
        expect(getNotificationActionPath('CONTRACT_GENERATED', '123', 'MANAGER')).toBeNull();
      });
    });

    describe('EMPLOYEE role', () => {
      it('returns housekeeping path for HOUSEKEEPING_TASK_UPDATED', () => {
        const result = getNotificationActionPath('HOUSEKEEPING_TASK_UPDATED', '123', 'EMPLOYEE');
        expect(result).toEqual({ to: '/employee/housekeeping', label: 'Xem công việc' });
      });

      it('returns housekeeping path for MAINTENANCE_UPDATED', () => {
        const result = getNotificationActionPath('MAINTENANCE_UPDATED', '456', 'EMPLOYEE');
        expect(result).toEqual({ to: '/employee/housekeeping', label: 'Xem công việc' });
      });

      it('returns null for unhandled notification types', () => {
        expect(getNotificationActionPath('BOOKING_CONFIRMED', '123', 'EMPLOYEE')).toBeNull();
        expect(getNotificationActionPath('PAYMENT_CONFIRMED', '123', 'EMPLOYEE')).toBeNull();
      });
    });

    describe('ADMIN role', () => {
      it('returns null for SYSTEM type', () => {
        expect(getNotificationActionPath('SYSTEM', '123', 'ADMIN')).toBeNull();
      });

      it('returns null for other notification types', () => {
        expect(getNotificationActionPath('BOOKING_CONFIRMED', '123', 'ADMIN')).toBeNull();
        expect(getNotificationActionPath('PAYMENT_CONFIRMED', '123', 'ADMIN')).toBeNull();
      });
    });

    describe('role defaults and edge cases', () => {
      it('defaults to CUSTOMER when role is null', () => {
        const result = getNotificationActionPath('BOOKING_CONFIRMED', '123', null);
        expect(result).toEqual({ to: '/customer/bookings/123', label: 'Xem booking' });
      });

      it('handles unknown roles', () => {
        expect(getNotificationActionPath('BOOKING_CONFIRMED', '123', 'UNKNOWN_ROLE')).toBeNull();
      });

      it('handles empty string for relatedEntityId', () => {
        expect(getNotificationActionPath('BOOKING_CONFIRMED', '', 'CUSTOMER')).toBeNull();
      });
    });
  });
});
