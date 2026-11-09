import type { Decoration } from '../model/decorations.ts';
import { paintImage, paintRingBelt } from './imagePainter.ts';
import type { PaintContext } from './paintContext.ts';
import { sceneryDrawOrder, type SceneryDraw } from './sceneryDepth.ts';
import { WHITE } from './sceneryMaths.ts';
import { paintStar } from './starPainter.ts';
import { createStarfield, placeFieldStar, STARFIELD_ACCENT_COLOUR, STARFIELD_SPRITE_PATHS, type Starfield } from './starfield.ts';
import { paintHypershunt, paintSerpent, paintShroudedMaw, paintThreatUnit } from './unitPainter.ts';

function paintDraw(context: PaintContext, { decoration, depth }: SceneryDraw): void {
  switch (decoration.kind) {
    case 'image':
      paintImage(context, decoration);
      return;
    case 'star':
      paintStar(context, decoration, depth);
      return;
    case 'ringBelt':
      paintRingBelt(context, decoration);
      return;
    case 'serpent':
      paintSerpent(context, decoration);
      return;
    case 'threatUnit':
      paintThreatUnit(context, decoration);
      return;
    case 'shroudedMaw':
      paintShroudedMaw(context, decoration);
      return;
    case 'hypershunt':
      paintHypershunt(context, decoration);
      return;
  }
}

interface DrawOrders {
  decorations: readonly Decoration[];
  behindTree: SceneryDraw[];
  overTree: SceneryDraw[];
}

export class SceneryPainter {
  private starfield: Starfield | null = null;
  private starfieldCanvasWidth = 0;
  private starfieldCanvasHeight = 0;
  private drawOrders: DrawOrders | null = null;

  paintBehindTree(context: PaintContext, decorations: readonly Decoration[]): void {
    this.paintStarfield(context);
    for (const draw of this.ordersFor(decorations).behindTree) {
      paintDraw(context, draw);
    }
  }

  paintOverTree(context: PaintContext, decorations: readonly Decoration[]): void {
    for (const draw of this.ordersFor(decorations).overTree) {
      paintDraw(context, draw);
    }
  }

  private ordersFor(decorations: readonly Decoration[]): DrawOrders {
    if (this.drawOrders?.decorations !== decorations) {
      this.drawOrders = { decorations, behindTree: sceneryDrawOrder(decorations, false), overTree: sceneryDrawOrder(decorations, true) };
    }
    return this.drawOrders;
  }

  private starfieldFor(canvasWidth: number, canvasHeight: number): Starfield {
    if (!this.starfield || this.starfieldCanvasWidth !== canvasWidth || this.starfieldCanvasHeight !== canvasHeight) {
      this.starfield = createStarfield(canvasWidth, canvasHeight);
      this.starfieldCanvasWidth = canvasWidth;
      this.starfieldCanvasHeight = canvasHeight;
    }
    return this.starfield;
  }

  private paintStarfield(context: PaintContext): void {
    const { viewport, gl, seconds } = context;
    const starfield = this.starfieldFor(viewport.canvasWidth, viewport.canvasHeight);
    const fieldLeft = (viewport.canvasWidth - starfield.fieldWidth) / 2;
    const fieldBottom = (viewport.canvasHeight - starfield.fieldHeight) / 2;
    STARFIELD_SPRITE_PATHS.forEach((spritePath, spriteIndex) => {
      const texture = gl.texture(spritePath);
      if (!texture) {
        return;
      }
      gl.use(texture, 'normal');
      for (const star of starfield.stars) {
        if (star.spriteIndex !== spriteIndex) {
          continue;
        }
        const placed = placeFieldStar(starfield, star, viewport.gamePanX, viewport.gamePanY, seconds);
        gl.colour(star.accent ? STARFIELD_ACCENT_COLOUR : WHITE, placed.alpha);
        gl.quadAtCenter(fieldLeft + placed.fieldX, fieldBottom + placed.fieldY, star.spriteSize, star.spriteSize, 0);
      }
    });
  }
}
