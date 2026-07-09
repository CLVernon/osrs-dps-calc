import {
  type ReactNode, useEffect, useMemo, useRef, useState,
} from 'react';
import { wikiImageUrl } from '../data/repository';
import { Tooltip } from './Tooltip';

interface Props<T> {
  items: T[];
  label: (item: T) => string;
  image?: (item: T) => string | null | undefined;
  tooltip?: (item: T) => ReactNode;
  onSelect: (item: T) => void;
  /** Currently selected item (display mode); omit for add-mode */
  value?: T | null;
  placeholder?: string;
  /** Clear the field after selecting (add-to-list usage) */
  clearOnSelect?: boolean;
  disabled?: boolean;
}

export const SearchableDropdown = <T,>({
  items, label, image, tooltip, onSelect, value = null, placeholder = 'Type to search...',
  clearOnSelect = false, disabled = false,
}: Props<T>) => {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState('');
  const [highlighted, setHighlighted] = useState(0);
  const rootRef = useRef<HTMLDivElement>(null);
  const listRef = useRef<HTMLDivElement>(null);

  const filtered = useMemo(() => {
    const needle = query.trim().toLowerCase();
    if (!needle) return items.slice(0, 250);
    return items.filter((i) => label(i).toLowerCase().includes(needle)).slice(0, 250);
  }, [items, query, label]);

  useEffect(() => {
    const onDocClick = (e: MouseEvent) => {
      if (rootRef.current && !rootRef.current.contains(e.target as Node)) {
        setOpen(false);
        setQuery('');
      }
    };
    document.addEventListener('mousedown', onDocClick);
    return () => document.removeEventListener('mousedown', onDocClick);
  }, []);

  const choose = (item: T) => {
    onSelect(item);
    setOpen(false);
    setQuery('');
  };

  const displayText = open ? query : (value ? label(value) : '');
  const valueImage = value && image ? wikiImageUrl(image(value)) : null;

  const onKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      if (!open) setOpen(true);
      else setHighlighted((h) => Math.min(h + 1, filtered.length - 1));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setHighlighted((h) => Math.max(h - 1, 0));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      if (open && filtered[highlighted]) choose(filtered[highlighted]);
    } else if (e.key === 'Escape') {
      setOpen(false);
      setQuery('');
    }
  };

  useEffect(() => {
    const el = listRef.current?.children[highlighted] as HTMLElement | undefined;
    el?.scrollIntoView({ block: 'nearest' });
  }, [highlighted]);

  return (
    <div className="dropdown grow" ref={rootRef}>
      {!clearOnSelect && valueImage && <img src={valueImage} alt="" />}
      <input
        type="text"
        value={displayText}
        placeholder={value && !clearOnSelect ? label(value) : placeholder}
        disabled={disabled}
        onFocus={() => {
          setOpen(true);
          setHighlighted(0);
        }}
        onChange={(e) => {
          setQuery(e.target.value);
          setOpen(true);
          setHighlighted(0);
        }}
        onKeyDown={onKeyDown}
      />
      {open && filtered.length > 0 && (
        <div className="dropdown-popup" ref={listRef}>
          {filtered.map((item, i) => {
            const row = (
              <div
                key={label(item) + i}
                className={`option${i === highlighted ? ' highlighted' : ''}`}
                onMouseEnter={() => setHighlighted(i)}
                onClick={() => choose(item)}
              >
                {image && wikiImageUrl(image(item))
                  ? <img src={wikiImageUrl(image(item))!} alt="" loading="lazy" />
                  : <span style={{ width: 22 }} />}
                <span>{label(item)}</span>
              </div>
            );
            return tooltip
              ? <Tooltip key={label(item) + i} content={tooltip(item)}>{row}</Tooltip>
              : row;
          })}
        </div>
      )}
    </div>
  );
};
