import { spawn, type ChildProcessWithoutNullStreams } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import readline from 'node:readline';
import type { ProjectPaths } from './paths.ts';

type DescribeStatus = 'idle' | 'building' | 'built' | 'ready' | 'failed';

const READY_LINE = '{"ready":true}';
const READY_TIMEOUT_MS = 30000;
const REQUEST_TIMEOUT_MS = 10000;
const HELPER_CLASS = 'exiledsector.tools.EditorDescribeServer';

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

function libsDir(projectRoot: string): string {
  const ownLibs = path.join(projectRoot, 'libs');
  if (fs.existsSync(path.join(ownLibs, 'starfarer.api.jar'))) {
    return ownLibs;
  }
  const mainCheckoutLibs = path.join(path.dirname(projectRoot), 'ExiledSector', 'libs');
  return fs.existsSync(path.join(mainCheckoutLibs, 'starfarer.api.jar')) ? mainCheckoutLibs : ownLibs;
}

export type DescribeResult = { body: unknown } | { raw: string };

export class DescribeService {
  private status: DescribeStatus = 'idle';
  private message = '';
  private helper: ChildProcessWithoutNullStreams | null = null;
  private helperLines: readline.Interface | null = null;
  private pendingLine: ((line: string | null) => void) | null = null;
  private queue: Promise<unknown> = Promise.resolve();
  private readonly classpathFile: string;
  private readonly buildLogFile = path.join(os.tmpdir(), 'exiledsector-editor-describe-build.log');

  constructor(
    private readonly paths: ProjectPaths,
    private readonly toolchain: DescribeToolchain = defaultToolchain(),
  ) {
    this.classpathFile = path.join(paths.projectRoot, 'target', 'editor-describe.classpath');
  }

  startBuild(): void {
    const { javaHome, maven } = this.toolchain;
    if (!javaHome || !maven) {
      this.status = 'failed';
      this.message = 'In-game descriptions need JDK 17 and Maven; neither JAVA_HOME nor the IntelliJ bundled Maven was found.';
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
      `"-Dlibs.dir=${libsDir(this.paths.projectRoot)}"`,
    ].join(' ');
    const build = spawn(mavenCommandLine, {
      cwd: this.paths.projectRoot,
      shell: true,
      windowsHide: true,
      stdio: ['ignore', buildLog, buildLog],
      env: { ...process.env, JAVA_HOME: javaHome },
    });
    build.on('exit', (exitCode) => {
      fs.closeSync(buildLog);
      if (exitCode === 0 && fs.existsSync(this.classpathFile)) {
        this.status = 'built';
      } else {
        this.status = 'failed';
        this.message = `Compiling the mod for in-game descriptions failed; see ${this.buildLogFile}`;
      }
    });
    build.on('error', () => {
      this.status = 'failed';
      this.message = `Could not run Maven; see ${this.buildLogFile}`;
    });
  }

  describe(requestBody: string): Promise<DescribeResult> {
    const result = this.queue.then(() => this.describeNow(requestBody));
    this.queue = result.catch(() => undefined);
    return result;
  }

  private async describeNow(requestBody: string): Promise<DescribeResult> {
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
    const classpath = 'target\\classes;target\\test-classes;' + fs.readFileSync(this.classpathFile, 'utf8').trim();
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
    const readyLine = await this.nextLine(READY_TIMEOUT_MS);
    if (readyLine !== READY_LINE) {
      this.stopHelper();
      this.status = 'failed';
      this.message = 'The description helper did not start.';
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
