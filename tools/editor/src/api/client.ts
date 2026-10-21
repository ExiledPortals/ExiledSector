export const SERVER_HINT = 'run tools\\start-editor.ps1';

export interface PostResult<TData = Record<string, unknown>> {
  ok: boolean;
  status: number;
  data: TData;
  message: string;
}

let pageCheckout: string | null = null;

export function setPageCheckout(root: string): void {
  pageCheckout ??= root;
}

export function resetPageCheckoutForTests(): void {
  pageCheckout = null;
}

export async function postJson<TData = Record<string, unknown>>(url: string, payload: unknown): Promise<PostResult<TData>> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json', 'X-Skill-Tree-Editor': '1' };
  if (pageCheckout) {
    headers['X-Editor-Checkout'] = encodeURIComponent(pageCheckout);
  }
  let response: Response;
  try {
    response = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify(payload),
    });
  } catch {
    return { ok: false, status: 0, data: {} as TData, message: `Could not reach the editor server - ${SERVER_HINT}` };
  }
  const data = (await response.json().catch(() => ({}))) as TData & { ok?: boolean; message?: string };
  return {
    ok: response.ok && Boolean(data.ok),
    status: response.status,
    data,
    message: data.message ?? (response.ok ? '' : `HTTP ${response.status}`),
  };
}

export async function getJson<TData>(url: string): Promise<TData> {
  const response = await fetch(url);
  if (!response.ok) {
    const body = (await response.json().catch(() => ({}))) as { message?: string };
    throw new Error(body.message ?? `${url} returned ${response.status}`);
  }
  return (await response.json()) as TData;
}

export interface DataFile {
  content: string;
  hash: string;
}

export type DataFiles = Record<string, DataFile>;

export async function fetchDataFiles(names: string[]): Promise<DataFiles> {
  const body = await getJson<{ ok: boolean; files: DataFiles; message?: string }>(`/api/files?names=${encodeURIComponent(names.join(','))}`);
  if (!body.ok || !body.files) {
    throw new Error(body.message ?? 'The data files could not be loaded.');
  }
  return body.files;
}

export interface ReservedNodeIds {
  replaced: string[];
  released: string[];
}

export async function fetchReservedNodeIds(): Promise<ReservedNodeIds> {
  const body = await getJson<{ ok: boolean; replaced?: string[]; released?: string[]; message?: string }>('/api/reserved-node-ids');
  if (!body.ok) {
    throw new Error(body.message ?? 'The retired node ids could not be loaded.');
  }
  return { replaced: body.replaced ?? [], released: body.released ?? [] };
}

export interface SaveResult extends PostResult<{ hashes?: Record<string, string>; changed?: string[]; conflict?: boolean }> {
  conflict: boolean;
}

export async function saveDataFiles(contents: Record<string, string>, baseHashes: Record<string, string>): Promise<SaveResult> {
  const files: Record<string, { content: string; baseHash: string | null }> = {};
  for (const [name, content] of Object.entries(contents)) {
    files[name] = { content, baseHash: baseHashes[name] ?? null };
  }
  const result = await postJson<{ hashes?: Record<string, string>; changed?: string[]; conflict?: boolean }>('/api/files', { files });
  return { ...result, conflict: result.status === 409 };
}

export function staleFileQuestion(message: string, discarded: string): string {
  return (
    `${message} Another tool or agent changed the file after this editor loaded it, so saving would overwrite that work. ` +
    `Reload from disk now? This discards ${discarded}.`
  );
}
