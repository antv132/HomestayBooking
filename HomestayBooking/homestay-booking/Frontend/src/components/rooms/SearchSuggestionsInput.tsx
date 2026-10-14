import { useEffect, useId, useRef, useState } from 'react';
import {
  fetchSearchSuggestions,
  type SearchSuggestion,
} from '../../api/publicApi';

const DEBOUNCE_MS = 300;
const MIN_QUERY_LEN = 2;

type Props = {
  value: string;
  onChange: (value: string) => void;
  onSelect?: (suggestion: SearchSuggestion) => void;
  onEnter?: () => void;
  placeholder?: string;
  ariaLabel?: string;
  className?: string;
  inputClassName?: string;
  style?: React.CSSProperties;
  id?: string;
};

/**
 * FR-004 — debounced location/property suggestions (≥2 chars).
 */
export default function SearchSuggestionsInput({
  value,
  onChange,
  onSelect,
  onEnter,
  placeholder = 'Đà Nẵng, Hội An...',
  ariaLabel = 'Địa điểm',
  className,
  inputClassName = 'input',
  style,
  id,
}: Props) {
  const listId = useId();
  const wrapRef = useRef<HTMLDivElement>(null);
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [items, setItems] = useState<SearchSuggestion[]>([]);
  const [emptyLabel, setEmptyLabel] = useState(false);

  useEffect(() => {
    const q = value.trim();
    if (q.length < MIN_QUERY_LEN) {
      setItems([]);
      setEmptyLabel(false);
      setLoading(false);
      return;
    }

    let cancelled = false;
    setLoading(true);
    const timer = window.setTimeout(() => {
      fetchSearchSuggestions(q)
        .then((data) => {
          if (cancelled) return;
          setItems(data);
          setEmptyLabel(data.length === 0);
          setOpen(true);
        })
        .catch(() => {
          if (cancelled) return;
          setItems([]);
          setEmptyLabel(true);
          setOpen(true);
        })
        .finally(() => {
          if (!cancelled) setLoading(false);
        });
    }, DEBOUNCE_MS);

    return () => {
      cancelled = true;
      window.clearTimeout(timer);
    };
  }, [value]);

  useEffect(() => {
    function onDocClick(e: MouseEvent) {
      if (!wrapRef.current?.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', onDocClick);
    return () => document.removeEventListener('mousedown', onDocClick);
  }, []);

  function handleSelect(item: SearchSuggestion) {
    onChange(item.label);
    setOpen(false);
    onSelect?.(item);
  }

  const showDropdown =
    open && value.trim().length >= MIN_QUERY_LEN && (loading || items.length > 0 || emptyLabel);

  return (
    <div ref={wrapRef} className={className} style={{ position: 'relative', ...style }}>
      <input
        id={id}
        className={inputClassName}
        placeholder={placeholder}
        value={value}
        autoComplete="off"
        aria-label={ariaLabel}
        aria-autocomplete="list"
        aria-controls={listId}
        aria-expanded={showDropdown}
        role="combobox"
        onChange={(e) => {
          onChange(e.target.value);
          setOpen(true);
        }}
        onFocus={() => {
          if (value.trim().length >= MIN_QUERY_LEN) setOpen(true);
        }}
        onKeyDown={(e) => {
          if (e.key === 'Enter') {
            setOpen(false);
            onEnter?.();
          } else if (e.key === 'Escape') {
            setOpen(false);
          }
        }}
      />
      {showDropdown && (
        <ul
          id={listId}
          role="listbox"
          className="search-suggestions-dropdown"
          style={{
            position: 'absolute',
            left: 0,
            right: 0,
            top: 'calc(100% + 4px)',
            zIndex: 40,
            margin: 0,
            padding: '6px 0',
            listStyle: 'none',
            background: 'var(--surface-card)',
            border: '1px solid var(--hairline)',
            borderRadius: 10,
            boxShadow: '0 8px 24px rgba(15, 23, 42, 0.12)',
            maxHeight: 240,
            overflowY: 'auto',
          }}
        >
          {loading && items.length === 0 ? (
            <li className="body-sm text-mute" style={{ padding: '10px 14px' }}>
              Đang tìm…
            </li>
          ) : emptyLabel && items.length === 0 ? (
            <li className="body-sm text-mute" style={{ padding: '10px 14px' }}>
              Không có gợi ý
            </li>
          ) : (
            items.map((item) => (
              <li key={`${item.type}-${item.label}`} role="option">
                <button
                  type="button"
                  onMouseDown={(e) => e.preventDefault()}
                  onClick={() => handleSelect(item)}
                  style={{
                    width: '100%',
                    textAlign: 'left',
                    padding: '10px 14px',
                    border: 'none',
                    background: 'transparent',
                    cursor: 'pointer',
                    display: 'flex',
                    gap: 8,
                    alignItems: 'center',
                  }}
                  className="body-sm text-ink search-suggestions-item"
                >
                  <span className="text-mute" style={{ fontSize: 11, minWidth: 56 }}>
                    {item.type === 'property' ? 'Homestay' : 'Địa điểm'}
                  </span>
                  <span style={{ fontWeight: 500 }}>{item.label}</span>
                </button>
              </li>
            ))
          )}
        </ul>
      )}
    </div>
  );
}
