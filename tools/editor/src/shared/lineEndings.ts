export type LineEnding = '\r\n' | '\n';

export function lineEndingOf(text: string): LineEnding {
  return text.includes('\r\n') ? '\r\n' : '\n';
}

export function withLineEnding(text: string, lineEnding: LineEnding): string {
  return lineEnding === '\n' ? text : text.replace(/\n/g, lineEnding);
}
