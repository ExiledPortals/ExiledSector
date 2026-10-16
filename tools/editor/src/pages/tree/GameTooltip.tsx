import { Portal } from '@mantine/core';
import { useEffect, useLayoutEffect, useRef, useState, type ReactNode } from 'react';
import { SERVER_HINT } from '../../api/client.ts';
import { skillTypesFileObject } from './model/serialization.ts';
import { HULL_SIZE_OPTIONS, nodeById } from './model/types.ts';
import { useTreeStore } from './state/treeStore.ts';
import { mergeDescriptions, type Described, type LabelledDescription, type TooltipContent, type TooltipGroup } from './tooltipContent.ts';
import classes from './GameTooltip.module.css';

const SHOW_DELAY_MS = 250;
const RETRY_DELAY_MS = 2000;
const MARKUP_COLOURS: Record<string, string> = { good: '#98fb00', bad: '#fc6300', hl: '#ffc800', hullmod: '#ffa03c', node: '#5abeff' };
const MARKUP_PATTERN = /<(\/?)(good|bad|hl|hullmod|node)>/g;

type TooltipState = { kind: 'note'; message: string } | { kind: 'content'; content: TooltipContent };

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

async function fetchDescription(typesJson: string, typeId: string, hullSize: string): Promise<Described> {
  try {
    const response = await fetch('/api/describe', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-Skill-Tree-Editor': '1' },
      body: `{"types":${typesJson},"typeId":${JSON.stringify(typeId)},"hullSize":${JSON.stringify(hullSize)}}`,
    });
    return (await response.json()) as Described;
  } catch {
    return { error: `In-game descriptions need the editor server - ${SERVER_HINT}.` };
  }
}

function Groups({ groups }: { groups: TooltipGroup[] }) {
  return (
    <>
      {groups.map((group, groupIndex) => (
        <div key={group.label ?? `shared-${groupIndex}`} className={classes.group}>
          {group.label ? <div className={classes.groupLabel}>{group.label}</div> : null}
          {group.lines.map((line, index) => (
            <div key={index} className={classes.line}>
              {renderMarkup(line)}
            </div>
          ))}
        </div>
      ))}
    </>
  );
}

export function GameTooltip({ hullSize }: { hullSize: string }) {
  const [shown, setShown] = useState<TooltipState | null>(null);
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

  const attachBox = (element: HTMLDivElement | null) => {
    boxRef.current = element;
    position();
  };

  useLayoutEffect(position, [shown, expanded]);

  useEffect(() => {
    const state = hover.current;

    const request = async () => {
      const typeId = state.typeId;
      if (!typeId) {
        return;
      }
      state.serial++;
      const serial = state.serial;
      const typesJson = JSON.stringify(skillTypesFileObject(useTreeStore.getState().document.types));
      const typesKey = `${typesJson.length}|${hashText(typesJson)}`;
      const sizes = hullSizeRef.current ? HULL_SIZE_OPTIONS.filter((option) => option.value === hullSizeRef.current) : HULL_SIZE_OPTIONS;
      const entries: LabelledDescription[] = [];
      for (const size of sizes) {
        const cacheKey = `${typeId}|${size.value}|${typesKey}`;
        const described = cache.current.get(cacheKey) ?? (await fetchDescription(typesJson, typeId, size.value));
        if (serial !== state.serial || state.typeId !== typeId) {
          return;
        }
        if (described.pending || described.error) {
          setShown({ kind: 'note', message: described.message || described.error || 'Preparing in-game descriptions...' });
          if (described.pending) {
            state.timer = window.setTimeout(() => void request(), RETRY_DELAY_MS);
          }
          return;
        }
        cache.current.set(cacheKey, described);
        entries.push({ label: size.label, described });
      }
      setShown({ kind: 'content', content: mergeDescriptions(entries) });
    };

    const hide = () => {
      window.clearTimeout(state.timer);
      state.typeId = null;
      state.serial++;
      setShown(null);
    };

    const onMouseOver = (event: MouseEvent) => {
      state.x = event.clientX;
      state.y = event.clientY;
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

  if (!shown) {
    return null;
  }
  return (
    <Portal>
      <div ref={attachBox} className={classes.tooltip}>
        {shown.kind === 'note' ? (
          <div className={classes.note}>{shown.message}</div>
        ) : (
          <>
            <div className={classes.title}>{shown.content.title}</div>
            {shown.content.flavour ? <div className={classes.flavour}>{shown.content.flavour}</div> : null}
            <Groups groups={shown.content.effects} />
            {expanded ? <Groups groups={shown.content.details} /> : null}
            {shown.content.details.length > 0 && !expanded ? <div className={classes.note}>{shown.content.expandHint}</div> : null}
          </>
        )}
      </div>
    </Portal>
  );
}
