import { spawn, type ChildProcessWithoutNullStreams } from 'node:child_process';
import crypto from 'node:crypto';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import readline from 'node:readline';
import { readDataFile, resolveDataResource } from './dataFiles.ts';
import type { ProjectPaths } from './paths.ts';
import { STRING_CATALOGUE_RESOURCES } from './stringCatalogues.ts';

export type TooltipCodeBuild = 'idle' | 'waiting' | 'building' | 'failed';

export interface TooltipCodeStatus {
  build: TooltipCodeBuild;
  message: string;
  copiedAtMs: number | null;
  codeRevision: number;
}

const READY_LINE = '{"ready":true}';
const READY_TIMEOUT_MS = 30000;
const REQUEST_TIMEOUT_MS = 30000;
const HELPER_CLASS = 'exiledsector.tools.EditorDescribeServer';
const HELPER_MODULE = 'exiledsector-mod';
const CLASSPATH_FILE = path.join('target', 'editor-describe.classpath');
const MANIFEST_FILE = 'manifest.json';
const COPIED_ENTRIES_DIR = 'classpath';
const PROJECT_ROOT_PROPERTY = 'exiledsector.root';
export const BUILD_LOCK_FILE = '.maven-build.lock';
export const BUILD_LOCK_STALE_MS = 10 * 60 * 1000;
export const BUILD_LOCK_POLL_MS = 2000;
const WAITING_MESSAGE = 'Waiting for another Maven build of the mod (deploy.ps1 or release.ps1) to finish before copying the tooltip code...';
const BUILDING_MESSAGE = 'Compiling the mod and copying its tooltip code into the editor...';
const MISSING_MESSAGE = 'The editor has no copy of the tooltip code yet; press Refresh tooltip code.';

export interface DescribeToolchain {
  javaHome: string | null;
  maven: string | null;
}

export interface TooltipCodeSnapshot {
  directory: string;
  copiedAtMs: number;
  classpath: string[];
}

interface SnapshotManifest {
  copiedAtMs: number;
  classpath: string[];
}

function findJavaHome(): string | null {
  const candidates = [process.env.JAVA_HOME, 'C:\\Program Files\\Java\\jdk-17'];
  for (const candidate of candidates) {
    if (candidate && fs.existsSync(path.join(candidate, 'bin', 'java.exe'))) {
      return candidate;
    }
  }
  return null;
}

function findMaven(): string | null {
  for (const directory of (process.env.PATH ?? '').split(path.delimiter)) {
    const candidate = path.join(directory, 'mvn.cmd');
    if (directory && fs.existsSync(candidate)) {
      return candidate;
    }
  }
  const jetBrainsDir = 'C:\\Program Files\\JetBrains';
  if (!fs.existsSync(jetBrainsDir)) {
    return null;
  }
  const bundled = fs
    .readdirSync(jetBrainsDir)
    .filter((name) => name.startsWith('IntelliJ IDEA'))
    .sort((first, second) => second.localeCompare(first))
    .map((name) => path.join(jetBrainsDir, name, 'plugins', 'maven-plugin', 'lib', 'maven3', 'bin', 'mvn.cmd'))
    .find((candidate) => fs.existsSync(candidate));
  return bundled ?? null;
}

export function defaultToolchain(): DescribeToolchain {
  return { javaHome: findJavaHome(), maven: findMaven() };
}

export function defaultSnapshotsDir(): string {
  return path.join(process.env.LOCALAPPDATA ?? os.tmpdir(), 'ExiledSectorEditor', 'tooltip-code');
}

export function libsDir(paths: ProjectPaths): string {
  const ownLibs = path.join(paths.projectRoot, 'libs');
  const candidates = [ownLibs, path.join(path.dirname(paths.projectRoot), 'ExiledSector', 'libs')];
  if (paths.mainCheckoutRoot) {
    candidates.splice(1, 0, path.join(paths.mainCheckoutRoot, 'libs'));
  }
  return candidates.find((candidate) => fs.existsSync(path.join(candidate, 'starfarer.api.jar'))) ?? ownLibs;
}

export type DescribeResult = { body: unknown } | { raw: string };

export function describeRequestWithStrings(paths: ProjectPaths, requestBody: unknown): string {
  if (typeof requestBody === 'string') {
    return requestBody;
  }
  const body = (requestBody ?? {}) as Record<string, unknown>;
  if ('strings' in body) {
    return JSON.stringify(body);
  }
  const catalogueFiles = STRING_CATALOGUE_RESOURCES.map((name) => resolveDataResource(paths, name)?.filePath)
    .filter((filePath): filePath is string => filePath !== undefined && fs.existsSync(filePath));
  if (catalogueFiles.length === 0) {
    return JSON.stringify(body);
  }
  const strings = Object.assign({}, ...catalogueFiles.map((filePath) => JSON.parse(readDataFile(filePath).content) as Record<string, unknown>));
  return JSON.stringify({ ...body, strings });
}

export function checkoutSnapshotsDir(snapshotsDir: string, projectRoot: string): string {
  const resolvedRoot = path.resolve(projectRoot).toLowerCase();
  const rootHash = crypto.createHash('sha1').update(resolvedRoot).digest('hex').slice(0, 12);
  return path.join(snapshotsDir, `${path.basename(resolvedRoot)}-${rootHash}`);
}

function isInsideMavenRepository(entry: string): boolean {
  return entry.split(/[\\/]/).some((part, index, parts) => part === '.m2' && parts[index + 1] === 'repository');
}

export function copySnapshot(checkoutDir: string, classpathEntries: string[], copiedAtMs: number): TooltipCodeSnapshot {
  const directory = path.join(checkoutDir, String(copiedAtMs));
  fs.rmSync(directory, { recursive: true, force: true });
  fs.mkdirSync(path.join(directory, COPIED_ENTRIES_DIR), { recursive: true });
  const classpath = classpathEntries.map((entry, index) => {
    if (isInsideMavenRepository(entry)) {
      return entry;
    }
    const copiedName = path.join(COPIED_ENTRIES_DIR, `${String(index).padStart(3, '0')}-${path.basename(entry)}`);
    fs.cpSync(entry, path.join(directory, copiedName), { recursive: true });
    return copiedName;
  });
  const manifest: SnapshotManifest = { copiedAtMs, classpath };
  fs.writeFileSync(path.join(directory, MANIFEST_FILE), JSON.stringify(manifest, null, 2));
  return snapshotAt(directory, manifest);
}

function snapshotAt(directory: string, manifest: SnapshotManifest): TooltipCodeSnapshot {
  return {
    directory,
    copiedAtMs: manifest.copiedAtMs,
    classpath: manifest.classpath.map((entry) => (path.isAbsolute(entry) ? entry : path.join(directory, entry))),
  };
}

function readSnapshot(directory: string): TooltipCodeSnapshot | null {
  try {
    const manifest = JSON.parse(fs.readFileSync(path.join(directory, MANIFEST_FILE), 'utf8')) as SnapshotManifest;
    const snapshot = snapshotAt(directory, manifest);
    return snapshot.classpath.every((entry) => fs.existsSync(entry)) ? snapshot : null;
  } catch {
    return null;
  }
}

export function latestSnapshot(checkoutDir: string): TooltipCodeSnapshot | null {
  if (!fs.existsSync(checkoutDir)) {
    return null;
  }
  const candidates = fs
    .readdirSync(checkoutDir)
    .filter((name) => /^\d+$/.test(name))
    .sort((first, second) => Number(second) - Number(first));
  for (const name of candidates) {
    const snapshot = readSnapshot(path.join(checkoutDir, name));
    if (snapshot) {
      return snapshot;
    }
  }
  return null;
}

export function removeOtherSnapshots(checkoutDir: string, keptDirectory: string): void {
  if (!fs.existsSync(checkoutDir)) {
    return;
  }
  for (const name of fs.readdirSync(checkoutDir)) {
    const directory = path.join(checkoutDir, name);
    if (path.resolve(directory) !== path.resolve(keptDirectory)) {
      try {
        fs.rmSync(directory, { recursive: true, force: true });
      } catch {
        continue;
      }
    }
  }
}

export function builtClasspath(projectRoot: string): string[] | null {
  const classpathFile = path.join(projectRoot, HELPER_MODULE, CLASSPATH_FILE);
  if (!fs.existsSync(classpathFile)) {
    return null;
  }
  const moduleTarget = path.join(projectRoot, HELPER_MODULE, 'target');
  const dependencies = fs
    .readFileSync(classpathFile, 'utf8')
    .trim()
    .split(path.delimiter)
    .filter((entry) => entry.length > 0);
  const entries = [path.join(moduleTarget, 'classes'), path.join(moduleTarget, 'test-classes'), ...dependencies];
  return entries.every((entry) => fs.existsSync(entry)) ? entries : null;
}

export class DescribeService {
  private build: TooltipCodeBuild = 'idle';
  private message = '';
  private snapshot: TooltipCodeSnapshot | null = null;
  private codeRevision = 0;
  private helper: ChildProcessWithoutNullStreams | null = null;
  private helperLines: readline.Interface | null = null;
  private pendingLine: ((line: string | null) => void) | null = null;
  private queue: Promise<unknown> = Promise.resolve();
  private lockPoll: NodeJS.Timeout | null = null;
  private readonly buildLogFile = path.join(os.tmpdir(), 'exiledsector-editor-describe-build.log');
  private generation = 0;

  constructor(
    private readonly paths: ProjectPaths,
    private readonly toolchain: DescribeToolchain = defaultToolchain(),
    private readonly nowMs: () => number = Date.now,
    private readonly snapshotsDir: string = defaultSnapshotsDir(),
  ) {}

  private get checkoutDir(): string {
    return checkoutSnapshotsDir(this.snapshotsDir, this.paths.projectRoot);
  }

  private get buildLockFile(): string {
    return path.join(this.paths.projectRoot, BUILD_LOCK_FILE);
  }

  status(): TooltipCodeStatus {
    return { build: this.build, message: this.message, copiedAtMs: this.snapshot?.copiedAtMs ?? null, codeRevision: this.codeRevision };
  }

  start(): void {
    this.snapshot = latestSnapshot(this.checkoutDir);
    if (this.snapshot) {
      removeOtherSnapshots(this.checkoutDir, this.snapshot.directory);
    } else {
      this.refresh();
    }
  }

  restartForProject(): void {
    this.generation++;
    this.stopHelper();
    this.stopLockPoll();
    this.build = 'idle';
    this.message = '';
    this.codeRevision++;
    this.start();
  }

  refresh(): TooltipCodeStatus {
    if (this.build === 'building' || this.build === 'waiting') {
      return this.status();
    }
    const { javaHome, maven } = this.toolchain;
    if (!javaHome || !maven) {
      this.fail('Copying the tooltip code needs JDK 17 and Maven; neither JAVA_HOME nor the IntelliJ bundled Maven was found.');
      return this.status();
    }
    if (this.otherBuildRunning()) {
      this.waitForOtherBuild();
      return this.status();
    }
    this.runBuild(maven, javaHome);
    return this.status();
  }

  private runBuild(maven: string, javaHome: string): void {
    this.build = 'building';
    this.message = BUILDING_MESSAGE;
    const buildLog = fs.openSync(this.buildLogFile, 'w');
    const mavenCommandLine = [
      `"${maven}"`,
      '-B',
      '-q',
      'test-compile',
      'dependency:build-classpath',
      `-Dmdep.outputFile=${CLASSPATH_FILE}`,
      '-Dmdep.includeScope=test',
      `"-Dlibs.dir=${libsDir(this.paths)}"`,
    ].join(' ');
    const generation = this.generation;
    const projectRoot = this.paths.projectRoot;
    const build = spawn(mavenCommandLine, {
      cwd: projectRoot,
      shell: true,
      windowsHide: true,
      stdio: ['ignore', buildLog, buildLog],
      env: { ...process.env, JAVA_HOME: javaHome },
    });
    build.on('exit', (exitCode) => {
      fs.closeSync(buildLog);
      if (generation !== this.generation) {
        return;
      }
      const classpath = exitCode === 0 ? builtClasspath(projectRoot) : null;
      if (!classpath) {
        this.fail(`Compiling the mod for the tooltip code failed; the previous copy stays in use. See ${this.buildLogFile}`);
        return;
      }
      this.takeSnapshot(classpath);
    });
    build.on('error', () => {
      if (generation !== this.generation) {
        return;
      }
      this.fail(`Could not run Maven; see ${this.buildLogFile}`);
    });
  }

  private takeSnapshot(classpath: string[]): void {
    let snapshot: TooltipCodeSnapshot;
    try {
      snapshot = copySnapshot(this.checkoutDir, classpath, this.nowMs());
    } catch (error) {
      this.fail(`Copying the compiled tooltip code failed (${(error as Error).message}); the previous copy stays in use.`);
      return;
    }
    this.queue = this.queue.then(() => {
      this.stopHelper();
      this.snapshot = snapshot;
      this.codeRevision++;
      removeOtherSnapshots(this.checkoutDir, snapshot.directory);
    });
    this.build = 'idle';
    this.message = '';
  }

  private otherBuildRunning(): boolean {
    try {
      return this.nowMs() - fs.statSync(this.buildLockFile).mtimeMs < BUILD_LOCK_STALE_MS;
    } catch {
      return false;
    }
  }

  private waitForOtherBuild(): void {
    this.build = 'waiting';
    this.message = WAITING_MESSAGE;
    this.lockPoll = setInterval(() => {
      if (!this.otherBuildRunning()) {
        this.stopLockPoll();
        this.build = 'idle';
        this.refresh();
      }
    }, BUILD_LOCK_POLL_MS);
    this.lockPoll.unref();
  }

  private stopLockPoll(): void {
    if (this.lockPoll) {
      clearInterval(this.lockPoll);
      this.lockPoll = null;
    }
  }

  private fail(message: string): void {
    this.build = 'failed';
    this.message = message;
  }

  describe(requestBody: string): Promise<DescribeResult> {
    const result = this.queue.then(() => this.describeNow(requestBody));
    this.queue = result.catch(() => undefined);
    return result;
  }

  private unavailable(): DescribeResult {
    const message = this.message || MISSING_MESSAGE;
    return { body: this.build === 'building' || this.build === 'waiting' ? { pending: true, message } : { error: message } };
  }

  private async describeNow(requestBody: string): Promise<DescribeResult> {
    if (!this.snapshot) {
      return this.unavailable();
    }
    if (!this.helper) {
      await this.startHelper(this.snapshot);
    }
    if (!this.helper) {
      return this.unavailable();
    }
    this.helper.stdin.write(requestBody.replace(/[\r\n]+/g, ' ') + '\n');
    const line = await this.nextLine(REQUEST_TIMEOUT_MS);
    if (line === null) {
      return { body: { error: 'The tooltip code stopped responding; it restarts on the next hover.' } };
    }
    return { raw: line };
  }

  private async startHelper(snapshot: TooltipCodeSnapshot): Promise<void> {
    const javaHome = this.toolchain.javaHome;
    if (!javaHome) {
      this.fail('Running the tooltip code needs JDK 17; JAVA_HOME was not found.');
      return;
    }
    if (!readSnapshot(snapshot.directory)) {
      this.start();
      return;
    }
    const helper = spawn(
      path.join(javaHome, 'bin', 'java.exe'),
      [`-D${PROJECT_ROOT_PROPERTY}=${this.paths.projectRoot}`, '-cp', snapshot.classpath.join(path.delimiter), HELPER_CLASS, this.paths.vanillaCoreDir],
      { cwd: this.paths.projectRoot, windowsHide: true },
    );
    helper.stderr.resume();
    helper.on('exit', () => this.helperExited(helper));
    helper.on('error', () => this.helperExited(helper));
    this.helper = helper;
    this.helperLines = readline.createInterface({ input: helper.stdout, crlfDelay: Infinity });
    this.helperLines.on('line', (line) => this.resolveLine(line));
    const generation = this.generation;
    const readyLine = await this.nextLine(READY_TIMEOUT_MS);
    if (generation !== this.generation) {
      return;
    }
    if (readyLine !== READY_LINE) {
      this.stopHelper();
      this.fail('The copied tooltip code did not start; press Refresh tooltip code to copy it again.');
    }
  }

  private nextLine(timeoutMs: number): Promise<string | null> {
    return new Promise((resolve) => {
      const timer = setTimeout(() => {
        this.pendingLine = null;
        this.stopHelper();
        resolve(null);
      }, timeoutMs);
      this.pendingLine = (line) => {
        clearTimeout(timer);
        this.pendingLine = null;
        resolve(line);
      };
    });
  }

  private resolveLine(line: string | null): void {
    if (this.pendingLine) {
      this.pendingLine(line);
    }
  }

  private helperExited(helper: ChildProcessWithoutNullStreams): void {
    if (this.helper !== helper) {
      return;
    }
    this.helper = null;
    this.helperLines?.close();
    this.helperLines = null;
    this.resolveLine(null);
  }

  stopHelper(): void {
    const helper = this.helper;
    if (helper && helper.exitCode === null) {
      helper.kill();
    }
    if (helper) {
      this.helperExited(helper);
    }
  }

  close(): void {
    this.stopLockPoll();
    this.stopHelper();
  }
}
