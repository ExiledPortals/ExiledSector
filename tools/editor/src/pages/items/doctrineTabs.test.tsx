import { fireEvent, screen, within } from '@testing-library/react';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import { buildVocabulary } from '../../api/vocabulary.ts';
import { findLabelled } from '../../test/labelledQueries.ts';
import { renderWithProviders } from '../../test/renderWithProviders.tsx';
import { readRepositoryFile, startTestServer, type TestServer } from '../../test/testServer.ts';
import { DefinitionsTab } from './tabs/DefinitionsTab.tsx';

const CONFIG_DIR = 'data/config/exiledSector';
const config = (name: string) => `${CONFIG_DIR}/${name}`;
const DOCTRINES_FILE = config('doctrine_archives.csv');
const vocabulary = buildVocabulary(JSON.parse(readRepositoryFile('tools/editor_vocabulary.json')));

let server: TestServer;

beforeAll(() => {
  server = startTestServer([
    'tools/editor_vocabulary.json',
    'data/skilltrees/skill_types.json',
    'data/skilltrees/ship_skill_tree.json',
    DOCTRINES_FILE,
    ...[
      'item_modifiers.csv',
      'subroutines.csv',
      'hull_items.csv',
      'hull_item_implicits.csv',
      'star_charts.csv',
      'star_chart_modifiers.csv',
      'star_chart_implicits.csv',
      'wormhole_anchors.csv',
    ].map(config),
  ]);
});

afterAll(async () => {
  await server.close();
});

function saveButton() {
  return screen.getByText('Save to project').closest('button') as HTMLButtonElement;
}

async function openDoctrine(doctrineName: string, doctrineId: string) {
  renderWithProviders(<DefinitionsTab vocabulary={vocabulary} />);
  await screen.findAllByDisplayValue('Halcyon Fire-Control Lattice', undefined, { timeout: 30000 });
  fireEvent.click(await findLabelled('Doctrine Archives'));
  await screen.findAllByText('TRANSFORM - large', undefined, { timeout: 30000 });
  fireEvent.click(await screen.findByText(doctrineName, undefined, { timeout: 30000 }));
  await screen.findAllByDisplayValue(`${doctrineName} (${doctrineId})`, undefined, { timeout: 30000 });
}

describe('doctrine archive editing', () => {
  it('lists every doctrine with its behaviour and radius, and saves an edit to doctrine_archives.csv only', async () => {
    await openDoctrine('Hegemony Doctrine Archive', 'doctrine_archive_hegemony');
    const before = server.readProjectFile(DOCTRINES_FILE);

    expect(screen.getAllByText('AUGMENT - medium').length).toBe(4);
    expect(screen.getAllByText('TRANSFORM - large').length).toBe(3);
    fireEvent.change(screen.getByLabelText('Max lines per node'), { target: { value: '3' } });
    fireEvent.click(saveButton());
    await screen.findByText('Saved doctrine_archives.csv', undefined, { timeout: 30000 });

    expect(server.readProjectFile(DOCTRINES_FILE)).toBe(before.replace('@WEAPON_DURABILITY_PERCENT_HEGEMONY_DOCTRINE,1,2', '@WEAPON_DURABILITY_PERCENT_HEGEMONY_DOCTRINE,1,3'));
  }, 90000);

  it('refuses to save a row whose line counts are out of order', async () => {
    await openDoctrine('Pirate Doctrine Archive', 'doctrine_archive_pirate');
    const before = server.readProjectFile(DOCTRINES_FILE);

    fireEvent.change(screen.getByLabelText('Min lines per node'), { target: { value: '5' } });
    fireEvent.click(saveButton());

    const refusal = await screen.findByRole('dialog');
    expect(within(refusal).getByText('doctrine_archive_pirate: min lines must be 1 or more and max lines at least min lines')).toBeTruthy();
    expect(server.readProjectFile(DOCTRINES_FILE)).toBe(before);
  }, 90000);
});
