import { getJson, postJson, setPageCheckout } from './client.ts';

export interface Checkout {
  root: string;
  name: string;
  branch: string | null;
  main: boolean;
  current: boolean;
  editorMatches: boolean;
}

export async function fetchCheckouts(): Promise<Checkout[]> {
  const body = await getJson<{ ok: boolean; current?: string; checkouts?: Checkout[]; message?: string }>('/api/checkouts');
  if (!body.ok || !body.checkouts || !body.current) {
    throw new Error(body.message ?? 'The checkouts could not be listed.');
  }
  setPageCheckout(body.current);
  return body.checkouts;
}

export async function switchCheckout(root: string): Promise<{ ok: boolean; message: string }> {
  const result = await postJson('/api/checkout', { root });
  return { ok: result.ok, message: result.message };
}

export function checkoutLabel(checkout: Pick<Checkout, 'branch' | 'name'>): string {
  return checkout.branch ?? `${checkout.name} (detached)`;
}
