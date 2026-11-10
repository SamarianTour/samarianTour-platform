import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { INTERVALO_DEL_RELOJ_MS, ahoraConReloj } from './reloj';

@Component({ template: '' })
class Anfitrion {
  readonly ahora = ahoraConReloj();
}

describe('ahoraConReloj', () => {
  beforeEach(() => {
    vi.useFakeTimers({
      now: new Date('2026-10-26T15:00:00Z'),
      toFake: ['Date', 'setInterval', 'clearInterval'],
    });
  });

  afterEach(() => vi.useRealTimers());

  it('empieza en el momento actual', () => {
    const anfitrion = TestBed.createComponent(Anfitrion).componentInstance;

    expect(anfitrion.ahora()).toBe(Date.parse('2026-10-26T15:00:00Z'));
  });

  it('se renueva cada 30 segundos', () => {
    const anfitrion = TestBed.createComponent(Anfitrion).componentInstance;

    vi.advanceTimersByTime(INTERVALO_DEL_RELOJ_MS);
    expect(anfitrion.ahora()).toBe(Date.parse('2026-10-26T15:00:30Z'));

    vi.advanceTimersByTime(4 * INTERVALO_DEL_RELOJ_MS);
    expect(anfitrion.ahora()).toBe(Date.parse('2026-10-26T15:02:30Z'));
  });

  it('se detiene cuando la pantalla se destruye', () => {
    const fixture = TestBed.createComponent(Anfitrion);
    const anfitrion = fixture.componentInstance;

    fixture.destroy();
    const antes = anfitrion.ahora();
    vi.advanceTimersByTime(10 * INTERVALO_DEL_RELOJ_MS);

    expect(anfitrion.ahora()).toBe(antes);
    expect(vi.getTimerCount()).toBe(0);
  });
});
