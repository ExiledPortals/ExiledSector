import { ActionIcon, Badge, Stack, Tooltip } from '@mantine/core';
import { IconAlertTriangle, IconFocusCentered, IconMaximize, IconMinus, IconPlus } from '@tabler/icons-react';
import { useMemo } from 'react';
import { useVocabulary } from '../../../api/vocabulary.ts';
import { showIssues } from '../../../shared/dialogs.tsx';
import { validateTree } from '../model/validation.ts';
import { centerOnOrigin, fitToScreen, zoomBy } from '../state/treeActions.ts';
import { useTreeStore } from '../state/treeStore.ts';
import classes from './TreeCanvas.module.css';

function IssuesBadge() {
  const document = useTreeStore((state) => state.document);
  const replacedNodeIds = useTreeStore((state) => state.replacedNodeIds);
  const vocabulary = useVocabulary();
  const issues = useMemo(
    () => validateTree(document, vocabulary.data ?? null, replacedNodeIds),
    [document, vocabulary.data, replacedNodeIds],
  );
  if (issues.length === 0) {
    return null;
  }
  return (
    <Badge
      className={classes.overlayBottomLeft}
      color="red"
      variant="light"
      size="lg"
      leftSection={<IconAlertTriangle size={14} />}
      style={{ cursor: 'pointer', textTransform: 'none' }}
      onClick={() => showIssues('Skill tree issues', issues)}
    >
      {issues.length} issue{issues.length > 1 ? 's' : ''} - click to view
    </Badge>
  );
}

function ZoomControls() {
  return (
    <Stack className={classes.overlayBottomRight} gap={6}>
      <Tooltip label="Zoom in" position="left">
        <ActionIcon variant="default" size="lg" onClick={() => zoomBy(1.2)} aria-label="Zoom in">
          <IconPlus size={16} />
        </ActionIcon>
      </Tooltip>
      <Tooltip label="Zoom out" position="left">
        <ActionIcon variant="default" size="lg" onClick={() => zoomBy(1 / 1.2)} aria-label="Zoom out">
          <IconMinus size={16} />
        </ActionIcon>
      </Tooltip>
      <Tooltip label="Fit to screen" position="left">
        <ActionIcon variant="default" size="lg" onClick={fitToScreen} aria-label="Fit to screen">
          <IconMaximize size={16} />
        </ActionIcon>
      </Tooltip>
      <Tooltip label="Center on origin" position="left">
        <ActionIcon variant="default" size="lg" onClick={centerOnOrigin} aria-label="Center on origin">
          <IconFocusCentered size={16} />
        </ActionIcon>
      </Tooltip>
    </Stack>
  );
}

export function CanvasOverlays() {
  return (
    <>
      <IssuesBadge />
      <ZoomControls />
    </>
  );
}
