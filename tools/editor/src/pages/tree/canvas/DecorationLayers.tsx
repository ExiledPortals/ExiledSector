import { memo, useMemo, type ReactNode } from 'react';
import { fileUrl } from '../../../shared/imagePaths.ts';
import type { Point } from '../model/geometry.ts';
import {
  DECORATION_KINDS,
  decorationHandles,
  DEFAULT_STAR_COLOR,
  HYPERSHUNT_SPRITE_PATH,
  hypershuntPosition,
  serpentPathPoint,
  SHROUDED_MAW_HEIGHT_PER_LENGTH,
  SHROUDED_MAW_SPRITE_PATH,
  threatUnitSprite,
  type Decoration,
  type DecorationKind,
  type DecorationOfKind,
} from '../model/decorations.ts';
import type { ActiveDrag } from '../state/treeStore.ts';
import { centerClass, centreHandleRadius, handleClass, handleRadius, isMoving } from './handleClasses.ts';
import classes from './TreeCanvas.module.css';

const SERPENT_PATH_STEPS = 180;
const SERPENT_HEAD_RADIUS = 60;
const LABEL_GAP = 16;

type ShapeProps<TKind extends DecorationKind> = { decoration: DecorationOfKind<TKind>; selected: boolean; sceneryPainted: boolean };

function outlineClass(selected: boolean): string {
  return `${classes.decorationOutline}${selected ? ` ${classes.decorationOutlineSelected}` : ''}`;
}

function StarShape({ decoration: star, selected, sceneryPainted }: ShapeProps<'star'>) {
  if (sceneryPainted) {
    return selected ? <circle cx={star.x} cy={star.y} r={star.radius} className={outlineClass(true)} /> : null;
  }
  const colour = star.color || DEFAULT_STAR_COLOR;
  return (
    <g>
      <circle cx={star.x} cy={star.y} r={star.radius * 3} fill={colour} fillOpacity={0.12} />
      <circle cx={star.x} cy={star.y} r={star.radius} fill={colour} fillOpacity={0.85} />
    </g>
  );
}

function ImageShape({ decoration: image, selected, sceneryPainted }: ShapeProps<'image'>) {
  const transform = image.rotation ? `rotate(${image.rotation} ${image.x} ${image.y})` : undefined;
  const left = image.x - image.width / 2;
  const top = image.y - image.height / 2;
  if (sceneryPainted && image.imagePath) {
    return selected ? <rect x={left} y={top} width={image.width} height={image.height} transform={transform} className={outlineClass(true)} /> : null;
  }
  return image.imagePath ? (
    <image href={fileUrl(image.imagePath)} x={left} y={top} width={image.width} height={image.height} transform={transform} />
  ) : (
    <rect x={left} y={top} width={image.width} height={image.height} transform={transform} className={classes.imagePlaceholder} />
  );
}

function RingBeltShape({ decoration: belt, selected, sceneryPainted }: ShapeProps<'ringBelt'>) {
  if (sceneryPainted && !selected) {
    return null;
  }
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

interface SpriteShapeProps {
  centre: Point;
  spriteWidth: number;
  spriteHeight: number;
  rotation: number;
  href: string;
  label: string;
  selected: boolean;
  sceneryPainted: boolean;
}

function uprightHalfHeight(spriteWidth: number, spriteHeight: number, rotation: number): number {
  const radians = (rotation * Math.PI) / 180;
  return Math.hypot((spriteWidth / 2) * Math.sin(radians), (spriteHeight / 2) * Math.cos(radians));
}

function SpriteShape({ centre, spriteWidth, spriteHeight, rotation, href, label, selected, sceneryPainted }: SpriteShapeProps) {
  const transform = rotation ? `rotate(${rotation} ${centre.x} ${centre.y})` : undefined;
  return (
    <g>
      <g transform={transform}>
        {sceneryPainted ? null : <image href={href} x={centre.x - spriteWidth / 2} y={centre.y - spriteHeight / 2} width={spriteWidth} height={spriteHeight} preserveAspectRatio="none" />}
        {sceneryPainted && !selected ? null : <ellipse cx={centre.x} cy={centre.y} rx={spriteWidth / 2} ry={spriteHeight / 2} className={outlineClass(selected)} />}
      </g>
      <text x={centre.x} y={centre.y + uprightHalfHeight(spriteWidth, spriteHeight, rotation) + LABEL_GAP} textAnchor="middle" className={classes.decorationLabel}>
        {label}
      </text>
    </g>
  );
}

function ThreatUnitShape({ decoration: unit, selected, sceneryPainted }: ShapeProps<'threatUnit'>) {
  const sprite = threatUnitSprite(unit.unitType);
  return (
    <SpriteShape
      centre={unit}
      spriteWidth={unit.length * sprite.widthPerLength}
      spriteHeight={unit.length}
      rotation={unit.rotation || 0}
      href={fileUrl(sprite.spritePath)}
      label={sprite.label}
      selected={selected}
      sceneryPainted={sceneryPainted}
    />
  );
}

function ShroudedMawShape({ decoration: maw, selected, sceneryPainted }: ShapeProps<'shroudedMaw'>) {
  return (
    <SpriteShape
      centre={maw}
      spriteWidth={maw.length}
      spriteHeight={maw.length * SHROUDED_MAW_HEIGHT_PER_LENGTH}
      rotation={maw.rotation || 0}
      href={fileUrl(SHROUDED_MAW_SPRITE_PATH)}
      label={DECORATION_KINDS.shroudedMaw.label}
      selected={selected}
      sceneryPainted={sceneryPainted}
    />
  );
}

function orbitPathClass(selected: boolean): string {
  return `${classes.orbitPath}${selected ? ` ${classes.orbitPathSelected}` : ''}`;
}

function SerpentShape({ decoration: serpent, selected, sceneryPainted }: ShapeProps<'serpent'>) {
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
  if (sceneryPainted && !selected) {
    return null;
  }
  return (
    <g>
      <polyline points={points} className={orbitPathClass(selected)} />
      {sceneryPainted ? null : (
        <polygon
          points={`${SERPENT_HEAD_RADIUS},0 ${-SERPENT_HEAD_RADIUS},${-SERPENT_HEAD_RADIUS * 0.7} ${-SERPENT_HEAD_RADIUS},${SERPENT_HEAD_RADIUS * 0.7}`}
          transform={`translate(${head.x} ${head.y}) rotate(${facing})`}
          className={classes.serpentHead}
        />
      )}
      <text x={head.x} y={head.y + SERPENT_HEAD_RADIUS + LABEL_GAP} textAnchor="middle" className={classes.decorationLabel}>
        {DECORATION_KINDS.serpent.label}
      </text>
    </g>
  );
}

function HypershuntShape({ decoration: hypershunt, selected, sceneryPainted }: ShapeProps<'hypershunt'>) {
  if (sceneryPainted && !selected) {
    return null;
  }
  return (
    <g>
      <circle cx={hypershunt.x} cy={hypershunt.y} r={hypershunt.orbitRadius} className={orbitPathClass(selected)} />
      <SpriteShape
        centre={hypershuntPosition(hypershunt)}
        spriteWidth={hypershunt.size}
        spriteHeight={hypershunt.size}
        rotation={hypershunt.startAngle + 90}
        href={fileUrl(HYPERSHUNT_SPRITE_PATH)}
        label={DECORATION_KINDS.hypershunt.label}
        selected={selected}
        sceneryPainted={sceneryPainted}
      />
    </g>
  );
}

const SHAPES: { [TKind in DecorationKind]: (props: ShapeProps<TKind>) => ReactNode } = {
  star: StarShape,
  image: ImageShape,
  ringBelt: RingBeltShape,
  threatUnit: ThreatUnitShape,
  serpent: SerpentShape,
  shroudedMaw: ShroudedMawShape,
  hypershunt: HypershuntShape,
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
    case 'shroudedMaw':
    case 'hypershunt':
      return 5;
  }
}

function DecorationShape({ decoration, selected, sceneryPainted }: { decoration: Decoration; selected: boolean; sceneryPainted: boolean }) {
  const Shape = SHAPES[decoration.kind] as (props: ShapeProps<DecorationKind>) => ReactNode;
  return <Shape decoration={decoration as never} selected={selected} sceneryPainted={sceneryPainted} />;
}

interface DecorationLayerProps {
  decorations: Decoration[];
  selectedIds: string[];
  sceneryPainted: boolean;
}

export const DecorationLayer = memo(function DecorationLayer({ decorations, selectedIds, sceneryPainted }: DecorationLayerProps) {
  const ordered = useMemo(() => decorations.map((decoration, index) => ({ decoration, index })).sort((first, second) => depthRank(first.decoration) - depthRank(second.decoration) || first.index - second.index), [decorations]);
  const selected = useMemo(() => new Set(selectedIds), [selectedIds]);
  return (
    <g pointerEvents="none">
      {ordered.map(({ decoration }) => (
        <DecorationShape key={decoration.id} decoration={decoration} selected={selected.has(decoration.id)} sceneryPainted={sceneryPainted} />
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
  handleScale: number;
}

export const DecorationHandleLayer = memo(function DecorationHandleLayer({ decorations, selectedIds, activeDrag, handleScale }: DecorationHandleLayerProps) {
  return (
    <g>
      {decorations.map((decoration) => {
        const selected = selectedIds.includes(decoration.id);
        const moving = isMoving(activeDrag, 'decorationIds', decoration.id);
        return (
          <g key={decoration.id}>
            <circle cx={decoration.x} cy={decoration.y} r={centreHandleRadius(moving, handleScale)} className={centerClass(selected || moving)} data-decoration-id={decoration.id} />
            {selected
              ? decorationHandles(decoration).map((handle) => {
                  const active = isActiveHandle(activeDrag, decoration.id, handle.name);
                  return (
                    <g key={handle.name}>
                      {handle.rotates ? <line x1={decoration.x} y1={decoration.y} x2={handle.x} y2={handle.y} className={classes.rotateLine} /> : null}
                      <circle
                        cx={handle.x}
                        cy={handle.y}
                        r={handleRadius(active, handleScale)}
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
