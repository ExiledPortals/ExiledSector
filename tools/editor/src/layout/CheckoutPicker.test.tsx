import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fireEvent, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { resetPageCheckoutForTests, saveDataFiles } from '../api/client.ts';
import type { Checkout } from '../../server/checkouts.ts';
import { renderWithProviders } from '../test/renderWithProviders.tsx';
import { startTestServer, type TestServer } from '../test/testServer.ts';
import { CheckoutPicker } from './CheckoutPicker.tsx';

let server: TestServer;
let branchRoot: string;

beforeEach(() => {
  resetPageCheckoutForTests();
  branchRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'editor-branch-'));
  fs.mkdirSync(path.join(branchRoot, 'data', 'skilltrees'), { recursive: true });
  const checkouts: Checkout[] = [];
  server = startTestServer([], { listCheckouts: () => checkouts, editorDir: path.join(branchRoot, 'no-editor') });
  checkouts.push({ root: server.projectRoot, branch: 'main', main: true }, { root: branchRoot, branch: 'parallel-work', main: false });
});

afterEach(async () => {
  await server.close();
  fs.rmSync(branchRoot, { recursive: true, force: true });
});

describe('checkout picker', () => {
  it('switches the server to another branch, reloads the page and stops a stale page from saving', async () => {
    const reloadPage = vi.fn();
    renderWithProviders(<CheckoutPicker reloadPage={reloadPage} />);
    const picker = await screen.findByRole('textbox', { name: 'Branch' });
    await waitFor(() => expect(picker).toHaveProperty('value', 'main'));

    fireEvent.click(picker);
    fireEvent.click(await screen.findByRole('option', { name: 'parallel-work' }));

    await waitFor(() => expect(reloadPage).toHaveBeenCalledTimes(1));
    const listing = (await server.app.inject({ method: 'GET', url: '/api/checkouts' })).json();
    expect(listing.current).toBe(branchRoot);
    const staleSave = await saveDataFiles({}, {});
    expect(staleSave.status).toBe(412);
  });
});
