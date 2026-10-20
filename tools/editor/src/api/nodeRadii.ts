import { useQuery } from '@tanstack/react-query';
import { fetchDataFiles } from './client.ts';

export interface NodeRadius {
  size: string;
  distance: number;
}

export function parseNodeRadii(content: string, sizes: string[]): NodeRadius[] {
  const parsed = JSON.parse(content) as Record<string, unknown>;
  return sizes.flatMap((size) => {
    const distance = parsed[size];
    return typeof distance === 'number' && distance > 0 ? [{ size, distance }] : [];
  });
}

export function useNodeRadii(sizes: string[] | undefined) {
  return useQuery({
    queryKey: ['node-radii', sizes],
    enabled: sizes !== undefined,
    queryFn: async () => parseNodeRadii((await fetchDataFiles(['node-radii']))['node-radii'].content, sizes ?? []),
  });
}
