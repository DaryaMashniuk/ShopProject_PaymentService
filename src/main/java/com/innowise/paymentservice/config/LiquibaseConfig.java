package com.innowise.paymentservice.config;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LiquibaseConfig {

  @Value("${spring.liquibase.url}")
  private String mongoUri;

  @Value("${spring.liquibase.change-log}")
  private String changeLogPath;

  @Bean
  public InitializingBean liquibaseRunner() {
    return () -> {
      try {
        Database database = DatabaseFactory.getInstance()
                .openDatabase(mongoUri, null, null, null, new ClassLoaderResourceAccessor());

        if (database != null) {
          try (Liquibase liquibase = new Liquibase(changeLogPath, new ClassLoaderResourceAccessor(), database)) {
            liquibase.update("");
          }
        }
      } catch (Exception e) {
        throw e;
      }
    };
  }
}