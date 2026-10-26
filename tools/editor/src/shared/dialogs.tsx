import { List, Text } from '@mantine/core';
import { modals } from '@mantine/modals';
import { notifications } from '@mantine/notifications';

export interface ConfirmOptions {
  title: string;
  message: string;
  confirmLabel?: string;
  danger?: boolean;
}

export function confirmAction(options: ConfirmOptions): Promise<boolean> {
  return new Promise((resolve) => {
    modals.openConfirmModal({
      title: options.title,
      children: (
        <Text size="sm" style={{ whiteSpace: 'pre-line' }}>
          {options.message}
        </Text>
      ),
      labels: { confirm: options.confirmLabel ?? 'Continue', cancel: 'Cancel' },
      confirmProps: options.danger ? { color: 'red' } : undefined,
      onConfirm: () => resolve(true),
      onCancel: () => resolve(false),
      onClose: () => resolve(false),
    });
  });
}

export function showIssues(title: string, issues: string[]): void {
  modals.open({
    title,
    size: 'lg',
    children: (
      <List size="sm" spacing={4}>
        {issues.map((issue) => (
          <List.Item key={issue}>{issue}</List.Item>
        ))}
      </List>
    ),
  });
}

export function notifySuccess(message: string): void {
  notifications.show({ color: 'teal', message });
}

export function notifyError(message: string): void {
  notifications.show({ color: 'red', title: 'Something went wrong', message, autoClose: 8000 });
}

export function notifyInfo(message: string): void {
  notifications.show({ message, autoClose: 2000 });
}
