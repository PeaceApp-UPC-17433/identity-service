# language: es
@EP-01 @US-02 @identity-service
Característica: Inicio de sesión
  Como usuario registrado
  Quiero iniciar sesión de forma segura
  Para usar la aplicación con mi identidad y mi reputación

  Antecedentes:
    Dado que existe una cuenta verificada con el correo "ana@peaceapp.pe" y la contraseña "Segura2026"

  @smoke
  Escenario: Inicio de sesión con credenciales válidas
    Cuando el usuario inicia sesión con el correo "ana@peaceapp.pe" y la contraseña "Segura2026"
    Entonces el usuario queda autenticado
    Y recibe un token de acceso y un token de renovación de sesión

  Esquema del escenario: Credenciales inválidas con mensaje genérico
    Cuando el usuario inicia sesión con el correo "<correo>" y la contraseña "<contraseña>"
    Entonces el acceso es rechazado con el mensaje "Credenciales invalidas"

    Ejemplos:
      | correo             | contraseña   |
      | ana@peaceapp.pe    | Incorrecta99 |
      | nadie@peaceapp.pe  | Segura2026   |

  Esquema del escenario: Bloqueo tras intentos fallidos consecutivos
    Dado que el usuario falló <intentos> veces consecutivas al iniciar sesión
    Cuando inicia sesión con el correo "ana@peaceapp.pe" y la contraseña "Segura2026"
    Entonces el inicio de sesión resulta "<resultado>"

    Ejemplos:
      | intentos | resultado |
      | 3        | exitoso   |
      | 4        | exitoso   |
      | 5        | bloqueado |

  Escenario: El bloqueo no revela que la cuenta está bloqueada
    Dado que la cuenta "ana@peaceapp.pe" fue bloqueada por intentos fallidos
    Cuando el usuario inicia sesión con el correo "ana@peaceapp.pe" y la contraseña "Segura2026"
    Entonces el acceso es rechazado con el mensaje "Credenciales invalidas"

  Esquema del escenario: Desbloqueo automático al vencer el bloqueo temporal
    Dado que la cuenta "ana@peaceapp.pe" fue bloqueada hace <minutos> minutos
    Cuando el usuario inicia sesión con el correo "ana@peaceapp.pe" y la contraseña "Segura2026"
    Entonces el inicio de sesión resulta "<resultado>"

    Ejemplos:
      | minutos | resultado |
      | 10      | bloqueado |
      | 16      | exitoso   |

  Escenario: Inicio de sesión con una cuenta sin verificar
    Dado que existe una cuenta sin verificar con el correo "luis@peaceapp.pe" y la contraseña "Segura2026"
    Cuando el usuario inicia sesión con el correo "luis@peaceapp.pe" y la contraseña "Segura2026"
    Entonces el acceso es rechazado con el mensaje "Debes verificar tu correo antes de iniciar sesion"
