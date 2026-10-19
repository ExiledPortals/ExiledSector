import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import { buildVocabulary } from '../../api/vocabulary.ts';
import { findLabelled } from '../../test/labelledQueries.ts';
import { renderWithProviders } from '../../test/renderWithProviders.tsx';
import { readRepositoryFile, startTestServer, type TestServer } from '../../test/testServer.ts';
import { DefinitionsTab } from './tabs/DefinitionsTab.tsx';

const CONFIG_DIR = 'data/config/exiledSector';
const config = (name: string) => `${CONFIG_DIR}/${name}`;
const ANCHORS_FILE = config('wormhole_anchors.csv');
const vocabulary = buildVocabulary(JSON.parse(readRepositoryFile('tools/editor_vocabulary.json')));

let server: TestServer;

beforeAll(() => {
  server = startTestServer([
    'tools/editor_vocabulary.json',
    'data/skilltrees/skill_types.json',
    'data/skilltrees/ship_skill_tree.json',
    ANCHORS_FILE,
    ...[
      'item_modifiers.csv',
      'doctrine_archives.csv',
      'subroutines.csv',
      'hull_items.csv',
      'hull_item_implicits.csv',
      'star_charts.csv',
      'star_chart_modifiers.csv',
      'star_chart_implicits.csv',
    ].map(config),
  ]);
});

afterAll(async () => {
  await server.close();
});

function saveButton() {
  return screen.getByText('Save to project').closest('button') as HTMLButtonElement;
}

async function openAnchor(anchorName: string) {
  renderWithProviders(<DefinitionsTab vocabulary={vocabulary} />);
  await screen.findAllByDisplayValue('Halcyon Fire-Control Lattice', undefined, { timeout: 30000 });
  fireEvent.click(await findLabelled('Wormhole anchors'));
  fireEvent.click(await screen.findByText(anchorName, undefined, { timeout: 30000 }));
  await screen.findAllByDisplayValue(anchorName, undefined, { timeout: 30000 });
}

describe('wormhole anchor editing', () => {
  it('lists every anchor with its volume and exit, and saves an edit to wormhole_anchors.csv only', async () => {
    await openAnchor('Hyper-Nav Beacon');
    const before = server.readProjectFile(ANCHORS_FILE);

    expect(screen.getByText('exotech - exoship')).toBeTruthy();
    expect(screen.getByText('hegemony - hegemony_2')).toBeTruthy();
    expect(await findLabelled('Exit node')).toHaveProperty('value', 'exoship');
    fireEvent.change(screen.getByLabelText('Name'), { target: { value: 'Portable Hyper-Nav Beacon' } });
    fireEvent.click(saveButton());
    await screen.findByText('Saved wormhole_anchors.csv', undefined, { timeout: 30000 });

    expect(server.readProjectFile(ANCHORS_FILE)).toBe(before.replace(',Hyper-Nav Beacon,', ',Portable Hyper-Nav Beacon,'));
  }, 90000);

  it('refuses to save two anchors with the same id', async () => {
    await openAnchor('Hegemony Corridor Lock');
    const before = server.readProjectFile(ANCHORS_FILE);

    fireEvent.change(screen.getByLabelText(/^Id/), { target: { value: 'wormhole_anchor_tritachyon' } });
    fireEvent.blur(screen.getByLabelText(/^Id/));
    fireEvent.click(await screen.findByRole('button', { name: 'Rename' }));
    await waitFor(() => expect(screen.getByLabelText(/^Id/)).toHaveProperty('value', 'wormhole_anchor_tritachyon'));
    fireEvent.click(saveButton());

    const refusal = await screen.findByRole('dialog');
    expect(within(refusal).getByText('wormhole_anchor_tritachyon: id is used more than once')).toBeTruthy();
    expect(server.readProjectFile(ANCHORS_FILE)).toBe(before);
  }, 90000);
});
