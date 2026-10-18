import { csvCell, csvTableFromRows, parseCsvTable, serializeCsvTable, setCsvCell, trimmedCsvCells, type CsvTable } from '../../../shared/csv.ts';
import { isUnshippedImagePath } from '../../../shared/imagePaths.ts';

export const MATERIAL_ICON_COLUMN = 'plugin params';

export interface CraftingItem {
  id: string;
  label: string;
  material: boolean;
  cost: boolean;
}

export const CRAFTING_ITEMS: CraftingItem[] = [
  { id: 'exiledSector_socketable_parts', label: 'Hull Item Parts', material: true, cost: false },
  { id: 'common_subroutine', label: 'New common subroutine', material: false, cost: true },
  { id: 'exiledSector_augmentation_kernel', label: 'Augmentation Kernel', material: true, cost: true },
  { id: 'exiledSector_attunement_kernel', label: 'Attunement Kernel', material: true, cost: true },
  { id: 'exiledSector_recalibration_kernel', label: 'Recalibration Kernel', material: true, cost: true },
  { id: 'exiledSector_substitution_kernel', label: 'Substitution Kernel', material: true, cost: true },
  { id: 'exiledSector_transposition_kernel', label: 'Transposition Kernel', material: true, cost: true },
  { id: 'exiledSector_fixation_kernel', label: 'Fixation Kernel', material: true, cost: true },
  { id: 'exiledSector_replication_kernel', label: 'Replication Kernel', material: true, cost: false },
];

export interface CostRow {
  cells: string[];
  item: string;
  partsCost: string;
}

export interface CraftingModel {
  costTable: CsvTable;
  costs: CostRow[];
  specialItems: CsvTable;
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

export function materialRow(specialItems: CsvTable, id: string): string[] | undefined {
  return specialItems.rows.find((cells) => csvCell(specialItems, cells, 'id').trim() === id);
}

export function materialIcon(specialItems: CsvTable, id: string): string {
  const row = materialRow(specialItems, id);
  return row ? csvCell(specialItems, row, MATERIAL_ICON_COLUMN).trim() : '';
}

export function setMaterialIcon(specialItems: CsvTable, id: string, path: string): void {
  const row = materialRow(specialItems, id);
  if (row) {
    setCsvCell(specialItems, row, MATERIAL_ICON_COLUMN, path);
  }
}

export function costRow(model: Pick<CraftingModel, 'costs'>, id: string): CostRow | undefined {
  return model.costs.find((row) => row.item === id);
}

export function validateCrafting(model: CraftingModel): string[] {
  const issues: string[] = [];
  for (const item of CRAFTING_ITEMS) {
    if (item.material) {
      const icon = materialIcon(model.specialItems, item.id);
      if (!materialRow(model.specialItems, item.id)) {
        issues.push(`${item.label}: no row in special_items.csv`);
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
