# language: es
@EP-08 @US-26 @identity-service
Característica: Acceso institucional al panel
  Como representante de una municipalidad o junta vecinal
  Quiero acceder al panel web con credenciales institucionales
  Para consultar la data consolidada de mi jurisdicción

  # identity-service es responsable de crear la cuenta institucional, asignarle su
  # jurisdicción e incluirla en el token de sesión. El filtrado de los datos por
  # jurisdicción lo aplica geo-analytics-service a partir de ese token.

  @smoke
  Escenario: Alta de una cuenta institucional
    Dado que el administrador ha iniciado sesión en PeaceApp
    Cuando crea una cuenta institucional con el correo "miraflores@muni.gob.pe" para la jurisdicción "Miraflores"
    Entonces la cuenta queda creada con rol "INSTITUCIONAL" y jurisdicción "Miraflores"
    Y la cuenta queda verificada sin requerir confirmación de correo

  Escenario: Inicio de sesión institucional con jurisdicción en el token
    Dado que existe una cuenta institucional "miraflores@muni.gob.pe" para la jurisdicción "Miraflores"
    Cuando el representante inicia sesión con sus credenciales institucionales
    Entonces el token de acceso identifica el rol "INSTITUCIONAL" y la jurisdicción "Miraflores"

  Escenario: Solo un administrador puede crear cuentas institucionales
    Dado que el usuario "ana@peaceapp.pe" con rol "CIUDADANO" ha iniciado sesión en PeaceApp
    Cuando intenta crear una cuenta institucional para la jurisdicción "Miraflores"
    Entonces la operación es rechazada por falta de permisos

  Esquema del escenario: Reglas de jurisdicción al asignar un rol
    Dado que el administrador ha iniciado sesión en PeaceApp
    Y existe el usuario "luis@peaceapp.pe" con rol "CIUDADANO"
    Cuando le asigna el rol "<rol>" con la jurisdicción "<jurisdicción>"
    Entonces la asignación resulta "<resultado>"

    Ejemplos:
      | rol           | jurisdicción | resultado                                            |
      | INSTITUCIONAL | San Isidro   | exitosa                                              |
      | INSTITUCIONAL |              | Una cuenta institucional requiere una jurisdiccion   |
      | MODERADOR     | San Isidro   | exitosa sin jurisdicción                             |
