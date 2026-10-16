import { describe, expect, it } from 'vitest';
import { readRepositoryFile } from '../../../test/testServer.ts';
import { analyseTemplate, currentStrings, parseStrings, serializeStrings, stringIssues, validateStrings, type StringsModel } from './strings.ts';

const SAMPLE = '{\r\n    "stat.armor": "armor",\r\n\r\n    "desc.flat.up": "Increases {stat} by <good>{value}</good>.",\r\n    "count.one": "{count} ship",\r\n    "count.other": "{count} ships in {system}"\r\n}\r\n';

function edit(model: StringsModel, key: string, value: string): StringsModel {
  return { ...model, entries: model.entries.map((entry) => (entry.key === key ? { ...entry, value } : entry)) };
}

function issuesFor(model: StringsModel, key: string) {
  const entry = model.entries.find((candidate) => candidate.key === key);
  if (!entry) {
    throw new Error(`no ${key}`);
  }
  return stringIssues(entry, model);
}

describe('English strings file', () => {
  it('round-trips the real en.json byte for byte', () => {
    const text = readRepositoryFile('data/strings/exiledSector/en.json');
    const model = parseStrings(text);
    expect(model.entries.length).toBe(Object.keys(JSON.parse(text) as object).length);
    expect(serializeStrings(model)).toBe(text);
  });

  it('rewrites only the edited line and keeps blank lines and line endings', () => {
    const model = edit(parseStrings(SAMPLE), 'stat.armor', 'armour "plating"');
    expect(serializeStrings(model)).toBe(SAMPLE.replace('"armor",', '"armour \\"plating\\"",'));
    expect(currentStrings(model)['stat.armor']).toBe('armour "plating"');
  });

  it('refuses a layout it cannot edit line by line', () => {
    expect(() => parseStrings('{\n  "a": "x", "b": "y"\n}\n')).toThrow(/line 2/);
  });
});

describe('template checks', () => {
  it('splits text, placeholders and highlight tags the way the game does', () => {
    const analysis = analyseTemplate('Increases {stat} by <good>{value}</good>.');
    expect(analysis.placeholders).toEqual(['stat', 'value']);
    expect(analysis.balancedTags).toBe(true);
    expect(analysis.segments).toEqual([
      { kind: 'text', text: 'Increases ', style: null },
      { kind: 'placeholder', name: 'stat', style: null },
      { kind: 'text', text: ' by ', style: null },
      { kind: 'placeholder', name: 'value', style: 'good' },
      { kind: 'text', text: '.', style: null },
    ]);
    expect(analyseTemplate('<good>a<bad>b</bad></good>').balancedTags).toBe(false);
    expect(analyseTemplate('<b>bold</b> {not valid}').unknownTags).toEqual(['<b>', '</b>']);
  });

  it('blocks text the game cannot show and warns about dropped values', () => {
    const model = parseStrings(SAMPLE);
    expect(issuesFor(edit(model, 'desc.flat.up', 'Raises {stat} by {amount}.'), 'desc.flat.up')).toEqual({
      errors: ['The game never fills {amount}, so it would show literally.'],
      warnings: ['No longer shows {value}.'],
    });
    expect(issuesFor(edit(model, 'desc.flat.up', '<good>{stat}'), 'desc.flat.up').errors[0]).toMatch(/unbalanced/);
    expect(issuesFor(edit(model, 'stat.armor', 'armor #1'), 'stat.armor').errors[0]).toMatch(/comment/);
    expect(issuesFor(edit(model, 'stat.armor', 'within 500su'), 'stat.armor').errors[0]).toMatch(/space before 'su'/);
    expect(issuesFor(edit(model, 'count.one', 'one ship in {system}'), 'count.one')).toEqual({ errors: [], warnings: ['No longer shows {count}.'] });
  });

  it('only reports problems in strings that were edited', () => {
    const broken = parseStrings('{\n    "a": "<good>x",\n    "b": "fine"\n}\n');
    expect(validateStrings(broken)).toEqual([]);
    expect(validateStrings(edit(broken, 'b', ''))).toEqual(['b: The text is empty.']);
  });
});
