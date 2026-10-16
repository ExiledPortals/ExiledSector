export const FALLBACK_ICON = 'graphics/icons/cargo/chip1.png';

export function fileUrl(projectPath: string | null | undefined): string {
  const path = projectPath || FALLBACK_ICON;
  return '/files/' + path.split('/').map(encodeURIComponent).join('/');
}

export function isUnshippedImagePath(path: string | null | undefined): boolean {
  return /^graphics\/(unused|description)\//.test(path ?? '');
}

export function imageFolder(path: string): string {
  const slash = path.lastIndexOf('/');
  return slash === -1 ? '' : path.substring(0, slash);
}

export function imageFileName(path: string): string {
  return path.split('/').pop() ?? path;
}

export function imageBaseName(path: string): string {
  const name = imageFileName(path);
  const dot = name.lastIndexOf('.');
  return dot === -1 ? name : name.substring(0, dot);
}

export interface FolderCount {
  folder: string;
  count: number;
}

export function folderCounts(paths: string[]): FolderCount[] {
  const counts = new Map<string, number>();
  for (const path of paths) {
    const folder = imageFolder(path);
    counts.set(folder, (counts.get(folder) ?? 0) + 1);
  }
  return [...counts.entries()].sort(([first], [second]) => (first < second ? -1 : first > second ? 1 : 0)).map(([folder, count]) => ({ folder, count }));
}
