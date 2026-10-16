import { nodeById, type ConnectorCurve, type NodePair, type TreeDocument, type TreeNode } from './types.ts';

export function curveKey(a: string, b: string): string {
  return a < b ? `${a}|${b}` : `${b}|${a}`;
}

export function findCurve(document: Pick<TreeDocument, 'curves'>, a: string, b: string): ConnectorCurve | undefined {
  const key = curveKey(a, b);
  return document.curves.find((curve) => curveKey(curve.a, curve.b) === key);
}

export function defaultControlPoint(start: Pick<TreeNode, 'x' | 'y'>, end: Pick<TreeNode, 'x' | 'y'>): { x: number; y: number } {
  return { x: (start.x + end.x) / 2, y: (start.y + end.y) / 2 };
}

export function bezierHandleFor(throughX: number, throughY: number, startX: number, startY: number, endX: number, endY: number): { x: number; y: number } {
  return { x: 2 * throughX - (startX + endX) / 2, y: 2 * throughY - (startY + endY) / 2 };
}

export function setCurveControl(document: TreeDocument, a: string, b: string, controlX: number, controlY: number): void {
  const curve = findCurve(document, a, b);
  if (curve) {
    curve.controlX = controlX;
    curve.controlY = controlY;
  } else {
    document.curves.push({ a, b, controlX, controlY });
  }
}

export function removeCurve(document: TreeDocument, a: string, b: string): void {
  const key = curveKey(a, b);
  document.curves = document.curves.filter((curve) => curveKey(curve.a, curve.b) !== key);
}

export function snapCurveToStraightIfClose(document: TreeDocument, a: string, b: string): void {
  const curve = findCurve(document, a, b);
  const start = nodeById(document, a);
  const end = nodeById(document, b);
  if (!curve) {
    return;
  }
  const straight = start && end ? defaultControlPoint(start, end) : { x: 0, y: 0 };
  if (Math.hypot(curve.controlX - straight.x, curve.controlY - straight.y) < 8) {
    removeCurve(document, a, b);
  }
}

export function isConnectorHidden(document: Pick<TreeDocument, 'hiddenConnectors'>, a: string, b: string): boolean {
  const key = curveKey(a, b);
  return document.hiddenConnectors.some((pair) => curveKey(pair.a, pair.b) === key);
}

export function toggleConnectorHidden(document: TreeDocument, a: string, b: string): void {
  if (isConnectorHidden(document, a, b)) {
    const key = curveKey(a, b);
    document.hiddenConnectors = document.hiddenConnectors.filter((pair) => curveKey(pair.a, pair.b) !== key);
  } else {
    document.hiddenConnectors.push({ a, b });
  }
}

function removeEntriesForNodes(document: TreeDocument, nodeIds: Set<string>): void {
  document.curves = document.curves.filter((curve) => !nodeIds.has(curve.a) && !nodeIds.has(curve.b));
  document.hiddenConnectors = document.hiddenConnectors.filter((pair) => !nodeIds.has(pair.a) && !nodeIds.has(pair.b));
}

export function isLinkedPair(document: Pick<TreeDocument, 'nodes'>, a: string, b: string): boolean {
  const start = nodeById(document, a);
  const end = nodeById(document, b);
  return Boolean(start && end && (start.connectedTo.includes(b) || end.connectedTo.includes(a)));
}

export function linkedEntries<TEntry extends NodePair>(document: Pick<TreeDocument, 'nodes'>, entries: TEntry[]): TEntry[] {
  return entries.filter((entry) => isLinkedPair(document, entry.a, entry.b));
}

export function pruneConnectorEntries(document: TreeDocument): void {
  document.curves = linkedEntries(document, document.curves);
  document.hiddenConnectors = linkedEntries(document, document.hiddenConnectors);
}

export function renameNode(document: TreeDocument, node: TreeNode, newId: string): void {
  const oldId = node.id;
  removeEntriesForNodes(document, new Set([newId]));
  node.id = newId;
  for (const other of document.nodes) {
    other.connectedTo = other.connectedTo.map((connectedId) => (connectedId === oldId ? newId : connectedId));
    if (other.pairedWith === oldId) {
      other.pairedWith = newId;
    }
  }
  for (const entry of [...document.curves, ...document.hiddenConnectors]) {
    if (entry.a === oldId) {
      entry.a = newId;
    }
    if (entry.b === oldId) {
      entry.b = newId;
    }
  }
}

export function connectNodes(document: TreeDocument, node: TreeNode, otherId: string): void {
  const other = nodeById(document, otherId);
  if (!node.connectedTo.includes(otherId)) {
    node.connectedTo.push(otherId);
  }
  if (other && !other.connectedTo.includes(node.id)) {
    other.connectedTo.push(node.id);
  }
}

export function unlinkNodes(document: TreeDocument, node: TreeNode, otherId: string): void {
  node.connectedTo = node.connectedTo.filter((connectedId) => connectedId !== otherId);
  const other = nodeById(document, otherId);
  if (other) {
    other.connectedTo = other.connectedTo.filter((connectedId) => connectedId !== node.id);
  }
  removeCurve(document, node.id, otherId);
  const key = curveKey(node.id, otherId);
  document.hiddenConnectors = document.hiddenConnectors.filter((pair) => curveKey(pair.a, pair.b) !== key);
}

export function deleteNodes(document: TreeDocument, nodeIds: string[]): void {
  const removed = new Set(nodeIds);
  document.nodes = document.nodes.filter((node) => !removed.has(node.id));
  for (const node of document.nodes) {
    node.connectedTo = node.connectedTo.filter((connectedId) => !removed.has(connectedId));
    if (node.pairedWith && removed.has(node.pairedWith)) {
      delete node.pairedWith;
    }
  }
  removeEntriesForNodes(document, removed);
}

export function pairWormholes(document: TreeDocument, node: TreeNode, newPartnerId: string): void {
  const oldPartner = node.pairedWith ? nodeById(document, node.pairedWith) : undefined;
  if (oldPartner && oldPartner.pairedWith === node.id) {
    delete oldPartner.pairedWith;
  }
  if (!newPartnerId) {
    delete node.pairedWith;
    return;
  }
  node.pairedWith = newPartnerId;
  const newPartner = nodeById(document, newPartnerId);
  if (newPartner) {
    const priorPartner = newPartner.pairedWith ? nodeById(document, newPartner.pairedWith) : undefined;
    if (priorPartner && priorPartner.id !== node.id && priorPartner.pairedWith === newPartner.id) {
      delete priorPartner.pairedWith;
    }
    newPartner.pairedWith = node.id;
  }
}
