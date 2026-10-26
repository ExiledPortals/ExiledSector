import { memo, useMemo } from 'react';
import type { NodeRadius } from '../../../api/nodeRadii.ts';
import { fileUrl } from '../../../shared/imagePaths.ts';
import { bezierHandleFor, curveKey, defaultControlPoint } from '../model/connectors.ts';
import { anchorShapeVertices, countNodesWithinRadius, positionOnAnchor } from '../model/geometry.ts';
import {
  radiusForTier,
  TIER_COLOR_VARIABLES,
  TIER_RADIUS,
  tierOf,
  type ConnectorCurve,
  type NodePair,
  type OrbitAnchor,
  type SkillType,
  type Tier,
  type TreeNode,
} from '../model/types.ts';
import type { ActiveDrag } from '../state/treeStore.ts';
import { centerClass, handleClass, isHandleActive, isMoving } from './handleClasses.ts';
import classes from './TreeCanvas.module.css';

export function IconClipDefinitions() {
  return (
    <defs>
      {Object.entries(TIER_RADIUS).map(([tier, radius]) => (
        <clipPath key={tier} id={`tree-node-clip-${tier}`}>
          <circle cx={0} cy={0} r={radius} />
        </clipPath>
      ))}
    </defs>
  );
}

function useTypeMap(types: SkillType[]): Map<string, SkillType> {
  return useMemo(() => new Map(types.map((type) => [type.id, type])), [types]);
}

interface NodeShapeProps {
  nodeId: string;
  x: number;
  y: number;
  tier: Tier;
  icon: string | null;
  broken: boolean;
  selected: boolean;
  dimmed: boolean;
}

const NodeShape = memo(function NodeShape({ nodeId, x, y, tier, icon, broken, selected, dimmed }: NodeShapeProps) {
  const radius = radiusForTier(tier);
  const colour = broken ? 'var(--tree-danger)' : TIER_COLOR_VARIABLES[tier];
  const hasIcon = Boolean(icon) && !broken;
  let fillOpacity = 0.85;
  if (hasIcon) {
    fillOpacity = 0;
  } else if (dimmed) {
    fillOpacity = 0.25;
  } else if (selected) {
    fillOpacity = 1;
  }
  let stroke = 'rgba(0,0,0,.25)';
  if (selected) {
    stroke = 'var(--tree-text)';
  } else if (hasIcon) {
    stroke = colour;
  }
  let strokeWidth = 1;
  if (selected) {
    strokeWidth = 2.5;
  } else if (hasIcon) {
    strokeWidth = 2;
  }
  const shapeProps = {
    className: classes.nodeShape,
    fill: colour,
    fillOpacity,
    stroke,
    strokeWidth,
    strokeOpacity: hasIcon && dimmed ? 0.25 : 1,
    'data-node-id': nodeId,
  };
  return (
    <g>
      {hasIcon && icon ? (
        <g transform={`translate(${x},${y})`} pointerEvents="none">
          <image
            href={fileUrl(icon)}
            x={-radius}
            y={-radius}
            width={radius * 2}
            height={radius * 2}
            clipPath={`url(#tree-node-clip-${tier})`}
            opacity={dimmed ? 0.25 : 1}
          />
        </g>
      ) : null}
      {tier === 'SOCKET' ? (
        <rect x={x - radius} y={y - radius} width={radius * 2} height={radius * 2} rx={3} {...shapeProps} />
      ) : (
        <circle cx={x} cy={y} r={radius} {...shapeProps} />
      )}
    </g>
  );
});

interface NodeLayerProps {
  nodes: TreeNode[];
  types: SkillType[];
  selectedIds: string[];
  matchIds: Set<string> | null;
  brokenIds: Set<string>;
}

export const NodeLayer = memo(function NodeLayer({ nodes, types, selectedIds, matchIds, brokenIds }: NodeLayerProps) {
  const typeMap = useTypeMap(types);
  const selected = useMemo(() => new Set(selectedIds), [selectedIds]);
  return (
    <g>
      {nodes.map((node) => {
        const type = typeMap.get(node.type);
        return (
          <NodeShape
            key={node.id}
            nodeId={node.id}
            x={node.x}
            y={node.y}
            tier={tierOf(type)}
            icon={type?.icon || null}
            broken={brokenIds.has(node.id)}
            selected={selected.has(node.id)}
            dimmed={matchIds !== null && !matchIds.has(node.id)}
          />
        );
      })}
    </g>
  );
});

interface LabelLayerProps {
  nodes: TreeNode[];
  types: SkillType[];
  matchIds: Set<string> | null;
}

export const LabelLayer = memo(function LabelLayer({ nodes, types, matchIds }: LabelLayerProps) {
  const typeMap = useTypeMap(types);
  return (
    <g>
      {nodes.map((node) => {
        if (matchIds !== null && !matchIds.has(node.id)) {
          return null;
        }
        const type = typeMap.get(node.type);
        return (
          <text key={node.id} x={node.x} y={node.y + radiusForTier(tierOf(type)) + 12} textAnchor="middle" className={classes.nodeLabel}>
            {type ? type.name : node.type}
          </text>
        );
      })}
    </g>
  );
});

const EdgePath = memo(function EdgePath({ path, hidden }: { path: string; hidden: boolean }) {
  return <path d={path} className={`${classes.edgeLine}${hidden ? ` ${classes.hiddenConnector}` : ''}`} />;
});

interface EdgeGeometry {
  key: string;
  a: string;
  b: string;
  path: string;
  hidden: boolean;
  controlX: number;
  controlY: number;
}

function edgeGeometry(nodes: TreeNode[], curves: ConnectorCurve[], hiddenConnectors: NodePair[]): EdgeGeometry[] {
  const nodeMap = new Map(nodes.map((node) => [node.id, node]));
  const curveMap = new Map(curves.map((curve) => [curveKey(curve.a, curve.b), curve]));
  const hidden = new Set(hiddenConnectors.map((pair) => curveKey(pair.a, pair.b)));
  const edges: EdgeGeometry[] = [];
  for (const node of nodes) {
    for (const otherId of node.connectedTo) {
      const other = nodeMap.get(otherId);
      if (!other || node.id >= otherId) {
        continue;
      }
      const key = curveKey(node.id, otherId);
      const curve = curveMap.get(key);
      const control = curve ? { x: curve.controlX, y: curve.controlY } : defaultControlPoint(node, other);
      const handle = bezierHandleFor(control.x, control.y, other.x, other.y, node.x, node.y);
      edges.push({
        key,
        a: node.id,
        b: otherId,
        path: `M ${other.x} ${other.y} Q ${handle.x} ${handle.y} ${node.x} ${node.y}`,
        hidden: hidden.has(key),
        controlX: control.x,
        controlY: control.y,
      });
    }
  }
  return edges;
}

interface EdgeLayerProps {
  nodes: TreeNode[];
  curves: ConnectorCurve[];
  hiddenConnectors: NodePair[];
  showCurveHandles: boolean;
  activeCurveKey: string | null;
}

export const EdgeLayer = memo(function EdgeLayer({ nodes, curves, hiddenConnectors, showCurveHandles, activeCurveKey }: EdgeLayerProps) {
  const edges = useMemo(() => edgeGeometry(nodes, curves, hiddenConnectors), [nodes, curves, hiddenConnectors]);
  return (
    <g>
      <path d="M -14 0 L 0 -14 L 14 0 L 0 14 Z" className={classes.centerMark} />
      {edges.map((edge) => (
        <EdgePath key={edge.key} path={edge.path} hidden={edge.hidden} />
      ))}
      {showCurveHandles
        ? edges.map((edge) => {
            const active = edge.key === activeCurveKey;
            return (
              <circle
                key={edge.key}
                cx={edge.controlX}
                cy={edge.controlY}
                r={active ? 7 : 5}
                className={handleClass(classes.curveHandle, active)}
                data-curve-a={edge.a}
                data-curve-b={edge.b}
              />
            );
          })
        : null}
    </g>
  );
});

export const AnchorRingLayer = memo(function AnchorRingLayer({ anchors, selectedIds }: { anchors: OrbitAnchor[]; selectedIds: string[] }) {
  return (
    <g>
      {anchors.map((anchor) => {
        const className = `${classes.orbitRing}${selectedIds.includes(anchor.id) ? ` ${classes.orbitRingSelected}` : ''}`;
        const vertices = anchorShapeVertices(anchor);
        return vertices ? (
          <polygon key={anchor.id} points={vertices.map((vertex) => `${vertex.x},${vertex.y}`).join(' ')} className={className} />
        ) : (
          <circle key={anchor.id} cx={anchor.x} cy={anchor.y} r={anchor.radius} className={className} />
        );
      })}
    </g>
  );
});

interface HandleLayerProps<TItem> {
  items: TItem[];
  selectedIds: string[];
  activeDrag: ActiveDrag | null;
}

export const AnchorHandleLayer = memo(function AnchorHandleLayer({ items, selectedIds, activeDrag }: HandleLayerProps<OrbitAnchor>) {
  return (
    <g>
      {items.map((anchor) => {
        const selected = selectedIds.includes(anchor.id);
        const moving = isMoving(activeDrag, 'anchorIds', anchor.id);
        const resizing = isHandleActive(activeDrag, 'anchorResize', anchor.id);
        const rotating = isHandleActive(activeDrag, 'anchorRotate', anchor.id);
        const rotatePosition = anchorShapeVertices(anchor) ? positionOnAnchor(anchor, (anchor.rotation || 0) - 90) : null;
        return (
          <g key={anchor.id}>
            <circle cx={anchor.x} cy={anchor.y} r={moving ? 8 : 6} className={centerClass(selected || moving)} data-anchor-id={anchor.id} />
            <rect
              x={anchor.x - anchor.radius / 2 - 5}
              y={anchor.y - 5}
              width={10}
              height={10}
              className={handleClass(classes.shapeCycleHandle, false)}
              data-anchor-shape-cycle={anchor.id}
            />
            {selected ? (
              <circle
                cx={anchor.x + anchor.radius / 2}
                cy={anchor.y}
                r={resizing ? 7 : 5}
                className={handleClass(classes.resizeHandle, resizing)}
                data-anchor-resize={anchor.id}
              />
            ) : null}
            {selected && rotatePosition ? (
              <circle
                cx={rotatePosition.x}
                cy={rotatePosition.y}
                r={rotating ? 7 : 5}
                className={handleClass(classes.rotateHandle, rotating)}
                data-anchor-rotate={anchor.id}
              />
            ) : null}
          </g>
        );
      })}
    </g>
  );
});

const RADIUS_LABEL_SCREEN_SIZE = 13;

function describeTierCounts(counts: Map<Tier, number>): string {
  const parts = (Object.keys(TIER_RADIUS) as Tier[]).flatMap((tier) => {
    const count = counts.get(tier);
    return count ? [`${count} ${tier.toLowerCase().replace('_', ' ')}`] : [];
  });
  return parts.length > 0 ? parts.join(', ') : 'no nodes';
}

interface SocketRadiusLayerProps {
  socket: TreeNode;
  nodes: TreeNode[];
  types: SkillType[];
  radii: NodeRadius[];
  zoom: number;
}

export const SocketRadiusLayer = memo(function SocketRadiusLayer({ socket, nodes, types, radii, zoom }: SocketRadiusLayerProps) {
  const typeMap = useTypeMap(types);
  const labelSize = RADIUS_LABEL_SCREEN_SIZE / zoom;
  return (
    <g pointerEvents="none" data-testid="socket-radius-layer">
      {radii.map((radius) => (
        <g key={radius.size}>
          <circle cx={socket.x} cy={socket.y} r={radius.distance} className={classes.socketRadius} />
          <text x={socket.x} y={socket.y - radius.distance - labelSize * 0.5} textAnchor="middle" fontSize={labelSize} className={classes.socketRadiusLabel}>
            {`${radius.size} ${radius.distance}: ${describeTierCounts(countNodesWithinRadius(nodes, typeMap, socket, radius.distance))}`}
          </text>
        </g>
      ))}
    </g>
  );
});
