import { Portal } from '@mantine/core';
import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { DescribedTypesCache, typeTooltip, useTooltipCodeStatus, type DescribedTypes } from '../../api/tooltipCode.ts';
import { TooltipBody, TooltipNote } from '../../shared/TooltipBody.tsx';
import { TOOLTIP_PANEL_CLASS } from '../../shared/tooltipMarkup.ts';
import type { TooltipContent } from '../../shared/tooltipContent.ts';
import { skillTypesFileObject } from './model/serialization.ts';
import { HULL_SIZE_OPTIONS, nodeById } from './model/types.ts';
import { shipTreeEditor } from './state/treeEditor.ts';
import classes from './GameTooltip.module.css';

const SHOW_DELAY_MS = 250;
const RETRY_DELAY_MS = 2000;
const PREFETCH_DELAY_MS = 600;

type TooltipState = { kind: 'note'; message: string } | { kind: 'content'; content: TooltipContent };

const describedTypes = new DescribedTypesCache(HULL_SIZE_OPTIONS.map((option) => option.value));

function currentTypesJson(): string {
  return JSON.stringify(skillTypesFileObject(shipTreeEditor.store.getState().document.types));
}

function tooltipFor(described: DescribedTypes, typeId: string, hullSize: string): TooltipState {
  const sizes = hullSize ? HULL_SIZE_OPTIONS.filter((option) => option.value === hullSize) : HULL_SIZE_OPTIONS;
  const tooltip = typeTooltip(described, typeId, sizes);
  return 'content' in tooltip ? { kind: 'content', content: tooltip.content } : { kind: 'note', message: tooltip.note };
}

function hoveredTypeId(target: EventTarget | null): string | null {
  const element = target instanceof Element ? target.closest<HTMLElement | SVGElement>('[data-node-id], [data-type-id]') : null;
  if (!element) {
    return null;
  }
  if (element.dataset.typeId) {
    return element.dataset.typeId;
  }
  return nodeById(shipTreeEditor.store.getState().document, element.dataset.nodeId ?? '')?.type ?? null;
}

export function GameTooltip({ hullSize }: { hullSize: string }) {
  const [shown, setShown] = useState<TooltipState | null>(null);
  const [expanded, setExpanded] = useState(false);
  const boxRef = useRef<HTMLDivElement>(null);
  const hover = useRef({ typeId: null as string | null, timer: 0, x: 0, y: 0, serial: 0 });
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
    let prefetchTimer = 0;
    const prefetch = () => {
      window.clearTimeout(prefetchTimer);
      prefetchTimer = window.setTimeout(() => void describedTypes.describe(currentTypesJson()), PREFETCH_DELAY_MS);
    };
    prefetch();
    const unsubscribeTypes = shipTreeEditor.store.subscribe((state, previous) => {
      if (state.document.types !== previous.document.types) {
        prefetch();
      }
    });
    const unsubscribeCode = useTooltipCodeStatus.subscribe((state, previous) => {
      if (state.status?.codeRevision !== previous.status?.codeRevision) {
        prefetch();
      }
    });
    return () => {
      window.clearTimeout(prefetchTimer);
      unsubscribeTypes();
      unsubscribeCode();
    };
  }, []);

  useEffect(() => {
    const state = hover.current;

    const request = async () => {
      const typeId = state.typeId;
      if (!typeId) {
        return;
      }
      state.serial++;
      const serial = state.serial;
      const typesJson = currentTypesJson();
      const settled = describedTypes.current(typesJson);
      if (settled?.kind === 'ready') {
        setShown(tooltipFor(settled, typeId, hullSizeRef.current));
        return;
      }
      const previous = describedTypes.lastDescribed();
      if (previous?.kind === 'ready' && previous.byTypeId.has(typeId)) {
        setShown(tooltipFor(previous, typeId, hullSizeRef.current));
      }
      const described = await describedTypes.describe(typesJson);
      if (serial !== state.serial || state.typeId !== typeId) {
        return;
      }
      if (described.kind === 'note' && previous?.kind === 'ready' && previous.byTypeId.has(typeId)) {
        return;
      }
      setShown(tooltipFor(described, typeId, hullSizeRef.current));
      if (described.kind === 'note' && described.pending) {
        state.timer = window.setTimeout(() => void request(), RETRY_DELAY_MS);
      }
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
      <div ref={attachBox} className={`${classes.tooltip} ${TOOLTIP_PANEL_CLASS}`}>
        {shown.kind === 'note' ? <TooltipNote>{shown.message}</TooltipNote> : <TooltipBody content={shown.content} showDetails={expanded} />}
      </div>
    </Portal>
  );
}
