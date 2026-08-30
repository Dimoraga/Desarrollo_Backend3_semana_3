package com.duoc.sumativauno.sumativauno.batch.cuentaanual;

import javax.sql.DataSource;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import com.duoc.sumativauno.sumativauno.batch.listener.LoggingRetryListener;
import com.duoc.sumativauno.sumativauno.batch.listener.LoggingSkipListener;
import com.duoc.sumativauno.sumativauno.batch.policy.EscrituraTransitoriaRetryPolicy;
import com.duoc.sumativauno.sumativauno.batch.policy.RegistroInvalidoSkipPolicy;
import com.duoc.sumativauno.sumativauno.dto.CuentaAnualCsv;
import com.duoc.sumativauno.sumativauno.model.MovimientoCuentaAnual;

/**
 * Job 3: Generación de Estados de Cuenta Anuales.
 * limpiar -> procesar movimientos (multi-thread, fault tolerant) -> compilar estado por cuenta.
 */
@Configuration
public class GeneracionEstadosCuentaAnualesJobConfig {

    private static final int TAMANO_CHUNK = 50;
    private static final int LIMITE_SKIPS = 50;
    private static final String NOMBRE_JOB = "generacionEstadosCuentaAnualesJob";

    @Bean
    public Job generacionEstadosCuentaAnualesJob(JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            DataSource dataSource,
            JdbcTemplate jdbcTemplate,
            AsyncTaskExecutor batchTaskExecutor) {
        return new JobBuilder(NOMBRE_JOB, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(limpiarStep(jobRepository, transactionManager, jdbcTemplate))
                .next(procesarStep(jobRepository, transactionManager, dataSource, batchTaskExecutor))
                .next(compilarEstadosStep(jobRepository, transactionManager, jdbcTemplate))
                .build();
    }

    private Step limpiarStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            JdbcTemplate jdbcTemplate) {
        return new StepBuilder("limpiarMovimientosCuentaAnualStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    jdbcTemplate.update("DELETE FROM movimientos_cuenta_anual");
                    jdbcTemplate.update("DELETE FROM estados_cuenta_anual");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    private Step procesarStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            DataSource dataSource, AsyncTaskExecutor batchTaskExecutor) {
        return new StepBuilder("procesarMovimientosCuentaAnualStep", jobRepository)
                .<CuentaAnualCsv, MovimientoCuentaAnual>chunk(TAMANO_CHUNK)
                .reader(sincronizarLector(csvReader()))
                .processor(new MovimientoCuentaAnualItemProcessor())
                .writer(escritor(dataSource))
                .transactionManager(transactionManager)
                .faultTolerant()
                .skipPolicy(new RegistroInvalidoSkipPolicy(LIMITE_SKIPS))
                .skipListener(new LoggingSkipListener<CuentaAnualCsv, MovimientoCuentaAnual>(NOMBRE_JOB))
                .retryPolicy(new EscrituraTransitoriaRetryPolicy())
                .retryListener(new LoggingRetryListener(NOMBRE_JOB))
                .taskExecutor(batchTaskExecutor)
                .build();
    }

    private Step compilarEstadosStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            JdbcTemplate jdbcTemplate) {
        return new StepBuilder("compilarEstadosCuentaAnualStep", jobRepository)
                .tasklet(new EstadoCuentaAnualTasklet(jdbcTemplate), transactionManager)
                .build();
    }

    private FlatFileItemReader<CuentaAnualCsv> csvReader() {
        return new FlatFileItemReaderBuilder<CuentaAnualCsv>()
                .name("cuentaAnualCsvReader")
                .resource(new ClassPathResource("cuentas_anuales.csv"))
                .linesToSkip(1)
                .delimited()
                .names("cuentaId", "fecha", "transaccion", "monto", "descripcion")
                .targetType(CuentaAnualCsv.class)
                .build();
    }

    private ItemReader<CuentaAnualCsv> sincronizarLector(FlatFileItemReader<CuentaAnualCsv> delegate) {
        return new SynchronizedItemStreamReader<>(delegate);
    }

    private ItemWriter<MovimientoCuentaAnual> escritor(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<MovimientoCuentaAnual>()
                .dataSource(dataSource)
                .sql("INSERT INTO movimientos_cuenta_anual "
                        + "(cuenta_id, fecha, fecha_original, tipo_movimiento, monto, descripcion, anomalia, observacion) "
                        + "VALUES (:cuentaId, :fecha, :fechaOriginal, :tipoMovimiento, :monto, :descripcion, :anomalia, :observacion)")
                .beanMapped()
                .build();
    }
}
