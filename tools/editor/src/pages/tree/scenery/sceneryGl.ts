import { fileUrl } from '../../../shared/imagePaths.ts';
import type { Rgba } from './sceneryMaths.ts';

export type SceneryBlend = 'normal' | 'additive';
export type TextureWrap = 'clamp' | 'repeat';

export interface SceneryTexture {
  glTexture: WebGLTexture;
  imageWidth: number;
  imageHeight: number;
}

type TextureSlot = SceneryTexture | 'loading' | 'failed';

const FLOATS_PER_VERTEX = 8;
const BATCH_VERTICES = 6 * 8192;

const VERTEX_SHADER = `#version 300 es
in vec2 screenPosition;
in vec2 textureCoordinate;
in vec4 vertexColour;
uniform vec2 canvasSize;
out vec2 fragmentTextureCoordinate;
out vec4 fragmentColour;
void main() {
  gl_Position = vec4(screenPosition / canvasSize * 2.0 - 1.0, 0.0, 1.0);
  fragmentTextureCoordinate = textureCoordinate;
  fragmentColour = vertexColour;
}`;

const FRAGMENT_SHADER = `#version 300 es
precision highp float;
in vec2 fragmentTextureCoordinate;
in vec4 fragmentColour;
uniform sampler2D spriteTexture;
out vec4 outputColour;
void main() {
  outputColour = texture(spriteTexture, fragmentTextureCoordinate) * fragmentColour;
}`;

function compiledShader(gl: WebGL2RenderingContext, type: number, source: string): WebGLShader | null {
  const shader = gl.createShader(type);
  if (!shader) {
    return null;
  }
  gl.shaderSource(shader, source);
  gl.compileShader(shader);
  if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
    gl.deleteShader(shader);
    return null;
  }
  return shader;
}

function linkedProgram(gl: WebGL2RenderingContext): WebGLProgram | null {
  const vertexShader = compiledShader(gl, gl.VERTEX_SHADER, VERTEX_SHADER);
  const fragmentShader = compiledShader(gl, gl.FRAGMENT_SHADER, FRAGMENT_SHADER);
  const program = vertexShader && fragmentShader ? gl.createProgram() : null;
  if (!program || !vertexShader || !fragmentShader) {
    return null;
  }
  gl.attachShader(program, vertexShader);
  gl.attachShader(program, fragmentShader);
  gl.linkProgram(program);
  gl.deleteShader(vertexShader);
  gl.deleteShader(fragmentShader);
  return gl.getProgramParameter(program, gl.LINK_STATUS) ? program : null;
}

export function webGl2Available(): boolean {
  return typeof window !== 'undefined' && typeof window.WebGL2RenderingContext !== 'undefined';
}

export class SceneryGl {
  private readonly gl: WebGL2RenderingContext;
  private readonly program: WebGLProgram;
  private readonly vertexBuffer: WebGLBuffer;
  private readonly canvasSizeLocation: WebGLUniformLocation | null;
  private readonly vertexData = new Float32Array(BATCH_VERTICES * FLOATS_PER_VERTEX);
  private readonly textures = new Map<string, TextureSlot>();
  private readonly opaque: boolean;
  private vertexCount = 0;
  private batchTexture: SceneryTexture | null = null;
  private batchBlend: SceneryBlend = 'normal';
  private colourRed = 1;
  private colourGreen = 1;
  private colourBlue = 1;
  private colourAlpha = 1;
  private readonly stripPrevious = new Float64Array(8);
  private stripStarted = false;
  private disposed = false;
  private cssWidth = 1;
  private cssHeight = 1;

  private constructor(gl: WebGL2RenderingContext, program: WebGLProgram, vertexBuffer: WebGLBuffer, opaque: boolean) {
    this.gl = gl;
    this.program = program;
    this.vertexBuffer = vertexBuffer;
    this.opaque = opaque;
    this.canvasSizeLocation = gl.getUniformLocation(program, 'canvasSize');
    gl.useProgram(program);
    gl.bindBuffer(gl.ARRAY_BUFFER, vertexBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, this.vertexData.byteLength, gl.DYNAMIC_DRAW);
    const stride = FLOATS_PER_VERTEX * 4;
    const attributes: [string, number, number][] = [
      ['screenPosition', 2, 0],
      ['textureCoordinate', 2, 2],
      ['vertexColour', 4, 4],
    ];
    for (const [name, size, offset] of attributes) {
      const location = gl.getAttribLocation(program, name);
      gl.enableVertexAttribArray(location);
      gl.vertexAttribPointer(location, size, gl.FLOAT, false, stride, offset * 4);
    }
    gl.uniform1i(gl.getUniformLocation(program, 'spriteTexture'), 0);
    gl.enable(gl.BLEND);
    gl.disable(gl.DEPTH_TEST);
    gl.disable(gl.CULL_FACE);
  }

  static create(canvas: HTMLCanvasElement, opaque: boolean): SceneryGl | null {
    if (!webGl2Available()) {
      return null;
    }
    const gl = canvas.getContext('webgl2', { alpha: !opaque, antialias: true, premultipliedAlpha: true, preserveDrawingBuffer: false });
    if (!gl) {
      return null;
    }
    const program = linkedProgram(gl);
    const vertexBuffer = program ? gl.createBuffer() : null;
    if (!program || !vertexBuffer) {
      return null;
    }
    return new SceneryGl(gl, program, vertexBuffer, opaque);
  }

  dispose(): void {
    this.disposed = true;
    for (const slot of this.textures.values()) {
      if (typeof slot === 'object') {
        this.gl.deleteTexture(slot.glTexture);
      }
    }
    this.textures.clear();
    this.gl.deleteBuffer(this.vertexBuffer);
    this.gl.deleteProgram(this.program);
  }

  beginFrame(cssWidth: number, cssHeight: number, pixelRatio: number, clearColour: Rgba): void {
    const canvas = this.gl.canvas as HTMLCanvasElement;
    const bufferWidth = Math.max(1, Math.round(cssWidth * pixelRatio));
    const bufferHeight = Math.max(1, Math.round(cssHeight * pixelRatio));
    if (canvas.width !== bufferWidth || canvas.height !== bufferHeight) {
      canvas.width = bufferWidth;
      canvas.height = bufferHeight;
    }
    this.cssWidth = Math.max(1, cssWidth);
    this.cssHeight = Math.max(1, cssHeight);
    const gl = this.gl;
    gl.viewport(0, 0, bufferWidth, bufferHeight);
    gl.useProgram(this.program);
    gl.uniform2f(this.canvasSizeLocation, this.cssWidth, this.cssHeight);
    if (this.opaque) {
      gl.clearColor(clearColour.red / 255, clearColour.green / 255, clearColour.blue / 255, 1);
    } else {
      gl.clearColor(0, 0, 0, 0);
    }
    gl.clear(gl.COLOR_BUFFER_BIT);
    this.vertexCount = 0;
    this.batchTexture = null;
  }

  endFrame(): void {
    this.flush();
  }

  texture(path: string, wrap: TextureWrap = 'clamp'): SceneryTexture | null {
    const key = `${wrap}:${path}`;
    const slot = this.textures.get(key);
    if (slot === undefined) {
      this.textures.set(key, 'loading');
      this.load(key, path, wrap);
      return null;
    }
    return typeof slot === 'object' ? slot : null;
  }

  private load(key: string, path: string, wrap: TextureWrap): void {
    const image = new Image();
    image.onload = () => {
      if (this.disposed) {
        return;
      }
      const gl = this.gl;
      const glTexture = gl.createTexture();
      if (!glTexture) {
        this.textures.set(key, 'failed');
        return;
      }
      this.flush();
      gl.bindTexture(gl.TEXTURE_2D, glTexture);
      gl.pixelStorei(gl.UNPACK_FLIP_Y_WEBGL, true);
      gl.pixelStorei(gl.UNPACK_PREMULTIPLY_ALPHA_WEBGL, false);
      gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, image);
      gl.generateMipmap(gl.TEXTURE_2D);
      gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR_MIPMAP_LINEAR);
      gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
      const glWrap = wrap === 'repeat' ? gl.REPEAT : gl.CLAMP_TO_EDGE;
      gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, glWrap);
      gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, glWrap);
      this.textures.set(key, { glTexture, imageWidth: image.naturalWidth, imageHeight: image.naturalHeight });
    };
    image.onerror = () => this.textures.set(key, 'failed');
    image.src = fileUrl(path);
  }

  use(texture: SceneryTexture, blend: SceneryBlend): void {
    if (texture !== this.batchTexture || blend !== this.batchBlend) {
      this.flush();
      this.batchTexture = texture;
      this.batchBlend = blend;
    }
  }

  colour(tint: Rgba, alphaMult: number): void {
    this.colourRed = tint.red / 255;
    this.colourGreen = tint.green / 255;
    this.colourBlue = tint.blue / 255;
    this.colourAlpha = Math.max(0, Math.min(1, (tint.alpha * alphaMult) / 255));
  }

  private vertex(x: number, y: number, texX: number, texY: number): void {
    const offset = this.vertexCount * FLOATS_PER_VERTEX;
    const data = this.vertexData;
    data[offset] = x;
    data[offset + 1] = y;
    data[offset + 2] = texX;
    data[offset + 3] = texY;
    data[offset + 4] = this.colourRed;
    data[offset + 5] = this.colourGreen;
    data[offset + 6] = this.colourBlue;
    data[offset + 7] = this.colourAlpha;
    this.vertexCount++;
  }

  quadThrough(
    firstX: number, firstY: number, firstTexX: number, firstTexY: number,
    secondX: number, secondY: number, secondTexX: number, secondTexY: number,
    thirdX: number, thirdY: number, thirdTexX: number, thirdTexY: number,
    fourthX: number, fourthY: number, fourthTexX: number, fourthTexY: number,
  ): void {
    this.reserve(6);
    this.vertex(firstX, firstY, firstTexX, firstTexY);
    this.vertex(secondX, secondY, secondTexX, secondTexY);
    this.vertex(thirdX, thirdY, thirdTexX, thirdTexY);
    this.vertex(firstX, firstY, firstTexX, firstTexY);
    this.vertex(thirdX, thirdY, thirdTexX, thirdTexY);
    this.vertex(fourthX, fourthY, fourthTexX, fourthTexY);
  }

  quadAtCenter(centerX: number, centerY: number, quadWidth: number, quadHeight: number, angleDegrees: number, texLeft = 0, texBottom = 0, texRight = 1, texTop = 1): void {
    const halfWidth = quadWidth / 2;
    const halfHeight = quadHeight / 2;
    const angle = (angleDegrees * Math.PI) / 180;
    const cos = Math.cos(angle);
    const sin = Math.sin(angle);
    const leftBottomX = centerX - halfWidth * cos + halfHeight * sin;
    const leftBottomY = centerY - halfWidth * sin - halfHeight * cos;
    const leftTopX = centerX - halfWidth * cos - halfHeight * sin;
    const leftTopY = centerY - halfWidth * sin + halfHeight * cos;
    const rightTopX = centerX + halfWidth * cos - halfHeight * sin;
    const rightTopY = centerY + halfWidth * sin + halfHeight * cos;
    const rightBottomX = centerX + halfWidth * cos + halfHeight * sin;
    const rightBottomY = centerY + halfWidth * sin - halfHeight * cos;
    this.reserve(6);
    this.vertex(leftBottomX, leftBottomY, texLeft, texBottom);
    this.vertex(leftTopX, leftTopY, texLeft, texTop);
    this.vertex(rightTopX, rightTopY, texRight, texTop);
    this.vertex(leftBottomX, leftBottomY, texLeft, texBottom);
    this.vertex(rightTopX, rightTopY, texRight, texTop);
    this.vertex(rightBottomX, rightBottomY, texRight, texBottom);
  }

  beginStrip(): void {
    this.stripStarted = false;
  }

  stripPair(innerX: number, innerY: number, innerTexX: number, innerTexY: number, outerX: number, outerY: number, outerTexX: number, outerTexY: number): void {
    const previous = this.stripPrevious;
    if (this.stripStarted) {
      this.reserve(6);
      this.vertex(previous[0], previous[1], previous[2], previous[3]);
      this.vertex(previous[4], previous[5], previous[6], previous[7]);
      this.vertex(outerX, outerY, outerTexX, outerTexY);
      this.vertex(previous[0], previous[1], previous[2], previous[3]);
      this.vertex(outerX, outerY, outerTexX, outerTexY);
      this.vertex(innerX, innerY, innerTexX, innerTexY);
    }
    previous[0] = innerX;
    previous[1] = innerY;
    previous[2] = innerTexX;
    previous[3] = innerTexY;
    previous[4] = outerX;
    previous[5] = outerY;
    previous[6] = outerTexX;
    previous[7] = outerTexY;
    this.stripStarted = true;
  }

  private reserve(vertices: number): void {
    if (this.vertexCount + vertices > BATCH_VERTICES) {
      this.flush();
    }
  }

  private flush(): void {
    const texture = this.batchTexture;
    if (this.vertexCount === 0 || !texture) {
      this.vertexCount = 0;
      return;
    }
    const gl = this.gl;
    gl.useProgram(this.program);
    gl.bindBuffer(gl.ARRAY_BUFFER, this.vertexBuffer);
    gl.bufferSubData(gl.ARRAY_BUFFER, 0, this.vertexData, 0, this.vertexCount * FLOATS_PER_VERTEX);
    gl.activeTexture(gl.TEXTURE0);
    gl.bindTexture(gl.TEXTURE_2D, texture.glTexture);
    if (this.batchBlend === 'additive') {
      gl.blendFuncSeparate(gl.SRC_ALPHA, gl.ONE, gl.ONE, gl.ONE);
    } else {
      gl.blendFuncSeparate(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA, gl.ONE, gl.ONE_MINUS_SRC_ALPHA);
    }
    gl.drawArrays(gl.TRIANGLES, 0, this.vertexCount);
    this.vertexCount = 0;
  }
}
