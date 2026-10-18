import { csvCell, csvTableFromRows, parseCsvTable, serializeCsvTable, setCsvCell, trimmedCsvCells, type CsvTable } from '../../../shared/csv.ts';
import { isUnshippedImagePath } from '../../../shared/imagePaths.ts';

export interface CraftingItem {
  id: string;
  label: string;
  commodity: boolean;
  cost: boolean;
}

export const CRAFTING_ITEMS: CraftingItem[] = [
  { id: 'exiledSector_socketable_parts', label: 'Hull Item Parts', commodity: true, cost: false },
  { id: 'common_subroutine', label: 'New common subroutine', commodity: false, cost: true },
  { id: 'exiledSector_augmentation_kernel', label: 'Augmentation Kernel', commodity: true, cost: true },
  { id: 'exiledSector_recalibration_kernel', label: 'Recalibration Kernel', commodity: true, cost: true },
  { id: 'exiledSector_transposition_kernel', label: 'Transposition Kernel', commodity: true, cost: true },
];

export interface CostRow {
  cells: string[];
  item: string;
  partsCost: string;
}

export interface CraftingModel {
  costTable: CsvTable;
  costs: CostRow[];
  commodities: CsvTable;
}

export function parseCraftingCsv(text: string): { table: CsvTable; rows: CostRow[] } {
  const table = parseCsvTable(text);
  const rows = table.rows.map((cells) => ({ cells, ...trimmedCsvCells(table, cells, ['item', 'partsCost']) }));
  for (const item of CRAFTING_ITEMS) {
    if (item.cost && !rows.some((row) => row.item === item.id)) {
      rows.push({ cells: [], item: item.id, partsCost: '' });
    }
  }
  return { table, rows };
}

export function serializeCraftingCsv(table: CsvTable, rows: CostRow[]): string {
  return serializeCsvTable(csvTableFromRows(table, rows, (row) => ({ item: row.item, partsCost: row.partsCost })));
}

export function commodityRow(commodities: CsvTable, id: string): string[] | undefined {
  return commodities.rows.find((cells) => csvCell(commodities, cells, 'id').trim() === id);
}

export function commodityIcon(commodities: CsvTable, id: string): string {
  const row = commodityRow(commodities, id);
  return row ? csvCell(commodities, row, 'icon').trim() : '';
}

export function setCommodityIcon(commodities: CsvTable, id: string, path: string): void {
  const row = commodityRow(commodities, id);
  if (row) {
    setCsvCell(commodities, row, 'icon', path);
  }
}

export function costRow(model: Pick<CraftingModel, 'costs'>, id: string): CostRow | undefined {
  return model.costs.find((row) => row.item === id);
}

export function validateCrafting(model: CraftingModel): string[] {
  const issues: string[] = [];
  for (const item of CRAFTING_ITEMS) {
    if (item.commodity) {
      const icon = commodityIcon(model.commodities, item.id);
      if (!commodityRow(model.commodities, item.id)) {
        issues.push(`${item.label}: no row in commodities.csv`);
      }
      if (!icon) {
        issues.push(`${item.label}: needs an image`);
      }
      if (isUnshippedImagePath(icon)) {
        issues.push(`${item.label}: its image is in a folder that is not deployed`);
      }
    }
    if (item.cost && !/^\d+$/.test(costRow(model, item.id)?.partsCost ?? '')) {
      issues.push(`${item.label}: the cost must be a whole number of parts`);
    }
  }
  return issues;
}
