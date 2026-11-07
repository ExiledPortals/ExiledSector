import { useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson, type PostResult } from './client.ts';
import { imageFileName } from '../shared/imagePaths.ts';

export const ALL_IMAGES_QUERY_KEY = ['images', 'all'];
export const STATIC_IMAGES_QUERY_KEY = ['images', 'static'];

export interface ImageListing {
  projectImages: string[];
  vanillaImages: string[];
}

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

export function useStaticImages() {
  return useQuery({ queryKey: STATIC_IMAGES_QUERY_KEY, queryFn: () => getJson<string[]>('/api/images/static') });
}

export function useRefreshImages() {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: ['images'] });
}

export type MoveOutcome = { path: string } | { error: string };

export async function moveImageIfNeeded(path: string, targetFolder: string): Promise<MoveOutcome> {
  if (!path.startsWith('graphics/unused/')) {
    return { path };
  }
  const destination = targetFolder + imageFileName(path);
  const result = await postJson<{ path?: string }>('/api/images/move', { from: path, to: destination });
  if (!result.ok) {
    return { error: `Image not changed - could not move it out of graphics/unused: ${result.message}` };
  }
  return { path: result.data.path ?? destination };
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
): Promise<PostResult<{ path?: string; exists?: boolean }>> {
  return postJson<{ path?: string; exists?: boolean }>(`/api/images/${operation}`, { ...request, ...parameters });
}

export function deleteImage(path: string): Promise<PostResult> {
  return postJson('/api/images/delete', { path });
}
