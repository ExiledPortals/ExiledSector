import { toRadians } from './sceneryMaths.ts';

export const SPHERE_DETAIL = 32;

export type Matrix3 = [number, number, number, number, number, number, number, number, number];

function multiply(first: Matrix3, second: Matrix3): Matrix3 {
  const product = new Array<number>(9).fill(0) as Matrix3;
  for (let row = 0; row < 3; row++) {
    for (let column = 0; column < 3; column++) {
      let sum = 0;
      for (let step = 0; step < 3; step++) {
        sum += first[row * 3 + step] * second[step * 3 + column];
      }
      product[row * 3 + column] = sum;
    }
  }
  return product;
}

function rotationAboutX(degrees: number): Matrix3 {
  const cos = Math.cos(toRadians(degrees));
  const sin = Math.sin(toRadians(degrees));
  return [1, 0, 0, 0, cos, -sin, 0, sin, cos];
}

function rotationAboutY(degrees: number): Matrix3 {
  const cos = Math.cos(toRadians(degrees));
  const sin = Math.sin(toRadians(degrees));
  return [cos, 0, sin, 0, 1, 0, -sin, 0, cos];
}

function rotationAboutZ(degrees: number): Matrix3 {
  const cos = Math.cos(toRadians(degrees));
  const sin = Math.sin(toRadians(degrees));
  return [cos, -sin, 0, sin, cos, 0, 0, 0, 1];
}

export function sphereOrientation(tiltDegrees: number, pitchDegrees: number, spinDegrees: number): Matrix3 {
  return [rotationAboutX(pitchDegrees), rotationAboutY(spinDegrees), rotationAboutX(-90)].reduce(multiply, rotationAboutZ(tiltDegrees));
}

export interface ProjectedSphere {
  detail: number;
  unitX: Float64Array;
  unitY: Float64Array;
  texS: Float64Array;
  texT: Float64Array;
  quadShown: Uint8Array;
}

export function gridIndex(detail: number, stack: number, slice: number): number {
  return stack * (detail + 1) + slice;
}

interface UnitSphereGrid {
  positions: Float64Array;
  texS: Float64Array;
  texT: Float64Array;
}

const GRIDS_BY_DETAIL = new Map<number, UnitSphereGrid>();

function unitSphereGrid(detail: number): UnitSphereGrid {
  const cached = GRIDS_BY_DETAIL.get(detail);
  if (cached) {
    return cached;
  }
  const vertexCount = (detail + 1) * (detail + 1);
  const grid = { positions: new Float64Array(vertexCount * 3), texS: new Float64Array(vertexCount), texT: new Float64Array(vertexCount) };
  for (let stack = 0; stack <= detail; stack++) {
    const rho = (stack * Math.PI) / detail;
    for (let slice = 0; slice <= detail; slice++) {
      const theta = slice === detail ? 0 : (slice * 2 * Math.PI) / detail;
      const vertex = gridIndex(detail, stack, slice);
      grid.positions[vertex * 3] = -Math.sin(theta) * Math.sin(rho);
      grid.positions[vertex * 3 + 1] = Math.cos(theta) * Math.sin(rho);
      grid.positions[vertex * 3 + 2] = Math.cos(rho);
      grid.texS[vertex] = slice / detail;
      grid.texT[vertex] = 1 - stack / detail;
    }
  }
  GRIDS_BY_DETAIL.set(detail, grid);
  return grid;
}

export function projectSphere(orientation: Matrix3, detail = SPHERE_DETAIL): ProjectedSphere {
  const grid = unitSphereGrid(detail);
  const vertexCount = grid.texS.length;
  const unitX = new Float64Array(vertexCount);
  const unitY = new Float64Array(vertexCount);
  for (let vertex = 0; vertex < vertexCount; vertex++) {
    const x = grid.positions[vertex * 3];
    const y = grid.positions[vertex * 3 + 1];
    const z = grid.positions[vertex * 3 + 2];
    unitX[vertex] = orientation[0] * x + orientation[1] * y + orientation[2] * z;
    unitY[vertex] = orientation[3] * x + orientation[4] * y + orientation[5] * z;
  }
  const quadShown = new Uint8Array(detail * detail);
  for (let stack = 0; stack < detail; stack++) {
    for (let slice = 0; slice < detail; slice++) {
      const corners = [gridIndex(detail, stack, slice), gridIndex(detail, stack + 1, slice), gridIndex(detail, stack + 1, slice + 1), gridIndex(detail, stack, slice + 1)];
      let doubledArea = 0;
      for (let corner = 0; corner < 4; corner++) {
        const from = corners[corner];
        const to = corners[(corner + 1) % 4];
        doubledArea += unitX[from] * unitY[to] - unitX[to] * unitY[from];
      }
      quadShown[stack * detail + slice] = doubledArea < 0 ? 1 : 0;
    }
  }
  return { detail, unitX, unitY, texS: grid.texS, texT: grid.texT, quadShown };
}
