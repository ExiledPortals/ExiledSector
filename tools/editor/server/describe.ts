import { spawn, type ChildProcessWithoutNullStreams } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import readline from 'node:readline';
import { readDataFile, resolveDataResource } from './dataFiles.ts';
import type { ProjectPaths } from './paths.ts';
import { STRING_CATALOGUE_RESOURCES } from './stringCatalogues.ts';

export type DescribeStatus = 'idle' | 'waiting' | 'building' | 'built' | 'ready' | 'failed';

const READY_LINE = '{"ready":true}';
const READY_TIMEOUT_MS = 30000;
const REQUEST_TIMEOUT_MS = 10000;
const HELPER_CLASS = 'exiledsector.tools.EditorDescribeServer';
const HELPER_MODULE = 'exiledsector-mod';
export const REBUILD_COOLDOWN_MS = 15000;
export const BUILD_LOCK_FILE = '.maven-build.lock';
export const BUILD_LOCK_STALE_MS = 10 * 60 * 1000;
const WAITING_MESSAGE = 'Waiting for another Maven build of the mod (deploy.ps1 or release.ps1) to finish...';

export interface DescribeToolchain {
  javaHome: string | null;
  maven: string | null;
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

export function libsDir(paths: ProjectPaths): string {
  const ownLibs = path.join(paths.projectRoot, 'libs');
  const candidates = [ownLibs, path.join(path.dirname(paths.projectRoot), 'ExiledSector', 'libs')];
  if (paths.mainCheckoutRoot) {
    candidates.splice(1, 0, path.join(paths.mainCheckoutRoot, 'libs'));
  }
  return candidates.find((candidate) => fs.existsSync(path.join(candidate, 'starfarer.api.jar'))) ?? ownLibs;
}

export type DescribeResult = { body: unknown } | { raw: string };

export function needsRebuild(
  status: DescribeStatus,
  helperRunning: boolean,
  classpathExists: boolean,
  msSinceFailure: number,
  otherBuildRunning = false,
): boolean {
  if (otherBuildRunning) {
    return false;
  }
  if (status === 'waiting') {
    return true;
  }
  if (status === 'failed') {
    return msSinceFailure >= REBUILD_COOLDOWN_MS;
  }
  return (status === 'built' || status === 'ready') && !helperRunning && !classpathExists;
}

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

export class DescribeService {
  private status: DescribeStatus = 'idle';
  private message = '';
  private helper: ChildProcessWithoutNullStreams | null = null;
  private helperLines: readline.Interface | null = null;
  private pendingLine: ((line: string | null) => void) | null = null;
  private queue: Promise<unknown> = Promise.resolve();
  private failedAtMs = 0;
  private readonly buildLogFile = path.join(os.tmpdir(), 'exiledsector-editor-describe-build.log');
  private generation = 0;

  constructor(
    private readonly paths: ProjectPaths,
    private readonly toolchain: DescribeToolchain = defaultToolchain(),
    private readonly nowMs: () => number = Date.now,
  ) {}

  private get classpathFile(): string {
    return path.join(this.paths.projectRoot, HELPER_MODULE, 'target', 'editor-describe.classpath');
  }

  private get buildLockFile(): string {
    return path.join(this.paths.projectRoot, BUILD_LOCK_FILE);
  }

  restartForProject(): void {
    this.generation++;
    this.stopHelper();
    this.status = 'idle';
    this.message = '';
    this.failedAtMs = 0;
    this.startBuild();
  }

  startBuild(): void {
    const { javaHome, maven } = this.toolchain;
    if (!javaHome || !maven) {
      this.fail('In-game descriptions need JDK 17 and Maven; neither JAVA_HOME nor the IntelliJ bundled Maven was found.');
      return;
    }
    if (this.otherBuildRunning()) {
      this.wait();
      return;
    }
    this.status = 'building';
    this.message = 'Compiling the mod for in-game descriptions...';
    const buildLog = fs.openSync(this.buildLogFile, 'w');
    const mavenCommandLine = [
      `"${maven}"`,
      '-B',
      '-q',
      'test-compile',
      'dependency:build-classpath',
      '-Dmdep.outputFile=target\\editor-describe.classpath',
      '-Dmdep.includeScope=test',
      `"-Dlibs.dir=${libsDir(this.paths)}"`,
    ].join(' ');
    const generation = this.generation;
    const classpathFile = this.classpathFile;
    const build = spawn(mavenCommandLine, {
      cwd: this.paths.projectRoot,
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
      if (exitCode === 0 && fs.existsSync(classpathFile)) {
        this.status = 'built';
      } else {
        this.fail(`Compiling the mod for in-game descriptions failed; see ${this.buildLogFile}`);
      }
    });
    build.on('error', () => {
      if (generation !== this.generation) {
        return;
      }
      this.fail(`Could not run Maven; see ${this.buildLogFile}`);
    });
  }

  private otherBuildRunning(): boolean {
    try {
      return this.nowMs() - fs.statSync(this.buildLockFile).mtimeMs < BUILD_LOCK_STALE_MS;
    } catch {
      return false;
    }
  }

  private wait(): void {
    this.status = 'waiting';
    this.message = WAITING_MESSAGE;
  }

  private fail(message: string): void {
    this.status = 'failed';
    this.message = message;
    this.failedAtMs = this.nowMs();
  }

  private rebuildIfNeeded(): void {
    if (!this.toolchain.javaHome || !this.toolchain.maven) {
      return;
    }
    const otherBuildRunning = this.otherBuildRunning();
    if (otherBuildRunning && this.helper === null && this.status !== 'building') {
      this.wait();
      return;
    }
    if (needsRebuild(this.status, this.helper !== null, fs.existsSync(this.classpathFile), this.nowMs() - this.failedAtMs, otherBuildRunning)) {
      this.startBuild();
    }
  }

  describe(requestBody: string): Promise<DescribeResult> {
    const result = this.queue.then(() => this.describeNow(requestBody));
    this.queue = result.catch(() => undefined);
    return result;
  }

  private async describeNow(requestBody: string): Promise<DescribeResult> {
    this.rebuildIfNeeded();
    if (this.status === 'built' || (this.status === 'ready' && !this.helper)) {
      await this.startHelper();
    }
    if (this.status !== 'ready' || !this.helper) {
      return { body: { pending: this.status !== 'failed', message: this.message } };
    }
    this.helper.stdin.write(requestBody.replace(/[\r\n]+/g, ' ') + '\n');
    const line = await this.nextLine(REQUEST_TIMEOUT_MS);
    if (line === null) {
      return { body: { error: 'The description helper stopped responding and will restart on the next hover.' } };
    }
    return { raw: line };
  }

  private async startHelper(): Promise<void> {
    const javaHome = this.toolchain.javaHome;
    if (!javaHome) {
      return;
    }
    if (!fs.existsSync(this.classpathFile)) {
      this.fail('The compiled mod went missing; it will be rebuilt on the next hover.');
      return;
    }
    const moduleTarget = path.join(HELPER_MODULE, 'target');
    const classpath = `${path.join(moduleTarget, 'classes')};${path.join(moduleTarget, 'test-classes')};${fs.readFileSync(this.classpathFile, 'utf8').trim()}`;
    const helper = spawn(path.join(javaHome, 'bin', 'java.exe'), ['-cp', classpath, HELPER_CLASS], {
      cwd: this.paths.projectRoot,
      windowsHide: true,
    });
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
      this.fail('The description helper did not start; it will be rebuilt on the next hover.');
      return;
    }
    this.status = 'ready';
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
    if (this.status === 'ready') {
      this.status = 'built';
    }
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
}
