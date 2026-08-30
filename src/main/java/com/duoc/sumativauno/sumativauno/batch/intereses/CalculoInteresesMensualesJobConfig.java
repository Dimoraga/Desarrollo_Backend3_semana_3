package com.duoc.sumativauno.sumativauno.batch.intereses;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.duoc.sumativauno.sumativauno.dto.InteresCsv;
import com.duoc.sumativauno.sumativauno.model.CuentaInteres;

/**
 * Job 2: Cálculo de Intereses Mensuales.
 * limpiar -> procesar (multi-thread, fault tolerant, calcula saldoFinal) -> resumen en log.
 */
@Configuration
public class CalculoInteresesMensualesJobConfig {

    private static final Logger log = LoggerFactory.getLogger(CalculoInteresesMensualesJobConfig.class);

    private static final int TAMANO_CHUNK = 50;
    private static final int LIMITE_SKIPS = 50;
    private static final String NOMBRE_JOB = "calculoInteresesMensualesJob";

    @Bean
    public Job calculoInteresesMensualesJob(JobRepository jobRepository,
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
        return new StepBuilder("limpiarCuentasInteresesStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    jdbcTemplate.update("DELETE FROM cuentas_intereses");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    private Step procesarStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            DataSource dataSource, AsyncTaskExecutor batchTaskExecutor) {
        return new StepBuilder("procesarInteresesStep", jobRepository)
                .<InteresCsv, CuentaInteres>chunk(TAMANO_CHUNK)
                .reader(sincronizarLector(csvReader()))
                .processor(new CuentaInteresItemProcessor())
                .writer(escritor(dataSource))
                .transactionManager(transactionManager)
                .faultTolerant()
                .skipPolicy(new RegistroInvalidoSkipPolicy(LIMITE_SKIPS))
                .skipListener(new LoggingSkipListener<InteresCsv, CuentaInteres>(NOMBRE_JOB))
                .retryPolicy(new EscrituraTransitoriaRetryPolicy())
                .retryListener(new LoggingRetryListener(NOMBRE_JOB))
                .taskExecutor(batchTaskExecutor)
                .build();
    }

    private Step resumenStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            JdbcTemplate jdbcTemplate) {
        return new StepBuilder("resumenInteresesStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cuentas_intereses", Long.class);
                    Long anomalias = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM cuentas_intereses WHERE anomalia = true", Long.class);
                    log.info("Intereses mensuales aplicados -> cuentas procesadas: {}, anomalías: {}", total, anomalias);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    private FlatFileItemReader<InteresCsv> csvReader() {
        return new FlatFileItemReaderBuilder<InteresCsv>()
                .name("interesCsvReader")
                .resource(new ClassPathResource("intereses.csv"))
                .linesToSkip(1)
                .delimited()
                .names("cuentaId", "nombre", "saldo", "edad", "tipo")
                .targetType(InteresCsv.class)
                .build();
    }

    private ItemReader<InteresCsv> sincronizarLector(FlatFileItemReader<InteresCsv> delegate) {
        return new SynchronizedItemStreamReader<>(delegate);
    }

    private ItemWriter<CuentaInteres> escritor(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<CuentaInteres>()
                .dataSource(dataSource)
                .sql("INSERT INTO cuentas_intereses "
                        + "(cuenta_id, nombre, saldo, edad, tipo, tasa_aplicada, saldo_final, anomalia, observacion) "
                        + "VALUES (:cuentaId, :nombre, :saldo, :edad, :tipo, :tasaAplicada, :saldoFinal, :anomalia, :observacion)")
                .beanMapped()
                .build();
    }
}
