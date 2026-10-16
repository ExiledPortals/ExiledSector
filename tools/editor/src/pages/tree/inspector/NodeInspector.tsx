import { ActionIcon, Badge, Button, Checkbox, Divider, Group, Paper, Select, Stack, Text, Title, Tooltip } from '@mantine/core';
import { IconCopy, IconEdit, IconExchange, IconTrash, IconX } from '@tabler/icons-react';
import { useMemo } from 'react';
import type { Vocabulary } from '../../../api/vocabulary.ts';
import { CommitNumberInput, CommitTextInput } from '../../../shared/CommitInputs.tsx';
import { notifyError, notifyInfo } from '../../../shared/dialogs.tsx';
import { ImagePickButton } from '../../../shared/ImagePickButton.tsx';
import { openTypeEditor, openTypePicker } from '../dialogs/treeDialogs.tsx';
import { connectNodes, isConnectorHidden, pairWormholes, renameNode, toggleConnectorHidden, unlinkNodes } from '../model/connectors.ts';
import { NODE_TAG_GROUPS } from '../model/tags.ts';
import {
  AURORA_RING_BELT_PATH,
  isAuroraBelt,
  KEYSTONE_BELT_DEFAULT_WIDTH,
  nodeById,
  TIER_COLOR_VARIABLES,
  tierOf,
  typeById,
  type TreeDocument,
  type TreeNode,
} from '../model/types.ts';
import { deleteSelection, duplicateNode } from '../state/treeActions.ts';
import { editDocument, setSelection, useTreeStore } from '../state/treeStore.ts';
import { CommitColorInput } from './CommitColorInput.tsx';
import { TagGroupsField } from './TagGroupsField.tsx';
import { TypeForm, type TypeRecipe } from './TypeForm.tsx';
import { INSPECTOR_TYPE_SECTIONS } from './typeFormSections.ts';

function editNode(nodeId: string, change: (node: TreeNode, document: TreeDocument) => void): void {
  editDocument((document) => {
    const node = nodeById(document, nodeId);
    if (node) {
      change(node, document);
    }
  });
}

function typeHint(node: TreeNode, document: TreeDocument): string {
  const type = typeById(document, node.type);
  let hint = `Type "${node.type}"`;
  if (type?.vanillaHullMod) {
    hint += `, vanillaHullMod: ${type.vanillaHullMod}`;
  }
  if (type?.exclusiveHullMods?.length) {
    hint += `, exclusive: ${type.exclusiveHullMods.join(', ')}`;
  }
  if (type?.todo) {
    hint += ` [TODO: ${type.todo}]`;
  }
  return hint;
}

function ConnectionList({ node, document }: { node: TreeNode; document: TreeDocument }) {
  const nodeIds = useMemo(() => new Set(document.nodes.map((other) => other.id)), [document.nodes]);
  const otherOptions = useMemo(() => document.nodes.filter((other) => other.id !== node.id).map((other) => other.id), [document.nodes, node.id]);
  return (
    <Stack gap={4}>
      <Text size="sm" fw={500}>
        Connected to
      </Text>
      {node.connectedTo.length === 0 ? (
        <Text size="xs" c="dimmed">
          None.
        </Text>
      ) : null}
      {node.connectedTo.map((connectedId) => (
        <Group key={connectedId} gap={6} wrap="nowrap">
          <Paper withBorder px={8} py={3} style={{ flex: 1, minWidth: 0 }}>
            <Group gap={4} wrap="nowrap" justify="space-between">
              <Text size="xs" ff="monospace" truncate c={nodeIds.has(connectedId) ? undefined : 'red'}>
                {connectedId}
                {nodeIds.has(connectedId) ? '' : ' (missing!)'}
              </Text>
              <ActionIcon size="xs" variant="subtle" color="gray" aria-label="Remove connection" onClick={() => editNode(node.id, (draft, doc) => unlinkNodes(doc, draft, connectedId))}>
                <IconX size={12} />
              </ActionIcon>
            </Group>
          </Paper>
          <Tooltip label="Hide this connector's line in-game (the connection still counts for unlocking)">
            <Checkbox
              size="xs"
              aria-label="Hide connector"
              checked={isConnectorHidden(document, node.id, connectedId)}
              onChange={() => editDocument((doc) => toggleConnectorHidden(doc, node.id, connectedId))}
            />
          </Tooltip>
        </Group>
      ))}
      <Select
        size="xs"
        placeholder="Connect to..."
        data={otherOptions}
        value={null}
        searchable
        limit={60}
        onChange={(otherId) => {
          if (otherId) {
            editNode(node.id, (draft, doc) => connectNodes(doc, draft, otherId));
          }
        }}
      />
    </Stack>
  );
}

function KeystoneFields({ node }: { node: TreeNode }) {
  return (
    <Stack gap={6}>
      <Group gap={6} align="flex-end" wrap="nowrap">
        <CommitTextInput
          size="xs"
          style={{ flex: 1 }}
          label="Ring belt art (optional)"
          placeholder={AURORA_RING_BELT_PATH}
          value={node.ringBeltPath ?? ''}
          onCommit={(text) =>
            editNode(node.id, (draft) => {
              if (text.trim()) {
                draft.ringBeltPath = text.trim();
              } else {
                delete draft.ringBeltPath;
              }
            })
          }
        />
        <ImagePickButton
          size="xs"
          targetFolder={null}
          onPicked={(path) => {
            editNode(node.id, (draft) => {
              draft.ringBeltPath = path;
            });
            notifyInfo(`Ring belt art set to "${path}"`);
          }}
        />
      </Group>
      {isAuroraBelt(node) ? (
        <CommitColorInput
          size="xs"
          label="Aurora colour"
          value={node.ringBeltColor || '#8c78ff'}
          onCommit={(colour) =>
            editNode(node.id, (draft) => {
              draft.ringBeltColor = colour;
            })
          }
        />
      ) : null}
      <CommitNumberInput
        size="xs"
        label={`Ring belt width (x node size, default ${KEYSTONE_BELT_DEFAULT_WIDTH})`}
        placeholder={String(KEYSTONE_BELT_DEFAULT_WIDTH)}
        value={node.ringBeltWidth || null}
        onCommit={(width) =>
          editNode(node.id, (draft) => {
            if (width > 0) {
              draft.ringBeltWidth = width;
            } else {
              delete draft.ringBeltWidth;
            }
          })
        }
      />
    </Stack>
  );
}

function WormholeFields({ node, document }: { node: TreeNode; document: TreeDocument }) {
  const partners = document.nodes.filter((other) => other.id !== node.id && tierOf(typeById(document, other.type)) === 'WORMHOLE').map((other) => other.id);
  return (
    <Stack gap={6}>
      <CommitColorInput
        size="xs"
        label="Wormhole colour"
        value={node.wormholeColor || '#ffffff'}
        onCommit={(colour) =>
          editNode(node.id, (draft) => {
            draft.wormholeColor = colour;
          })
        }
      />
      <Select
        size="xs"
        label="Paired wormhole node"
        data={[{ value: '', label: 'None (invalid)' }, ...partners.map((id) => ({ value: id, label: id }))]}
        value={node.pairedWith ?? ''}
        allowDeselect={false}
        onChange={(partnerId) => editNode(node.id, (draft, doc) => pairWormholes(doc, draft, partnerId ?? ''))}
      />
    </Stack>
  );
}

export function NodeInspector({ nodeId, vocabulary }: { nodeId: string; vocabulary: Vocabulary }) {
  const document = useTreeStore((state) => state.document);
  const node = nodeById(document, nodeId);
  const typeOptions = useMemo(() => document.types.map((type) => ({ value: type.id, label: `${type.name} (${type.id})` })), [document.types]);
  if (!node) {
    return null;
  }
  const type = typeById(document, node.type);
  const tier = tierOf(type);
  const sharedNote = type ? ` - shared by every "${type.id}" node` : '';
  const editType = (recipe: TypeRecipe) =>
    editDocument((doc) => {
      const target = typeById(doc, node.type);
      if (target) {
        recipe(target);
      }
    });

  const rename = (text: string) => {
    const newId = text.trim();
    if (!newId || nodeById(document, newId)) {
      notifyError('Id must be unique and non-empty');
      return;
    }
    editNode(node.id, (draft, doc) => renameNode(doc, draft, newId));
    setSelection({ nodes: [newId] });
  };

  return (
    <Stack gap="sm">
      <Group justify="space-between" wrap="nowrap" align="flex-start">
        <Title order={4} style={{ wordBreak: 'break-word' }}>
          {type ? type.name : node.type}
        </Title>
        <Badge variant="outline" style={{ color: TIER_COLOR_VARIABLES[tier], borderColor: TIER_COLOR_VARIABLES[tier], flexShrink: 0 }}>
          {tier}
        </Badge>
      </Group>
      {type ? <TypeForm type={type} onEdit={editType} sections={['tags']} vocabulary={vocabulary} allTypes={document.types} note={sharedNote} /> : null}
      <TagGroupsField
        label="Node tags (this node only)"
        vocabulary={vocabulary}
        tags={node.tags ?? []}
        groups={NODE_TAG_GROUPS}
        onChange={(tags) =>
          editNode(node.id, (draft) => {
            if (tags.length) {
              draft.tags = tags;
            } else {
              delete draft.tags;
            }
          })
        }
      />
      <CommitTextInput size="xs" label="Node id" value={node.id} onCommit={rename} />
      <Select
        size="xs"
        label="Type"
        data={typeOptions}
        value={node.type}
        searchable
        allowDeselect={false}
        onChange={(typeId) => {
          if (typeId) {
            editNode(node.id, (draft) => {
              draft.type = typeId;
            });
          }
        }}
      />
      <Group grow gap={6}>
        <CommitNumberInput
          size="xs"
          label="X"
          value={node.x}
          onCommit={(value) =>
            editNode(node.id, (draft) => {
              draft.x = value || 0;
            })
          }
        />
        <CommitNumberInput
          size="xs"
          label="Y"
          value={node.y}
          onCommit={(value) =>
            editNode(node.id, (draft) => {
              draft.y = value || 0;
            })
          }
        />
      </Group>
      {tier === 'KEYSTONE' ? <KeystoneFields node={node} /> : null}
      {tier === 'WORMHOLE' ? <WormholeFields node={node} document={document} /> : null}
      <Divider />
      {type ? (
        <>
          <TypeForm type={type} onEdit={editType} sections={INSPECTOR_TYPE_SECTIONS} vocabulary={vocabulary} allTypes={document.types} note={sharedNote} />
          <Divider />
        </>
      ) : null}
      <ConnectionList node={node} document={document} />
      <Divider />
      <Text size="xs" c="dimmed">
        {typeHint(node, document)}
      </Text>
      <Group gap={6}>
        <Button
          size="xs"
          variant="default"
          leftSection={<IconExchange size={14} />}
          onClick={() =>
            openTypePicker(node.type, (typeId) => {
              editNode(node.id, (draft) => {
                draft.type = typeId;
              });
              notifyInfo(`Type changed to "${typeId}"`);
            })
          }
        >
          Change type
        </Button>
        <Button size="xs" variant="default" leftSection={<IconEdit size={14} />} disabled={!type} onClick={() => openTypeEditor(vocabulary, node.type)}>
          Edit type
        </Button>
        {type ? (
          <ImagePickButton
            size="xs"
            label="Select icon"
            onPicked={(path) => {
              editType((draft) => {
                draft.icon = path;
              });
              notifyInfo(`Icon set for type "${type.id}"`);
            }}
          />
        ) : null}
        <Button size="xs" variant="default" leftSection={<IconCopy size={14} />} onClick={() => duplicateNode(node.id)}>
          Duplicate (Ctrl+D)
        </Button>
      </Group>
      <Button size="xs" color="red" variant="light" leftSection={<IconTrash size={14} />} onClick={deleteSelection}>
        Delete this node (Q)
      </Button>
    </Stack>
  );
}
