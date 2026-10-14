import { create } from 'zustand';
import { persist } from 'zustand/middleware';

// Giỏ phòng — lưu ở localStorage (cả khách chưa đăng nhập). Mỗi phòng vẫn đặt + cọc riêng qua /request-booking.
export interface CartItem {
  id: string;
  roomId: string;
  roomNumber: string;
  roomType: string;
  propertyName: string;
  imageUrl: string;
  pricePerNight: number;
  capacity: number;
  checkIn: string;   // yyyy-MM-dd
  checkOut: string;  // yyyy-MM-dd
  guests: number;
  addedAt: number;
}

export type NewCartItem = Omit<CartItem, 'id' | 'addedAt'>;

interface CartState {
  items: CartItem[];
  /** Thêm phòng; nếu cùng phòng và trùng khoảng ngày thì thay thế mục cũ. Trả về true nếu là thay thế. */
  addItem: (item: NewCartItem) => boolean;
  removeItem: (id: string) => void;
  /** Gọi sau khi tạo booking thành công để bỏ mục đã đặt khỏi giỏ. */
  removeBooked: (roomId: string, checkIn: string, checkOut: string) => void;
  clear: () => void;
}

function rangesOverlap(aIn: string, aOut: string, bIn: string, bOut: string) {
  return aIn < bOut && bIn < aOut;
}

export function cartItemNights(item: Pick<CartItem, 'checkIn' | 'checkOut'>): number {
  const diff = Math.round(
    (new Date(`${item.checkOut}T00:00:00`).getTime() - new Date(`${item.checkIn}T00:00:00`).getTime()) / 86400000,
  );
  return diff > 0 ? diff : 0;
}

export function cartItemTotal(item: CartItem): number {
  return cartItemNights(item) * item.pricePerNight;
}

export const useCartStore = create<CartState>()(
  persist(
    (set, get) => ({
      items: [],

      addItem: (item) => {
        const replaced = get().items.some(
          (i) => i.roomId === item.roomId && rangesOverlap(i.checkIn, i.checkOut, item.checkIn, item.checkOut),
        );
        const kept = get().items.filter(
          (i) => !(i.roomId === item.roomId && rangesOverlap(i.checkIn, i.checkOut, item.checkIn, item.checkOut)),
        );
        const id = `${item.roomId}_${item.checkIn}_${item.checkOut}`;
        set({ items: [...kept, { ...item, id, addedAt: Date.now() }] });
        return replaced;
      },

      removeItem: (id) => set({ items: get().items.filter((i) => i.id !== id) }),

      removeBooked: (roomId, checkIn, checkOut) =>
        set({
          items: get().items.filter(
            (i) => !(i.roomId === roomId && i.checkIn === checkIn && i.checkOut === checkOut),
          ),
        }),

      clear: () => set({ items: [] }),
    }),
    { name: 'homestay-cart' },
  ),
);
