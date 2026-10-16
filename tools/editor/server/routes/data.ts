import type { FastifyInstance } from 'fastify';
import fs from 'node:fs';
import path from 'node:path';
import { readDataFile, resolveDataResource, saveDataFiles, type DataFile } from '../dataFiles.ts';
import { DEFAULT_SALVAGE_FILE, salvageDir, type ProjectPaths } from '../paths.ts';
import { reservedNodeIds } from '../reservedNodeIds.ts';

interface SaveBody {
  files?: Record<string, { content?: unknown; baseHash?: unknown }>;
}

export function registerDataRoutes(app: FastifyInstance, paths: ProjectPaths): void {
  app.get('/api/vocabulary', async (_request, reply) => {
    const vocabularyPath = path.join(paths.projectRoot, 'tools', 'editor_vocabulary.json');
    reply.header('content-type', 'application/json; charset=utf-8');
    return fs.readFileSync(vocabularyPath, 'utf8');
  });

  app.get('/api/reserved-node-ids', async () => ({ ok: true, ...reservedNodeIds(paths) }));

  app.get<{ Querystring: { names?: string } }>('/api/files', async (request, reply) => {
    const names = (request.query.names ?? '')
      .split(',')
      .map((name) => name.trim())
      .filter((name) => name.length > 0);
    const files: Record<string, DataFile> = {};
    for (const name of names) {
      const resource = resolveDataResource(paths, name);
      if (!resource || !fs.existsSync(resource.filePath)) {
        return reply.code(404).send({ ok: false, message: `Unknown data file: ${name}` });
      }
      files[name] = readDataFile(resource.filePath);
    }
    return { ok: true, files };
  });

  app.post<{ Body: SaveBody }>('/api/files', async (request, reply) => {
    const outcome = saveDataFiles(paths, request.body?.files ?? {});
    return reply.code(outcome.status).send(outcome.body);
  });

  app.get('/api/salvage-files', async () => {
    const compatDir = path.join(salvageDir(paths), 'compat', 'salvage');
    const compatFiles = fs.existsSync(compatDir)
      ? fs
          .readdirSync(compatDir)
          .filter((name) => name.toLowerCase().endsWith('.csv'))
          .sort((first, second) => first.localeCompare(second))
          .map((name) => `compat/salvage/${name}`)
      : [];
    return [DEFAULT_SALVAGE_FILE, ...compatFiles];
  });
}
