import { type ReactNode, useCallback, useRef, useState } from 'react';
import { createPortal } from 'react-dom';

/** Wraps children with a hover tooltip rendered near the cursor. */
export const Tooltip = ({ content, children }: { content: ReactNode; children: ReactNode }) => {
  const [pos, setPos] = useState<{ x: number; y: number } | null>(null);
  const timer = useRef<number | null>(null);

  const show = useCallback((e: React.MouseEvent) => {
    const { clientX, clientY } = e;
    if (timer.current) window.clearTimeout(timer.current);
    timer.current = window.setTimeout(() => setPos({ x: clientX, y: clientY }), 250);
  }, []);

  const hide = useCallback(() => {
    if (timer.current) window.clearTimeout(timer.current);
    setPos(null);
  }, []);

  const style = pos
    ? {
      left: Math.min(pos.x + 14, window.innerWidth - 400),
      top: Math.min(pos.y + 14, window.innerHeight - 220),
    }
    : undefined;

  return (
    <span className="has-tooltip" onMouseEnter={show} onMouseLeave={hide} onMouseDown={hide}>
      {children}
      {pos && content
        && createPortal(<div className="tooltip" style={style}>{content}</div>, document.body)}
    </span>
  );
};
