package com.rymar.concurency.locking;

import static java.lang.Thread.sleep;

import com.rymar.repository.BaseRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.Future;
import lombok.SneakyThrows;

public class AccessExclusiveLock extends BaseRepository {
  private static final String LOCK_SQL = "LOCK TABLE users IN ACCESS EXCLUSIVE MODE;";
  private static final String SELECT_SQL = "SELECT COUNT(*) FROM users ";
  private static final String INSERT_SQL = "INSERT INTO users (name) VALUES ('Alex');";
  private static final String INIT_SQL =
      """
          DROP TABLE IF EXISTS users;
          CREATE TABLE users (
              id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
              name TEXT NOT NULL
          );
          TRUNCATE TABLE users;
                    """;

  @SneakyThrows
  public static void main(String[] args) {
    setupBaseRepo();
    initSqlTask();

    Connection tx1 = getConnection(Connection.TRANSACTION_READ_COMMITTED);
    Connection tx2 = getConnection(Connection.TRANSACTION_READ_COMMITTED);

    executorService.execute(
        () -> {
          runTx1(tx1);
        });
    executorService.execute(
        () -> {
          runTx2(tx2);
        });
    executorService.shutdown();
  }

  @SneakyThrows
  private static void initSqlTask() {
    Future<?> initTask = executorService.submit(() -> setInitSql(INIT_SQL));
    initTask.get();
  }

  @SneakyThrows
  private static Connection getConnection(int lvl) {
    Connection tx = HIKARI_POOL.getConnection();
    tx.setAutoCommit(false);
    tx.setTransactionIsolation(lvl);
    return tx;
  }

  @SneakyThrows
  private static void runTx1(Connection tx1) {
    System.out.println("TX1: START");
    PreparedStatement s1 = tx1.prepareStatement(LOCK_SQL);
    s1.execute();
    sleep(5000);
    tx1.commit();
    System.out.println("TX1: Commit");
  }

  @SneakyThrows
  private static void runTx2(Connection tx) {
    sleep(1000);
    System.out.println("TX2: START");
    var start = System.nanoTime();
    PreparedStatement statement = tx.prepareStatement(SELECT_SQL);
    ResultSet resultSet = statement.executeQuery();
    printCountRows(resultSet);
    var end = System.nanoTime();
    var duration = end - start;
    System.out.print("Blocking: ");
    System.out.println(duration / Math.pow(10, -9));
    tx.commit();
    System.out.println("TX2: Commit");
  }
}
