import { TestBed } from '@angular/core/testing';
import { establecerIdioma } from '../../core/idioma/idioma';
import { parsearMarkdown, segmentos } from './markdown-basico';
import { TextoFormateado } from './texto-formateado';

const MUESTRA = `# Política de tratamiento de datos personales de Samarian Tour

**Versión v1 · publicada el 28 de octubre de 2026**

> Texto de demostración del prototipo académico [SUPUESTO] (DN-28). No ha sido revisado por un abogado y debe
> reemplazarse por el texto definitivo.

## 1. Responsable

Samarian Tour es responsable del
tratamiento de los datos personales.

## 2. Datos que tratamos

- **Todas las cuentas:** nombre, correo electrónico y
  contraseña.
- **Turistas:** número de WhatsApp.

## 3. Finalidades

1. Crear y administrar la cuenta.
2. Gestionar las solicitudes de
disponibilidad.
`;

describe('parsearMarkdown', () => {
  it('separa la negrita del texto normal', () => {
    expect(segmentos('Hola **mundo** y **más**.')).toEqual([
      { texto: 'Hola ', negrita: false },
      { texto: 'mundo', negrita: true },
      { texto: ' y ', negrita: false },
      { texto: 'más', negrita: true },
      { texto: '.', negrita: false },
    ]);
  });

  it('una ** sin pareja queda como texto', () => {
    expect(segmentos('Abierta ** sin cerrar')).toEqual([{ texto: 'Abierta ** sin cerrar', negrita: false }]);
  });

  it('los títulos bajan un nivel: # es h2 y ## es h3', () => {
    const bloques = parsearMarkdown('# Uno\n\n## Dos');

    expect(bloques.map((b) => [b.tipo, b.nivel])).toEqual([
      ['titulo', 2],
      ['titulo', 3],
    ]);
  });

  it('une las líneas seguidas de un mismo párrafo, y un párrafo nuevo empieza tras una línea en blanco', () => {
    const bloques = parsearMarkdown('Primera línea\ny segunda línea.\n\nOtro párrafo.');

    expect(bloques.map((b) => b.segmentos.map((s) => s.texto).join(''))).toEqual([
      'Primera línea y segunda línea.',
      'Otro párrafo.',
    ]);
  });

  it('la cita junta sus líneas sin el signo >', () => {
    const [cita] = parsearMarkdown('> Primera.\n> Segunda.');

    expect(cita.tipo).toBe('cita');
    expect(cita.segmentos.map((s) => s.texto).join('')).toBe('Primera. Segunda.');
  });

  it('las listas juntan las líneas de cada elemento y se separan en elementos', () => {
    const [lista] = parsearMarkdown('- **Uno:** a\n  b\n- Dos');

    expect(lista.tipo).toBe('lista');
    expect(lista.items.map((i) => i.map((s) => s.texto).join(''))).toEqual(['Uno: a b', 'Dos']);
    expect(lista.items[0][0]).toEqual({ texto: 'Uno:', negrita: true });
  });

  it('la lista numerada es otro bloque, no se mezcla con la de viñetas', () => {
    const bloques = parsearMarkdown('- a\n1. b\n2. c');

    expect(bloques.map((b) => [b.tipo, b.items.length])).toEqual([
      ['lista', 1],
      ['numerada', 2],
    ]);
  });

  it('acepta saltos de línea de Windows', () => {
    expect(parsearMarkdown('Uno\r\ndos\r\n\r\nTres').map((b) => b.tipo)).toEqual(['parrafo', 'parrafo']);
  });
});

describe('TextoFormateado', () => {
  const raiz = (f: { nativeElement: unknown }) => f.nativeElement as HTMLElement;

  function crear(texto: string, omitirTitulo?: 'legal.politica.titulo') {
    const fixture = TestBed.createComponent(TextoFormateado);
    fixture.componentRef.setInput('texto', texto);
    if (omitirTitulo) {
      fixture.componentRef.setInput('omitirTitulo', omitirTitulo);
    }
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(() => establecerIdioma('es', { recordar: false }));
  afterEach(() => establecerIdioma('es', { recordar: false }));

  it('pinta títulos, negrita, cita y las dos listas con sus elementos HTML, sin símbolos de Markdown', () => {
    const fixture = crear(MUESTRA);
    const el = raiz(fixture);

    expect(Array.from(el.querySelectorAll('h2')).map((h) => h.textContent)).toEqual([
      'Política de tratamiento de datos personales de Samarian Tour',
    ]);
    expect(Array.from(el.querySelectorAll('h3')).map((h) => h.textContent)).toEqual([
      '1. Responsable',
      '2. Datos que tratamos',
      '3. Finalidades',
    ]);
    expect(el.querySelector('blockquote')?.textContent).toContain('Texto de demostración del prototipo académico');
    expect(el.querySelectorAll('ul > li')).toHaveLength(2);
    expect(el.querySelectorAll('ol > li')).toHaveLength(2);
    expect(el.querySelector('p strong')?.textContent).toBe('Versión v1 · publicada el 28 de octubre de 2026');
    expect(el.querySelector('li strong')?.textContent).toBe('Todas las cuentas:');
    expect(el.textContent).not.toMatch(/[#>]|\*\*/);
    expect(el.textContent).not.toContain('- ');
  });

  it('las frases no se cortan: las líneas del archivo se unen en un solo párrafo, elemento o cita', () => {
    const el = raiz(crear(MUESTRA));

    expect(Array.from(el.querySelectorAll('p')).map((p) => p.textContent)).toContain(
      'Samarian Tour es responsable del tratamiento de los datos personales.',
    );
    expect(el.querySelector('li')?.textContent).toBe('Todas las cuentas: nombre, correo electrónico y contraseña.');
    expect(el.querySelector('blockquote')?.textContent).toContain(
      '(DN-28). No ha sido revisado por un abogado y debe reemplazarse',
    );
    expect(el.querySelectorAll('ol li')[1].textContent).toBe('Gestionar las solicitudes de disponibilidad.');
  });

  it('baja los niveles de título: # es h2 y ## es h3 (la página ya tiene su h1)', () => {
    const el = raiz(crear('# Uno\n\n## Dos'));

    expect(el.querySelector('h1')).toBeNull();
    expect(el.querySelector('h2')?.textContent).toBe('Uno');
    expect(el.querySelector('h3')?.textContent).toBe('Dos');
  });

  it('un <script> o una imagen se ven como texto: no se crea ningún elemento', () => {
    const el = raiz(crear('<script>alert(1)</script>\n\n- <img src=x onerror="alert(2)">\n\n**<b>x</b>**'));

    expect(el.querySelector('script')).toBeNull();
    expect(el.querySelector('img')).toBeNull();
    expect(el.querySelector('b')).toBeNull();
    expect(el.textContent).toContain('<script>alert(1)</script>');
    expect(el.textContent).toContain('<img src=x onerror="alert(2)">');
    expect(el.querySelector('strong')?.textContent).toBe('<b>x</b>');
  });

  it('con omitirTitulo quita el primer título si repite el de la página, en español o en inglés', () => {
    const texto = '# Política de tratamiento de datos personales de Samarian Tour\n\n## 1. Responsable\n\nTexto.';

    expect(Array.from(raiz(crear(texto, 'legal.politica.titulo')).querySelectorAll('h2, h3')).map((h) => h.textContent)).toEqual([
      '1. Responsable',
    ]);

    establecerIdioma('en', { recordar: false });
    expect(raiz(crear(texto, 'legal.politica.titulo')).querySelector('h2')).toBeNull();
  });

  it('con omitirTitulo no quita un título distinto, ni uno que no sea el primero', () => {
    const el = raiz(crear('# Otro título\n\nTexto.\n\n# Política de tratamiento de datos', 'legal.politica.titulo'));

    expect(Array.from(el.querySelectorAll('h2')).map((h) => h.textContent)).toEqual([
      'Otro título',
      'Política de tratamiento de datos',
    ]);
  });

  it('sin omitirTitulo conserva el primer título', () => {
    const el = raiz(crear('# Política de tratamiento de datos\n\nTexto.'));

    expect(el.querySelector('h2')?.textContent).toBe('Política de tratamiento de datos');
  });
});
