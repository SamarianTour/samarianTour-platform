/** Un turista de los datos de demostración de opiniones. */
export interface TuristaDemo {
  readonly id: string;
  readonly nombre: string;
}

/**
 * IDENTIDAD PROVISIONAL: los turistas de los datos de demostración del backend (DatosOpiniones). Laura ya pagó
 * ofertas que todavía no calificó, así que puede opinar sin habilitaciones por consola. Cuando exista el JWT del
 * núcleo, esta lista y el selector desaparecen.
 */
export const TURISTAS_DEMO: readonly TuristaDemo[] = [
  { id: '70000000-0000-0000-0000-000000000007', nombre: 'Laura' },
  { id: '80000000-0000-0000-0000-000000000008', nombre: 'Andrés' },
];
