import { Text } from '@mantine/core';
import type { CSSProperties } from 'react';
import { renderMarkup } from '../../../shared/tooltipMarkup.ts';
import type { LineDescriptions } from '../describedLines.ts';
import type { ModifierLine } from '../model/modifiers.ts';
import classes from './GameText.module.css';

interface GameLinesProps {
  lines: readonly ModifierLine[];
  itemType: string;
  descriptions: LineDescriptions;
  selected?: boolean;
  style?: CSSProperties;
}

export function GameLines({ lines, itemType, descriptions, selected = false, style }: GameLinesProps) {
  return (
    <div className={selected ? `${classes.lines} ${classes.selected}` : classes.lines} style={style}>
      {lines.map((line, index) => (
        <div key={index} className={classes.line}>
          {renderMarkup(descriptions.markupOf(line, itemType))}
        </div>
      ))}
    </div>
  );
}

export function MissingModifier({ text }: { text: string }) {
  return (
    <div className={classes.lines}>
      <span className={classes.missing}>{text}</span>
    </div>
  );
}

export function DescriptionNote({ descriptions }: { descriptions: LineDescriptions }) {
  return descriptions.note ? (
    <Text size="xs" c="dimmed">
      {descriptions.note}
    </Text>
  ) : null;
}
