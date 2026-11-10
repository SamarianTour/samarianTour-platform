/** Un UUID v4 para la cabecera `Idempotency-Key` (ADR-04). */
export function nuevaClave(): string {
  const cripto = globalThis.crypto;
  if (cripto?.randomUUID) {
    return cripto.randomUUID();
  }
  // Respaldo para navegadores sin `randomUUID`.
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (letra) => {
    const azar = cripto?.getRandomValues
      ? cripto.getRandomValues(new Uint8Array(1))[0] % 16
      : Math.floor(Math.random() * 16);
    return (letra === 'x' ? azar : (azar & 0x3) | 0x8).toString(16);
  });
}

/**
 * La clave de idempotencia de una operación. El núcleo devuelve la respuesta original si recibe la misma clave con el mismo
 * cuerpo, y responde 422 si la clave llega con otro cuerpo. Por eso una operación que se reintenta sin cambios conserva su
 * clave, y si el usuario corrige algún dato antes de reintentar, la clave cambia.
 */
export class ClaveDeIdempotencia {
  private ultimo: { readonly cuerpo: string; readonly clave: string } | null = null;

  /** La clave para enviar este cuerpo: la misma que la vez anterior si el cuerpo no cambió; una nueva si cambió. */
  para(cuerpo: unknown): string {
    const texto = JSON.stringify(cuerpo);
    if (this.ultimo?.cuerpo !== texto) {
      this.ultimo = { cuerpo: texto, clave: nuevaClave() };
    }
    return this.ultimo.clave;
  }

  /** Olvida la clave: la siguiente operación, aunque lleve el mismo cuerpo, será una nueva (por ejemplo, tras un éxito). */
  reiniciar(): void {
    this.ultimo = null;
  }
}
