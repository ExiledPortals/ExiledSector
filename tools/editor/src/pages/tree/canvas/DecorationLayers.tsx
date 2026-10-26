import { memo, useMemo, type ReactNode } from 'react';
import { fileUrl } from '../../../shared/imagePaths.ts';
import {
  DECORATION_KINDS,
  decorationHandles,
  DEFAULT_STAR_COLOR,
  serpentPathPoint,
  threatUnitSprite,
  type Decoration,
  type DecorationKind,
  type DecorationOfKind,
} from '../model/decorations.ts';
import type { ActiveDrag } from '../state/treeStore.ts';
import { centerClass, handleClass, isMoving } from './handleClasses.ts';
import classes from './TreeCanvas.module.css';

const SERPENT_PATH_STEPS = 180;
const SERPENT_HEAD_RADIUS = 60;
const LABEL_GAP = 16;

type ShapeProps<TKind extends DecorationKind> = { decoration: DecorationOfKind<TKind>; selected: boolean };

function outlineClass(selected: boolean): string {
  return `${classes.decorationOutline}${selected ? ` ${classes.decorationOutlineSelected}` : ''}`;
}

function StarShape({ decoration: star }: ShapeProps<'star'>) {
  const colour = star.color || DEFAULT_STAR_COLOR;
  return (
    <g>
      <circle cx={star.x} cy={star.y} r={star.radius * 3} fill={colour} fillOpacity={0.12} />
      <circle cx={star.x} cy={star.y} r={star.radius} fill={colour} fillOpacity={0.85} />
    </g>
  );
}

function ImageShape({ decoration: image }: ShapeProps<'image'>) {
  const transform = image.rotation ? `rotate(${image.rotation} ${image.x} ${image.y})` : undefined;
  const left = image.x - image.width / 2;
  const top = image.y - image.height / 2;
  return image.imagePath ? (
    <image href={fileUrl(image.imagePath)} x={left} y={top} width={image.width} height={image.height} transform={transform} />
  ) : (
    <rect x={left} y={top} width={image.width} height={image.height} transform={transform} className={classes.imagePlaceholder} />
  );
}

function RingBeltShape({ decoration: belt, selected }: ShapeProps<'ringBelt'>) {
  return (
    <circle
      cx={belt.x}
      cy={belt.y}
      r={(belt.innerRadius + belt.outerRadius) / 2}
      strokeWidth={Math.max(1, belt.outerRadius - belt.innerRadius)}
      className={`${classes.ringBeltBand}${selected ? ` ${classes.ringBeltBandSelected}` : ''}`}
    />
  );
}

function ThreatUnitShape({ decoration: unit, selected }: ShapeProps<'threatUnit'>) {
  const sprite = threatUnitSprite(unit.unitType);
  const spriteWidth = unit.length * sprite.widthPerLength;
  const transform = unit.rotation ? `rotate(${unit.rotation} ${unit.x} ${unit.y})` : undefined;
  return (
    <g transform={transform}>
      <image href={fileUrl(sprite.spritePath)} x={unit.x - spriteWidth / 2} y={unit.y - unit.length / 2} width={spriteWidth} height={unit.length} preserveAspectRatio="none" />
      <ellipse cx={unit.x} cy={unit.y} rx={spriteWidth / 2} ry={unit.length / 2} className={outlineClass(selected)} />
      <text x={unit.x} y={unit.y + unit.length / 2 + LABEL_GAP} textAnchor="middle" className={classes.decorationLabel}>
        {sprite.label}
      </text>
    </g>
  );
}

function SerpentShape({ decoration: serpent, selected }: ShapeProps<'serpent'>) {
  const points = useMemo(
    () =>
      Array.from({ length: SERPENT_PATH_STEPS + 1 }, (_, step) => serpentPathPoint(serpent, (step * 360) / SERPENT_PATH_STEPS))
        .map((point) => `${point.x},${point.y}`)
        .join(' '),
    [serpent],
  );
  const head = serpentPathPoint(serpent, serpent.startAngle);
  const ahead = serpentPathPoint(serpent, serpent.startAngle + (serpent.clockwise ? 2 : -2));
  const facing = (Math.atan2(ahead.y - head.y, ahead.x - head.x) * 180) / Math.PI;
  return (
    <g>
      <polyline points={points} className={`${classes.serpentPath}${selected ? ` ${classes.serpentPathSelected}` : ''}`} />
      <polygon
        points={`${SERPENT_HEAD_RADIUS},0 ${-SERPENT_HEAD_RADIUS},${-SERPENT_HEAD_RADIUS * 0.7} ${-SERPENT_HEAD_RADIUS},${SERPENT_HEAD_RADIUS * 0.7}`}
        transform={`translate(${head.x} ${head.y}) rotate(${facing})`}
        className={classes.serpentHead}
      />
      <text x={head.x} y={head.y + SERPENT_HEAD_RADIUS + LABEL_GAP} textAnchor="middle" className={classes.decorationLabel}>
        {DECORATION_KINDS.serpent.label}
      </text>
    </g>
  );
}

const SHAPES: { [TKind in DecorationKind]: (props: ShapeProps<TKind>) => ReactNode } = {
  star: StarShape,
  image: ImageShape,
  ringBelt: RingBeltShape,
  threatUnit: ThreatUnitShape,
  serpent: SerpentShape,
};

function depthRank(decoration: Decoration): number {
  switch (decoration.kind) {
    case 'image':
      return decoration.behindStars ? 0 : 4;
    case 'star':
      return 1;
    case 'ringBelt':
      return 2;
    case 'serpent':
      return 3;
    case 'threatUnit':
      return 5;
  }
}

function DecorationShape({ decoration, selected }: { decoration: Decoration; selected: boolean }) {
  const Shape = SHAPES[decoration.kind] as (props: ShapeProps<DecorationKind>) => ReactNode;
  return <Shape decoration={decoration as never} selected={selected} />;
}

export const DecorationLayer = memo(function DecorationLayer({ decorations, selectedIds }: { decorations: Decoration[]; selectedIds: string[] }) {
  const ordered = useMemo(() => decorations.map((decoration, index) => ({ decoration, index })).sort((first, second) => depthRank(first.decoration) - depthRank(second.decoration) || first.index - second.index), [decorations]);
  const selected = useMemo(() => new Set(selectedIds), [selectedIds]);
  return (
    <g pointerEvents="none">
      {ordered.map(({ decoration }) => (
        <DecorationShape key={decoration.id} decoration={decoration} selected={selected.has(decoration.id)} />
      ))}
    </g>
  );
});

function isActiveHandle(activeDrag: ActiveDrag | null, id: string, handle: string): boolean {
  return activeDrag?.kind === 'decorationHandle' && activeDrag.id === id && activeDrag.handle === handle;
}

interface DecorationHandleLayerProps {
  decorations: Decoration[];
  selectedIds: string[];
  activeDrag: ActiveDrag | null;
}

export const DecorationHandleLayer = memo(function DecorationHandleLayer({ decorations, selectedIds, activeDrag }: DecorationHandleLayerProps) {
  return (
    <g>
      {decorations.map((decoration) => {
        const selected = selectedIds.includes(decoration.id);
        const moving = isMoving(activeDrag, 'decorationIds', decoration.id);
        return (
          <g key={decoration.id}>
            <circle cx={decoration.x} cy={decoration.y} r={moving ? 8 : 6} className={centerClass(selected || moving)} data-decoration-id={decoration.id} />
            {selected
              ? decorationHandles(decoration).map((handle) => {
                  const active = isActiveHandle(activeDrag, decoration.id, handle.name);
                  return (
                    <g key={handle.name}>
                      {handle.rotates ? <line x1={decoration.x} y1={decoration.y} x2={handle.x} y2={handle.y} className={classes.rotateLine} /> : null}
                      <circle
                        cx={handle.x}
                        cy={handle.y}
                        r={active ? 7 : 5}
                        className={handleClass(handle.rotates ? classes.rotateHandle : classes.resizeHandle, active)}
                        data-decoration-handle={handle.name}
                        data-decoration-handle-of={decoration.id}
                      />
                    </g>
                  );
                })
              : null}
          </g>
        );
      })}
    </g>
  );
});
