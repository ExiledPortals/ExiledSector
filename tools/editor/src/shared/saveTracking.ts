export interface SaveTicket {
  readonly revision: number;
  readonly loadGeneration: number;
}

export type SaveOutcome = 'superseded' | 'clean' | 'editedSince';

export function saveOutcome(ticket: SaveTicket, current: SaveTicket): SaveOutcome {
  if (current.loadGeneration !== ticket.loadGeneration) {
    return 'superseded';
  }
  return current.revision === ticket.revision ? 'clean' : 'editedSince';
}
