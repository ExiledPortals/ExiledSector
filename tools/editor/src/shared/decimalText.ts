const NUMBER_TEXT = /^[-+]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][-+]?\d+)?$/;

export function isNumberText(text: unknown): boolean {
  return NUMBER_TEXT.test(String(text).trim());
}
