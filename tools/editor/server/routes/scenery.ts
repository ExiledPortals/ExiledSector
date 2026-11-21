import type { FastifyInstance } from 'fastify';
import fs from 'node:fs';
import { existingProjectFile, existingVanillaFile, type ProjectPaths } from '../paths.ts';
import { parseStarsectorJson, strictJsonText } from '../starsectorJson.ts';

const PLANETS_FILE = 'data/config/planets.json';
const SETTINGS_FILE = 'data/config/settings.json';

const SPEC_NUMBER_FIELDS = ['tilt', 'pitch', 'rotation', 'atmosphereThickness', 'atmosphereThicknessMin', 'starCoronaSizeMult'] as const;
const SPEC_COLOUR_FIELDS = ['planetColor', 'atmosphereColor', 'starCoronaColor'] as const;
const SPEC_TEXT_FIELDS = ['texture', 'starCoronaSprite'] as const;

const SCENERY_SPRITE_IDS = {
  atmosphere: { id: 'atmosphere2', fallbackPath: 'graphics/planets/atmosphere2.png' },
  aurora: { id: 'aurora', fallbackPath: 'graphics/planets/aurorae.png' },
};

export type SceneryPlanetSpec = Partial<Record<(typeof SPEC_NUMBER_FIELDS)[number], number>> &
  Partial<Record<(typeof SPEC_COLOUR_FIELDS)[number], number[]>> &
  Partial<Record<(typeof SPEC_TEXT_FIELDS)[number], string>>;

export interface SceneryData {
  ok: true;
  planetSpecs: Record<string, SceneryPlanetSpec>;
  sprites: Record<keyof typeof SCENERY_SPRITE_IDS, string>;
}

function readIfPresent(filePath: string | null): string | null {
  return filePath ? fs.readFileSync(filePath, 'utf8') : null;
}

function sceneryFieldsOf(entry: unknown): SceneryPlanetSpec {
  const json = (entry ?? {}) as Record<string, unknown>;
  const spec: Record<string, unknown> = {};
  for (const field of SPEC_NUMBER_FIELDS) {
    if (typeof json[field] === 'number') {
      spec[field] = json[field];
    }
  }
  for (const field of SPEC_COLOUR_FIELDS) {
    const colour = json[field];
    if (Array.isArray(colour) && colour.length >= 3 && colour.every((channel) => typeof channel === 'number')) {
      spec[field] = colour.slice(0, 4);
    }
  }
  for (const field of SPEC_TEXT_FIELDS) {
    if (typeof json[field] === 'string') {
      spec[field] = json[field];
    }
  }
  return spec as SceneryPlanetSpec;
}

function planetSpecsFrom(fileTexts: (string | null)[]): Record<string, SceneryPlanetSpec> {
  const planetSpecs: Record<string, SceneryPlanetSpec> = {};
  for (const text of fileTexts) {
    if (text === null) {
      continue;
    }
    const entries = parseStarsectorJson(text) as Record<string, unknown>;
    for (const [planetType, entry] of Object.entries(entries ?? {})) {
      planetSpecs[planetType] = { ...planetSpecs[planetType], ...sceneryFieldsOf(entry) };
    }
  }
  return planetSpecs;
}

function spritePathIn(settingsTexts: (string | null)[], spriteId: string, fallbackPath: string): string {
  const pattern = new RegExp(`"${spriteId}"\\s*:\\s*"([^"]+)"`);
  let spritePath = fallbackPath;
  for (const text of settingsTexts) {
    const match = text === null ? null : strictJsonText(text).match(pattern);
    if (match) {
      spritePath = match[1];
    }
  }
  return spritePath;
}

export function readSceneryData(paths: ProjectPaths): SceneryData {
  const planetTexts = [readIfPresent(existingVanillaFile(paths, PLANETS_FILE)), readIfPresent(existingProjectFile(paths, PLANETS_FILE))];
  const settingsTexts = [readIfPresent(existingVanillaFile(paths, SETTINGS_FILE)), readIfPresent(existingProjectFile(paths, SETTINGS_FILE))];
  return {
    ok: true,
    planetSpecs: planetSpecsFrom(planetTexts),
    sprites: {
      atmosphere: spritePathIn(settingsTexts, SCENERY_SPRITE_IDS.atmosphere.id, SCENERY_SPRITE_IDS.atmosphere.fallbackPath),
      aurora: spritePathIn(settingsTexts, SCENERY_SPRITE_IDS.aurora.id, SCENERY_SPRITE_IDS.aurora.fallbackPath),
    },
  };
}

export function registerSceneryRoutes(app: FastifyInstance, paths: ProjectPaths): void {
  app.get('/api/scenery', async (_request, reply) => {
    try {
      return readSceneryData(paths);
    } catch (error) {
      return reply.code(500).send({ ok: false, message: `The planet specs could not be read: ${error instanceof Error ? error.message : String(error)}` });
    }
  });
}
