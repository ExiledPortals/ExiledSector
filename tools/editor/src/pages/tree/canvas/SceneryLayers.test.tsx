import { MantineProvider } from '@mantine/core';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, fireEvent, render, screen } from '@testing-library/react';
import type { ReactNode } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { editorTheme } from '../../../theme.ts';
import { DECORATION_KINDS, type Decoration } from '../model/decorations.ts';
import { SceneryToggle } from '../SceneryToggle.tsx';
import { SCENERY_STORAGE_KEY } from '../state/sceneryPreference.ts';
import { createTreeEditor } from '../state/treeEditor.ts';
import { TreeEditorContext } from '../state/treeEditorContext.ts';
import { EMPTY_DOCUMENT } from '../state/treeStore.ts';
import { SHIP_TREE_KIND } from '../treeKinds.ts';
import { DecorationLayer } from './DecorationLayers.tsx';
import { TreeCanvas } from './TreeCanvas.tsx';

const CONTEXT = { imagePath: 'graphics/backgrounds/static_images/hyperspace_cloud_01.png', region: undefined };

const DECORATIONS: Decoration[] = [
  DECORATION_KINDS.star.create('sun', { x: 0, y: 0 }, CONTEXT),
  DECORATION_KINDS.image.create('cloud', { x: 500, y: 0 }, CONTEXT),
  DECORATION_KINDS.hypershunt.create('shunt', { x: 0, y: 0 }, CONTEXT),
  DECORATION_KINDS.threatUnit.create('fabricator', { x: -900, y: 0 }, CONTEXT),
];

function renderInScheme(ui: ReactNode, colourScheme: 'dark' | 'light') {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <MantineProvider theme={editorTheme} env="test" forceColorScheme={colourScheme}>
      <QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>
    </MantineProvider>,
  );
}

function decorationSvg(sceneryPainted: boolean, selectedIds: string[] = []) {
  return renderInScheme(
    <svg>
      <DecorationLayer decorations={DECORATIONS} selectedIds={selectedIds} sceneryPainted={sceneryPainted} />
    </svg>,
    'dark',
  );
}

beforeEach(() => {
  localStorage.clear();
  vi.stubGlobal('fetch', () => Promise.reject(new Error('offline')));
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('scenery layers', () => {
  it('leaves the painted look to the scenery canvas and keeps only selection outlines and unit labels in the SVG', () => {
    const placeholders = decorationSvg(false);
    expect(placeholders.container.querySelectorAll('image')).toHaveLength(3);
    placeholders.unmount();

    const painted = decorationSvg(true);
    expect(painted.container.querySelectorAll('image')).toHaveLength(0);
    expect(painted.container.querySelectorAll('circle, rect, ellipse')).toHaveLength(0);
    expect(painted.container.querySelector('svg')?.textContent).toBe('Fabricator');
    painted.unmount();

    const selected = decorationSvg(true, ['sun', 'cloud', 'shunt']);
    expect(selected.container.querySelectorAll('image')).toHaveLength(0);
    expect(selected.container.querySelector('rect')).toBeTruthy();
    expect(selected.container.querySelectorAll('ellipse')).toHaveLength(1);
    expect(selected.container.querySelector('svg')?.textContent).toContain(DECORATION_KINDS.hypershunt.label);
  });

  it('keeps the SVG placeholders and every handle when the browser cannot paint the scenery', () => {
    const editor = createTreeEditor(SHIP_TREE_KIND);
    editor.store.setState({ document: { ...EMPTY_DOCUMENT, decorations: DECORATIONS }, showScenery: true });
    const { container } = renderInScheme(
      <TreeEditorContext value={editor}>
        <TreeCanvas onRequestAddNode={() => undefined} />
      </TreeEditorContext>,
      'dark',
    );
    const canvases = [...container.querySelectorAll('canvas')];
    expect(canvases).toHaveLength(2);
    expect(canvases.indexOf(container.querySelector('svg')?.previousElementSibling as HTMLCanvasElement)).toBe(0);
    expect(canvases.indexOf(container.querySelector('svg')?.nextElementSibling as HTMLCanvasElement)).toBe(1);
    expect(container.querySelector('image[href$="coronal_shunt2.png"]')).toBeTruthy();
    expect(container.querySelectorAll('[data-decoration-id]')).toHaveLength(DECORATIONS.length);

    act(() => editor.store.setState({ showScenery: false }));
    expect(container.querySelectorAll('canvas')).toHaveLength(0);
  });

  it('draws no scenery canvas in the light theme', () => {
    const editor = createTreeEditor(SHIP_TREE_KIND);
    editor.store.setState({ document: { ...EMPTY_DOCUMENT, decorations: DECORATIONS }, showScenery: true });
    const { container } = renderInScheme(
      <TreeEditorContext value={editor}>
        <TreeCanvas onRequestAddNode={() => undefined} />
      </TreeEditorContext>,
      'light',
    );
    expect(container.querySelectorAll('canvas')).toHaveLength(0);
  });

  it('remembers the scenery toggle and disables it in the light theme', () => {
    const editor = createTreeEditor(SHIP_TREE_KIND);
    const dark = renderInScheme(
      <TreeEditorContext value={editor}>
        <SceneryToggle />
      </TreeEditorContext>,
      'dark',
    );
    const toggle = screen.getByLabelText('Scenery') as HTMLInputElement;
    expect(toggle.checked).toBe(true);
    fireEvent.click(toggle);
    expect(editor.store.getState().showScenery).toBe(false);
    expect(localStorage.getItem(SCENERY_STORAGE_KEY)).toBe('off');
    expect(createTreeEditor(SHIP_TREE_KIND).store.getState().showScenery).toBe(false);
    dark.unmount();

    renderInScheme(
      <TreeEditorContext value={editor}>
        <SceneryToggle />
      </TreeEditorContext>,
      'light',
    );
    expect((screen.getByLabelText('Scenery') as HTMLInputElement).disabled).toBe(true);
  });
});
