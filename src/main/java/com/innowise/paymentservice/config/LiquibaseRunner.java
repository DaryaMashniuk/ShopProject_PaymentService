package com.innowise.paymentservice.config;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "spring.liquibase.enabled", havingValue = "true", matchIfMissing = false)
public class LiquibaseRunner implements CommandLineRunner {

  @Value("${liquibase.mongodb.url}") String mongoUri;
  @Value("${spring.liquibase.change-log}") String changeLogPath;
  private static final Logger logger = LogManager.getLogger(LiquibaseRunner.class);
  @Override
  public void run(String... args) throws Exception {
    try (Database database = DatabaseFactory.getInstance()
                .openDatabase(mongoUri, null, null, null, new ClassLoaderResourceAccessor())) {
      if (database != null) {
        database.setDefaultSchemaName("payments");
        try (Liquibase liquibase = new Liquibase(changeLogPath, new ClassLoaderResourceAccessor(), database)) {
          liquibase.update("");
        }
      }
    } catch (Exception e) {
      logger.warn("There was an error while updating the liquibase database", e);
      throw e;
    }

  }

}
