import { Button } from '@mantine/core';
import { modals } from '@mantine/modals';
import { IconStack2 } from '@tabler/icons-react';
import { atlasTypesRequest, atlasTypeTooltip, DescribedTypesCache } from '../../api/tooltipCode.ts';
import type { Described } from '../../shared/tooltipContent.ts';
import { GameTooltip, type GameTooltipSource } from '../tree/GameTooltip.tsx';
import { inTreeEditor } from '../tree/dialogs/treeDialogs.tsx';
import { atlasTreeEditor } from '../tree/state/treeEditor.ts';
import { TreeEditorPage } from '../tree/TreeEditorPage.tsx';
import { VolumesEditor } from './VolumesEditor.tsx';

const atlasTooltipSource: GameTooltipSource<Described> = {
  editor: atlasTreeEditor,
  describedTypes: new DescribedTypesCache<Described>(atlasTypesRequest),
  tooltip: atlasTypeTooltip,
};

function openVolumesDialog(): void {
  modals.open({ title: `${atlasTreeEditor.kind.label} volumes`, size: 'md', children: inTreeEditor(atlasTreeEditor, <VolumesEditor />) });
}

export function AtlasTreePage() {
  const volumesButton = (
    <Button size="xs" variant="default" leftSection={<IconStack2 size={14} />} onClick={openVolumesDialog}>
      Volumes
    </Button>
  );
  return <TreeEditorPage editor={atlasTreeEditor} toolbarControls={volumesButton} overlay={<GameTooltip source={atlasTooltipSource} />} />;
}
