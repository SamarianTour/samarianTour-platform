# seguridad-jwt

Librería compartida por los tres servicios: valida el token de acceso RS256 que emite el núcleo y entrega sus roles a Spring Security (diagrama de paquetes, interacción 5; DN-37).

| Clase | Qué hace |
|---|---|
| `DecodificadorToken` | Valida el token con la clave pública del núcleo: solo RS256 con esa clave, `iss` del núcleo, `exp` obligatorio (60 s de tolerancia), `sub` como UUID, `rol`, y `tipoPrestador` solo con el rol PRESTADOR. |
| `ReclamosToken` | Los nombres de los reclamos (`rol`, `tipoPrestador`, `sid`), que el núcleo usa al firmar, y su lectura. |
| `AutenticacionToken` | El usuario autenticado, con la autoridad `ROLE_<rol>` que comparan las reglas `hasRole`. |
| `SeguridadJwt` | La configuración común de Spring Security: servidor de recursos sin estado, sin sesión, cookie, CSRF ni formulario. Cada servicio agrega después sus reglas por prefijo. |
| `RespuestasSeguridad` | 401 `NO_AUTENTICADO` con `WWW-Authenticate: Bearer` y 403 `ACCESO_DENEGADO`, en ProblemDetail. |
| `ClavesRsa` | Lee las claves en Base64 de una línea o en PEM, como las genera `infra/generar-claves.sh`. |

## Uso en un servicio

```java
@Bean
JwtDecoder decodificadorToken(@Value("${seguridad.jwt.clave-publica:}") String clave,
                              @Value("${seguridad.jwt.emisor}") String emisor) {
    return DecodificadorToken.crear(ClavesRsa.publica(clave), emisor);
}

@Bean
SecurityFilterChain seguridad(HttpSecurity http, JwtDecoder decodificador, ObjectMapper json) throws Exception {
    return SeguridadJwt.aplicar(http, decodificador, json)
            .authorizeHttpRequests(rutas -> rutas
                    .requestMatchers("/prestador/**").hasRole("PRESTADOR")
                    .anyRequest().permitAll())
            .build();
}
```

La identidad se lee con `peticion.getUserPrincipal()`, que es un `AutenticacionToken` con los `ReclamosToken` (en los servicios, `Identidad.de(peticion)`).

## Pruebas

El test-jar (`<type>test-jar</type>`, `<scope>test</scope>`) trae:

- `TokensDePrueba`: firma tokens como el núcleo (`bearer(usuario, rol)`, `bearerPrestador(usuario, tipo)`), y también vencidos, con otra clave o ajustados, para probar los rechazos. El par de claves se genera al cargar la clase: no hay claves en el repositorio.
- `ClavesDePrueba`: registrado en `META-INF/spring.factories`, entrega a cada contexto de Spring de las pruebas `seguridad.jwt.clave-publica` y `seguridad.jwt.pruebas.clave-privada` (el núcleo firma con esta).

En cada servicio, `src/test/resources/META-INF/spring/...AutoConfigureWebMvc.imports` agrega su `SeguridadConfig` a las pruebas `@WebMvcTest`, para que corran con las reglas reales.

```bash
mvn -pl libs/seguridad-jwt verify
```
