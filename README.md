# El siguiente proyecto corresponde a la primera actividad sumativa del curso Desarrollo Backend III donde se utiliza Spring Batch para el procesamiento masivo de datos.

# ⚙️ Tecnologías utilizadas en el Proyecto
- Java 21
- Spring Boot 4.1.1
- Maven
- MySQL 

# Arquitectura del Job
El fujo implementado sigue el patrón clásico de Spring Batch:
(1) ItemReader :    Lectura de datos desde los archivos CSV
(2) ItemProcessor : Transformación y validación de todos los registros. En la actividad se nos entregaron tres archivos: cuentas_anuales.csv, intereses.csv y transacciones.csv
(3) ItemWriter :    Inserta los datos procesados previamente en la base de datos respectiva.

# Estructura del Proyecto

