import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { createEditorServer } from './app.ts';
import { DescribeService } from './describe.ts';
import type { ProjectPaths } from './paths.ts';
import { readSceneryData } from './routes/scenery.ts';
import { parseStarsectorJson, strictJsonText } from './starsectorJson.ts';

const VANILLA_PLANETS = `{
\t"star_yellow":{
\t\t"tilt":0, # left-right
\t\t"rotation":-3, # degrees/second
\t\t"planetColor":[255,255,255,255],
\t\t"atmosphereColor":[255,255,160,255],
\t\t"texture":"graphics/planets/star_texture_yellow.jpg",
\t\t"starCoronaSprite":"graphics/fx/star_halo.png", # optional
\t\t"starCoronaColor":[255,255,150,60],
\t\t"starCoronaSizeMult":5.12,
\t\t"name":"Yellow # Star",
\t},
\t#"star_hidden":{},
\t"black_hole":{ "rotation":-3, "texture":"graphics/planets/star_white.jpg", },
}`;

let projectRoot: string;
let paths: ProjectPaths;

function writeFile(root: string, relativePath: string, content: string): void {
  const fullPath = path.join(root, relativePath);
  fs.mkdirSync(path.dirname(fullPath), { recursive: true });
  fs.writeFileSync(fullPath, content);
}

beforeEach(() => {
  projectRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'editor-scenery-'));
  paths = { projectRoot, vanillaCoreDir: path.join(projectRoot, 'vanilla-core') };
});

afterEach(() => {
  fs.rmSync(projectRoot, { recursive: true, force: true });
});

describe('Starsector JSON', () => {
  it('drops comments and trailing commas but keeps hashes inside strings', () => {
    expect(strictJsonText('{"a":"x # y", # note\n "b":[1,2,],}')).toBe('{"a":"x # y", \n "b":[1,2]}');
    expect(parseStarsectorJson(`${String.fromCharCode(0xfeff)}{"escaped":"quote \\" # still text",}`)).toEqual({ escaped: 'quote " # still text' });
  });
});

describe('scenery data', () => {
  it('reads the star specs from the vanilla planets file, lets the project override them and resolves the scenery sprites', () => {
    writeFile(paths.vanillaCoreDir, 'data/config/planets.json', VANILLA_PLANETS);
    writeFile(paths.vanillaCoreDir, 'data/config/settings.json', '{"graphics":{"planets":{\n"atmosphere2":"graphics/planets/atmosphere2.png", # used\n},"terrain":{"aurora":"graphics/planets/aurorae.png",},},"speed":1f,}');
    writeFile(projectRoot, 'data/config/planets.json', '{"black_hole":{"rotation":5}}');
    writeFile(projectRoot, 'data/config/settings.json', '{"graphics":{"terrain":{"aurora":"graphics/planets/custom_aurora.png"}}}');

    const data = readSceneryData(paths);
    expect(Object.keys(data.planetSpecs).sort()).toEqual(['black_hole', 'star_yellow']);
    expect(data.planetSpecs.star_yellow).toEqual({
      tilt: 0,
      rotation: -3,
      planetColor: [255, 255, 255, 255],
      atmosphereColor: [255, 255, 160, 255],
      texture: 'graphics/planets/star_texture_yellow.jpg',
      starCoronaSprite: 'graphics/fx/star_halo.png',
      starCoronaColor: [255, 255, 150, 60],
      starCoronaSizeMult: 5.12,
    });
    expect(data.planetSpecs.black_hole).toEqual({ rotation: 5, texture: 'graphics/planets/star_white.jpg' });
    expect(data.sprites).toEqual({ atmosphere: 'graphics/planets/atmosphere2.png', aurora: 'graphics/planets/custom_aurora.png' });
  });

  it('serves empty specs and the default sprites when no vanilla install is found', async () => {
    const app = createEditorServer({ paths, port: 8792, describe: new DescribeService(paths, { javaHome: null, maven: null }) });
    const response = await app.inject({ method: 'GET', url: '/api/scenery' });
    expect(response.statusCode).toBe(200);
    expect(response.json()).toEqual({ ok: true, planetSpecs: {}, sprites: { atmosphere: 'graphics/planets/atmosphere2.png', aurora: 'graphics/planets/aurorae.png' } });

    writeFile(paths.vanillaCoreDir, 'data/config/planets.json', '{ broken');
    const broken = await app.inject({ method: 'GET', url: '/api/scenery' });
    expect(broken.statusCode).toBe(500);
    expect(broken.json().ok).toBe(false);
    await app.close();
  });
});
