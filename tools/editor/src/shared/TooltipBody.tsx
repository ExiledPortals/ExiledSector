import type { ReactNode } from 'react';
import type { TooltipContent, TooltipGroup } from './tooltipContent.ts';
import { renderMarkup } from './tooltipMarkup.ts';
import classes from './TooltipBody.module.css';

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

interface TooltipBodyProps {
  content: TooltipContent;
  showDetails: boolean;
}

export function TooltipBody({ content, showDetails }: TooltipBodyProps) {
  return (
    <>
      <div className={classes.title}>{content.title}</div>
      {content.flavour ? <div className={classes.flavour}>{content.flavour}</div> : null}
      <Groups groups={content.effects} />
      {showDetails ? <Groups groups={content.details} /> : null}
      {content.details.length > 0 && !showDetails ? <div className={classes.note}>{content.expandHint}</div> : null}
    </>
  );
}

export function TooltipEffects({ content }: { content: TooltipContent }) {
  return <Groups groups={content.effects} />;
}

export function TooltipNote({ children }: { children: ReactNode }) {
  return <div className={classes.note}>{children}</div>;
}
