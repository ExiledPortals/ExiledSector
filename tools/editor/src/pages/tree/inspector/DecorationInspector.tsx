import { Button, Divider, Group, Select, Stack, Switch, Text, Title } from '@mantine/core';
import { IconArrowsExchange, IconTrash } from '@tabler/icons-react';
import type { ReactNode } from 'react';
import { useStaticImages } from '../../../api/images.ts';
import type { Vocabulary } from '../../../api/vocabulary.ts';
import { CommitNumberInput, CommitTextInput } from '../../../shared/CommitInputs.tsx';
import { notifyError } from '../../../shared/dialogs.tsx';
import { ImageBrowser } from '../../../shared/ImageBrowser.tsx';
import {
  DECORATION_KINDS,
  decorationById,
  DEFAULT_STAR_COLOR,
  MIN_DECORATION_SIZE,
  RING_BELT_STYLES,
  ringBeltArtPath,
  ringBeltStyleOf,
  THREAT_UNIT_SPRITES,
  THREAT_UNIT_TYPES,
  VANILLA_STAR_TYPES,
  type Decoration,
  type DecorationKind,
  type DecorationOfKind,
} from '../model/decorations.ts';
import { deleteSelection } from '../state/treeActions.ts';
import { editDocument, setSelection, useTreeStore } from '../state/treeStore.ts';
import { CommitColorInput } from './CommitColorInput.tsx';
import { degreesOrZero, numberOrZero, setTags } from './inspectorEdits.ts';
import { RegionField } from './RegionField.tsx';

type Edit<TKind extends DecorationKind> = (change: (decoration: DecorationOfKind<TKind>) => void) => void;

interface FieldsProps<TKind extends DecorationKind> {
  decoration: DecorationOfKind<TKind>;
  edit: Edit<TKind>;
}

function sizeOrMinimum(value: number): number {
  return Math.max(MIN_DECORATION_SIZE, Math.trunc(value) || MIN_DECORATION_SIZE);
}

function SpinFields({ rotation, rotationSpeed, edit }: { rotation: number; rotationSpeed: number; edit: (change: (target: { rotation: number; rotationSpeed: number }) => void) => void }) {
  return (
    <>
      <Group grow gap={6} align="flex-end">
        <CommitNumberInput size="xs" label="Rotation (deg)" value={rotation || 0} onCommit={(value) => edit((target) => void (target.rotation = degreesOrZero(value)))} />
        <CommitNumberInput size="xs" label="Spin (deg/sec)" value={rotationSpeed || 0} onCommit={(value) => edit((target) => void (target.rotationSpeed = numberOrZero(value)))} />
      </Group>
      <Button
        size="xs"
        variant="default"
        w="fit-content"
        leftSection={<IconArrowsExchange size={14} />}
        onClick={() => edit((target) => void (target.rotationSpeed = -(target.rotationSpeed || 0)))}
      >
        Reverse spin
      </Button>
    </>
  );
}

function StarFields({ decoration: star, edit }: FieldsProps<'star'>) {
  return (
    <>
      <CommitNumberInput
        size="xs"
        label="Radius (vanilla stars run roughly 400-1800)"
        value={star.radius}
        onCommit={(value) => edit((target) => void (target.radius = sizeOrMinimum(value)))}
      />
      <Select
        size="xs"
        label="Star type"
        data={VANILLA_STAR_TYPES}
        value={star.starType}
        allowDeselect={false}
        onChange={(value) => edit((target) => void (target.starType = value ?? 'star_yellow'))}
      />
      <Group gap={6} align="flex-end" wrap="nowrap">
        <CommitColorInput
          size="xs"
          style={{ flex: 1 }}
          label="Colour override"
          value={star.color || DEFAULT_STAR_COLOR}
          onCommit={(colour) => edit((target) => void (target.color = colour))}
        />
        <Button size="xs" variant="default" onClick={() => edit((target) => void (target.color = null))}>
          Use vanilla colour
        </Button>
      </Group>
    </>
  );
}

function ImageFields({ decoration: image, edit }: FieldsProps<'image'>) {
  const staticImages = useStaticImages();
  return (
    <>
      <Group grow gap={6}>
        <CommitNumberInput size="xs" label="Width" value={image.width} onCommit={(value) => edit((target) => void (target.width = sizeOrMinimum(value)))} />
        <CommitNumberInput size="xs" label="Height" value={image.height} onCommit={(value) => edit((target) => void (target.height = sizeOrMinimum(value)))} />
      </Group>
      <SpinFields rotation={image.rotation} rotationSpeed={image.rotationSpeed} edit={edit} />
      <Switch
        size="xs"
        label="Draw behind the stars"
        checked={Boolean(image.behindStars)}
        onChange={(event) => {
          const behindStars = event.currentTarget.checked;
          edit((target) => {
            if (behindStars) {
              target.behindStars = true;
            } else {
              delete target.behindStars;
            }
          });
        }}
      />
      {staticImages.data && staticImages.data.length > 0 ? (
        <ImageBrowser paths={staticImages.data} selectedPath={image.imagePath} onSelect={(path) => edit((target) => void (target.imagePath = path))} height={260} />
      ) : (
        <Text size="xs" c="dimmed">
          No images found under graphics/backgrounds/static_images/.
        </Text>
      )}
    </>
  );
}

function RingBeltFields({ decoration: belt, edit }: FieldsProps<'ringBelt'>) {
  const style = ringBeltStyleOf(belt.ringArtPath);
  return (
    <>
      <Group grow gap={6}>
        <CommitNumberInput
          size="xs"
          label="Inner radius"
          value={belt.innerRadius}
          onCommit={(value) => edit((target) => void (target.innerRadius = Math.max(0, Math.min(Math.trunc(value) || 0, target.outerRadius - MIN_DECORATION_SIZE))))}
        />
        <CommitNumberInput
          size="xs"
          label="Outer radius"
          value={belt.outerRadius}
          onCommit={(value) => edit((target) => void (target.outerRadius = Math.max(target.innerRadius + MIN_DECORATION_SIZE, Math.trunc(value) || 0)))}
        />
      </Group>
      <Select
        size="xs"
        label="Ring style"
        data={style ? RING_BELT_STYLES : [{ value: '', label: `Custom: ${belt.ringArtPath}` }, ...RING_BELT_STYLES]}
        value={style ?? ''}
        allowDeselect={false}
        onChange={(value) => value && edit((target) => void (target.ringArtPath = ringBeltArtPath(value)))}
      />
      <SpinFields rotation={belt.rotation} rotationSpeed={belt.rotationSpeed} edit={edit} />
    </>
  );
}

function ThreatUnitFields({ decoration: unit, edit }: FieldsProps<'threatUnit'>) {
  return (
    <>
      <Select
        size="xs"
        label="Unit type"
        data={THREAT_UNIT_TYPES.map((value) => ({ value, label: THREAT_UNIT_SPRITES[value].label }))}
        value={unit.unitType}
        allowDeselect={false}
        onChange={(value) => edit((target) => void (target.unitType = value ?? 'fabricator'))}
      />
      <Group grow gap={6}>
        <CommitNumberInput size="xs" label="Length" value={unit.length} onCommit={(value) => edit((target) => void (target.length = sizeOrMinimum(value)))} />
        <CommitNumberInput size="xs" label="Rotation (deg)" value={unit.rotation || 0} onCommit={(value) => edit((target) => void (target.rotation = degreesOrZero(value)))} />
      </Group>
    </>
  );
}

function SerpentFields({ decoration: serpent, edit }: FieldsProps<'serpent'>) {
  return (
    <>
      <Group grow gap={6}>
        <CommitNumberInput size="xs" label="Orbit radius" value={serpent.orbitRadius} onCommit={(value) => edit((target) => void (target.orbitRadius = sizeOrMinimum(value)))} />
        <CommitNumberInput
          size="xs"
          label="Seconds per orbit"
          value={serpent.orbitSeconds}
          onCommit={(value) => edit((target) => void (target.orbitSeconds = Math.max(1, numberOrZero(value))))}
        />
      </Group>
      <Group grow gap={6} align="flex-end">
        <CommitNumberInput size="xs" label="Start angle (deg)" value={serpent.startAngle} onCommit={(value) => edit((target) => void (target.startAngle = degreesOrZero(value)))} />
        <Switch size="xs" label="Clockwise" checked={serpent.clockwise} onChange={(event) => {
          const clockwise = event.currentTarget.checked;
          edit((target) => void (target.clockwise = clockwise));
        }} />
      </Group>
      <Group grow gap={6}>
        <CommitNumberInput
          size="xs"
          label="Wobble (share of radius)"
          value={serpent.wobble}
          onCommit={(value) => edit((target) => void (target.wobble = Math.max(0, numberOrZero(value))))}
        />
        <CommitNumberInput
          size="xs"
          label="Wobble waves"
          value={serpent.wobbleWaves}
          onCommit={(value) => edit((target) => void (target.wobbleWaves = Math.max(0, Math.trunc(numberOrZero(value)))))}
        />
        <CommitNumberInput size="xs" label="Wobble phase (deg)" value={serpent.wobblePhase} onCommit={(value) => edit((target) => void (target.wobblePhase = degreesOrZero(value)))} />
      </Group>
    </>
  );
}

const FIELDS: { [TKind in DecorationKind]: (props: FieldsProps<TKind>) => ReactNode } = {
  star: StarFields,
  image: ImageFields,
  ringBelt: RingBeltFields,
  threatUnit: ThreatUnitFields,
  serpent: SerpentFields,
};

function editDecoration(decorationId: string, change: (decoration: Decoration) => void): void {
  editDocument((document) => {
    const decoration = decorationById(document, decorationId);
    if (decoration) {
      change(decoration);
    }
  });
}

export function DecorationInspector({ decorationId, vocabulary }: { decorationId: string; vocabulary: Vocabulary }) {
  const document = useTreeStore((state) => state.document);
  const decoration = decorationById(document, decorationId);
  if (!decoration) {
    return null;
  }
  const spec = DECORATION_KINDS[decoration.kind];
  const edit = (change: (target: Decoration) => void) => editDecoration(decorationId, change);
  const rename = (text: string) => {
    const newId = text.trim();
    if (!newId || decorationById(document, newId)) {
      notifyError('Id must be unique among the decorations and non-empty');
      return;
    }
    edit((target) => void (target.id = newId));
    setSelection({ decorations: [newId] });
  };
  const Fields = FIELDS[decoration.kind] as (props: FieldsProps<DecorationKind>) => ReactNode;

  return (
    <Stack gap="sm">
      <Title order={4}>{spec.label}</Title>
      <CommitTextInput size="xs" label={`${spec.label} id`} value={decoration.id} onCommit={rename} />
      <Group grow gap={6}>
        <CommitNumberInput size="xs" label="X" value={decoration.x} onCommit={(value) => edit((target) => void (target.x = value || 0))} />
        <CommitNumberInput size="xs" label="Y" value={decoration.y} onCommit={(value) => edit((target) => void (target.y = value || 0))} />
      </Group>
      <Fields decoration={decoration as never} edit={edit as Edit<DecorationKind>} />
      <RegionField vocabulary={vocabulary} tags={decoration.tags} onChange={(tags) => edit((target) => setTags(target, tags))} />
      <Divider />
      <Button size="xs" color="red" variant="light" leftSection={<IconTrash size={14} />} onClick={deleteSelection}>
        Delete this {spec.label.toLowerCase()} (Q)
      </Button>
    </Stack>
  );
}
