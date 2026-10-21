import { Select, Text } from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { checkoutLabel, fetchCheckouts, switchCheckout } from '../api/checkouts.ts';
import { confirmAction, notifyError } from '../shared/dialogs.tsx';
import { confirmUnload, hasUnsavedWork } from '../shared/unsavedChanges.ts';

const CHECKOUTS_QUERY_KEY = ['checkouts'];

export function CheckoutPicker({ reloadPage = () => window.location.reload() }: { reloadPage?: () => void }) {
  const checkouts = useQuery({ queryKey: CHECKOUTS_QUERY_KEY, queryFn: fetchCheckouts });
  const [switching, setSwitching] = useState(false);
  const current = checkouts.data?.find((checkout) => checkout.current);

  const choose = async (root: string | null) => {
    if (!root || root === current?.root) {
      return;
    }
    if (
      hasUnsavedWork() &&
      !(await confirmAction({
        title: 'Unsaved changes',
        message: 'Switching checkouts reloads the editor. Discard the unsaved changes?',
        confirmLabel: 'Discard and switch',
        danger: true,
      }))
    ) {
      return;
    }
    setSwitching(true);
    const result = await switchCheckout(root);
    if (!result.ok) {
      setSwitching(false);
      notifyError(result.message);
      return;
    }
    confirmUnload();
    reloadPage();
  };

  if (checkouts.isError) {
    return (
      <Text size="xs" c="red" px="xs">
        {checkouts.error.message}
      </Text>
    );
  }

  return (
    <>
      <Select
        size="xs"
        px="xs"
        label="Branch"
        aria-label="Branch"
        allowDeselect={false}
        disabled={!checkouts.data || switching}
        value={current?.root ?? null}
        data={(checkouts.data ?? []).map((checkout) => ({
          value: checkout.root,
          label: checkout.editorMatches ? checkoutLabel(checkout) : `${checkoutLabel(checkout)} (editor differs)`,
        }))}
        onChange={(root) => void choose(root)}
      />
      {current && !current.editorMatches && (
        <Text size="xs" c="yellow" px="xs">
          This branch has different editor code. Saving may drop data fields this editor does not know.
        </Text>
      )}
    </>
  );
}
