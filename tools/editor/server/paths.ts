import path from 'node:path';

export interface ProjectPaths {
  projectRoot: string;
  vanillaCoreDir: string;
}

export function defaultProjectPaths(): ProjectPaths {
  const projectRoot = path.resolve(import.meta.dirname, '..', '..', '..');
  return {
    projectRoot,
    vanillaCoreDir: path.join(path.dirname(projectRoot), 'Starsector', 'starsector-core'),
  };
}

function resolveInside(rootDir: string, relativePath: string | undefined | null): string | null {
  if (!relativePath || path.isAbsolute(relativePath) || /^[a-zA-Z]:/.test(relativePath)) {
    return null;
  }
  const resolvedRoot = path.resolve(rootDir);
  const rootPrefix = resolvedRoot.endsWith(path.sep) ? resolvedRoot : resolvedRoot + path.sep;
  const resolved = path.resolve(rootDir, relativePath);
  return resolved.toLowerCase().startsWith(rootPrefix.toLowerCase()) ? resolved : null;
}

export function resolveProjectPath(paths: ProjectPaths, relativePath: string | undefined | null): string | null {
  return resolveInside(paths.projectRoot, relativePath);
}

export function resolveVanillaPath(paths: ProjectPaths, relativePath: string | undefined | null): string | null {
  return resolveInside(paths.vanillaCoreDir, relativePath);
}

export const DEFAULT_SALVAGE_FILE = 'socketable_salvage.csv';
const COMPAT_SALVAGE_PREFIX = 'compat/salvage/';

export function salvageDir(paths: ProjectPaths): string {
  return path.join(paths.projectRoot, 'data', 'config', 'exiledSector');
}

export function resolveSalvagePath(paths: ProjectPaths, file: string | undefined | null): string | null {
  if (!file || file === DEFAULT_SALVAGE_FILE) {
    return path.join(salvageDir(paths), DEFAULT_SALVAGE_FILE);
  }
  if (/^compat\/salvage\/[A-Za-z0-9_.-]+\.csv$/.test(file)) {
    return path.join(salvageDir(paths), 'compat', 'salvage', file.slice(COMPAT_SALVAGE_PREFIX.length));
  }
  return null;
}

export function toProjectRelative(paths: ProjectPaths, fullPath: string): string {
  return path.relative(paths.projectRoot, fullPath).split(path.sep).join('/');
}
