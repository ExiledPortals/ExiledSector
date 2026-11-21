import { useQuery } from '@tanstack/react-query';
import { getJson } from './client.ts';

export interface PlanetSpec {
  tilt?: number;
  pitch?: number;
  rotation?: number;
  atmosphereThickness?: number;
  atmosphereThicknessMin?: number;
  starCoronaSizeMult?: number;
  planetColor?: number[];
  atmosphereColor?: number[];
  starCoronaColor?: number[];
  texture?: string;
  starCoronaSprite?: string;
}

export interface SceneryData {
  planetSpecs: Record<string, PlanetSpec>;
  sprites: { atmosphere: string; aurora: string };
}

export async function fetchSceneryData(): Promise<SceneryData> {
  const body = await getJson<SceneryData & { ok: boolean; message?: string }>('/api/scenery');
  if (!body.ok) {
    throw new Error(body.message ?? 'The scenery data could not be loaded.');
  }
  return { planetSpecs: body.planetSpecs, sprites: body.sprites };
}

export function useSceneryData(enabled: boolean) {
  return useQuery({ queryKey: ['scenery'], enabled, staleTime: Infinity, queryFn: fetchSceneryData });
}
