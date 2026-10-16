import type { FastifyInstance } from 'fastify';
import fs from 'node:fs';
import path from 'node:path';
import { resolveProjectPath, resolveVanillaPath, type ProjectPaths } from '../paths.ts';

const IMAGE_CONTENT_TYPES: Record<string, string> = {
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
};

function existingFile(fullPath: string | null): string | null {
  return fullPath && fs.existsSync(fullPath) && fs.statSync(fullPath).isFile() ? fullPath : null;
}

export function registerFileRoutes(app: FastifyInstance, paths: ProjectPaths): void {
  app.get<{ Params: { '*': string } }>('/files/*', async (request, reply) => {
    const relativePath = decodeURIComponent(request.params['*'] ?? '');
    const contentType = IMAGE_CONTENT_TYPES[path.extname(relativePath).toLowerCase()];
    const filePath = contentType
      ? existingFile(resolveProjectPath(paths, relativePath)) ?? existingFile(resolveVanillaPath(paths, relativePath))
      : null;
    if (!filePath || !contentType) {
      return reply.code(404).send({ ok: false, message: 'Not found.' });
    }
    reply.header('content-type', contentType);
    reply.header('cache-control', 'no-cache');
    return reply.send(fs.readFileSync(filePath));
  });
}
