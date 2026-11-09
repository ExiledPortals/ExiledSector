import { Button } from '@mantine/core';
import { modals } from '@mantine/modals';
import { IconStack2 } from '@tabler/icons-react';
import { inTreeEditor } from '../tree/dialogs/treeDialogs.tsx';
import { atlasTreeEditor } from '../tree/state/treeEditor.ts';
import { TreeEditorPage } from '../tree/TreeEditorPage.tsx';
import { VolumesEditor } from './VolumesEditor.tsx';

function openVolumesDialog(): void {
  modals.open({ title: `${atlasTreeEditor.kind.label} volumes`, size: 'md', children: inTreeEditor(atlasTreeEditor, <VolumesEditor />) });
}

export function AtlasTreePage() {
  const volumesButton = (
    <Button size="xs" variant="default" leftSection={<IconStack2 size={14} />} onClick={openVolumesDialog}>
      Volumes
    </Button>
  );
  return <TreeEditorPage editor={atlasTreeEditor} toolbarControls={volumesButton} />;
}
