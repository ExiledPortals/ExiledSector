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
