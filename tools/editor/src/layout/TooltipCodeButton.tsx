import { Button, Stack, Text } from '@mantine/core';
import { IconCode } from '@tabler/icons-react';
import { useEffect } from 'react';
import { loadTooltipCodeStatus, refreshTooltipCode, useTooltipCodeStatus, type TooltipCodeStatus } from '../api/tooltipCode.ts';

function copiedAgo(copiedAtMs: number, nowMs: number): string {
  const minutes = Math.floor((nowMs - copiedAtMs) / 60000);
  if (minutes < 1) {
    return 'just now';
  }
  if (minutes < 60) {
    return `${minutes} min ago`;
  }
  const hours = Math.floor(minutes / 60);
  return hours < 24 ? `${hours} h ago` : new Date(copiedAtMs).toLocaleDateString();
}

function statusLine(status: TooltipCodeStatus | null): { text: string; failed: boolean } {
  if (!status) {
    return { text: 'Checking the tooltip code...', failed: false };
  }
  if (status.build !== 'idle') {
    return { text: status.message, failed: status.build === 'failed' };
  }
  return { text: status.copiedAtMs === null ? 'No copy yet' : `Copied ${copiedAgo(status.copiedAtMs, Date.now())}`, failed: false };
}

export function TooltipCodeButton() {
  const status = useTooltipCodeStatus((state) => state.status);
  const busy = status?.build === 'building' || status?.build === 'waiting';
  const line = statusLine(status);

  useEffect(() => {
    void loadTooltipCodeStatus();
  }, []);

  return (
    <Stack gap={4} px="xs">
      <Button
        size="xs"
        variant="default"
        leftSection={<IconCode size={14} />}
        loading={busy}
        onClick={() => void refreshTooltipCode()}
        title="Recompile the mod and copy its tooltip code into the editor, so tooltips show the latest Java changes"
      >
        Refresh tooltip code
      </Button>
      <Text size="xs" c={line.failed ? 'red' : 'dimmed'} data-testid="tooltip-code-status">
        {line.text}
      </Text>
    </Stack>
  );
}
