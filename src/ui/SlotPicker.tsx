import { useEffect, useMemo, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { type EquipmentItem, displayName } from '../model/types';
import { wikiImageUrl } from '../data/repository';
import { Tooltip } from './Tooltip';
import { equipmentTooltip } from './statTooltips';

interface Props {
  items: EquipmentItem[];
  anchor: DOMRect;
  onSelect: (item: EquipmentItem) => void;
  onClear: () => void;
  onClose: () => void;
}

/** A search popover anchored beneath the clicked equipment slot. */
export const SlotPicker = ({ items, anchor, onSelect, onClear, onClose }: Props) => {
  const [query, setQuery] = useState('');
  const [highlighted, setHighlighted] = useState(0);
  const rootRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const listRef = useRef<HTMLDivElement>(null);

  const filtered = useMemo(() => {
    const needle = query.trim().toLowerCase();
    if (!needle) return items.slice(0, 250);
    return items.filter((i) => displayName(i).toLowerCase().includes(needle)).slice(0, 250);
  }, [items, query]);

  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  useEffect(() => {
    const onDocMouseDown = (e: MouseEvent) => {
      if (rootRef.current && !rootRef.current.contains(e.target as Node)) onClose();
    };
    document.addEventListener('mousedown', onDocMouseDown);
    return () => document.removeEventListener('mousedown', onDocMouseDown);
  }, [onClose]);

  useEffect(() => {
    const el = listRef.current?.children[highlighted] as HTMLElement | undefined;
    el?.scrollIntoView({ block: 'nearest' });
  }, [highlighted]);

  const onKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setHighlighted((h) => Math.min(h + 1, filtered.length - 1));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setHighlighted((h) => Math.max(h - 1, 0));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      if (filtered[highlighted]) onSelect(filtered[highlighted]);
    } else if (e.key === 'Escape') {
      onClose();
    }
  };

  // Anchor to the slot: open below when there is room, otherwise above
  const width = 360;
  const margin = 8;
  const left = Math.max(margin, Math.min(anchor.left, window.innerWidth - width - margin));
  const spaceBelow = window.innerHeight - anchor.bottom;
  const openBelow = spaceBelow >= 240;
  const style: React.CSSProperties = openBelow
    ? { left, top: anchor.bottom + 4, width, maxHeight: spaceBelow - margin }
    : { left, bottom: window.innerHeight - anchor.top + 4, width, maxHeight: anchor.top - margin };

  return createPortal(
    <div
      ref={rootRef}
      className="slot-popover"
      style={style}
      onMouseDown={(e) => e.stopPropagation()}
    >
      <div className="slot-popover-header">
        <input
          ref={inputRef}
          type="text"
          placeholder="Type to search..."
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setHighlighted(0);
          }}
          onKeyDown={onKeyDown}
        />
        <button title="Clear this slot" onClick={onClear}>✕</button>
      </div>
      <div className="slot-popover-list" ref={listRef}>
        {filtered.map((item, i) => {
          const img = wikiImageUrl(item.image);
          return (
            <Tooltip key={displayName(item) + i} content={equipmentTooltip(item)}>
              <div
                className={`option${i === highlighted ? ' highlighted' : ''}`}
                onMouseEnter={() => setHighlighted(i)}
                onClick={() => onSelect(item)}
              >
                {img ? <img src={img} alt="" loading="lazy" /> : <span style={{ width: 22 }} />}
                <span>{displayName(item)}</span>
              </div>
            </Tooltip>
          );
        })}
        {filtered.length === 0 && <div className="option subtle">No matches</div>}
      </div>
    </div>,
    document.body,
  );
};
