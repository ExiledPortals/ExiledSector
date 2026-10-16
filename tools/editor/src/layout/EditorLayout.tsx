import { ActionIcon, AppShell, Group, NavLink, Stack, Text, Tooltip, useMantineColorScheme } from '@mantine/core';
import { IconBinaryTree, IconCpu, IconMoon, IconPhoto, IconSun } from '@tabler/icons-react';
import type { ComponentType } from 'react';
import { NavLink as RouterNavLink, Outlet } from 'react-router';

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

export function EditorLayout() {
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
