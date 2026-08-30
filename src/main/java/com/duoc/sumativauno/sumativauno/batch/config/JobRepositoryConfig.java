package com.duoc.sumativauno.sumativauno.batch.config;

import org.springframework.batch.core.configuration.support.JdbcDefaultBatchConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * Por defecto, Spring Boot arma un JobRepository en memoria
 * (ResourcelessJobRepository), que no deja rastro de las corridas si la
 * aplicación se reinicia. Como el enunciado pide persistir todo en MySQL,
 * se reemplaza extendiendo JdbcDefaultBatchConfiguration: al detectar un
 * bean de tipo DefaultBatchConfiguration ya definido, la autoconfiguración
 * de Spring Boot (que registra una variante en memoria) se retira y se usa
 * esta, que arma el JobRepository sobre el DataSource/PlatformTransactionManager
 * ya configurados. Las tablas BATCH_* las crea schema-batch-mysql.sql
 * (ver spring.sql.init.* en application.properties).
 */
@Configuration
public class JobRepositoryConfig extends JdbcDefaultBatchConfiguration {
}
