import { cleanup, fireEvent, screen, waitFor } from '@testing-library/react';
import { createElement } from 'react';
import { afterEach, beforeEach, vi } from 'vitest';
import type { ModifierLineRequest } from '../api/describe.ts';
import { forgetDescriptionsForTests } from '../pages/items/describedLines.ts';
import { ItemsPage } from '../pages/items/ItemsPage.tsx';
import { findLabelled } from './labelledQueries.ts';
import { renderWithProviders } from './renderWithProviders.tsx';
import { startTestServer, type TestServer } from './testServer.ts';

const config = (name: string) => `data/config/exiledSector/${name}`;
export const HULL_ITEMS_FILE = config('hull_items.csv');
export const SUBROUTINES_FILE = config('subroutines.csv');
export const MODIFIERS_FILE = config('item_modifiers.csv');
export const CHART_MODIFIERS_FILE = config('star_chart_modifiers.csv');
export const STAR_CHARTS_FILE = config('star_charts.csv');
export const DOCTRINES_FILE = config('doctrine_archives.csv');
export const ANCHORS_FILE = config('wormhole_anchors.csv');
export const IMPLICITS_FILE = config('hull_item_implicits.csv');
export const LOAD_TIMEOUT = { timeout: 30000 };

const PROJECT_FILES = [
  'tools/editor_vocabulary.json',
  'data/skilltrees/skill_types.json',
  'data/skilltrees/ship_skill_tree.json',
  ...[
    'subroutines.csv',
    'hull_items.csv',
    'star_charts.csv',
    'item_modifiers.csv',
    'star_chart_modifiers.csv',
    'hull_item_implicits.csv',
    'star_chart_implicits.csv',
    'subroutine_corruption_implicits.csv',
    'hull_item_corruption_implicits.csv',
    'doctrine_archives.csv',
    'wormhole_anchors.csv',
  ].map(config),
];

export const describeHelper = { calls: 0, fails: false };

let server: TestServer | undefined;

function describedText(line: ModifierLineRequest): string {
  const values = line.tierGroups
    ? line.tierGroups.map((groupValue) => `${groupValue.group} ${groupValue.min} to ${groupValue.max}`).join(', ')
    : line.hullValues
      ? line.hullValues.join('/')
      : `${line.min} to ${line.max}`;
  return `<good>${values}</good> ${line.effect.toLowerCase().replaceAll('_', ' ')}`;
}

export function setUpItemsProject() {
  beforeEach(() => {
    server = startTestServer(PROJECT_FILES);
    const serverFetch = globalThis.fetch;
    forgetDescriptionsForTests();
    describeHelper.calls = 0;
    describeHelper.fails = false;
    vi.stubGlobal('fetch', async (input: RequestInfo | URL, init?: RequestInit) => {
      if (String(input) === '/api/describe') {
        describeHelper.calls++;
        const { modifierLines } = JSON.parse(String(init?.body)) as { modifierLines?: ModifierLineRequest[] };
        if (describeHelper.fails) {
          return new Response(JSON.stringify({ error: 'The description helper stopped responding.' }), { status: 200, headers: { 'content-type': 'application/json' } });
        }
        const lines = (modifierLines ?? []).map((line) => (line.effect === 'NOT_DESCRIBABLE' ? null : describedText(line)));
        return new Response(JSON.stringify({ lines }), { status: 200, headers: { 'content-type': 'application/json' } });
      }
      return serverFetch(input, init);
    });
  });

  afterEach(async () => {
    cleanup();
    await server?.close();
    server = undefined;
  });
}

export function readProjectFile(relativePath: string): string {
  if (!server) {
    throw new Error('The items project is only open while a test runs');
  }
  return server.readProjectFile(relativePath);
}

export function readProjectFiles(...relativePaths: string[]): Record<string, string> {
  return Object.fromEntries(relativePaths.map((file) => [file, readProjectFile(file)]));
}

export function saveButton() {
  return screen.getByText('Save to project').closest('button') as HTMLButtonElement;
}

export async function save(message: string | RegExp) {
  fireEvent.click(saveButton());
  await waitFor(() => {
    const refusal = screen.queryByRole('dialog');
    if (refusal) {
      throw new Error(`Not saved: ${refusal.textContent}`);
    }
    screen.getByText(message);
  }, LOAD_TIMEOUT);
}

export function tab(tabName: string): HTMLElement {
  const matches = [...document.querySelectorAll<HTMLElement>('[role="tab"]')].filter((candidate) => candidate.textContent?.trim() === tabName);
  if (matches.length !== 1) {
    throw new Error(`Expected one tab named "${tabName}" but found ${matches.length}`);
  }
  return matches[0];
}

function isShown(tabPanel: HTMLElement): boolean {
  for (let current: HTMLElement | null = tabPanel; current; current = current.parentElement?.closest<HTMLElement>('[role="tabpanel"]') ?? null) {
    if (getComputedStyle(current).display === 'none') {
      return false;
    }
  }
  return true;
}

export function panel(tabName: string): HTMLElement {
  const tabPanel = document.querySelector<HTMLElement>(`[role="tabpanel"][aria-labelledby="${tab(tabName).id}"]`);
  if (!tabPanel || !isShown(tabPanel)) {
    throw new Error(`The "${tabName}" tab panel is not shown`);
  }
  return tabPanel;
}

export async function openItems(...tabs: string[]): Promise<HTMLElement> {
  renderWithProviders(createElement(ItemsPage));
  await waitFor(() => tab('Hull Items'), LOAD_TIMEOUT);
  for (const tabName of tabs) {
    fireEvent.click(tab(tabName));
  }
  return panel(tabs[tabs.length - 1] ?? 'Item types');
}

export async function pickOption(container: HTMLElement, pickerLabel: string, search: string, optionName: RegExp) {
  const picker = await findLabelled(pickerLabel, container);
  fireEvent.click(picker);
  fireEvent.change(picker, { target: { value: search } });
  fireEvent.click(await screen.findByRole('option', { name: optionName }, LOAD_TIMEOUT));
}
