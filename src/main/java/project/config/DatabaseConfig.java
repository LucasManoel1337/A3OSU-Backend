package project.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import project.properties.DatabaseConfigProperties;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
public class DatabaseConfig {

    @Primary
    @Bean("datasource")
    public DataSource dataSource(DatabaseConfigProperties dbConfig) {
        final HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(dbConfig.getJdbcUrl());
        ds.setUsername(dbConfig.getUsername());
        ds.setPassword(dbConfig.getPassword());
        ds.setDriverClassName(dbConfig.getDriverClassName());
        return ds;
    }

    @Bean("transactionManager")
    @Primary
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
