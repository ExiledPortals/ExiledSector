import { useEffect, useRef, useState, type RefObject } from 'react';
import { useSceneryData, type SceneryData } from '../../../api/scenery.ts';
import type { TreeStore } from '../state/treeStore.ts';
import { SceneryGl, webGl2Available } from './sceneryGl.ts';
import { parseHexColour, rgb, type Rgba } from './sceneryMaths.ts';
import { SceneryPainter } from './sceneryPainter.ts';
import { sceneryViewport } from './sceneryViewport.ts';

const MAX_FRAME_SECONDS = 0.1;
const FALLBACK_BACKGROUND: Rgba = rgb(10, 14, 21);

export interface SceneryTargets {
  frameRef: RefObject<HTMLDivElement | null>;
  behindTreeCanvasRef: RefObject<HTMLCanvasElement | null>;
  overTreeCanvasRef: RefObject<HTMLCanvasElement | null>;
}

function canvasBackground(frame: HTMLDivElement | null): Rgba {
  const declared = frame ? getComputedStyle(frame).getPropertyValue('--tree-canvas-bg').trim() : '';
  return parseHexColour(declared || null, FALLBACK_BACKGROUND);
}

export function useSceneryLoop(store: TreeStore, targets: SceneryTargets, enabled: boolean): boolean {
  const [painting, setPainting] = useState(false);
  const [contextGeneration, setContextGeneration] = useState(0);
  const sceneryData = useSceneryData(enabled && webGl2Available());
  const sceneryDataRef = useRef<SceneryData | undefined>(undefined);
  const { frameRef, behindTreeCanvasRef, overTreeCanvasRef } = targets;

  useEffect(() => {
    sceneryDataRef.current = sceneryData.data;
  }, [sceneryData.data]);

  useEffect(() => {
    const behindTreeCanvas = behindTreeCanvasRef.current;
    const overTreeCanvas = overTreeCanvasRef.current;
    const behindTreeGl = enabled && behindTreeCanvas && overTreeCanvas ? SceneryGl.create(behindTreeCanvas, true) : null;
    const overTreeGl = behindTreeGl && overTreeCanvas ? SceneryGl.create(overTreeCanvas, false) : null;
    if (!behindTreeGl || !overTreeGl || !behindTreeCanvas || !overTreeCanvas) {
      behindTreeGl?.dispose();
      setPainting(false);
      return;
    }
    setPainting(true);
    const painter = new SceneryPainter();
    const background = canvasBackground(frameRef.current);
    let elapsedSeconds = 0;
    let lastFrameAt = performance.now();
    let frameHandle = 0;
    let running = false;

    const paintFrame = (now: number) => {
      elapsedSeconds += Math.min(MAX_FRAME_SECONDS, Math.max(0, (now - lastFrameAt) / 1000));
      lastFrameAt = now;
      const { view, canvasSize, document: treeDocument } = store.getState();
      const pixelRatio = window.devicePixelRatio || 1;
      const viewport = sceneryViewport(view, canvasSize.width, canvasSize.height);
      const sceneryDataNow = sceneryDataRef.current;
      behindTreeGl.beginFrame(canvasSize.width, canvasSize.height, pixelRatio, background);
      painter.paintBehindTree({ gl: behindTreeGl, viewport, seconds: elapsedSeconds, sceneryData: sceneryDataNow }, treeDocument.decorations);
      behindTreeGl.endFrame();
      overTreeGl.beginFrame(canvasSize.width, canvasSize.height, pixelRatio, background);
      painter.paintOverTree({ gl: overTreeGl, viewport, seconds: elapsedSeconds, sceneryData: sceneryDataNow }, treeDocument.decorations);
      overTreeGl.endFrame();
      frameHandle = requestAnimationFrame(paintFrame);
    };
    const start = () => {
      if (!running && !window.document.hidden) {
        running = true;
        lastFrameAt = performance.now();
        frameHandle = requestAnimationFrame(paintFrame);
      }
    };
    const stop = () => {
      running = false;
      cancelAnimationFrame(frameHandle);
    };
    const followVisibility = () => (window.document.hidden ? stop() : start());
    const loseContext = (event: Event) => {
      event.preventDefault();
      stop();
      setPainting(false);
    };
    const restoreContext = () => setContextGeneration((generation) => generation + 1);

    window.document.addEventListener('visibilitychange', followVisibility);
    for (const canvas of [behindTreeCanvas, overTreeCanvas]) {
      canvas.addEventListener('webglcontextlost', loseContext);
      canvas.addEventListener('webglcontextrestored', restoreContext);
    }
    start();
    return () => {
      stop();
      window.document.removeEventListener('visibilitychange', followVisibility);
      for (const canvas of [behindTreeCanvas, overTreeCanvas]) {
        canvas.removeEventListener('webglcontextlost', loseContext);
        canvas.removeEventListener('webglcontextrestored', restoreContext);
      }
      behindTreeGl.dispose();
      overTreeGl.dispose();
    };
  }, [enabled, store, frameRef, behindTreeCanvasRef, overTreeCanvasRef, contextGeneration]);

  return enabled && painting && sceneryData.data !== undefined;
}
