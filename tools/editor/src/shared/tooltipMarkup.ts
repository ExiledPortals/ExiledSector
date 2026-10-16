import { createElement, type ReactNode } from 'react';
import classes from './TooltipBody.module.css';

export const TOOLTIP_PANEL_CLASS = classes.panel;

export const MARKUP_COLOURS: Record<string, string> = { good: '#98fb00', bad: '#fc6300', hl: '#ffc800', hullmod: '#ffa03c', node: '#5abeff' };
const MARKUP_PATTERN = /<(\/?)(good|bad|hl|hullmod|node)>/g;

export function renderMarkup(markup: string): ReactNode[] {
  const parts: ReactNode[] = [];
  let cursor = 0;
  let colour: string | null = null;
  const push = (text: string) => {
    if (text) {
      parts.push(colour ? createElement('span', { key: parts.length, style: { color: colour } }, text) : text);
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
