import type { IncomingHttpHeaders } from 'node:http';

export const EDITOR_HEADER = 'x-skill-tree-editor';
export const DEV_CLIENT_ORIGIN = 'http://localhost:5173';

function sameText(first: string | undefined, second: string): boolean {
  return first !== undefined && first.toLowerCase() === second.toLowerCase();
}

export function isEditorRequest(headers: IncomingHttpHeaders, port: number): boolean {
  if (headers[EDITOR_HEADER] !== '1' || !sameText(headers.host, `localhost:${port}`)) {
    return false;
  }
  const origin = headers.origin;
  return !origin || sameText(origin, `http://localhost:${port}`) || sameText(origin, DEV_CLIENT_ORIGIN);
}
