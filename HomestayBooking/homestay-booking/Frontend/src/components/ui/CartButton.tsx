import { Link } from 'react-router-dom';
import { useCartStore } from '../../store/cartStore';

/** Icon giỏ phòng + badge số lượng, link tới /cart. */
export default function CartButton({ onClick }: { onClick?: () => void }) {
  const count = useCartStore((s) => s.items.length);

  return (
    <Link
      to="/cart"
      onClick={onClick}
      aria-label={count > 0 ? `Giỏ phòng (${count})` : 'Giỏ phòng'}
      title="Giỏ phòng"
      style={{
        position: 'relative',
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        width: 40,
        height: 40,
        borderRadius: 9999,
        color: 'var(--charcoal)',
        textDecoration: 'none',
      }}
    >
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <circle cx="9" cy="21" r="1" />
        <circle cx="20" cy="21" r="1" />
        <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
      </svg>
      {count > 0 && (
        <span
          style={{
            position: 'absolute',
            top: 2,
            right: 0,
            minWidth: 18,
            height: 18,
            padding: '0 5px',
            borderRadius: 9999,
            background: '#FF5B00',
            color: '#fff',
            fontSize: 11,
            fontWeight: 700,
            lineHeight: '18px',
            textAlign: 'center',
          }}
        >
          {count > 99 ? '99+' : count}
        </span>
      )}
    </Link>
  );
}
