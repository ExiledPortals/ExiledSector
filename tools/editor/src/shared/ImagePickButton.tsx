import { Button, Center, Loader, Modal, type ButtonProps } from '@mantine/core';
import { IconPhoto } from '@tabler/icons-react';
import { useState } from 'react';
import { moveImageIfNeeded, useAllImages, useRefreshImages } from '../api/images.ts';
import { notifyError } from './dialogs.tsx';
import { ImageBrowser } from './ImageBrowser.tsx';

interface ImagePickButtonProps extends ButtonProps {
  targetFolder?: string;
  onPicked: (path: string) => void;
  label?: string;
}

export function ImagePickButton({ targetFolder = 'graphics/icons/', onPicked, label = 'Select image', ...buttonProps }: ImagePickButtonProps) {
  const [opened, setOpened] = useState(false);
  const images = useAllImages();
  const refreshImages = useRefreshImages();

  const pick = async (path: string) => {
    setOpened(false);
    const outcome = await moveImageIfNeeded(path, targetFolder);
    if ('error' in outcome) {
      notifyError(outcome.error);
      return;
    }
    if (outcome.path !== path) {
      void refreshImages();
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
          <ImageBrowser paths={images.data} onSelect={(path) => void pick(path)} height="60vh" />
        ) : (
          <Center p="xl">{images.isError ? 'Could not load the image list.' : <Loader />}</Center>
        )}
      </Modal>
    </>
  );
}
