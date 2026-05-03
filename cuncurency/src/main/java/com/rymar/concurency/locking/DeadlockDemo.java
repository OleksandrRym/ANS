package com.rymar.concurency.locking;

import static java.lang.Thread.sleep;

import com.rymar.repository.BaseRepository;
import java.sql.Connection;
import java.util.concurrent.Future;
import lombok.SneakyThrows;

public class DeadlockDemo extends BaseRepository {
  private static final String LOCK_ROW_1 = "SELECT * FROM users WHERE id = 1 FOR UPDATE;";
  private static final String LOCK_ROW_2 = "SELECT * FROM users WHERE id = 2 FOR UPDATE;";
  private static final String INIT_SQL =
      """
          DROP TABLE IF EXISTS users;
          CREATE TABLE users (
              id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
              name TEXT NOT NULL
          );
          TRUNCATE TABLE users;
          INSERT INTO users (name) VALUES ('A');
          INSERT INTO users (name) VALUES ('B');
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
    private static void runTx1(Connection tx1) {
        try {
            System.out.println("TX1: START");
            tx1.prepareStatement(LOCK_ROW_1).execute();
            System.out.println("TX1: locked 1");
            sleep(1000);
            System.out.println("TX1: try lock 2");
            tx1.prepareStatement(LOCK_ROW_2).execute();
            System.out.println("TX1: locked 2");
            tx1.commit();
        } catch (Exception e) {
            System.out.println("TX1 ERROR: " + e.getMessage());
            rollbackQuiet(tx1);
        }
    }

    private static void runTx2(Connection tx2) {
        try {
            sleep(200);
            System.out.println("TX2: START");
            tx2.prepareStatement(LOCK_ROW_2).execute();
            System.out.println("TX2: locked 2");
            sleep(1000);
            System.out.println("TX2: try lock 1");
            tx2.prepareStatement(LOCK_ROW_1).execute();
            System.out.println("TX2: locked 1");
            tx2.commit();
        } catch (Exception e) {
            System.out.println("TX2 ERROR: " + e.getMessage());
            rollbackQuiet(tx2);
        }
    }

    private static void rollbackQuiet(Connection tx) {
        try {
            tx.rollback();
        } catch (Exception ignored) {}
    }
}
