import fs from 'node:fs';
import path from 'node:path';

export const TEST_PORT = 8792;
export const EDITOR_HEADERS = { 'x-skill-tree-editor': '1', host: `localhost:${TEST_PORT}` };

export function writeTestFile(root: string, relativePath: string, content: string | Buffer): string {
  const fullPath = path.join(root, relativePath);
  fs.mkdirSync(path.dirname(fullPath), { recursive: true });
  fs.writeFileSync(fullPath, content);
  return fullPath;
}
