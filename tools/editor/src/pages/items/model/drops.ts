import { csvCell, csvTableFromRows, parseCsvTable, serializeCsvTable, type CsvTable } from '../../../shared/csv.ts';
import { formatPercent, isNumberText } from '../../../shared/decimalText.ts';

export const DROP_SLOTS = 5;

export interface DropRow {
  cells: string[];
  passthrough: boolean;
  site: string;
  chances: string[];
  unique: string;
}

export interface DropsModel {
  table: CsvTable;
  rows: DropRow[];
}

export function parseSalvageCsv(text: string): DropsModel {
  const table = parseCsvTable(text);
  return {
    table,
    rows: table.rows.map((cells) => {
      const site = csvCell(table, cells, 'site').trim();
      return {
        cells,
        passthrough: !site || site.startsWith('#'),
        site,
        chances: csvCell(table, cells, 'chances')
          .split(';')
          .map((chance) => chance.trim())
          .filter(Boolean),
        unique: csvCell(table, cells, 'uniqueChance').trim(),
      };
    }),
  };
}

export function serializeSalvageCsv(model: DropsModel): string {
  return serializeCsvTable(
    csvTableFromRows(model.table, model.rows, (row) =>
      row.passthrough ? {} : { site: row.site, chances: row.chances.join(';'), uniqueChance: row.unique },
    ),
  );
}

export function chancePercent(fraction: string | undefined): number {
  return fraction === '' || fraction === undefined ? Number.NaN : Number.parseFloat(fraction) * 100;
}

export function atLeastPercent(row: Pick<DropRow, 'chances'>, count: number): number {
  let chance = 1;
  for (let slot = 0; slot < count; slot++) {
    const percent = chancePercent(row.chances[slot]);
    if (Number.isNaN(percent)) {
      return 0;
    }
    chance *= percent / 100;
  }
  return chance * 100;
}

export function dropSummary(row: Pick<DropRow, 'chances'>): string {
  const parts: string[] = [];
  for (let count = 1; count <= DROP_SLOTS; count++) {
    const percent = atLeastPercent(row, count);
    if (percent <= 0) {
      break;
    }
    parts.push(`${count}+: ${formatPercent(percent)}%`);
  }
  return parts.join(', ');
}

export function setChance(row: DropRow, slot: number, fraction: string): void {
  while (row.chances.length < slot) {
    row.chances.push('');
  }
  row.chances[slot] = fraction;
  while (row.chances.length > 0 && row.chances[row.chances.length - 1] === '') {
    row.chances.pop();
  }
}

export function validateDrops(model: DropsModel): string[] {
  const issues: string[] = [];
  const seen = new Set<string>();
  model.rows.forEach((row, index) => {
    if (row.passthrough) {
      return;
    }
    const label = row.site || `row ${index + 1}`;
    if (!/^[A-Za-z0-9_.-]+$/.test(row.site)) {
      issues.push(`${label}: site ids may only use letters, digits, _ . and -`);
    }
    if (seen.has(row.site)) {
      issues.push(`${label}: listed more than once`);
    }
    seen.add(row.site);
    row.chances.forEach((chance, slot) => {
      const percent = chancePercent(chance);
      if (chance === '') {
        issues.push(`${label}: item ${slot + 1} is blank but a later item is set`);
      } else if (!isNumberText(chance) || percent < 0 || percent > 100) {
        issues.push(`${label}: item ${slot + 1} must be between 0 and 100`);
      }
    });
    const uniquePercent = chancePercent(row.unique);
    if (row.unique !== '' && (!isNumberText(row.unique) || uniquePercent < 0 || uniquePercent > 100)) {
      issues.push(`${label}: unique must be between 0 and 100`);
    }
  });
  return issues;
}
