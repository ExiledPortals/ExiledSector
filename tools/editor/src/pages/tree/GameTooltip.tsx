import { Portal } from '@mantine/core';
import { useEffect, useLayoutEffect, useRef, useState, type ReactNode } from 'react';
import { SERVER_HINT } from '../../api/client.ts';
import { skillTypesFileObject } from './model/serialization.ts';
import { nodeById } from './model/types.ts';
import { useTreeStore } from './state/treeStore.ts';
import classes from './GameTooltip.module.css';

const SHOW_DELAY_MS = 250;
const RETRY_DELAY_MS = 2000;
const MARKUP_COLOURS: Record<string, string> = { good: '#98fb00', bad: '#fc6300', hl: '#ffc800', hullmod: '#ffa03c', node: '#5abeff' };
const MARKUP_PATTERN = /<(\/?)(good|bad|hl|hullmod|node)>/g;

interface Described {
  pending?: boolean;
  error?: string;
  message?: string;
  title?: string;
  flavour?: string;
  effects?: string[];
  details?: string[];
  expandHint?: string;
}

function hashText(text: string): number {
  let hash = 5381;
  for (let index = 0; index < text.length; index++) {
    hash = (hash * 33 + (text.codePointAt(index) ?? 0)) % 2147483647;
  }
  return hash;
}

function renderMarkup(markup: string): ReactNode[] {
  const parts: ReactNode[] = [];
  let cursor = 0;
  let colour: string | null = null;
  const push = (text: string) => {
    if (text) {
      parts.push(colour ? <span key={parts.length} style={{ color: colour }}>{text}</span> : text);
    }
  };
  for (const match of markup.matchAll(MARKUP_PATTERN)) {
    push(markup.slice(cursor, match.index));
    colour = match[1] ? null : MARKUP_COLOURS[match[2]];
    cursor = (match.index ?? 0) + match[0].length;
  }
  push(markup.slice(cursor));
  return parts;
}

function hoveredTypeId(target: EventTarget | null): string | null {
  const element = target instanceof Element ? target.closest<HTMLElement | SVGElement>('[data-node-id], [data-type-id]') : null;
  if (!element) {
    return null;
  }
  if (element.dataset.typeId) {
    return element.dataset.typeId;
  }
  return nodeById(useTreeStore.getState().document, element.dataset.nodeId ?? '')?.type ?? null;
}

export function GameTooltip({ hullSize }: { hullSize: string }) {
  const [described, setDescribed] = useState<Described | null>(null);
  const [expanded, setExpanded] = useState(false);
  const boxRef = useRef<HTMLDivElement>(null);
  const hover = useRef({ typeId: null as string | null, timer: 0, x: 0, y: 0, serial: 0 });
  const cache = useRef(new Map<string, Described>());
  const hullSizeRef = useRef(hullSize);
  hullSizeRef.current = hullSize;

  const position = () => {
    const box = boxRef.current;
    if (!box) {
      return;
    }
    const margin = 16;
    let left = hover.current.x + margin;
    let top = hover.current.y + margin;
    if (left + box.offsetWidth > window.innerWidth - 4) {
      left = Math.max(4, hover.current.x - margin - box.offsetWidth);
    }
    if (top + box.offsetHeight > window.innerHeight - 4) {
      top = Math.max(4, window.innerHeight - 4 - box.offsetHeight);
    }
    box.style.left = `${left}px`;
    box.style.top = `${top}px`;
  };

  useLayoutEffect(position, [described, expanded]);

  useEffect(() => {
    const state = hover.current;

    const request = async () => {
      const typeId = state.typeId;
      if (!typeId) {
        return;
      }
      const typesJson = JSON.stringify(skillTypesFileObject(useTreeStore.getState().document.types));
      const size = hullSizeRef.current || null;
      const cacheKey = `${typeId}|${size}|${typesJson.length}|${hashText(typesJson)}`;
      const cached = cache.current.get(cacheKey);
      if (cached) {
        setDescribed(cached);
        return;
      }
      state.serial++;
      const serial = state.serial;
      let result: Described;
      try {
        const response = await fetch('/api/describe', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'X-Skill-Tree-Editor': '1' },
          body: `{"types":${typesJson},"typeId":${JSON.stringify(typeId)},"hullSize":${JSON.stringify(size)}}`,
        });
        result = (await response.json()) as Described;
      } catch {
        result = { error: `In-game descriptions need the editor server - ${SERVER_HINT}.` };
      }
      if (serial !== state.serial || state.typeId !== typeId) {
        return;
      }
      if (!result.pending && !result.error) {
        cache.current.set(cacheKey, result);
      }
      setDescribed(result);
      if (result.pending) {
        state.timer = window.setTimeout(() => void request(), RETRY_DELAY_MS);
      }
    };

    const hide = () => {
      window.clearTimeout(state.timer);
      state.typeId = null;
      state.serial++;
      setDescribed(null);
    };

    const onMouseOver = (event: MouseEvent) => {
      const typeId = hoveredTypeId(event.target);
      if (typeId === state.typeId) {
        return;
      }
      hide();
      if (typeId) {
        state.typeId = typeId;
        state.timer = window.setTimeout(() => void request(), SHOW_DELAY_MS);
      }
    };

    const onMouseMove = (event: MouseEvent) => {
      state.x = event.clientX;
      state.y = event.clientY;
      position();
    };

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key !== 'F1') {
        return;
      }
      event.preventDefault();
      setExpanded((current) => !current);
    };

    document.addEventListener('mouseover', onMouseOver);
    document.addEventListener('mousemove', onMouseMove);
    document.addEventListener('mousedown', hide, true);
    document.addEventListener('wheel', hide, { passive: true });
    document.addEventListener('keydown', onKeyDown);
    return () => {
      hide();
      document.removeEventListener('mouseover', onMouseOver);
      document.removeEventListener('mousemove', onMouseMove);
      document.removeEventListener('mousedown', hide, true);
      document.removeEventListener('wheel', hide);
      document.removeEventListener('keydown', onKeyDown);
    };
  }, []);

  if (!described) {
    return null;
  }
  const lines = [...(described.effects ?? []), ...(expanded ? (described.details ?? []) : [])];
  return (
    <Portal>
      <div ref={boxRef} className={classes.tooltip}>
        {described.pending || described.error ? (
          <div className={classes.note}>{described.message || described.error || 'Preparing in-game descriptions...'}</div>
        ) : (
          <>
            <div className={classes.title}>{described.title}</div>
            {described.flavour ? <div className={classes.flavour}>{described.flavour}</div> : null}
            {lines.map((line, index) => (
              <div key={index} className={classes.line}>
                {renderMarkup(line)}
              </div>
            ))}
            {(described.details ?? []).length > 0 && !expanded ? <div className={classes.note}>{described.expandHint}</div> : null}
          </>
        )}
      </div>
    </Portal>
  );
}
