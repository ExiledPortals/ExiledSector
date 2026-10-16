import { Alert, Box, Button, Center, Divider, Group, Image, Loader, NumberInput, Paper, ScrollArea, Stack, Text, TextInput, Title } from '@mantine/core';
import { IconAlertTriangle, IconCircle, IconColorSwatch, IconTrash } from '@tabler/icons-react';
import { useState } from 'react';
import { deleteImage, postImageCopy, useAllImages, useRefreshImages, type ImageCopyRequest } from '../../api/images.ts';
import { confirmAction, notifyError, notifySuccess } from '../../shared/dialogs.tsx';
import { ImageBrowser } from '../../shared/ImageBrowser.tsx';
import { fileUrl, isUnshippedImagePath } from '../../shared/imagePaths.ts';

const DETAIL_WIDTH = 320;
const DEFAULT_HUE_SHIFT = 240;
const DEFAULT_SUFFIX = 'shifted';

type CopyUrl = '/api/images/colorshift' | '/api/images/circular';

async function createCopy(url: CopyUrl, request: ImageCopyRequest): Promise<string | null> {
  const result = await postImageCopy(url, request);
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
    return overwrite ? createCopy(url, { ...request, overwrite: true }) : null;
  }
  notifyError(`Could not complete: ${result.message}`);
  return null;
}

interface ImageDetailProps {
  path: string;
  onCreated: (path: string) => void;
  onDeleted: () => void;
}

function ImageDetail({ path, onCreated, onDeleted }: ImageDetailProps) {
  const [hueShift, setHueShift] = useState<number>(DEFAULT_HUE_SHIFT);
  const [suffix, setSuffix] = useState(DEFAULT_SUFFIX);
  const [busy, setBusy] = useState(false);

  const runCopy = async (url: CopyUrl) => {
    if (!suffix.trim()) {
      notifyError('Suffix is required.');
      return;
    }
    setBusy(true);
    try {
      const created = await createCopy(url, { path, suffix: suffix.trim(), hueShift: url === '/api/images/colorshift' ? hueShift : undefined });
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
      <Paper withBorder p="xs" bg="dark.8">
        <Center>
          <Image src={fileUrl(path)} alt={path} mah={240} w="auto" fit="contain" style={{ filter: `hue-rotate(${hueShift}deg)` }} />
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
      <NumberInput label="Hue shift (degrees)" value={hueShift} onChange={(value) => setHueShift(typeof value === 'number' ? value : Number(value) || 0)} />
      <TextInput label="New file suffix" value={suffix} onChange={(event) => setSuffix(event.currentTarget.value)} />
      <Button leftSection={<IconColorSwatch size={16} />} loading={busy} onClick={() => void runCopy('/api/images/colorshift')}>
        Create colour-shifted copy
      </Button>
      <Button variant="default" leftSection={<IconCircle size={16} />} loading={busy} onClick={() => void runCopy('/api/images/circular')}>
        Make circular copy
      </Button>
      <Text size="xs" c="dimmed">
        The colour-shifted copy is saved next to the original as name_suffix.png. Circular copies go to graphics/unused/circular/.
      </Text>
      <Divider />
      <Button color="red" variant="light" leftSection={<IconTrash size={16} />} onClick={() => void remove()}>
        Delete this image
      </Button>
    </Stack>
  );
}

export function ImagesPage() {
  const images = useAllImages();
  const refreshImages = useRefreshImages();
  const [selectedPath, setSelectedPath] = useState<string | null>(null);

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
        <ImageBrowser paths={images.data} selectedPath={selectedPath} onSelect={setSelectedPath} height="calc(100dvh - 190px)" />
      </Box>
      <Divider orientation="vertical" />
      <ScrollArea w={DETAIL_WIDTH} type="auto" style={{ flexShrink: 0 }}>
        <Box p="md">
          {selectedPath ? (
            <ImageDetail
              key={selectedPath}
              path={selectedPath}
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
              Select an image to recolour it, make a circular copy or delete it.
            </Text>
          )}
        </Box>
      </ScrollArea>
    </Group>
  );
}
