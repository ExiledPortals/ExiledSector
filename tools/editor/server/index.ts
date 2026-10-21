import path from 'node:path';
import { createEditorServer } from './app.ts';
import { gitCheckoutLister } from './checkouts.ts';
import { DescribeService } from './describe.ts';
import { defaultProjectPaths } from './paths.ts';

const DEFAULT_PORT = 8792;

const port = Number(process.env.EDITOR_PORT) || DEFAULT_PORT;
const paths = defaultProjectPaths();
const listCheckouts = gitCheckoutLister(paths.projectRoot);
paths.mainCheckoutRoot = listCheckouts().find((checkout) => checkout.main)?.root;
const describe = new DescribeService(paths);
const app = createEditorServer({
  paths,
  port,
  clientDir: path.resolve(import.meta.dirname, '..', 'dist'),
  describe,
  listCheckouts,
});

describe.startBuild();

const shutDown = () => {
  void app.close().finally(() => process.exit(0));
};
process.on('SIGINT', shutDown);
process.on('SIGTERM', shutDown);

await app.listen({ host: 'localhost', port });
console.log(`Exiled Sector editor running at http://localhost:${port}/`);
console.log(`Saves write directly to the data files under ${paths.projectRoot}; switch checkouts from the editor's sidebar.`);
