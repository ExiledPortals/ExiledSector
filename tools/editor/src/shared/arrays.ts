export function moveItem<TItem>(items: TItem[], from: number, to: number): void {
  if (to < 0 || to >= items.length) {
    return;
  }
  const [moved] = items.splice(from, 1);
  items.splice(to, 0, moved);
}

export function compareText(first: string, second: string): number {
  if (first < second) {
    return -1;
  }
  return first > second ? 1 : 0;
}

export function withCurrentOptions(options: readonly string[], currentValues: readonly string[]): readonly string[] {
  const missing = currentValues.filter((value) => value !== '' && !options.includes(value));
  return missing.length === 0 ? options : [...missing, ...options];
}

export function withCurrentOption(options: readonly string[], currentValue: string): readonly string[] {
  return withCurrentOptions(options, [currentValue]);
}
