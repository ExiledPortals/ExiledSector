import fs from 'node:fs';
import path from 'node:path';
import { describe, expect, it } from 'vitest';
import { marketConditionIds, parsePhasedTraitsCsv, serializePhasedTraitsCsv, validatePhasedTraits } from './phasedTraits.ts';

const repositoryRoot = path.resolve(import.meta.dirname, '..', '..', '..', '..', '..', '..');

function readData(...parts: string[]): string {
  const text = fs.readFileSync(path.join(repositoryRoot, ...parts), 'utf8');
  return text.startsWith(String.fromCharCode(0xfeff)) ? text.slice(1) : text;
}

const HEADER = 'id,condition,weight,regions,anomalies,excludes,notes\n';

describe('phased planet traits', () => {
  it('saving the unchanged file reproduces it byte for byte', () => {
    const text = readData('data', 'config', 'exiledSector', 'phased_planet_traits.csv');
    expect(serializePhasedTraitsCsv(parsePhasedTraitsCsv(text))).toBe(text);
  });

  it('every shipped trait is valid against the shipped market conditions', () => {
    const traits = parsePhasedTraitsCsv(readData('data', 'config', 'exiledSector', 'phased_planet_traits.csv')).traits;
    const conditions = marketConditionIds(readData('data', 'campaign', 'market_conditions.csv'));
    expect(traits.length).toBeGreaterThan(0);
    expect(validatePhasedTraits(traits, conditions)).toEqual([]);
  });

  it('keeps an edited field and every other cell through a save', () => {
    const model = parsePhasedTraitsCsv(`${HEADER}mantle,c_mantle,1,,neutron_star:10,furnace,"Leans, heavily"\n`);
    model.traits[0].weight = '2';
    const saved = parsePhasedTraitsCsv(serializePhasedTraitsCsv(model)).traits[0];
    expect(saved.weight).toBe('2');
    expect(saved.anomalies).toBe('neutron_star:10');
    expect(saved.notes).toBe('Leans, heavily');
  });

  it('reports unknown conditions, bad multipliers and dangling exclusions', () => {
    const traits = parsePhasedTraitsCsv(`${HEADER}mantle,c_missing,-1,furnace:x,,ghost,\n`).traits;
    const issues = validatePhasedTraits(traits, new Set(['c_mantle']));
    expect(issues).toHaveLength(4);
  });
});
