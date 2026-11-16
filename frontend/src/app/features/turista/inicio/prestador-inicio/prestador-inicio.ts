import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TPipe } from '../../../../core/idioma/t.pipe';
import { Icono } from '../../../../shared/icono/icono';

/** Invitación a los prestadores turísticos: lleva al registro, donde se valida el RNT. */
@Component({
  selector: 'app-prestador-inicio',
  imports: [RouterLink, Icono, TPipe],
  templateUrl: './prestador-inicio.html',
  styleUrl: './prestador-inicio.css',
})
export class PrestadorInicio {}
