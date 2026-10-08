# language: es
@EP-01 @US-03 @identity-service
Característica: Gestión de perfil
  Como usuario registrado
  Quiero editar mis datos de perfil y mis zonas de interés
  Para recibir información relevante a mis desplazamientos

  Antecedentes:
    Dado que el usuario "ana@peaceapp.pe" ha iniciado sesión en PeaceApp

  @smoke
  Escenario: Agregar una zona de interés
    Cuando agrega la zona de interés "Casa" en latitud -12.1211, longitud -77.0297 con radio de 500 metros
    Entonces su perfil muestra la zona de interés "Casa"
    Y se notifica a los demás servicios la actualización de sus zonas de interés

  Esquema del escenario: Validación del radio de la zona de interés
    Cuando agrega la zona de interés "Trabajo" en latitud -12.0464, longitud -77.0428 con radio de <radio> metros
    Entonces el registro de la zona resulta "<resultado>"

    Ejemplos:
      | radio | resultado |
      | 100   | aceptado  |
      | 5000  | aceptado  |
      | 50    | rechazado |
      | 6000  | rechazado |

  Escenario: Límite máximo de zonas de interés
    Dado que el usuario ya tiene 10 zonas de interés registradas
    Cuando agrega la zona de interés "Gimnasio" en latitud -12.0897, longitud -77.0500 con radio de 300 metros
    Entonces la zona es rechazada con el mensaje "Se alcanzo el maximo de 10 zonas de interes"

  Escenario: Eliminar una zona de interés
    Dado que el usuario tiene registradas las siguientes zonas de interés:
      | nombre      | latitud  | longitud | radio |
      | Casa        | -12.1211 | -77.0297 | 500   |
      | Universidad | -12.1040 | -76.9630 | 800   |
    Cuando elimina la zona de interés "Universidad"
    Entonces su perfil muestra únicamente la zona de interés "Casa"
    Y se notifica a los demás servicios la actualización de sus zonas de interés

  Escenario: Actualizar el nombre visible del perfil
    Cuando actualiza su nombre visible a "Ana Torres"
    Entonces su perfil muestra el nombre visible "Ana Torres"
