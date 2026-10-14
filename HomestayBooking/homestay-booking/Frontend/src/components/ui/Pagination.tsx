interface PaginationProps {
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

function pageNumbers(current: number, total: number): (number | 'ellipsis')[] {
  if (total <= 7) {
    return Array.from({ length: total }, (_, i) => i);
  }
  const pages: (number | 'ellipsis')[] = [0];
  const start = Math.max(1, current - 1);
  const end = Math.min(total - 2, current + 1);

  if (start > 1) pages.push('ellipsis');
  for (let i = start; i <= end; i++) pages.push(i);
  if (end < total - 2) pages.push('ellipsis');
  if (total > 1) pages.push(total - 1);
  return pages;
}

export default function Pagination({ page, totalPages, onPageChange }: PaginationProps) {
  if (totalPages <= 1) return null;

  const pages = pageNumbers(page, totalPages);

  return (
    <nav
      className="flex items-center justify-center gap-2 flex-wrap"
      aria-label="Pagination"
      style={{ marginTop: 40 }}
    >
      <button
        type="button"
        className="btn-ghost btn-sm"
        disabled={page === 0}
        onClick={() => onPageChange(page - 1)}
        aria-label="Previous page"
      >
        ← Prev
      </button>

      {pages.map((p, idx) =>
        p === 'ellipsis' ? (
          <span key={`e-${idx}`} className="body-sm text-charcoal" style={{ padding: '0 4px' }}>
            …
          </span>
        ) : (
          <button
            key={p}
            type="button"
            className={p === page ? 'btn-primary btn-sm' : 'btn-ghost btn-sm'}
            style={{ minWidth: 36, borderRadius: 9999 }}
            onClick={() => onPageChange(p)}
            aria-current={p === page ? 'page' : undefined}
          >
            {p + 1}
          </button>
        )
      )}

      <button
        type="button"
        className="btn-ghost btn-sm"
        disabled={page >= totalPages - 1}
        onClick={() => onPageChange(page + 1)}
        aria-label="Next page"
      >
        Next →
      </button>
    </nav>
  );
}
