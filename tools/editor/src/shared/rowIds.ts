export interface SavedRow {
  id: string;
  loadedId?: string;
}

export function markRowsSaved(rows: readonly SavedRow[], savedRows: readonly SavedRow[]): void {
  for (const row of rows) {
    const savedRow = savedRows.find((candidate) => (row.loadedId ? candidate.loadedId === row.loadedId : !candidate.loadedId && candidate.id === row.id));
    if (savedRow) {
      row.loadedId = savedRow.id;
    }
  }
}

export function firstFreeNumberedId(base: string, isTaken: (id: string) => boolean, firstNumber: number): string {
  let number = firstNumber;
  while (isTaken(`${base}_${number}`)) {
    number++;
  }
  return `${base}_${number}`;
}

export function uniqueSuffixedId(base: string, isTaken: (id: string) => boolean): string {
  return isTaken(base) ? firstFreeNumberedId(base, isTaken, 2) : base;
}
