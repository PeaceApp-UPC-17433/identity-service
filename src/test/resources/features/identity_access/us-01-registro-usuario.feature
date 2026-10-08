# language: es
@EP-01 @US-01 @identity-service
Característica: Registro de usuario
  Como ciudadano
  Quiero registrarme con mi correo y una contraseña
  Para acceder a las funciones de reporte y validación

  @smoke
  Escenario: Registro exitoso
    Dado que el correo "ana@peaceapp.pe" no está registrado en PeaceApp
    Cuando el ciudadano se registra con el correo "ana@peaceapp.pe" y la contraseña "Segura2026"
    Entonces la cuenta queda creada con rol "CIUDADANO" y pendiente de verificación
    Y se envía un correo de verificación a "ana@peaceapp.pe"
    Y el sistema responde "Cuenta creada. Revisa tu correo para verificarla antes de iniciar sesion"

  Escenario: Registro con un correo ya registrado
    Dado que el correo "ana@peaceapp.pe" ya está registrado en PeaceApp
    Cuando el ciudadano se registra con el correo "ana@peaceapp.pe" y la contraseña "Segura2026"
    Entonces el registro es rechazado con el mensaje "El correo ya se encuentra registrado"
    Y existe una sola cuenta con el correo "ana@peaceapp.pe"

  Esquema del escenario: El correo no distingue mayúsculas ni espacios
    Dado que el correo "ana@peaceapp.pe" ya está registrado en PeaceApp
    Cuando el ciudadano se registra con el correo "<correo>" y la contraseña "Segura2026"
    Entonces el registro es rechazado con el mensaje "El correo ya se encuentra registrado"

    Ejemplos:
      | correo             |
      | ANA@PEACEAPP.PE    |
      | Ana@PeaceApp.pe    |
      | ana@PeaceApp.PE    |

  Esquema del escenario: Contraseña que no cumple la política de seguridad
    Dado que el correo "luis@peaceapp.pe" no está registrado en PeaceApp
    Cuando el ciudadano se registra con el correo "luis@peaceapp.pe" y la contraseña "<contraseña>"
    Entonces el registro es rechazado indicando "<motivo>"
    Y no se crea ninguna cuenta con el correo "luis@peaceapp.pe"

    Ejemplos:
      | contraseña | motivo                                                    |
      | Abc12      | La contrasena debe tener entre 8 y 72 caracteres          |
      | solotexto  | La contrasena debe contener al menos una letra y un numero |
      | 12345678   | La contrasena debe contener al menos una letra y un numero |

  Esquema del escenario: Verificación del correo según la vigencia del enlace
    Dado que el ciudadano se registró con el correo "ana@peaceapp.pe" hace <horas> horas
    Cuando confirma su correo con el enlace de verificación recibido
    Entonces la verificación resulta "<resultado>"

    Ejemplos:
      | horas | resultado                                      |
      | 1     | exitosa                                        |
      | 23    | exitosa                                        |
      | 25    | El enlace de verificacion es invalido o expiro |
