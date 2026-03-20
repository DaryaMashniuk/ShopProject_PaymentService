package com.innowise.paymentservice.config;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;


public class LiquibaseRunner implements CommandLineRunner {

  @Value("${spring.liquibase.url}") String mongoUri;
  @Value("${spring.liquibase.change-log}") String changeLogPath;
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
      throw e;
    }

  }

}
