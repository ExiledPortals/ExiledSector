import { setCurveControl } from './connectors.ts';
import { ANCHOR_RECTANGLE_ASPECT, nodeById, tierOf, type AnchorShape, type OrbitAnchor, type SkillType, type Tier, type TreeDocument, type TreeNode } from './types.ts';

export interface Point {
  x: number;
  y: number;
}

export const ORBIT_SNAP_RADIUS = 18;
export const ORBIT_DETACH_RADIUS = 45;
const ORBIT_CARDINAL_SNAP_DEGREES = 6;
export const ORBIT_CARDINAL_ANGLES = [0, 90, 180, 270];
const ALIGN_SNAP_THRESHOLD = 10;

const DEGREES = Math.PI / 180;

export function rotateAroundOrigin(localX: number, localY: number, degrees: number): Point {
  const radians = degrees * DEGREES;
  const cos = Math.cos(radians);
  const sin = Math.sin(radians);
  return { x: localX * cos - localY * sin, y: localX * sin + localY * cos };
}

export function angleDegrees(fromX: number, fromY: number, toX: number, toY: number): number {
  return (Math.atan2(toY - fromY, toX - fromX) * 180) / Math.PI;
}

export function normalizeDegrees(degrees: number): number {
  return ((degrees % 360) + 360) % 360;
}

export function anchorShapeVertices(anchor: OrbitAnchor): Point[] | null {
  const shape = anchor.shape ?? 'circle';
  const radius = anchor.radius;
  let localVertices: Point[];
  if (shape === 'square') {
    const half = radius / Math.SQRT2;
    localVertices = [
      { x: -half, y: -half },
      { x: half, y: -half },
      { x: half, y: half },
      { x: -half, y: half },
    ];
  } else if (shape === 'rectangle') {
    const ratio = anchor.aspectRatio || ANCHOR_RECTANGLE_ASPECT;
    const halfHeight = radius / Math.sqrt(1 + ratio * ratio);
    const halfWidth = ratio * halfHeight;
    localVertices = [
      { x: -halfWidth, y: -halfHeight },
      { x: halfWidth, y: -halfHeight },
      { x: halfWidth, y: halfHeight },
      { x: -halfWidth, y: halfHeight },
    ];
  } else if (shape === 'triangle') {
    localVertices = [-90, 30, 150].map((degrees) => ({ x: radius * Math.cos(degrees * DEGREES), y: radius * Math.sin(degrees * DEGREES) }));
  } else {
    return null;
  }
  const rotation = (anchor.rotation || 0) * DEGREES;
  const cos = Math.cos(rotation);
  const sin = Math.sin(rotation);
  return localVertices.map((vertex) => ({ x: anchor.x + vertex.x * cos - vertex.y * sin, y: anchor.y + vertex.x * sin + vertex.y * cos }));
}

function rayPolygonIntersection(centerX: number, centerY: number, degrees: number, vertices: Point[]): Point {
  const directionX = Math.cos(degrees * DEGREES);
  const directionY = Math.sin(degrees * DEGREES);
  for (let index = 0; index < vertices.length; index++) {
    const start = vertices[index];
    const end = vertices[(index + 1) % vertices.length];
    const toCenterX = centerX - start.x;
    const toCenterY = centerY - start.y;
    const edgeX = end.x - start.x;
    const edgeY = end.y - start.y;
    const perpendicularX = -directionY;
    const perpendicularY = directionX;
    const denominator = edgeX * perpendicularX + edgeY * perpendicularY;
    if (Math.abs(denominator) < 1e-9) {
      continue;
    }
    const rayDistance = (edgeX * toCenterY - edgeY * toCenterX) / denominator;
    const edgeFraction = (toCenterX * perpendicularX + toCenterY * perpendicularY) / denominator;
    if (rayDistance >= 0 && edgeFraction >= 0 && edgeFraction <= 1) {
      return { x: centerX + directionX * rayDistance, y: centerY + directionY * rayDistance };
    }
  }
  return { x: centerX, y: centerY };
}

export function positionOnAnchor(anchor: OrbitAnchor, degrees: number): Point {
  const vertices = anchorShapeVertices(anchor);
  if (vertices) {
    return rayPolygonIntersection(anchor.x, anchor.y, degrees, vertices);
  }
  return { x: anchor.x + Math.cos(degrees * DEGREES) * anchor.radius, y: anchor.y + Math.sin(degrees * DEGREES) * anchor.radius };
}

export function snapAngleToTargets(degrees: number, targets: number[]): number {
  const normalized = normalizeDegrees(degrees);
  for (const target of targets) {
    const difference = Math.abs(normalized - normalizeDegrees(target));
    if (Math.min(difference, 360 - difference) <= ORBIT_CARDINAL_SNAP_DEGREES) {
      return target;
    }
  }
  return degrees;
}

function anchorSnapAngles(anchor: OrbitAnchor): number[] {
  const vertices = anchorShapeVertices(anchor);
  if (!vertices) {
    return ORBIT_CARDINAL_ANGLES;
  }
  return [...ORBIT_CARDINAL_ANGLES, ...vertices.map((vertex) => angleDegrees(anchor.x, anchor.y, vertex.x, vertex.y))];
}

function ringArcThroughPoint(anchor: OrbitAnchor, angleA: number, angleB: number): Point {
  const difference = ((((angleB - angleA + 180) % 360) + 360) % 360) - 180;
  return positionOnAnchor(anchor, angleA + difference / 2);
}

export function syncRingArcCurves(document: TreeDocument, anchor: OrbitAnchor): void {
  for (const node of document.nodes) {
    if (node.orbitAnchorId !== anchor.id) {
      continue;
    }
    for (const otherId of node.connectedTo) {
      if (node.id >= otherId) {
        continue;
      }
      const other = nodeById(document, otherId);
      if (!other || other.orbitAnchorId !== anchor.id) {
        continue;
      }
      const point = ringArcThroughPoint(anchor, node.orbitAngle ?? 0, other.orbitAngle ?? 0);
      setCurveControl(document, node.id, otherId, Math.round(point.x), Math.round(point.y));
    }
  }
}

export function snapNodeToAnchor(document: TreeDocument, node: TreeNode, anchor: OrbitAnchor, worldX: number, worldY: number): void {
  const degrees = snapAngleToTargets(angleDegrees(anchor.x, anchor.y, worldX, worldY), anchorSnapAngles(anchor));
  const position = positionOnAnchor(anchor, degrees);
  node.x = Math.round(position.x);
  node.y = Math.round(position.y);
  node.orbitAnchorId = anchor.id;
  node.orbitAngle = degrees;
  syncRingArcCurves(document, anchor);
}

export function detachNodeFromAnchor(node: TreeNode): void {
  delete node.orbitAnchorId;
  delete node.orbitAngle;
}

export function resyncNodesOnAnchor(document: TreeDocument, anchor: OrbitAnchor): void {
  for (const node of document.nodes) {
    if (node.orbitAnchorId !== anchor.id) {
      continue;
    }
    const position = positionOnAnchor(anchor, node.orbitAngle || 0);
    node.x = Math.round(position.x);
    node.y = Math.round(position.y);
  }
  syncRingArcCurves(document, anchor);
}

export function findSnapAnchor(document: TreeDocument, node: TreeNode, tentativeX: number, tentativeY: number): OrbitAnchor | null {
  let bestAnchor: OrbitAnchor | null = null;
  let bestDelta = Infinity;
  for (const anchor of document.anchors) {
    const distance = Math.hypot(tentativeX - anchor.x, tentativeY - anchor.y);
    const boundary = positionOnAnchor(anchor, angleDegrees(anchor.x, anchor.y, tentativeX, tentativeY));
    const localRadius = Math.hypot(boundary.x - anchor.x, boundary.y - anchor.y);
    const delta = Math.abs(distance - localRadius);
    const threshold = node.orbitAnchorId === anchor.id ? ORBIT_DETACH_RADIUS : ORBIT_SNAP_RADIUS;
    if (delta <= threshold && delta < bestDelta) {
      bestDelta = delta;
      bestAnchor = anchor;
    }
  }
  return bestAnchor;
}

export function computeAlignmentSnap(document: TreeDocument, node: TreeNode, tentativeX: number, tentativeY: number): Point | null {
  const neighbours = node.connectedTo.map((id) => nodeById(document, id)).filter((neighbour): neighbour is TreeNode => Boolean(neighbour));
  if (neighbours.length === 0) {
    return null;
  }
  let best: Point | null = null;
  let bestDistance = ALIGN_SNAP_THRESHOLD;
  const consider = (pointX: number, pointY: number) => {
    const distance = Math.hypot(tentativeX - pointX, tentativeY - pointY);
    if (distance < bestDistance) {
      bestDistance = distance;
      best = { x: pointX, y: pointY };
    }
  };
  for (let first = 0; first < neighbours.length; first++) {
    for (let second = first + 1; second < neighbours.length; second++) {
      const start = neighbours[first];
      const end = neighbours[second];
      const lineX = end.x - start.x;
      const lineY = end.y - start.y;
      const lengthSquared = lineX * lineX + lineY * lineY;
      if (lengthSquared < 1) {
        continue;
      }
      const along = ((tentativeX - start.x) * lineX + (tentativeY - start.y) * lineY) / lengthSquared;
      consider(start.x + along * lineX, start.y + along * lineY);
      const middleX = (start.x + end.x) / 2;
      const middleY = (start.y + end.y) / 2;
      const across = ((tentativeX - middleX) * -lineY + (tentativeY - middleY) * lineX) / lengthSquared;
      consider(middleX + across * -lineY, middleY + across * lineX);
    }
  }
  if (best) {
    return best;
  }
  let bestX: number | null = null;
  let bestXDistance = ALIGN_SNAP_THRESHOLD;
  let bestY: number | null = null;
  let bestYDistance = ALIGN_SNAP_THRESHOLD;
  for (const neighbour of neighbours) {
    const distanceX = Math.abs(tentativeX - neighbour.x);
    if (distanceX < bestXDistance) {
      bestXDistance = distanceX;
      bestX = neighbour.x;
    }
    const distanceY = Math.abs(tentativeY - neighbour.y);
    if (distanceY < bestYDistance) {
      bestYDistance = distanceY;
      bestY = neighbour.y;
    }
  }
  if (bestX !== null || bestY !== null) {
    return { x: bestX ?? tentativeX, y: bestY ?? tentativeY };
  }
  return null;
}

export function setAnchorShape(anchor: OrbitAnchor, shape: AnchorShape): void {
  if (shape === 'circle') {
    delete anchor.shape;
    delete anchor.aspectRatio;
    delete anchor.rotation;
    return;
  }
  anchor.shape = shape;
  if (shape === 'rectangle') {
    anchor.aspectRatio = anchor.aspectRatio || ANCHOR_RECTANGLE_ASPECT;
  } else {
    delete anchor.aspectRatio;
  }
}

export function deleteAnchor(document: TreeDocument, anchorId: string): void {
  document.anchors = document.anchors.filter((anchor) => anchor.id !== anchorId);
  for (const node of document.nodes) {
    if (node.orbitAnchorId === anchorId) {
      detachNodeFromAnchor(node);
    }
  }
}

const RADIUS_EXCLUDED_TYPE_ID = 'exoship';
const RADIUS_EXCLUDED_REGION_TAG = 'exotech';

export function countNodesWithinRadius(nodes: TreeNode[], typesById: Map<string, SkillType>, centre: TreeNode, distance: number): Map<Tier, number> {
  const limit = distance * distance;
  const counts = new Map<Tier, number>();
  for (const node of nodes) {
    if (node.id === centre.id || node.type === RADIUS_EXCLUDED_TYPE_ID || (node.tags ?? []).includes(RADIUS_EXCLUDED_REGION_TAG)) {
      continue;
    }
    if ((node.x - centre.x) ** 2 + (node.y - centre.y) ** 2 <= limit) {
      const tier = tierOf(typesById.get(node.type));
      counts.set(tier, (counts.get(tier) ?? 0) + 1);
    }
  }
  return counts;
}
