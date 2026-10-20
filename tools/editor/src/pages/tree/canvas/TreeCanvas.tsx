import { useEffect, useMemo, useRef, useState, type MouseEvent as ReactMouseEvent, type PointerEvent as ReactPointerEvent } from 'react';
import { useStaticImages } from '../../../api/images.ts';
import { useNodeRadii } from '../../../api/nodeRadii.ts';
import { useVocabulary } from '../../../api/vocabulary.ts';
import { curveKey, findCurve, setCurveControl, snapCurveToStraightIfClose } from '../model/connectors.ts';
import {
  angleDegrees,
  computeAlignmentSnap,
  detachNodeFromAnchor,
  findSnapAnchor,
  normalizeDegrees,
  ORBIT_CARDINAL_ANGLES,
  resyncNodesOnAnchor,
  rotateAroundOrigin,
  setAnchorShape,
  snapAngleToTargets,
  snapNodeToAnchor,
  type Point,
} from '../model/geometry.ts';
import {
  ANCHOR_RECTANGLE_ASPECT,
  ANCHOR_SHAPES,
  anchorById,
  imageById,
  nodeById,
  starById,
  threatUnitById,
  tierOf,
  typeById,
  type ConnectorCurve,
  type TreeDocument,
} from '../model/types.ts';
import { brokenNodeIds } from '../model/validation.ts';
import { addAnchorAt, addImageAt, addStarAt, addThreatUnitAt, clearSelection, fitToScreen, selectOnly, toggleLink, toggleSelected, zoomAtPoint } from '../state/treeActions.ts';
import { editDocument, setSelection, useTreeStore, type HandleKind, type SelectionKind } from '../state/treeStore.ts';
import { CanvasOverlays } from './CanvasOverlays.tsx';
import classes from './TreeCanvas.module.css';
import {
  AnchorHandleLayer,
  AnchorRingLayer,
  EdgeLayer,
  IconClipDefinitions,
  ImageHandleLayer,
  ImageLayer,
  LabelLayer,
  NodeLayer,
  SocketRadiusLayer,
  StarHandleLayer,
  StarLayer,
  ThreatUnitHandleLayer,
  ThreatUnitLayer,
} from './TreeLayers.tsx';

interface StartPosition {
  id: string;
  x: number;
  y: number;
}

interface MoveDrag {
  kind: 'move';
  startWorld: Point;
  nodes: StartPosition[];
  anchors: StartPosition[];
  images: StartPosition[];
  stars: StartPosition[];
  threatUnits: StartPosition[];
  curves: ConnectorCurve[];
  moved: boolean;
}

type DragState =
  | { kind: 'pan'; startClientX: number; startClientY: number; startPanX: number; startPanY: number }
  | { kind: 'marquee'; startClientX: number; startClientY: number }
  | { kind: 'curve'; a: string; b: string }
  | { kind: HandleKind; id: string }
  | MoveDrag;

const MODE_CLASSES: Record<string, string | undefined> = {
  add: classes.modeAdd,
  addAnchor: classes.modeAddAnchor,
  addImage: classes.modeAddImage,
  addStar: classes.modeAddStar,
  addThreatUnit: classes.modeAddThreatUnit,
};

function positionsOf(items: { id: string; x: number; y: number }[], ids: string[]): StartPosition[] {
  return ids.flatMap((id) => {
    const item = items.find((candidate) => candidate.id === id);
    return item ? [{ id, x: item.x, y: item.y }] : [];
  });
}

function matchingNodeIds(document: TreeDocument, search: string): Set<string> | null {
  const needle = search.trim().toLowerCase();
  if (!needle) {
    return null;
  }
  const matches = new Set<string>();
  for (const node of document.nodes) {
    const type = typeById(document, node.type);
    const name = type ? type.name : node.type;
    const tagText = [...(node.tags ?? []), ...(type?.tags ?? [])].join(' ').toLowerCase();
    if (node.id.toLowerCase().includes(needle) || (name || '').toLowerCase().includes(needle) || tagText.includes(needle)) {
      matches.add(node.id);
    }
  }
  return matches;
}

function applyMove(drag: MoveDrag, world: Point): void {
  const deltaX = world.x - drag.startWorld.x;
  const deltaY = world.y - drag.startWorld.y;
  if (Math.abs(deltaX) > 0.5 || Math.abs(deltaY) > 0.5) {
    drag.moved = true;
  }
  if (!drag.moved) {
    return;
  }
  editDocument((document) => {
    for (const start of drag.curves) {
      const curve = findCurve(document, start.a, start.b);
      if (curve) {
        curve.controlX = Math.round(start.controlX + deltaX);
        curve.controlY = Math.round(start.controlY + deltaY);
      }
    }
    const singleNode = drag.nodes.length === 1 && drag.anchors.length === 0 && drag.images.length === 0 && drag.stars.length === 0 && drag.threatUnits.length === 0;
    if (singleNode) {
      const start = drag.nodes[0];
      const node = nodeById(document, start.id);
      if (!node) {
        return;
      }
      const tentativeX = start.x + deltaX;
      const tentativeY = start.y + deltaY;
      const anchor = findSnapAnchor(document, node, tentativeX, tentativeY);
      if (anchor) {
        snapNodeToAnchor(document, node, anchor, tentativeX, tentativeY);
        return;
      }
      if (node.orbitAnchorId) {
        detachNodeFromAnchor(node);
      }
      const aligned = computeAlignmentSnap(document, node, tentativeX, tentativeY);
      node.x = Math.round(aligned ? aligned.x : tentativeX);
      node.y = Math.round(aligned ? aligned.y : tentativeY);
      return;
    }
    const shift = (target: { x: number; y: number } | undefined, start: StartPosition) => {
      if (target) {
        target.x = Math.round(start.x + deltaX);
        target.y = Math.round(start.y + deltaY);
      }
    };
    drag.nodes.forEach((start) => shift(nodeById(document, start.id), start));
    drag.images.forEach((start) => shift(imageById(document, start.id), start));
    drag.stars.forEach((start) => shift(starById(document, start.id), start));
    drag.threatUnits.forEach((start) => shift(threatUnitById(document, start.id), start));
    for (const start of drag.anchors) {
      const anchor = anchorById(document, start.id);
      shift(anchor, start);
      if (anchor) {
        resyncNodesOnAnchor(document, anchor);
      }
    }
  });
}

function applyHandleDrag(kind: HandleKind, id: string, world: Point): void {
  editDocument((document) => {
    if (kind === 'anchorResize' || kind === 'anchorRotate') {
      const anchor = anchorById(document, id);
      if (!anchor) {
        return;
      }
      if (kind === 'anchorResize') {
        anchor.radius = Math.max(10, Math.round(Math.hypot(world.x - anchor.x, world.y - anchor.y)));
      } else {
        const rotation = snapAngleToTargets(angleDegrees(anchor.x, anchor.y, world.x, world.y) + 90, ORBIT_CARDINAL_ANGLES);
        anchor.rotation = Math.round(normalizeDegrees(rotation) * 10) / 10;
      }
      resyncNodesOnAnchor(document, anchor);
    } else if (kind === 'imageResize' || kind === 'imageRotate') {
      const image = imageById(document, id);
      if (!image) {
        return;
      }
      if (kind === 'imageResize') {
        const local = rotateAroundOrigin(world.x - image.x, world.y - image.y, -(image.rotation || 0));
        image.width = Math.max(20, Math.round(Math.abs(local.x) * 2));
        image.height = Math.max(20, Math.round(Math.abs(local.y) * 2));
      } else {
        image.rotation = Math.round(normalizeDegrees(angleDegrees(image.x, image.y, world.x, world.y) + 90));
      }
    } else if (kind === 'threatUnitResize' || kind === 'threatUnitRotate') {
      const threatUnit = threatUnitById(document, id);
      if (!threatUnit) {
        return;
      }
      if (kind === 'threatUnitResize') {
        threatUnit.length = Math.max(20, Math.round(Math.hypot(world.x - threatUnit.x, world.y - threatUnit.y) * 2));
      } else {
        threatUnit.rotation = Math.round(normalizeDegrees(angleDegrees(threatUnit.x, threatUnit.y, world.x, world.y) + 90));
      }
    } else {
      const star = starById(document, id);
      if (star) {
        star.radius = Math.max(20, Math.round(Math.hypot(world.x - star.x, world.y - star.y)));
      }
    }
  });
}

function cycleAnchorShape(anchorId: string): void {
  editDocument((document) => {
    const anchor = anchorById(document, anchorId);
    if (!anchor) {
      return;
    }
    const nextShape = ANCHOR_SHAPES[(ANCHOR_SHAPES.indexOf(anchor.shape ?? 'circle') + 1) % ANCHOR_SHAPES.length];
    setAnchorShape(anchor, nextShape);
    if (nextShape === 'rectangle') {
      anchor.aspectRatio = ANCHOR_RECTANGLE_ASPECT;
    }
    resyncNodesOnAnchor(document, anchor);
  });
}

interface TreeCanvasProps {
  onRequestAddNode: (x: number, y: number) => void;
}

export function TreeCanvas({ onRequestAddNode }: TreeCanvasProps) {
  const frameRef = useRef<HTMLDivElement>(null);
  const svgRef = useRef<SVGSVGElement>(null);
  const dragRef = useRef<DragState | null>(null);
  const measuredRef = useRef(false);
  const staticImages = useStaticImages();
  const vocabulary = useVocabulary();
  const nodeRadii = useNodeRadii(vocabulary.data?.radiusSizes);
  const [hoveredNodeId, setHoveredNodeId] = useState<string | null>(null);

  const document = useTreeStore((state) => state.document);
  const view = useTreeStore((state) => state.view);
  const canvasSize = useTreeStore((state) => state.canvasSize);
  const mode = useTreeStore((state) => state.mode);
  const selection = useTreeStore((state) => state.selection);
  const showLabels = useTreeStore((state) => state.showLabels);
  const search = useTreeStore((state) => state.search);
  const activeDrag = useTreeStore((state) => state.activeDrag);
  const fitRequested = useTreeStore((state) => state.fitRequested);

  const matchIds = useMemo(() => matchingNodeIds(document, search), [document, search]);
  const brokenIds = useMemo(() => brokenNodeIds(document), [document]);
  const hoveredSocket = useMemo(() => {
    const node = hoveredNodeId ? nodeById(document, hoveredNodeId) : undefined;
    return node && tierOf(typeById(document, node.type)) === 'SOCKET' ? node : null;
  }, [document, hoveredNodeId]);

  useEffect(() => {
    const frame = frameRef.current;
    if (!frame) {
      return;
    }
    const observer = new ResizeObserver(() => {
      useTreeStore.setState({ canvasSize: { width: frame.clientWidth, height: frame.clientHeight } });
      measuredRef.current = true;
      if (useTreeStore.getState().fitRequested) {
        fitToScreen();
        useTreeStore.setState({ fitRequested: false });
      }
    });
    observer.observe(frame);
    const zoomWithWheel = (event: WheelEvent) => {
      event.preventDefault();
      const rect = frame.getBoundingClientRect();
      zoomAtPoint(event.deltaY < 0 ? 1.1 : 0.9, event.clientX - rect.left, event.clientY - rect.top);
    };
    frame.addEventListener('wheel', zoomWithWheel, { passive: false });
    return () => {
      observer.disconnect();
      frame.removeEventListener('wheel', zoomWithWheel);
    };
  }, []);

  useEffect(() => {
    if (fitRequested && measuredRef.current) {
      fitToScreen();
      useTreeStore.setState({ fitRequested: false });
    }
  }, [fitRequested]);

  useEffect(() => {
    const screenToWorld = (clientX: number, clientY: number): Point => {
      const rect = frameRef.current?.getBoundingClientRect();
      const { view: currentView } = useTreeStore.getState();
      const left = rect?.left ?? 0;
      const top = rect?.top ?? 0;
      const centerX = (rect?.width ?? 0) / 2 + currentView.panX;
      const centerY = (rect?.height ?? 0) / 2 + currentView.panY;
      return { x: (clientX - left - centerX) / currentView.zoom, y: (clientY - top - centerY) / currentView.zoom };
    };

    const onPointerMove = (event: PointerEvent) => {
      const drag = dragRef.current;
      if (!drag) {
        return;
      }
      if (drag.kind === 'marquee') {
        const rect = frameRef.current?.getBoundingClientRect();
        useTreeStore.setState({
          activeDrag: {
            kind: 'marquee',
            startX: drag.startClientX - (rect?.left ?? 0),
            startY: drag.startClientY - (rect?.top ?? 0),
            currentX: event.clientX - (rect?.left ?? 0),
            currentY: event.clientY - (rect?.top ?? 0),
          },
        });
        return;
      }
      if (drag.kind === 'pan') {
        useTreeStore.setState((state) => ({
          view: { ...state.view, panX: drag.startPanX + (event.clientX - drag.startClientX), panY: drag.startPanY + (event.clientY - drag.startClientY) },
        }));
        return;
      }
      const world = screenToWorld(event.clientX, event.clientY);
      if (drag.kind === 'curve') {
        editDocument((draft) => setCurveControl(draft, drag.a, drag.b, Math.round(world.x), Math.round(world.y)));
      } else if (drag.kind === 'move') {
        applyMove(drag, world);
      } else {
        applyHandleDrag(drag.kind, drag.id, world);
      }
    };

    const finishMarquee = (drag: { startClientX: number; startClientY: number }, event: PointerEvent) => {
      const start = screenToWorld(drag.startClientX, drag.startClientY);
      const end = screenToWorld(event.clientX, event.clientY);
      const inside = (item: { x: number; y: number }) =>
        item.x >= Math.min(start.x, end.x) && item.x <= Math.max(start.x, end.x) && item.y >= Math.min(start.y, end.y) && item.y <= Math.max(start.y, end.y);
      const current = useTreeStore.getState().document;
      setSelection({
        nodes: current.nodes.filter(inside).map((node) => node.id),
        anchors: current.anchors.filter(inside).map((anchor) => anchor.id),
        images: current.images.filter(inside).map((image) => image.id),
        stars: current.stars.filter(inside).map((star) => star.id),
        threatUnits: current.threatUnits.filter(inside).map((threatUnit) => threatUnit.id),
      });
    };

    const onPointerUp = (event: PointerEvent) => {
      const drag = dragRef.current;
      dragRef.current = null;
      if (!drag) {
        return;
      }
      if (drag.kind === 'marquee') {
        finishMarquee(drag, event);
      } else if (drag.kind === 'curve') {
        editDocument((draft) => snapCurveToStraightIfClose(draft, drag.a, drag.b));
      }
      useTreeStore.setState({ activeDrag: null });
    };

    window.addEventListener('pointermove', onPointerMove);
    window.addEventListener('pointerup', onPointerUp);
    window.addEventListener('pointercancel', onPointerUp);
    return () => {
      window.removeEventListener('pointermove', onPointerMove);
      window.removeEventListener('pointerup', onPointerUp);
      window.removeEventListener('pointercancel', onPointerUp);
    };
  }, []);

  const worldPoint = (clientX: number, clientY: number): Point => {
    const rect = frameRef.current?.getBoundingClientRect();
    const centerX = (rect?.width ?? 0) / 2 + view.panX;
    const centerY = (rect?.height ?? 0) / 2 + view.panY;
    return { x: (clientX - (rect?.left ?? 0) - centerX) / view.zoom, y: (clientY - (rect?.top ?? 0) - centerY) / view.zoom };
  };

  const beginDrag = (drag: DragState, event: ReactPointerEvent) => {
    dragRef.current = drag;
    svgRef.current?.setPointerCapture(event.pointerId);
  };

  const beginMove = (event: ReactPointerEvent) => {
    const { selection: current, document: currentDocument } = useTreeStore.getState();
    const nodeIds = new Set(current.nodes);
    beginDrag(
      {
        kind: 'move',
        startWorld: worldPoint(event.clientX, event.clientY),
        nodes: positionsOf(currentDocument.nodes, current.nodes),
        anchors: positionsOf(currentDocument.anchors, current.anchors),
        images: positionsOf(currentDocument.images, current.images),
        stars: positionsOf(currentDocument.stars, current.stars),
        threatUnits: positionsOf(currentDocument.threatUnits, current.threatUnits),
        curves: currentDocument.curves.filter((curve) => nodeIds.has(curve.a) || nodeIds.has(curve.b)).map((curve) => ({ ...curve })),
        moved: false,
      },
      event,
    );
    useTreeStore.setState({ activeDrag: { kind: 'move', anchorIds: current.anchors, imageIds: current.images, starIds: current.stars, threatUnitIds: current.threatUnits } });
  };

  const pressSelectable = (kind: SelectionKind, id: string, event: ReactPointerEvent) => {
    if (event.shiftKey) {
      toggleSelected(kind, id);
      return;
    }
    if (!useTreeStore.getState().selection[kind].includes(id)) {
      selectOnly(kind, id);
    }
    beginMove(event);
  };

  const pressHandle = (kind: HandleKind, id: string, event: ReactPointerEvent) => {
    beginDrag({ kind, id }, event);
    useTreeStore.setState({ activeDrag: { kind, id } });
  };

  const pressBackground = (event: ReactPointerEvent) => {
    const world = worldPoint(event.clientX, event.clientY);
    const x = Math.round(world.x);
    const y = Math.round(world.y);
    if (mode === 'add') {
      onRequestAddNode(x, y);
    } else if (mode === 'addAnchor') {
      addAnchorAt(x, y);
    } else if (mode === 'addImage') {
      addImageAt(x, y, staticImages.data?.[0] ?? null);
    } else if (mode === 'addStar') {
      addStarAt(x, y);
    } else if (mode === 'addThreatUnit') {
      addThreatUnitAt(x, y, vocabulary.data);
    } else if (event.shiftKey) {
      beginDrag({ kind: 'marquee', startClientX: event.clientX, startClientY: event.clientY }, event);
    } else {
      clearSelection();
      beginDrag({ kind: 'pan', startClientX: event.clientX, startClientY: event.clientY, startPanX: view.panX, startPanY: view.panY }, event);
    }
  };

  const onPointerDown = (event: ReactPointerEvent<SVGSVGElement>) => {
    if (event.button === 2) {
      return;
    }
    const target = event.target as SVGElement;
    const data = target.dataset;
    if (data.nodeId) {
      if (mode === 'add') {
        return;
      }
      const selectedNodes = useTreeStore.getState().selection.nodes;
      if ((event.ctrlKey || event.metaKey) && selectedNodes.length === 1 && selectedNodes[0] !== data.nodeId) {
        toggleLink(selectedNodes[0], data.nodeId);
        return;
      }
      pressSelectable('nodes', data.nodeId, event);
    } else if (data.curveA && data.curveB) {
      beginDrag({ kind: 'curve', a: data.curveA, b: data.curveB }, event);
      useTreeStore.setState({ activeDrag: { kind: 'curve', key: curveKey(data.curveA, data.curveB) } });
    } else if (data.anchorId) {
      pressSelectable('anchors', data.anchorId, event);
    } else if (data.imageId) {
      pressSelectable('images', data.imageId, event);
    } else if (data.starId) {
      pressSelectable('stars', data.starId, event);
    } else if (data.threatUnitId) {
      pressSelectable('threatUnits', data.threatUnitId, event);
    } else if (data.anchorResize) {
      pressHandle('anchorResize', data.anchorResize, event);
    } else if (data.anchorRotate) {
      pressHandle('anchorRotate', data.anchorRotate, event);
    } else if (data.imageResize) {
      pressHandle('imageResize', data.imageResize, event);
    } else if (data.imageRotate) {
      pressHandle('imageRotate', data.imageRotate, event);
    } else if (data.starResize) {
      pressHandle('starResize', data.starResize, event);
    } else if (data.threatUnitResize) {
      pressHandle('threatUnitResize', data.threatUnitResize, event);
    } else if (data.threatUnitRotate) {
      pressHandle('threatUnitRotate', data.threatUnitRotate, event);
    } else if (!data.anchorShapeCycle && target === svgRef.current) {
      pressBackground(event);
    }
  };

  const onPointerOver = (event: ReactPointerEvent<SVGSVGElement>) => {
    if (!dragRef.current) {
      setHoveredNodeId((event.target as SVGElement).dataset.nodeId ?? null);
    }
  };

  const onClick = (event: ReactMouseEvent<SVGSVGElement>) => {
    const anchorId = (event.target as SVGElement).dataset.anchorShapeCycle;
    if (anchorId) {
      cycleAnchorShape(anchorId);
    }
  };

  const showAnchorHandles = mode === 'select' || mode === 'addAnchor';
  const showImageHandles = mode === 'select' || mode === 'addImage';
  const showStarHandles = mode === 'select' || mode === 'addStar';
  const showThreatUnitHandles = mode === 'select' || mode === 'addThreatUnit';
  const rootTransform = `translate(${canvasSize.width / 2 + view.panX},${canvasSize.height / 2 + view.panY}) scale(${view.zoom})`;
  const marquee = activeDrag?.kind === 'marquee' ? activeDrag : null;

  return (
    <div ref={frameRef} className={`${classes.canvasFrame} ${MODE_CLASSES[mode] ?? ''}`}>
      <svg ref={svgRef} className={classes.stage} onPointerDown={onPointerDown} onPointerOver={onPointerOver} onPointerLeave={() => setHoveredNodeId(null)} onClick={onClick} onContextMenu={(event) => event.preventDefault()}>
        <IconClipDefinitions />
        <g transform={rootTransform}>
          <StarLayer stars={document.stars} />
          <ThreatUnitLayer threatUnits={document.threatUnits} selectedIds={selection.threatUnits} />
          <ImageLayer images={document.images} />
          <AnchorRingLayer anchors={document.anchors} selectedIds={selection.anchors} />
          <EdgeLayer
            nodes={document.nodes}
            curves={document.curves}
            hiddenConnectors={document.hiddenConnectors}
            showCurveHandles={mode === 'select'}
            activeCurveKey={activeDrag?.kind === 'curve' ? activeDrag.key : null}
          />
          {hoveredSocket && nodeRadii.data ? (
            <SocketRadiusLayer socket={hoveredSocket} nodes={document.nodes} types={document.types} radii={nodeRadii.data} zoom={view.zoom} />
          ) : null}
          <NodeLayer nodes={document.nodes} types={document.types} selectedIds={selection.nodes} matchIds={matchIds} brokenIds={brokenIds} />
          {showLabels ? <LabelLayer nodes={document.nodes} types={document.types} matchIds={matchIds} /> : null}
          {showAnchorHandles ? <AnchorHandleLayer items={document.anchors} selectedIds={selection.anchors} activeDrag={activeDrag} /> : null}
          {showImageHandles ? <ImageHandleLayer items={document.images} selectedIds={selection.images} activeDrag={activeDrag} /> : null}
          {showStarHandles ? <StarHandleLayer items={document.stars} selectedIds={selection.stars} activeDrag={activeDrag} /> : null}
          {showThreatUnitHandles ? <ThreatUnitHandleLayer items={document.threatUnits} selectedIds={selection.threatUnits} activeDrag={activeDrag} /> : null}
        </g>
        {marquee ? (
          <rect
            className={classes.marquee}
            x={Math.min(marquee.startX, marquee.currentX)}
            y={Math.min(marquee.startY, marquee.currentY)}
            width={Math.abs(marquee.currentX - marquee.startX)}
            height={Math.abs(marquee.currentY - marquee.startY)}
          />
        ) : null}
      </svg>
      <CanvasOverlays />
    </div>
  );
}
