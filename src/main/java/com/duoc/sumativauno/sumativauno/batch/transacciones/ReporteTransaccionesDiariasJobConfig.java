package com.duoc.sumativauno.sumativauno.batch.transacciones;

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
import com.duoc.sumativauno.sumativauno.dto.TransaccionCsv;
import com.duoc.sumativauno.sumativauno.model.Transaccion;

/**
 * Job 1: Reporte de Transacciones Diarias.
 * limpiar -> procesar (multi-thread, fault tolerant) -> resumen.
 */
@Configuration
public class ReporteTransaccionesDiariasJobConfig {

    private static final int TAMANO_CHUNK = 50;
    private static final int LIMITE_SKIPS = 50;
    private static final String NOMBRE_JOB = "reporteTransaccionesDiariasJob";

    @Bean
    public Job reporteTransaccionesDiariasJob(JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            DataSource dataSource,
            JdbcTemplate jdbcTemplate,
            AsyncTaskExecutor batchTaskExecutor) {
        return new JobBuilder(NOMBRE_JOB, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(limpiarStep(jobRepository, transactionManager, jdbcTemplate))
                .next(procesarStep(jobRepository, transactionManager, dataSource, batchTaskExecutor))
                .next(resumenStep(jobRepository, transactionManager, jdbcTemplate))
                .build();
    }

    private Step limpiarStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            JdbcTemplate jdbcTemplate) {
        return new StepBuilder("limpiarTransaccionesStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    jdbcTemplate.update("DELETE FROM transacciones");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    private Step procesarStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            DataSource dataSource, AsyncTaskExecutor batchTaskExecutor) {
        return new StepBuilder("procesarTransaccionesStep", jobRepository)
                .<TransaccionCsv, Transaccion>chunk(TAMANO_CHUNK)
                .reader(sincronizarLector(csvReader()))
                .processor(new TransaccionItemProcessor())
                .writer(escritor(dataSource))
                .transactionManager(transactionManager)
                .faultTolerant()
                .skipPolicy(new RegistroInvalidoSkipPolicy(LIMITE_SKIPS))
                .skipListener(new LoggingSkipListener<TransaccionCsv, Transaccion>(NOMBRE_JOB))
                .retryPolicy(new EscrituraTransitoriaRetryPolicy())
                .retryListener(new LoggingRetryListener(NOMBRE_JOB))
                .taskExecutor(batchTaskExecutor)
                .build();
    }

    private Step resumenStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            JdbcTemplate jdbcTemplate) {
        return new StepBuilder("resumenTransaccionesStep", jobRepository)
                .tasklet(new ResumenTransaccionesDiariasTasklet(jdbcTemplate), transactionManager)
                .build();
    }

    private FlatFileItemReader<TransaccionCsv> csvReader() {
        return new FlatFileItemReaderBuilder<TransaccionCsv>()
                .name("transaccionCsvReader")
                .resource(new ClassPathResource("transacciones.csv"))
                .linesToSkip(1)
                .delimited()
                .names("id", "fecha", "monto", "tipo")
                .targetType(TransaccionCsv.class)
                .build();
    }

    private ItemReader<TransaccionCsv> sincronizarLector(FlatFileItemReader<TransaccionCsv> delegate) {
        return new SynchronizedItemStreamReader<>(delegate);
    }

    private ItemWriter<Transaccion> escritor(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Transaccion>()
                .dataSource(dataSource)
                .sql("INSERT INTO transacciones "
                        + "(transaccion_id, fecha, fecha_original, monto, tipo, anomalia, observacion) "
                        + "VALUES (:transaccionId, :fecha, :fechaOriginal, :monto, :tipo, :anomalia, :observacion)")
                .beanMapped()
                .build();
    }
}
