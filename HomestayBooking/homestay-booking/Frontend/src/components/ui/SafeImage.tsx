import { useEffect, useState, type ImgHTMLAttributes } from 'react';
import { FALLBACK_IMAGE, resolveMediaUrl } from '../../utils/mediaUrl';

type SafeImageProps = Omit<ImgHTMLAttributes<HTMLImageElement>, 'src'> & {
  src?: string | null;
};

/** Room/property image with fallback when URL is missing or fails to load. */
export default function SafeImage({ src, alt, onError, ...props }: SafeImageProps) {
  const resolved = resolveMediaUrl(src);
  const [url, setUrl] = useState(resolved);

  // Sync when parent changes src (e.g. gallery next/prev) — useState alone keeps the first value.
  useEffect(() => {
    setUrl(resolved);
  }, [resolved]);

  return (
    <img
      {...props}
      src={url}
      alt={alt ?? ''}
      onError={(e) => {
        if (url !== FALLBACK_IMAGE) {
          setUrl(FALLBACK_IMAGE);
        }
        onError?.(e);
      }}
    />
  );
}
