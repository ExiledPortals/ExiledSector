import fs from 'node:fs';
import path from 'node:path';
import { describe, expect, it } from 'vitest';
import { lineEndingOf } from '../../../shared/lineEndings.ts';
import { parseTreeDocument, serializeTreeFiles } from './serialization.ts';

const BYTE_ORDER_MARK = String.fromCharCode(0xfeff);
const PROJECT_ROOT = path.resolve(import.meta.dirname, '../../../../../..');

function readProjectFile(relativePath: string): string {
  const text = fs.readFileSync(path.join(PROJECT_ROOT, relativePath), 'utf8');
  return text.startsWith(BYTE_ORDER_MARK) ? text.slice(1) : text;
}

describe('tree files round-trip', () => {
  it('serialises skill_types.json and ship_skill_tree.json back to the same bytes', () => {
    const typesText = readProjectFile('data/skilltrees/skill_types.json');
    const treeText = readProjectFile('data/skilltrees/ship_skill_tree.json');
    const document = parseTreeDocument(typesText, treeText);
    const texts = serializeTreeFiles(document, { types: lineEndingOf(typesText), tree: lineEndingOf(treeText) });
    expect(texts.types).toBe(typesText);
    expect(texts.tree).toBe(treeText);
  });
});
