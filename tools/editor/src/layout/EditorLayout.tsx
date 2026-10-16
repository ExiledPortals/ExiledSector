import { ActionIcon, AppShell, Group, NavLink, Stack, Text, Tooltip, useMantineColorScheme } from '@mantine/core';
import { IconBinaryTree, IconCpu, IconMoon, IconPhoto, IconSun } from '@tabler/icons-react';
import { useEffect, type ComponentType } from 'react';
import { NavLink as RouterNavLink, Outlet, useBlocker } from 'react-router';
import { confirmAction } from '../shared/dialogs.tsx';
import { hasUnsavedChanges } from '../shared/unsavedChanges.ts';

interface EditorPageLink {
  path: string;
  label: string;
  description: string;
  icon: ComponentType<{ size?: number; stroke?: number }>;
}

const PAGE_LINKS: EditorPageLink[] = [
  { path: '/tree', label: 'Skill tree', description: 'Nodes, types and layout', icon: IconBinaryTree },
  { path: '/images', label: 'Images', description: 'Browse, recolour and crop', icon: IconPhoto },
  { path: '/socketables', label: 'Socketables', description: 'Definitions, names, drops, crafting', icon: IconCpu },
];

export const NAVBAR_WIDTH = 230;

function ColorSchemeToggle() {
  const { colorScheme, toggleColorScheme } = useMantineColorScheme();
  const dark = colorScheme === 'dark';
  return (
    <Tooltip label={dark ? 'Light theme' : 'Dark theme'}>
      <ActionIcon variant="subtle" color="gray" onClick={toggleColorScheme} aria-label="Toggle colour theme">
        {dark ? <IconSun size={18} /> : <IconMoon size={18} />}
      </ActionIcon>
    </Tooltip>
  );
}

function useUnsavedChangesGuard() {
  const blocker = useBlocker(({ currentLocation, nextLocation }) => currentLocation.pathname !== nextLocation.pathname && hasUnsavedChanges());

  useEffect(() => {
    if (blocker.state !== 'blocked') {
      return;
    }
    void confirmAction({
      title: 'Unsaved changes',
      message: 'This page has unsaved changes. Leave and discard them?',
      confirmLabel: 'Discard and leave',
      danger: true,
    }).then((leave) => (leave ? blocker.proceed() : blocker.reset()));
  }, [blocker]);

  useEffect(() => {
    const warnBeforeUnload = (event: BeforeUnloadEvent) => {
      if (hasUnsavedChanges()) {
        event.preventDefault();
      }
    };
    window.addEventListener('beforeunload', warnBeforeUnload);
    return () => window.removeEventListener('beforeunload', warnBeforeUnload);
  }, []);
}

export function EditorLayout() {
  useUnsavedChangesGuard();
  return (
    <AppShell navbar={{ width: NAVBAR_WIDTH, breakpoint: 0 }} padding={0}>
      <AppShell.Navbar p="sm">
        <Stack gap="xs" h="100%">
          <Group justify="space-between" px="xs" pb="xs">
            <Text fw={700} size="sm">
              Exiled Sector Editor
            </Text>
            <ColorSchemeToggle />
          </Group>
          {PAGE_LINKS.map((link) => (
            <NavLink
              key={link.path}
              component={RouterNavLink}
              to={link.path}
              label={link.label}
              description={link.description}
              leftSection={<link.icon size={20} stroke={1.6} />}
            />
          ))}
        </Stack>
      </AppShell.Navbar>
      <AppShell.Main h="100dvh">
        <Outlet />
      </AppShell.Main>
    </AppShell>
  );
}
