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

## Estructura del Proyecto

```
sumativauno/
├── src/
│   ├── main/
│   │   ├── java/com/duoc/sumativauno/sumativauno/
│   │   │   ├── SumativaunoApplication.java        # Punto de entrada de la aplicación Spring Boot
│   │   │   │
│   │   │   ├── batch/
│   │   │   │   ├── config/
│   │   │   │   │   ├── BatchConcurrencyConfig.java    # Configuración del ThreadPoolTaskExecutor (steps multi-thread)
│   │   │   │   │   ├── BatchJobsRunner.java           # Ejecuta los 3 jobs en secuencia y define el exit code
│   │   │   │   │   └── JobRepositoryConfig.java       # Configuración del JobRepository de Spring Batch
│   │   │   │   │
│   │   │   │   ├── transacciones/                     # Job: reporteTransaccionesDiariasJob
│   │   │   │   │   ├── ReporteTransaccionesDiariasJobConfig.java
│   │   │   │   │   ├── TransaccionItemProcessor.java
│   │   │   │   │   └── ResumenTransaccionesDiariasTasklet.java
│   │   │   │   │
│   │   │   │   ├── intereses/                         # Job: calculoInteresesMensualesJob
│   │   │   │   │   ├── CalculoInteresesMensualesJobConfig.java
│   │   │   │   │   └── CuentaInteresItemProcessor.java
│   │   │   │   │
│   │   │   │   ├── cuentaanual/                       # Job: generacionEstadosCuentaAnualesJob
│   │   │   │   │   ├── GeneracionEstadosCuentaAnualesJobConfig.java
│   │   │   │   │   ├── MovimientoCuentaAnualItemProcessor.java
│   │   │   │   │   └── EstadoCuentaAnualTasklet.java
│   │   │   │   │
│   │   │   │   ├── listener/
│   │   │   │   │   ├── LoggingSkipListener.java       # Loguea cada registro omitido (skip)
│   │   │   │   │   └── LoggingRetryListener.java      # Loguea cada reintento (retry)
│   │   │   │   │
│   │   │   │   ├── policy/
│   │   │   │   │   ├── RegistroInvalidoSkipPolicy.java     # Política propia de skip para líneas mal formadas
│   │   │   │   │   └── EscrituraTransitoriaRetryPolicy.java # Política propia de retry para fallos transitorios
│   │   │   │   │
│   │   │   │   └── util/
│   │   │   │       ├── FechaUtils.java                # Normalización de fechas en múltiples formatos
│   │   │   │       └── NumeroUtils.java                # Normalización/parseo de montos y saldos
│   │   │   │
│   │   │   ├── dto/                                    # DTOs de lectura de los archivos CSV
│   │   │   │   ├── TransaccionCsv.java
│   │   │   │   ├── InteresCsv.java
│   │   │   │   └── CuentaAnualCsv.java
│   │   │   │
│   │   │   └── model/                                  # Entidades JPA persistidas en MySQL
│   │   │       ├── Transaccion.java
│   │   │       ├── ResumenTransaccionesDiarias.java
│   │   │       ├── CuentaInteres.java
│   │   │       ├── MovimientoCuentaAnual.java
│   │   │       └── CuentaAnual.java
│   │   │
│   │   └── resources/
│   │       ├── application.properties          # Configuración general de la aplicación
│   │       ├── application-local.properties     # Configuración del perfil local (BD, credenciales, etc.)
│   │       ├── schema-batch-mysql.sql           # Esquema de las tablas de metadatos de Spring Batch
│   │       ├── transacciones.csv                # Datos de entrada para reporteTransaccionesDiariasJob
│   │       ├── intereses.csv                    # Datos de entrada para calculoInteresesMensualesJob
│   │       └── cuentas_anuales.csv              # Datos de entrada para generacionEstadosCuentaAnualesJob
│   │
│   └── test/
│       └── java/com/duoc/sumativauno/sumativauno/
│           └── SumativaunoApplicationTests.java
│
├── pom.xml           # Definición de dependencias y build de Maven
├── mvnw / mvnw.cmd   # Maven Wrapper
└── README.md
```

## Cómo ejecutar el proyecto

### Requisitos previos
- Java 21 y una instancia de MySQL corriendo en `localhost:3306`.
- Credenciales del usuario MySQL configuradas en [`application-local.properties`](src/main/resources/application-local.properties) (por defecto usuario `root`; ajusta `spring.datasource.password` a la de tu instalación).
- No es necesario crear la base de datos a mano: la URL de conexión incluye `createDatabaseIfNotExist=true`.

### Levantar la aplicación
Desde la raíz del proyecto, usando el Maven Wrapper (no requiere tener Maven instalado):

```
./mvnw spring-boot:run       # Linux/macOS
mvnw.cmd spring-boot:run     # Windows
```

Alternativamente, para generar y ejecutar el jar:

```
./mvnw clean package
java -jar target/sumativauno-0.0.1-SNAPSHOT.jar
```

### Qué debería ocurrir
1. Al arrancar, Spring Boot activa el perfil `local` y se conecta a MySQL, creando la base de datos `springbatch_db` si no existe.
2. Se ejecuta `schema-batch-mysql.sql` para crear las tablas de metadatos de Spring Batch (es seguro reejecutarlo, usa `IF NOT EXISTS`), y Hibernate crea/actualiza las tablas de negocio (`ddl-auto=update`).
3. `BatchJobsRunner` lanza los tres jobs en secuencia, y en el log (nivel `INFO`) se ven mensajes como:
   - `Iniciando reporteTransaccionesDiariasJob`
   - `Iniciando calculoInteresesMensualesJob`
   - `Iniciando generacionEstadosCuentaAnualesJob`
   
   junto con las líneas de `LoggingSkipListener`/`LoggingRetryListener` cada vez que se omite un registro inválido o se reintenta una escritura.
4. Cada job lee su CSV correspondiente desde `src/main/resources`, procesa/valida los registros y persiste resultados en MySQL: `resumen_transacciones_diarias`, `cuentas_intereses` y `movimientos_cuenta_anual` / `estados_cuenta_anual`.
5. Al terminar los tres jobs, la aplicación **se cierra sola** (no queda un servidor esperando peticiones): el proceso termina con código de salida `0` si los tres jobs finalizaron en estado `COMPLETED`, o `1` si alguno falló.
