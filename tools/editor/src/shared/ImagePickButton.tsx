import { Alert, Button, Center, Loader, Modal, Text, type ButtonProps } from '@mantine/core';
import { IconPhoto } from '@tabler/icons-react';
import { useState } from 'react';
import { placePickedImage, useAllImages, useImageUsage, useRefreshImages } from '../api/images.ts';
import { notifyError, notifyInfo } from './dialogs.tsx';
import { ImageBrowser } from './ImageBrowser.tsx';
import { ATLAS_CIRCULAR_POOL_FOLDER, ATLAS_POOL_FOLDER, destinationPath, type ImageDestination } from './imageFolders.ts';

interface ImagePickButtonProps extends ButtonProps {
  destination: ImageDestination | null;
  currentPath?: string;
  onPicked: (path: string) => void;
  label?: string;
}

function PickerIntro({ destination }: { destination: ImageDestination | null }) {
  if (!destination) {
    return null;
  }
  const example = destinationPath('example.png', destination);
  if (destination.atlas) {
    return (
      <Alert color="cyan" title="Picking an Atlas Tree icon" p="xs">
        <Text size="xs">
          1. Find an icon in {ATLAS_POOL_FOLDER} (icons copied from the ship tree and items, not yet used in the Atlas Tree). 2. Make it circular on the
          Images page; that moves it into {ATLAS_CIRCULAR_POOL_FOLDER}. 3. Pick it here: it moves to {example} and becomes this type's icon. Mod prefixes
          such as rat_ stay on the name.
        </Text>
      </Alert>
    );
  }
  return (
    <Text size="xs" c="dimmed">
      Images under art/ are not deployed. Picking one moves it to {example}, keeping any mod prefix.
    </Text>
  );
}

export function ImagePickButton({ destination, currentPath, onPicked, label = 'Select image', ...buttonProps }: ImagePickButtonProps) {
  const [opened, setOpened] = useState(false);
  const images = useAllImages();
  const usage = useImageUsage();
  const refreshImages = useRefreshImages();

  const pick = async (path: string) => {
    setOpened(false);
    const outcome = destination === null ? { path } : await placePickedImage(path, destination, currentPath);
    if ('error' in outcome) {
      notifyError(outcome.error);
      return;
    }
    if (outcome.path !== path) {
      void refreshImages();
      notifyInfo(`Moved ${path} to ${outcome.path}${'displaced' in outcome && outcome.displaced ? `; the old icon went back to ${outcome.displaced}` : ''}`);
    }
    onPicked(outcome.path);
  };

  return (
    <>
      <Button variant="default" leftSection={<IconPhoto size={14} />} onClick={() => setOpened(true)} {...buttonProps}>
        {label}
      </Button>
      <Modal opened={opened} onClose={() => setOpened(false)} title="Select an image" size="80rem">
        {images.data ? (
          <ImageBrowser
            paths={images.data}
            usage={usage.data}
            onSelect={(path) => void pick(path)}
            height="60vh"
            initialFolder={destination?.atlas ? ATLAS_CIRCULAR_POOL_FOLDER.replace(/\/$/, '') : ''}
            initialUsageFilter={destination?.atlas ? 'notAtlas' : 'all'}
            intro={<PickerIntro destination={destination} />}
          />
        ) : (
          <Center p="xl">{images.isError ? 'Could not load the image list.' : <Loader />}</Center>
        )}
      </Modal>
    </>
  );
}
