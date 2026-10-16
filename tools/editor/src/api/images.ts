import { useQuery, useQueryClient } from '@tanstack/react-query';
import { getJson, postJson, type PostResult } from './client.ts';
import { imageFileName } from '../shared/imagePaths.ts';

export const ALL_IMAGES_QUERY_KEY = ['images', 'all'];
export const STATIC_IMAGES_QUERY_KEY = ['images', 'static'];

export function useAllImages() {
  return useQuery({ queryKey: ALL_IMAGES_QUERY_KEY, queryFn: () => getJson<string[]>('/api/images/all') });
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

export interface ImageCopyRequest {
  path: string;
  suffix: string;
  hueShift?: number;
  overwrite?: boolean;
}

export function postImageCopy(url: '/api/images/colorshift' | '/api/images/circular', request: ImageCopyRequest): Promise<PostResult<{ path?: string; exists?: boolean }>> {
  return postJson<{ path?: string; exists?: boolean }>(url, request);
}

export function deleteImage(path: string): Promise<PostResult> {
  return postJson('/api/images/delete', { path });
}
