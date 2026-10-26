import fs from 'node:fs';
import path from 'node:path';

export interface ProjectPaths {
  projectRoot: string;
  vanillaCoreDir: string;
  mainCheckoutRoot?: string;
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

function existingFile(fullPath: string | null): string | null {
  return fullPath && fs.existsSync(fullPath) && fs.statSync(fullPath).isFile() ? fullPath : null;
}

export function existingProjectFile(paths: ProjectPaths, relativePath: string | undefined | null): string | null {
  return existingFile(resolveProjectPath(paths, relativePath));
}

export function existingVanillaFile(paths: ProjectPaths, relativePath: string | undefined | null): string | null {
  return existingFile(resolveVanillaPath(paths, relativePath));
}

export function toProjectRelative(paths: ProjectPaths, fullPath: string): string {
  return path.relative(paths.projectRoot, fullPath).split(path.sep).join('/');
}
