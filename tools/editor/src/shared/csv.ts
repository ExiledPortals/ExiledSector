const BYTE_ORDER_MARK = String.fromCharCode(0xfeff);

export interface CsvTable {
  header: string[];
  rows: string[][];
  lineEnding: '\r\n' | '\n';
}

export function parseCsv(text: string): string[][] {
  const raw = String(text);
  const source = raw.startsWith(BYTE_ORDER_MARK) ? raw.slice(1) : raw;
  const rows: string[][] = [];
  let row: string[] = [];
  let field = '';
  let inQuotes = false;
  for (let index = 0; index < source.length; index++) {
    const character = source[index];
    if (inQuotes) {
      if (character === '"' && source[index + 1] === '"') {
        field += '"';
        index++;
      } else if (character === '"') {
        inQuotes = false;
      } else {
        field += character;
      }
    } else if (character === '"') {
      inQuotes = true;
    } else if (character === ',') {
      row.push(field);
      field = '';
    } else if (character === '\n') {
      row.push(field);
      rows.push(row);
      row = [];
      field = '';
    } else if (character !== '\r') {
      field += character;
    }
  }
  if (field !== '' || row.length > 0) {
    row.push(field);
    rows.push(row);
  }
  return rows.filter((cells) => cells.some((cell) => cell.trim() !== ''));
}

export function csvField(value: unknown): string {
  const text = value === undefined || value === null ? '' : String(value);
  return /[",\r\n]/.test(text) || text !== text.trim() ? `"${text.replace(/"/g, '""')}"` : text;
}

export function parseCsvTable(text: string): CsvTable {
  const rows = parseCsv(text);
  const header = (rows.shift() ?? []).map((column) => column.trim());
  return { header, rows, lineEnding: /\r\n/.test(String(text)) ? '\r\n' : '\n' };
}

export function csvCell(table: CsvTable, cells: string[], column: string): string {
  const index = table.header.indexOf(column);
  return index < 0 || cells[index] === undefined ? '' : cells[index];
}

export function setCsvCell(table: CsvTable, cells: string[], column: string, value: unknown): void {
  let index = table.header.indexOf(column);
  if (index < 0) {
    table.header.push(column);
    index = table.header.length - 1;
  }
  while (cells.length <= index) {
    cells.push('');
  }
  cells[index] = value === undefined || value === null ? '' : String(value);
}

export function emptyCsvTable(table: CsvTable): CsvTable {
  return { header: table.header.slice(), rows: [], lineEnding: table.lineEnding };
}

export function trimmedCsvCells<Column extends string>(table: CsvTable, cells: string[], columns: readonly Column[]): Record<Column, string> {
  return Object.fromEntries(columns.map((column) => [column, csvCell(table, cells, column).trim()])) as Record<Column, string>;
}

export function csvTableFromRows<Row extends { cells: string[] }>(
  table: CsvTable,
  rows: readonly Row[],
  columnValues: (row: Row) => Record<string, unknown>,
): CsvTable {
  const out = emptyCsvTable(table);
  for (const row of rows) {
    const cells = row.cells.slice();
    for (const [column, value] of Object.entries(columnValues(row))) {
      setCsvCell(out, cells, column, value);
    }
    out.rows.push(cells);
  }
  return out;
}

export function dropCsvColumn(table: CsvTable, column: string): void {
  const index = table.header.indexOf(column);
  if (index < 0) {
    return;
  }
  table.header.splice(index, 1);
  for (const cells of table.rows) {
    if (cells.length > index) {
      cells.splice(index, 1);
    }
  }
}

export function serializeCsvTable(table: CsvTable): string {
  const width = table.header.length;
  return (
    [table.header, ...table.rows]
      .map((cells) => {
        const padded = cells.slice();
        while (padded.length < width) {
          padded.push('');
        }
        return padded.map(csvField).join(',');
      })
      .join(table.lineEnding) + table.lineEnding
  );
}
