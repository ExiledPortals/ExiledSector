import { ActionIcon, Badge, Group, Paper, Stack, Text, Tooltip } from '@mantine/core';
import { IconAlertTriangle, IconFocusCentered, IconMaximize, IconMinus, IconPlus } from '@tabler/icons-react';
import { useMemo } from 'react';
import { useVocabulary } from '../../../api/vocabulary.ts';
import { showIssues } from '../../../shared/dialogs.tsx';
import { TIER_COLOR_VARIABLES, TIER_LABELS, type Tier } from '../model/types.ts';
import { validateTree } from '../model/validation.ts';
import { centerOnOrigin, fitToScreen, zoomBy } from '../state/treeActions.ts';
import { useTreeStore } from '../state/treeStore.ts';
import classes from './TreeCanvas.module.css';

const LEGEND_TIERS: Tier[] = ['SMALL', 'NOTABLE', 'KEYSTONE', 'WORMHOLE', 'SOCKET', 'ROOT'];

function Legend() {
  return (
    <Paper className={classes.overlayTopLeft} p={8} withBorder shadow="xs" opacity={0.92}>
      <Stack gap={3}>
        {LEGEND_TIERS.map((tier) => (
          <Group key={tier} gap={6} wrap="nowrap">
            <span className={classes.legendDot} style={{ background: TIER_COLOR_VARIABLES[tier], borderRadius: tier === 'SOCKET' ? 2 : undefined }} />
            <Text size="xs">{tier === 'ROOT' ? 'Root (ship start)' : TIER_LABELS[tier]}</Text>
          </Group>
        ))}
      </Stack>
    </Paper>
  );
}

function IssuesBadge() {
  const document = useTreeStore((state) => state.document);
  const vocabulary = useVocabulary();
  const issues = useMemo(() => validateTree(document, vocabulary.data ?? null), [document, vocabulary.data]);
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
      <Legend />
      <IssuesBadge />
      <ZoomControls />
    </>
  );
}
