import { Button, Divider, Group, Paper, Select, Stack, Text, Title, UnstyledButton } from '@mantine/core';
import { IconArrowsExchange, IconTrash } from '@tabler/icons-react';
import { useStaticImages } from '../../../api/images.ts';
import type { Vocabulary } from '../../../api/vocabulary.ts';
import { CommitNumberInput, CommitTextInput } from '../../../shared/CommitInputs.tsx';
import { notifyError } from '../../../shared/dialogs.tsx';
import { ImageBrowser } from '../../../shared/ImageBrowser.tsx';
import { normalizeDegrees, resyncNodesOnAnchor, setAnchorShape } from '../model/geometry.ts';
import { REGION_ONLY_TAG_GROUPS } from '../model/tags.ts';
import {
  ANCHOR_RECTANGLE_ASPECT,
  ANCHOR_SHAPE_LABELS,
  ANCHOR_SHAPES,
  anchorById,
  DEFAULT_STAR_COLOR,
  defaultBeltBandWidth,
  imageById,
  RING_BELT_STYLES,
  starById,
  typeById,
  VANILLA_STAR_TYPES,
  type AnchorShape,
  type OrbitAnchor,
  type Star,
  type StaticImage,
  type TreeDocument,
} from '../model/types.ts';
import { clearSelection, deleteSelection, selectOnly } from '../state/treeActions.ts';
import { editDocument, selectionCount, setSelection, useTreeStore } from '../state/treeStore.ts';
import { CommitColorInput } from './CommitColorInput.tsx';
import { TagGroupsField } from './TagGroupsField.tsx';

function degreesOrZero(value: number): number {
  return normalizeDegrees(Number.isNaN(value) ? 0 : value);
}

function setTags(target: { tags?: string[] }, tags: string[]): void {
  if (tags.length) {
    target.tags = tags;
  } else {
    delete target.tags;
  }
}

function RegionField({ vocabulary, tags, onChange }: { vocabulary: Vocabulary; tags: string[] | undefined; onChange: (tags: string[]) => void }) {
  return (
    <TagGroupsField label="Area (region tag, used by the in-game area toggles)" vocabulary={vocabulary} tags={tags ?? []} groups={REGION_ONLY_TAG_GROUPS} onChange={onChange} />
  );
}

function editAnchor(anchorId: string, change: (anchor: OrbitAnchor, document: TreeDocument) => void): void {
  editDocument((document) => {
    const anchor = anchorById(document, anchorId);
    if (anchor) {
      change(anchor, document);
      resyncNodesOnAnchor(document, anchor);
    }
  });
}

export function AnchorInspector({ anchorId, vocabulary }: { anchorId: string; vocabulary: Vocabulary }) {
  const document = useTreeStore((state) => state.document);
  const anchor = anchorById(document, anchorId);
  if (!anchor) {
    return null;
  }
  const shape = anchor.shape ?? 'circle';
  const isAsteroidBelt = shape === 'asteroidBelt';
  const rename = (text: string) => {
    const newId = text.trim();
    if (!newId || anchorById(document, newId)) {
      notifyError('Id must be unique and non-empty');
      return;
    }
    editDocument((draft) => {
      const target = anchorById(draft, anchorId);
      if (!target) {
        return;
      }
      target.id = newId;
      for (const node of draft.nodes) {
        if (node.orbitAnchorId === anchorId) {
          node.orbitAnchorId = newId;
        }
      }
    });
    setSelection({ anchors: [newId] });
  };

  return (
    <Stack gap="sm">
      <Title order={4}>Anchor</Title>
      <CommitTextInput size="xs" label="Anchor id" value={anchor.id} onCommit={rename} />
      <Group grow gap={6}>
        <CommitNumberInput size="xs" label="X" value={anchor.x} onCommit={(value) => editAnchor(anchorId, (target) => {
          target.x = value || 0;
        })} />
        <CommitNumberInput size="xs" label="Y" value={anchor.y} onCommit={(value) => editAnchor(anchorId, (target) => {
          target.y = value || 0;
        })} />
      </Group>
      <Group grow gap={6}>
        <CommitNumberInput
          size="xs"
          label="Radius"
          value={anchor.radius}
          onCommit={(value) => editAnchor(anchorId, (target) => {
            target.radius = Math.max(10, Math.trunc(value) || 10);
          })}
        />
        <Select
          size="xs"
          label="Shape"
          data={ANCHOR_SHAPES.map((value) => ({ value, label: ANCHOR_SHAPE_LABELS[value] }))}
          value={shape}
          allowDeselect={false}
          onChange={(value) => editAnchor(anchorId, (target) => setAnchorShape(target, (value ?? 'circle') as AnchorShape))}
        />
      </Group>
      {shape === 'rectangle' ? (
        <CommitNumberInput
          size="xs"
          label="Aspect ratio (width/height)"
          value={anchor.aspectRatio || ANCHOR_RECTANGLE_ASPECT}
          onCommit={(value) => editAnchor(anchorId, (target) => {
            target.aspectRatio = Math.max(0.1, value || ANCHOR_RECTANGLE_ASPECT);
          })}
        />
      ) : null}
      {shape !== 'circle' && !isAsteroidBelt ? (
        <CommitNumberInput
          size="xs"
          label="Rotation (degrees)"
          value={anchor.rotation || 0}
          onCommit={(value) => editAnchor(anchorId, (target) => {
            target.rotation = degreesOrZero(value);
          })}
        />
      ) : null}
      {isAsteroidBelt ? (
        <>
          <Divider />
          <Group grow gap={6}>
            <CommitNumberInput
              size="xs"
              label="Band width"
              value={anchor.beltBandWidth || defaultBeltBandWidth(anchor)}
              onCommit={(value) => editAnchor(anchorId, (target) => {
                target.beltBandWidth = Math.max(10, Math.trunc(value) || 10);
              })}
            />
            <Select
              size="xs"
              label="Ring style"
              data={RING_BELT_STYLES}
              value={anchor.beltStyle || 'asteroids'}
              allowDeselect={false}
              onChange={(value) => editAnchor(anchorId, (target) => {
                target.beltStyle = value ?? 'asteroids';
              })}
            />
          </Group>
          <Group grow gap={6}>
            <CommitNumberInput
              size="xs"
              label="Start angle (degrees)"
              value={anchor.beltRotation || 0}
              onCommit={(value) => editAnchor(anchorId, (target) => {
                target.beltRotation = degreesOrZero(value);
              })}
            />
            <CommitNumberInput
              size="xs"
              label="Rotation speed (degrees/sec)"
              value={anchor.beltRotationSpeed || 0}
              onCommit={(value) => editAnchor(anchorId, (target) => {
                target.beltRotationSpeed = value || 0;
              })}
            />
          </Group>
          <RegionField vocabulary={vocabulary} tags={anchor.tags} onChange={(tags) => editAnchor(anchorId, (target) => setTags(target, tags))} />
        </>
      ) : null}
      <Divider />
      <Button size="xs" color="red" variant="light" leftSection={<IconTrash size={14} />} onClick={deleteSelection}>
        Delete this anchor (Q)
      </Button>
    </Stack>
  );
}

function editImage(imageId: string, change: (image: StaticImage) => void): void {
  editDocument((document) => {
    const image = imageById(document, imageId);
    if (image) {
      change(image);
    }
  });
}

export function ImageInspector({ imageId, vocabulary }: { imageId: string; vocabulary: Vocabulary }) {
  const document = useTreeStore((state) => state.document);
  const staticImages = useStaticImages();
  const image = imageById(document, imageId);
  if (!image) {
    return null;
  }
  const rename = (text: string) => {
    const newId = text.trim();
    if (!newId || imageById(document, newId)) {
      notifyError('Id must be unique and non-empty');
      return;
    }
    editImage(imageId, (target) => {
      target.id = newId;
    });
    setSelection({ images: [newId] });
  };

  return (
    <Stack gap="sm">
      <Title order={4}>Static image</Title>
      <CommitTextInput size="xs" label="Image id" value={image.id} onCommit={rename} />
      <Group grow gap={6}>
        <CommitNumberInput size="xs" label="X" value={image.x} onCommit={(value) => editImage(imageId, (target) => {
          target.x = value || 0;
        })} />
        <CommitNumberInput size="xs" label="Y" value={image.y} onCommit={(value) => editImage(imageId, (target) => {
          target.y = value || 0;
        })} />
      </Group>
      <Group grow gap={6}>
        <CommitNumberInput
          size="xs"
          label="Width"
          value={image.width}
          onCommit={(value) => editImage(imageId, (target) => {
            target.width = Math.max(20, Math.trunc(value) || 20);
          })}
        />
        <CommitNumberInput
          size="xs"
          label="Height"
          value={image.height}
          onCommit={(value) => editImage(imageId, (target) => {
            target.height = Math.max(20, Math.trunc(value) || 20);
          })}
        />
      </Group>
      <Group grow gap={6} align="flex-end">
        <CommitNumberInput
          size="xs"
          label="Rotation (deg)"
          value={image.rotation || 0}
          onCommit={(value) => editImage(imageId, (target) => {
            target.rotation = degreesOrZero(value);
          })}
        />
        <CommitNumberInput
          size="xs"
          label="Spin (deg/sec)"
          value={image.rotationSpeed || 0}
          onCommit={(value) => editImage(imageId, (target) => {
            target.rotationSpeed = Number.isNaN(value) ? 0 : value;
          })}
        />
      </Group>
      <Button
        size="xs"
        variant="default"
        w="fit-content"
        leftSection={<IconArrowsExchange size={14} />}
        onClick={() => editImage(imageId, (target) => {
          target.rotationSpeed = -(target.rotationSpeed || 0);
        })}
      >
        Reverse spin
      </Button>
      {staticImages.data && staticImages.data.length > 0 ? (
        <ImageBrowser paths={staticImages.data} selectedPath={image.imagePath} onSelect={(path) => editImage(imageId, (target) => {
          target.imagePath = path;
        })} height={260} />
      ) : (
        <Text size="xs" c="dimmed">
          No images found under graphics/backgrounds/static_images/.
        </Text>
      )}
      <RegionField vocabulary={vocabulary} tags={image.tags} onChange={(tags) => editImage(imageId, (target) => setTags(target, tags))} />
      <Divider />
      <Button size="xs" color="red" variant="light" leftSection={<IconTrash size={14} />} onClick={deleteSelection}>
        Delete this image (Q)
      </Button>
    </Stack>
  );
}

function editStar(starId: string, change: (star: Star) => void): void {
  editDocument((document) => {
    const star = starById(document, starId);
    if (star) {
      change(star);
    }
  });
}

export function StarInspector({ starId, vocabulary }: { starId: string; vocabulary: Vocabulary }) {
  const document = useTreeStore((state) => state.document);
  const star = starById(document, starId);
  if (!star) {
    return null;
  }
  const rename = (text: string) => {
    const newId = text.trim();
    if (!newId || starById(document, newId)) {
      notifyError('Id must be unique and non-empty');
      return;
    }
    editStar(starId, (target) => {
      target.id = newId;
    });
    setSelection({ stars: [newId] });
  };

  return (
    <Stack gap="sm">
      <Title order={4}>Star</Title>
      <CommitTextInput size="xs" label="Star id" value={star.id} onCommit={rename} />
      <Group grow gap={6}>
        <CommitNumberInput size="xs" label="X" value={star.x} onCommit={(value) => editStar(starId, (target) => {
          target.x = value || 0;
        })} />
        <CommitNumberInput size="xs" label="Y" value={star.y} onCommit={(value) => editStar(starId, (target) => {
          target.y = value || 0;
        })} />
      </Group>
      <CommitNumberInput
        size="xs"
        label="Radius (vanilla stars run roughly 400-1800)"
        value={star.radius}
        onCommit={(value) => editStar(starId, (target) => {
          target.radius = Math.max(20, Math.trunc(value) || 20);
        })}
      />
      <Select
        size="xs"
        label="Star type"
        data={VANILLA_STAR_TYPES}
        value={star.starType}
        allowDeselect={false}
        onChange={(value) => editStar(starId, (target) => {
          target.starType = value ?? 'star_yellow';
        })}
      />
      <Group gap={6} align="flex-end" wrap="nowrap">
        <CommitColorInput
          size="xs"
          style={{ flex: 1 }}
          label="Colour override"
          value={star.color || DEFAULT_STAR_COLOR}
          onCommit={(colour) => editStar(starId, (target) => {
            target.color = colour;
          })}
        />
        <Button size="xs" variant="default" onClick={() => editStar(starId, (target) => {
          target.color = null;
        })}>
          Use vanilla colour
        </Button>
      </Group>
      <RegionField vocabulary={vocabulary} tags={star.tags} onChange={(tags) => editStar(starId, (target) => setTags(target, tags))} />
      <Divider />
      <Button size="xs" color="red" variant="light" leftSection={<IconTrash size={14} />} onClick={deleteSelection}>
        Delete this star (Q)
      </Button>
    </Stack>
  );
}

export function MultiSelectionInspector() {
  const selection = useTreeStore((state) => state.selection);
  const document = useTreeStore((state) => state.document);
  const total = selectionCount(selection);
  const rows = [
    ...selection.nodes.map((id) => {
      const node = document.nodes.find((candidate) => candidate.id === id);
      const type = node ? typeById(document, node.type) : undefined;
      return { kind: 'nodes' as const, id, label: type ? `${id} (${type.name})` : id };
    }),
    ...selection.anchors.map((id) => ({ kind: 'anchors' as const, id, label: `${id} (anchor)` })),
    ...selection.images.map((id) => ({ kind: 'images' as const, id, label: `${id} (image)` })),
    ...selection.stars.map((id) => ({ kind: 'stars' as const, id, label: `${id} (star)` })),
  ];
  return (
    <Stack gap="sm">
      <Title order={4}>{total} objects selected</Title>
      <Stack gap={4}>
        {rows.map((row) => (
          <UnstyledButton key={`${row.kind}:${row.id}`} onClick={() => selectOnly(row.kind, row.id)}>
            <Paper withBorder px={8} py={3}>
              <Text size="xs" ff="monospace" truncate>
                {row.label}
              </Text>
            </Paper>
          </UnstyledButton>
        ))}
      </Stack>
      <Group gap={6}>
        <Button size="xs" variant="default" onClick={clearSelection}>
          Clear selection
        </Button>
      </Group>
      <Button size="xs" color="red" variant="light" leftSection={<IconTrash size={14} />} onClick={deleteSelection}>
        Delete {total} selected (Q)
      </Button>
    </Stack>
  );
}
