import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import SafeImage from './SafeImage';
import { resolveMediaUrl } from '../../utils/mediaUrl';

interface ImageGallerySliderProps {
  images: { id?: string; imageUrl: string; sortOrder?: number }[];
  alt: string;
}

export default function ImageGallerySlider({ images, alt }: ImageGallerySliderProps) {
  const slides = useMemo(() => {
    const urls = images
      .slice()
      .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
      .map((img) => resolveMediaUrl(img.imageUrl));
    return urls.length > 0 ? urls : [resolveMediaUrl(null)];
  }, [images]);

  const slideKey = slides.join('|');
  const [index, setIndex] = useState(0);
  const touchStartX = useRef<number | null>(null);

  useEffect(() => {
    setIndex(0);
  }, [slideKey]);

  const go = useCallback(
    (delta: number) => {
      setIndex((i) => (i + delta + slides.length) % slides.length);
    },
    [slides.length],
  );

  const hasMultiple = slides.length > 1;

  function handleTouchStart(e: React.TouchEvent) {
    touchStartX.current = e.touches[0]?.clientX ?? null;
  }

  function handleTouchEnd(e: React.TouchEvent) {
    if (touchStartX.current == null || !hasMultiple) return;
    const delta = (e.changedTouches[0]?.clientX ?? touchStartX.current) - touchStartX.current;
    touchStartX.current = null;
    if (Math.abs(delta) < 40) return;
    if (delta > 0) go(-1);
    else go(1);
  }

  return (
    <div>
      <div
        style={{
          position: 'relative',
          borderRadius: 12,
          overflow: 'hidden',
          background: 'var(--surface-bone)',
          marginBottom: 10,
          touchAction: 'pan-y',
          userSelect: 'none',
        }}
        onTouchStart={handleTouchStart}
        onTouchEnd={handleTouchEnd}
      >
        <SafeImage
          key={slides[index]}
          src={slides[index]}
          alt={alt}
          style={{ width: '100%', height: 'clamp(260px, 50vw, 480px)', objectFit: 'cover', display: 'block' }}
        />

        {hasMultiple && (
          <>
            <button
              type="button"
              aria-label="Previous photo"
              onClick={(e) => {
                e.stopPropagation();
                go(-1);
              }}
              style={{
                position: 'absolute',
                left: 12,
                top: '50%',
                transform: 'translateY(-50%)',
                width: 40,
                height: 40,
                borderRadius: '50%',
                border: 'none',
                background: 'rgba(255,255,255,0.92)',
                cursor: 'pointer',
                boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
                fontSize: 20,
                lineHeight: 1,
                zIndex: 2,
              }}
            >
              ‹
            </button>
            <button
              type="button"
              aria-label="Next photo"
              onClick={(e) => {
                e.stopPropagation();
                go(1);
              }}
              style={{
                position: 'absolute',
                right: 12,
                top: '50%',
                transform: 'translateY(-50%)',
                width: 40,
                height: 40,
                borderRadius: '50%',
                border: 'none',
                background: 'rgba(255,255,255,0.92)',
                cursor: 'pointer',
                boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
                fontSize: 20,
                lineHeight: 1,
                zIndex: 2,
              }}
            >
              ›
            </button>
            <div
              style={{
                position: 'absolute',
                bottom: 12,
                right: 12,
                background: 'rgba(0,0,0,0.55)',
                color: '#fff',
                padding: '4px 10px',
                borderRadius: 9999,
                fontSize: 13,
                fontWeight: 600,
                zIndex: 2,
                pointerEvents: 'none',
              }}
            >
              {index + 1} / {slides.length}
            </div>
          </>
        )}
      </div>

      {hasMultiple && (
        <div
          style={{
            display: 'flex',
            gap: 8,
            overflowX: 'auto',
            paddingBottom: 4,
          }}
        >
          {slides.map((src, i) => (
            <button
              key={src + i}
              type="button"
              onClick={() => setIndex(i)}
              style={{
                flex: '0 0 88px',
                height: 64,
                borderRadius: 8,
                overflow: 'hidden',
                border: i === index ? '2px solid var(--primary)' : '2px solid transparent',
                padding: 0,
                cursor: 'pointer',
                opacity: i === index ? 1 : 0.75,
              }}
            >
              <SafeImage src={src} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
