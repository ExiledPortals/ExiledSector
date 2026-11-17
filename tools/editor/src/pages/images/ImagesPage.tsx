import { Alert, Box, Button, Center, Divider, Group, Image, List, Loader, NumberInput, Paper, ScrollArea, Stack, Text, TextInput, Title } from '@mantine/core';
import { IconAlertTriangle, IconArrowRight, IconCircle, IconColorSwatch, IconRotateClockwise, IconTrash } from '@tabler/icons-react';
import { useMemo, useState, type CSSProperties, type ReactNode } from 'react';
import {
  deleteImage,
  moveImage,
  postImageCopy,
  useImageListing,
  useImageUsage,
  useRefreshImages,
  type ImageUse,
  type ImageCopyOperation,
  type ImageCopyParameters,
  type ImageCopyRequest,
} from '../../api/images.ts';
import { confirmAction, notifyError, notifySuccess } from '../../shared/dialogs.tsx';
import { ImageBrowser } from '../../shared/ImageBrowser.tsx';
import { ATLAS_CIRCULAR_POOL_FOLDER, ATLAS_ICON_FOLDER, ATLAS_POOL_FOLDER, CIRCULAR_ART_FOLDER, UNUSED_ART_FOLDER, isInFolder } from '../../shared/imageFolders.ts';
import { fileUrl, imageFileName, isUnshippedImagePath } from '../../shared/imagePaths.ts';

const DETAIL_WIDTH = 320;
const DEFAULT_HUE_SHIFT = 240;
const DEFAULT_SUFFIX = 'shifted';
const DEFAULT_ROTATION = 90;
const ROTATION_STEP = 90;
const OPERATION_PREVIEW_SIZE = 120;

async function createCopy<TOperation extends ImageCopyOperation>(
  operation: TOperation,
  request: ImageCopyRequest,
  parameters: ImageCopyParameters[TOperation],
): Promise<string | null> {
  const result = await postImageCopy(operation, request, parameters);
  if (result.ok) {
    return result.data.path ?? null;
  }
  if (result.status === 409 && result.data.exists) {
    const overwrite = await confirmAction({
      title: 'Image already exists',
      message: `${result.message} Overwrite it? Anything that already uses that image will show the new copy.`,
      confirmLabel: 'Overwrite',
      danger: true,
    });
    return overwrite ? createCopy(operation, { ...request, overwrite: true }, parameters) : null;
  }
  notifyError(`Could not complete: ${result.message}`);
  return null;
}

function isQuarterTurn(degrees: number): boolean {
  return degrees % ROTATION_STEP === 0;
}

function OperationPreview({ children }: { children: ReactNode }) {
  return (
    <Paper withBorder bg="dark.8" h={OPERATION_PREVIEW_SIZE + 16} style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden' }}>
      {children}
    </Paper>
  );
}

const PREVIEW_IMAGE_STYLE: CSSProperties = { display: 'block', maxWidth: OPERATION_PREVIEW_SIZE, maxHeight: OPERATION_PREVIEW_SIZE };

function RotationPreview({ path, degrees }: { path: string; degrees: number }) {
  return (
    <OperationPreview>
      <span data-testid="rotation-clip" style={{ display: 'inline-block', lineHeight: 0, overflow: isQuarterTurn(degrees) ? 'visible' : 'hidden' }}>
        <img src={fileUrl(path)} alt={`${path} rotated`} style={{ ...PREVIEW_IMAGE_STYLE, transform: `rotate(${degrees}deg)` }} />
      </span>
    </OperationPreview>
  );
}

function UsageList({ uses }: { uses: ImageUse[] | undefined }) {
  if (!uses || uses.length === 0) {
    return (
      <Text size="xs" c="green">
        Not used by any data file or code.
      </Text>
    );
  }
  return (
    <Stack gap={2}>
      <Text size="xs" fw={600}>
        Used by
      </Text>
      <List size="xs" spacing={2}>
        {uses.map((use) => (
          <List.Item key={`${use.file}|${use.detail}`}>
            <Text span size="xs" ff="monospace">
              {use.file}
            </Text>
            {use.detail ? <Text span size="xs">{` - ${use.detail}`}</Text> : null}
            {use.atlas ? <Text span size="xs" c="cyan">{' (Atlas Tree)'}</Text> : null}
          </List.Item>
        ))}
      </List>
    </Stack>
  );
}

function AtlasPoolNote({ path, busy, onMoved }: { path: string; busy: boolean; onMoved: (path: string) => void }) {
  const moveAsIs = async () => {
    const result = await moveImage(path, ATLAS_CIRCULAR_POOL_FOLDER + imageFileName(path));
    if (!result.ok) {
      notifyError(`Could not move: ${result.message}`);
      return;
    }
    const moved = result.data.path ?? ATLAS_CIRCULAR_POOL_FOLDER + imageFileName(path);
    notifySuccess(`Moved to ${moved}`);
    onMoved(moved);
  };
  if (isInFolder(path, ATLAS_CIRCULAR_POOL_FOLDER)) {
    return (
      <Alert color="cyan" p="xs" title="Ready for the Atlas Tree">
        <Text size="xs">Pick this icon for an Atlas Tree type in its icon picker; it moves to {ATLAS_ICON_FOLDER} under the type's id.</Text>
      </Alert>
    );
  }
  return (
    <Alert color="cyan" p="xs" title="Atlas icon pool">
      <Stack gap={6}>
        <Text size="xs">
          Make a circular copy below to move this icon into {ATLAS_CIRCULAR_POOL_FOLDER}, where the Atlas Tree icon picker finds it. If it is already circular, move it
          as it is.
        </Text>
        <Button size="xs" variant="default" leftSection={<IconArrowRight size={14} />} loading={busy} onClick={() => void moveAsIs()}>
          Move to the circular pool as it is
        </Button>
      </Stack>
    </Alert>
  );
}

interface ImageDetailProps {
  path: string;
  vanilla: boolean;
  uses: ImageUse[] | undefined;
  onCreated: (path: string) => void;
  onDeleted: () => void;
}

function ImageDetail({ path, vanilla, uses, onCreated, onDeleted }: ImageDetailProps) {
  const [hueShift, setHueShift] = useState<number>(DEFAULT_HUE_SHIFT);
  const [degrees, setDegrees] = useState<number>(DEFAULT_ROTATION);
  const [suffix, setSuffix] = useState(DEFAULT_SUFFIX);
  const [busy, setBusy] = useState(false);

  const operationParameters: ImageCopyParameters = {
    colorshift: { hueShift },
    circular: {},
    rotate: { degrees },
  };

  const inAtlasPool = isInFolder(path, ATLAS_POOL_FOLDER);

  const runCopy = async (operation: ImageCopyOperation) => {
    if (!suffix.trim() && !(operation === 'circular' && inAtlasPool)) {
      notifyError('Suffix is required.');
      return;
    }
    setBusy(true);
    try {
      const created = await createCopy(operation, { path, suffix: suffix.trim() }, operationParameters[operation]);
      if (created) {
        notifySuccess(`Saved ${created}`);
        onCreated(created);
      }
    } finally {
      setBusy(false);
    }
  };

  const remove = async () => {
    const confirmed = await confirmAction({
      title: 'Delete this image?',
      message: `${path} will be deleted permanently. Anything that uses it will show a missing image.`,
      confirmLabel: 'Delete',
      danger: true,
    });
    if (!confirmed) {
      return;
    }
    const result = await deleteImage(path);
    if (!result.ok) {
      notifyError(`Could not delete: ${result.message}`);
      return;
    }
    notifySuccess(`Deleted ${path}`);
    onDeleted();
  };

  return (
    <Stack gap="sm">
      <Paper withBorder p="xs" bg="dark.8" style={{ overflow: 'hidden' }}>
        <Center>
          <Image src={fileUrl(path)} alt={path} mah={240} w="auto" fit="contain" />
        </Center>
      </Paper>
      <Text size="xs" ff="monospace" style={{ wordBreak: 'break-all' }}>
        {path}
      </Text>
      {isUnshippedImagePath(path) && (
        <Text size="xs" c="yellow">
          This folder is not deployed with the mod.
        </Text>
      )}
      {vanilla && (
        <Text size="xs" c="dimmed">
          Vanilla image: it is read-only, and copies are saved to {UNUSED_ART_FOLDER}.
        </Text>
      )}
      <UsageList uses={uses} />
      {isInFolder(path, ATLAS_POOL_FOLDER) || isInFolder(path, ATLAS_CIRCULAR_POOL_FOLDER) ? <AtlasPoolNote path={path} busy={busy} onMoved={onCreated} /> : null}
      <TextInput label="New file suffix" value={suffix} onChange={(event) => setSuffix(event.currentTarget.value)} />
      <Divider label="Colour shift" labelPosition="left" />
      <OperationPreview>
        <img src={fileUrl(path)} alt={`${path} colour-shifted`} style={{ ...PREVIEW_IMAGE_STYLE, filter: `hue-rotate(${hueShift}deg)` }} />
      </OperationPreview>
      <NumberInput label="Hue shift (degrees)" value={hueShift} onChange={(value) => setHueShift(typeof value === 'number' ? value : Number(value) || 0)} />
      <Button leftSection={<IconColorSwatch size={16} />} loading={busy} onClick={() => void runCopy('colorshift')}>
        Create colour-shifted copy
      </Button>
      <Divider label="Rotation" labelPosition="left" />
      <RotationPreview path={path} degrees={degrees} />
      <NumberInput label="Rotation (degrees clockwise)" step={ROTATION_STEP} value={degrees} onChange={(value) => setDegrees(typeof value === 'number' ? value : Number(value) || 0)} />
      <Button variant="default" leftSection={<IconRotateClockwise size={16} />} loading={busy} onClick={() => void runCopy('rotate')}>
        Create rotated copy
      </Button>
      <Divider label="Circular copy" labelPosition="left" />
      <OperationPreview>
        <img src={fileUrl(path)} alt={`${path} circular`} style={{ ...PREVIEW_IMAGE_STYLE, borderRadius: '50%' }} />
      </OperationPreview>
      <Button variant="default" leftSection={<IconCircle size={16} />} loading={busy} onClick={() => void runCopy('circular')}>
        {inAtlasPool ? 'Make circular and move to the circular pool' : 'Make circular copy'}
      </Button>
      <Text size="xs" c="dimmed">
        Copies of images under art/ stay in their folder; copies of shipped or vanilla images go to {UNUSED_ART_FOLDER}, named name_suffix.png. Circular copies go to{' '}
        {CIRCULAR_ART_FOLDER}, except icons in {ATLAS_POOL_FOLDER}, which move to {ATLAS_CIRCULAR_POOL_FOLDER} under the same name. Quarter turns are lossless; other
        angles keep the image size and clip the corners. Picking an art/ image as an icon moves it into graphics/.
      </Text>
      {!vanilla && (
        <>
          <Divider />
          <Button color="red" variant="light" leftSection={<IconTrash size={16} />} onClick={() => void remove()}>
            Delete this image
          </Button>
        </>
      )}
    </Stack>
  );
}

export function ImagesPage() {
  const images = useImageListing();
  const usage = useImageUsage();
  const refreshImages = useRefreshImages();
  const [selectedPath, setSelectedPath] = useState<string | null>(null);
  const allPaths = useMemo(() => (images.data ? [...images.data.projectImages, ...images.data.vanillaImages] : []), [images.data]);
  const vanillaPaths = useMemo(() => new Set(images.data?.vanillaImages), [images.data]);

  if (images.isError) {
    return (
      <Alert m="md" color="red" icon={<IconAlertTriangle />} title="Could not load the image list">
        {images.error.message}
      </Alert>
    );
  }
  if (!images.data) {
    return (
      <Center h="100%">
        <Loader />
      </Center>
    );
  }

  return (
    <Group align="stretch" gap={0} wrap="nowrap" h="100%">
      <Box p="md" style={{ flex: 1, minWidth: 0 }}>
        <Title order={3} mb="sm">
          Images
        </Title>
        <ImageBrowser paths={allPaths} usage={usage.data} selectedPath={selectedPath} onSelect={setSelectedPath} height="calc(100dvh - 190px)" />
      </Box>
      <Divider orientation="vertical" />
      <ScrollArea w={DETAIL_WIDTH} type="auto" style={{ flexShrink: 0 }}>
        <Box p="md">
          {selectedPath ? (
            <ImageDetail
              key={selectedPath}
              path={selectedPath}
              vanilla={vanillaPaths.has(selectedPath)}
              uses={usage.data?.[selectedPath]}
              onCreated={(created) => {
                void refreshImages();
                setSelectedPath(created);
              }}
              onDeleted={() => {
                void refreshImages();
                setSelectedPath(null);
              }}
            />
          ) : (
            <Text c="dimmed" size="sm">
              Select an image to see where it is used, or to recolour, rotate, make a circular copy of or delete it. For Atlas Tree icons, start in {ATLAS_POOL_FOLDER}.
            </Text>
          )}
        </Box>
      </ScrollArea>
    </Group>
  );
}
