package com.duoc.sumativauno.sumativauno.batch.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Al haber tres Job beans en el contexto, Spring Boot ya no los ejecuta
 * automáticamente al arrancar (exige indicar un único job por nombre).
 * Este runner los lanza explícitamente y en secuencia: primero el reporte
 * de transacciones diarias, luego el cálculo de intereses mensuales y por
 * último la generación de estados de cuenta anuales.
 *
 * Al ser una aplicación batch sin servidor web, no hay nada más que la
 * mantenga viva una vez terminan los tres jobs; sin cerrar el contexto
 * explícitamente el proceso quedaría colgado indefinidamente por los hilos
 * (no daemon) del pool de batchTaskExecutor. Por eso se cierra el contexto
 * y se sale con SpringApplication.exit(), devolviendo código distinto de 0
 * si algún job no terminó en estado COMPLETED.
 */
@Component
public class BatchJobsRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BatchJobsRunner.class);

    private final JobLauncher jobLauncher;
    private final Job reporteTransaccionesDiariasJob;
    private final Job calculoInteresesMensualesJob;
    private final Job generacionEstadosCuentaAnualesJob;
    private final ConfigurableApplicationContext applicationContext;

    public BatchJobsRunner(JobLauncher jobLauncher,
            Job reporteTransaccionesDiariasJob,
            Job calculoInteresesMensualesJob,
            Job generacionEstadosCuentaAnualesJob,
            ConfigurableApplicationContext applicationContext) {
        this.jobLauncher = jobLauncher;
        this.reporteTransaccionesDiariasJob = reporteTransaccionesDiariasJob;
        this.calculoInteresesMensualesJob = calculoInteresesMensualesJob;
        this.generacionEstadosCuentaAnualesJob = generacionEstadosCuentaAnualesJob;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(String... args) throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("ejecutadoEn", System.currentTimeMillis())
                .toJobParameters();

        log.info("Iniciando reporteTransaccionesDiariasJob");
        JobExecution reporte = jobLauncher.run(reporteTransaccionesDiariasJob, jobParameters);

        log.info("Iniciando calculoInteresesMensualesJob");
        JobExecution intereses = jobLauncher.run(calculoInteresesMensualesJob, jobParameters);

        log.info("Iniciando generacionEstadosCuentaAnualesJob");
        JobExecution estados = jobLauncher.run(generacionEstadosCuentaAnualesJob, jobParameters);

        boolean todosCompletados = List.of(reporte, intereses, estados).stream()
                .allMatch(ejecucion -> ejecucion.getStatus() == BatchStatus.COMPLETED);

        int codigoSalida = SpringApplication.exit(applicationContext, () -> todosCompletados ? 0 : 1);
        System.exit(codigoSalida);
    }
}
