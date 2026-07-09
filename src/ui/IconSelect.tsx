import { useEffect, useRef, useState } from 'react';
import { wikiImageUrl } from '../data/repository';

interface Props<T> {
  items: T[];
  value: T;
  label: (item: T) => string;
  image: (item: T) => string | null | undefined;
  onSelect: (item: T) => void;
}

/** A select-like dropdown that shows a wiki icon beside each option. */
export const IconSelect = <T,>({ items, value, label, image, onSelect }: Props<T>) => {
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const onDocMouseDown = (e: MouseEvent) => {
      if (rootRef.current && !rootRef.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', onDocMouseDown);
    return () => document.removeEventListener('mousedown', onDocMouseDown);
  }, []);

  const valueImage = wikiImageUrl(image(value));

  return (
    <div className="icon-select grow" ref={rootRef}>
      <button className="icon-select-button" onClick={() => setOpen((o) => !o)}>
        {valueImage
          ? <img src={valueImage} alt="" />
          : <span style={{ width: 18 }} />}
        <span className="grow" style={{ textAlign: 'left' }}>{label(value)}</span>
        <span aria-hidden>▾</span>
      </button>
      {open && (
        <div className="dropdown-popup">
          {items.map((item, i) => {
            const img = wikiImageUrl(image(item));
            return (
              <div
                key={i}
                className="option"
                onClick={() => {
                  onSelect(item);
                  setOpen(false);
                }}
              >
                {img ? <img src={img} alt="" /> : <span style={{ width: 22 }} />}
                <span>{label(item)}</span>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
