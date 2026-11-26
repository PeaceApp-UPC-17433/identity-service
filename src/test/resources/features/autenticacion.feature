# language: es
Característica: Registro e inicio de sesión de usuarios
  Como ciudadano
  Quiero registrarme e iniciar sesión en PeaceApp
  Para reportar y consultar incidentes de forma segura

  Escenario: Registro exitoso de un nuevo usuario
    Dado que no existe un usuario con el correo "nuevo@peaceapp.pe"
    Cuando registro un usuario con los siguientes datos
      | email             | password   |
      | nuevo@peaceapp.pe | Clave12345 |
    Entonces el sistema responde con código 201

  Escenario: No se permite registrar un correo duplicado
    Dado que existe un usuario registrado con el correo "duplicado@peaceapp.pe" y contraseña "Clave12345"
    Cuando registro un usuario con los siguientes datos
      | email                 | password   |
      | duplicado@peaceapp.pe | Clave12345 |
    Entonces el sistema responde con código 409

  Escenario: Registro rechazado por datos inválidos
    Cuando registro un usuario con los siguientes datos
      | email           | password |
      | correo-invalido | corta    |
    Entonces el sistema responde con código 400

  Escenario: Inicio de sesión exitoso devuelve un token JWT
    Dado que existe un usuario registrado con el correo "login@peaceapp.pe" y contraseña "Clave12345"
    Cuando inicio sesión con el correo "login@peaceapp.pe" y contraseña "Clave12345"
    Entonces el sistema responde con código 200
    Y la respuesta contiene un token de acceso

  Escenario: Inicio de sesión con contraseña incorrecta
    Dado que existe un usuario registrado con el correo "clave@peaceapp.pe" y contraseña "Clave12345"
    Cuando inicio sesión con el correo "clave@peaceapp.pe" y contraseña "Incorrecta99"
    Entonces el sistema responde con código 401

  Escenario: La cuenta se bloquea tras cinco intentos fallidos
    Dado que existe un usuario registrado con el correo "bloqueo@peaceapp.pe" y contraseña "Clave12345"
    Cuando intento iniciar sesión 5 veces con la contraseña "Incorrecta99" para "bloqueo@peaceapp.pe"
    Y inicio sesión con el correo "bloqueo@peaceapp.pe" y contraseña "Clave12345"
    Entonces el sistema responde con código 403
