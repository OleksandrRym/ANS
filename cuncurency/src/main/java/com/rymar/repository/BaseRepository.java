package com.rymar.repository;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BaseRepository {

  protected static final ExecutorService executorService = Executors.newFixedThreadPool(5);
  protected static final String URL = "jdbc:postgresql://localhost:5432/postgres";
  protected static final String USER = "postgres";
  protected static final String PASS = "postgres";

  protected static HikariDataSource HIKARI_POOL = null;

  protected static void setupBaseRepo() {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(URL);
    config.setUsername(USER);
    config.setPassword(PASS);
    config.setMaximumPoolSize(5);
    HIKARI_POOL = new HikariDataSource(config);
  }

  public static void setInitSql(String initSql) {
    try (Connection tx1 = HIKARI_POOL.getConnection()) {
      PreparedStatement ps = tx1.prepareStatement(initSql);
      ps.execute();
    } catch (SQLException e) {
        throw new RuntimeException(e);
    }
  }
}
