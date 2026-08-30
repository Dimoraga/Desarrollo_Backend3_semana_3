# El siguiente proyecto corresponde a la primera actividad sumativa del curso Desarrollo Backend III donde se utiliza Spring Batch para el procesamiento masivo de datos.

# ⚙️ Tecnologías utilizadas en el Proyecto
- Java 21
- Spring Boot 4.1.1
- Maven
- MySQL 

# Arquitectura del Job
El flujo implementado sigue el patrón clásico de Spring Batch:
(1) ItemReader :    Lectura de datos desde los archivos CSV
(2) ItemProcessor : Transformación y validación de todos los registros. En la actividad se nos entregaron tres archivos: cuentas_anuales.csv, intereses.csv y transacciones.csv
(3) ItemWriter :    Inserta los datos procesados previamente en la base de datos respectiva.

Se implementaron tres jobs, cada uno con la secuencia limpiar -> procesar -> resumen/compilar:

1. **reporteTransaccionesDiariasJob** (`transacciones.csv`): valida fecha/monto/tipo de cada transacción, marca anomalías (tipo no reconocido, monto negativo/ausente, fecha inválida) y genera un resumen agregado en `resumen_transacciones_diarias`.
2. **calculoInteresesMensualesJob** (`intereses.csv`): aplica una tasa mensual según el tipo de cuenta (ahorro 0.4%, con incentivo +0.1% para adultos mayores; préstamo 1.8%; hipoteca 1.1%) y persiste el saldo final en `cuentas_intereses`.
3. **generacionEstadosCuentaAnualesJob** (`cuentas_anuales.csv`): normaliza cada movimiento (incluye variantes con tilde como "depósito"), lo guarda en `movimientos_cuenta_anual` y compila un estado de cuenta por cuenta en `estados_cuenta_anual` para auditoría.

Los tres jobs corren en secuencia al levantar la aplicación (`BatchJobsRunner`), y el proceso finaliza solo (exit code 0 si los tres terminan `COMPLETED`, 1 en caso contrario).

## Manejo de errores y tolerancia a fallos
- Cada `ItemProcessor` corrige lo que se puede (fechas en varios formatos, montos/saldos ausentes) y marca como `anomalia=true` lo que no se puede corregir, en vez de descartar el registro silenciosamente.
- `RegistroInvalidoSkipPolicy` (skip policy propia): omite líneas de CSV mal formadas hasta un límite (50), sin afectar a otras excepciones.
- `EscrituraTransitoriaRetryPolicy` (retry policy propia, `org.springframework.core.retry.RetryPolicy`): reintenta hasta 3 veces solo fallos transitorios de acceso a datos (timeouts, deadlocks) con backoff fijo de 500ms.
- `LoggingSkipListener` / `LoggingRetryListener`: dejan constancia en el log de cada omisión o reintento, para que sean auditables.

## Escalamiento
Se optó por **steps multi-thread** (pool `ThreadPoolTaskExecutor`: 4 hilos core, 8 máximo, cola de 100) en lugar de particionamiento: cada CSV es un único archivo de ~1000 filas, por lo que particionar agregaría complejidad (repartir el archivo, agregar resultados por partición) sin ganancia real de throughput. El lector se envuelve en `SynchronizedItemStreamReader` para que sea seguro de usar entre hilos.

## Cómo ejecutar
1. Tener MySQL corriendo en local y ajustar `src/main/resources/application-local.properties` con tus credenciales (la URL crea la base `springbatch_db` si no existe).
2. `./mvnw spring-boot:run` (o ejecutar `SumativaunoApplication` desde el IDE).
3. Los datos quedan en `transacciones`, `cuentas_intereses`, `movimientos_cuenta_anual`, `estados_cuenta_anual` y `resumen_transacciones_diarias`; el historial de ejecuciones de Spring Batch queda en las tablas `BATCH_*` (creadas por `schema-batch-mysql.sql`, ya que Spring Boot 4.1.1 no trae más la inicialización automática de ese esquema).

# Estructura del Proyecto

