import { useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson, type PostResult } from './client.ts';
import { ATLAS_POOL_FOLDER, destinationPath, displacementFolder, isInFolder, isSourceArtPath, type ImageDestination } from '../shared/imageFolders.ts';

export const ALL_IMAGES_QUERY_KEY = ['images', 'all'];
export const STATIC_IMAGES_QUERY_KEY = ['images', 'static'];
export const IMAGE_USAGE_QUERY_KEY = ['images', 'usage'];

export interface ImageListing {
  projectImages: string[];
  vanillaImages: string[];
}

export interface ImageUse {
  file: string;
  detail: string;
  atlas: boolean;
}

export type ImageUsage = Record<string, ImageUse[]>;

function fetchImageListing(): Promise<ImageListing> {
  return getJson<ImageListing>('/api/images/all');
}

export function useImageListing() {
  return useQuery({ queryKey: ALL_IMAGES_QUERY_KEY, queryFn: fetchImageListing });
}

function allImagePaths(listing: ImageListing): string[] {
  return [...listing.projectImages, ...listing.vanillaImages];
}

export function useAllImages() {
  return useQuery({ queryKey: ALL_IMAGES_QUERY_KEY, queryFn: fetchImageListing, select: allImagePaths });
}

export function useImageUsage() {
  return useQuery({ queryKey: IMAGE_USAGE_QUERY_KEY, queryFn: () => getJson<ImageUsage>('/api/images/usage') });
}

export function useStaticImages() {
  return useQuery({ queryKey: STATIC_IMAGES_QUERY_KEY, queryFn: () => getJson<string[]>('/api/images/static') });
}

export function useRefreshImages() {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: ['images'] });
}

export type PlaceOutcome = { path: string; displaced?: string } | { error: string };

export async function placePickedImage(pickedPath: string, destination: ImageDestination, currentPath?: string): Promise<PlaceOutcome> {
  if (!isSourceArtPath(pickedPath)) {
    return { path: pickedPath };
  }
  if (destination.atlas && isInFolder(pickedPath, ATLAS_POOL_FOLDER)) {
    return { error: `Image not changed - make ${pickedPath} circular on the Images page first; that moves it into the circular Atlas pool.` };
  }
  const target = destinationPath(pickedPath, destination);
  const result = await postJson<{ path?: string; displaced?: string }>('/api/images/place', {
    from: pickedPath,
    to: target,
    displaceTo: displacementFolder(destination),
    replacing: currentPath,
  });
  if (!result.ok) {
    return { error: `Image not changed - could not move it out of art/: ${result.message}` };
  }
  return { path: result.data.path ?? target, ...(result.data.displaced ? { displaced: result.data.displaced } : {}) };
}

export interface ImageCopyParameters {
  colorshift: { hueShift: number };
  circular: Record<string, never>;
  rotate: { degrees: number };
}

export type ImageCopyOperation = keyof ImageCopyParameters;

export interface ImageCopyRequest {
  path: string;
  suffix: string;
  overwrite?: boolean;
}

export function postImageCopy<TOperation extends ImageCopyOperation>(
  operation: TOperation,
  request: ImageCopyRequest,
  parameters: ImageCopyParameters[TOperation],
): Promise<PostResult<{ path?: string; exists?: boolean; consumed?: string }>> {
  return postJson<{ path?: string; exists?: boolean; consumed?: string }>(`/api/images/${operation}`, { ...request, ...parameters });
}

export function moveImage(from: string, to: string): Promise<PostResult<{ path?: string }>> {
  return postJson<{ path?: string }>('/api/images/move', { from, to });
}

export function deleteImage(path: string): Promise<PostResult> {
  return postJson('/api/images/delete', { path });
}
