const BYTE_ORDER_MARK = String.fromCharCode(0xfeff);
const HEADER_QUOTE_KEY = '\u0001header';

export interface CsvTable {
  header: string[];
  rows: string[][];
  lineEnding: '\r\n' | '\n';
  quotedValues?: Record<string, true>;
}

interface ParsedCsvRow {
  cells: string[];
  quoted: boolean[];
}

function parseCsvRows(text: string): ParsedCsvRow[] {
  const raw = String(text);
  const source = raw.startsWith(BYTE_ORDER_MARK) ? raw.slice(1) : raw;
  const rows: ParsedCsvRow[] = [];
  let row: ParsedCsvRow = { cells: [], quoted: [] };
  let field = '';
  let fieldQuoted = false;
  let inQuotes = false;
  const endField = () => {
    row.cells.push(field);
    row.quoted.push(fieldQuoted);
    field = '';
    fieldQuoted = false;
  };
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
      fieldQuoted = true;
    } else if (character === ',') {
      endField();
    } else if (character === '\n') {
      endField();
      rows.push(row);
      row = { cells: [], quoted: [] };
    } else if (character !== '\r') {
      field += character;
    }
  }
  if (field !== '' || fieldQuoted || row.cells.length > 0) {
    endField();
    rows.push(row);
  }
  return rows.filter((parsed) => parsed.cells.some((cell) => cell.trim() !== ''));
}

export function parseCsv(text: string): string[][] {
  return parseCsvRows(text).map((parsed) => parsed.cells);
}

function needsQuotes(text: string): boolean {
  return /[",\r\n]/.test(text) || text !== text.trim();
}

export function csvField(value: unknown, keepQuotes = false): string {
  const text = value === undefined || value === null ? '' : String(value);
  return keepQuotes || needsQuotes(text) ? `"${text.replace(/"/g, '""')}"` : text;
}

function quoteKey(column: string, value: string): string {
  return `${column}\u0000${value}`;
}

export function parseCsvTable(text: string): CsvTable {
  const parsedRows = parseCsvRows(text);
  const headerRow = parsedRows.shift() ?? { cells: [], quoted: [] };
  const header = headerRow.cells.map((column) => column.trim());
  const quotedValues: Record<string, true> = {};
  headerRow.quoted.forEach((quoted, index) => {
    if (quoted && !needsQuotes(headerRow.cells[index])) {
      quotedValues[quoteKey(HEADER_QUOTE_KEY, headerRow.cells[index])] = true;
    }
  });
  for (const parsed of parsedRows) {
    parsed.quoted.forEach((quoted, index) => {
      const cell = parsed.cells[index];
      if (quoted && cell !== '' && !needsQuotes(cell)) {
        quotedValues[quoteKey(header[index] ?? String(index), cell)] = true;
      }
    });
  }
  const table: CsvTable = { header, rows: parsedRows.map((parsed) => parsed.cells), lineEnding: /\r\n/.test(String(text)) ? '\r\n' : '\n' };
  if (Object.keys(quotedValues).length > 0) {
    table.quotedValues = quotedValues;
  }
  return table;
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
  const empty: CsvTable = { header: table.header.slice(), rows: [], lineEnding: table.lineEnding };
  if (table.quotedValues) {
    empty.quotedValues = { ...table.quotedValues };
  }
  return empty;
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
  const quotedValues = table.quotedValues ?? {};
  const headerLine = table.header.map((column) => csvField(column, Boolean(quotedValues[quoteKey(HEADER_QUOTE_KEY, column)]))).join(',');
  const rowLines = table.rows.map((cells) => {
    const padded = cells.slice();
    while (padded.length < width) {
      padded.push('');
    }
    return padded.map((cell, index) => csvField(cell, cell !== '' && Boolean(quotedValues[quoteKey(table.header[index] ?? String(index), cell)]))).join(',');
  });
  return [headerLine, ...rowLines].join(table.lineEnding) + table.lineEnding;
}
