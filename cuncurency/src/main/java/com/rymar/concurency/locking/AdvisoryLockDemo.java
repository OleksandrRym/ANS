package com.rymar.concurency.locking;

import com.rymar.repository.BaseRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static java.lang.Thread.sleep;

public class AdvisoryLockDemo extends BaseRepository {

    private static final String TRY_LOCK =
            "SELECT pg_try_advisory_lock(12345);";

    private static final String UNLOCK =
            "SELECT pg_advisory_unlock(12345);";

    private static final String INIT_SQL =
            """
                DROP TABLE IF EXISTS daily_reports;
                CREATE TABLE daily_reports (
                    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                    report_date DATE NOT NULL
                );
            """;

    public static void main(String[] args) {

        try {
            setupBaseRepo();
        initSqlTask();

        Connection tx1 = getConnection(Connection.TRANSACTION_READ_COMMITTED);

        Connection tx2 = getConnection(Connection.TRANSACTION_READ_COMMITTED);

        executorService.execute(() -> runJob("INSTANCE-1", tx1));
        executorService.execute(() -> runJob("INSTANCE-2", tx2));

        executorService.shutdown();
        } catch (SQLException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void initSqlTask() throws ExecutionException, InterruptedException {
        Future<?> initTask = executorService.submit(() -> setInitSql(INIT_SQL));
        initTask.get();
    }

    private static Connection getConnection(int lvl) throws SQLException {
        Connection tx = HIKARI_POOL.getConnection();
        tx.setAutoCommit(false);
        tx.setTransactionIsolation(lvl);
        return tx;
    }

    private static void runJob(String instanceName, Connection tx) {
        System.out.println(instanceName + ": START JOB");

        boolean locked = tryAcquireLock(tx);

        if (!locked) {
            System.out.println(instanceName + ": SKIP (another instance is running)");
            return;
        }

        try {

            sleep(10_000);

            System.out.println(instanceName + ": LOCK ACQUIRED");

            // simulate job
            sleep(15_000);

            tx.prepareStatement(
                    "INSERT INTO daily_reports(report_date) VALUES (CURRENT_DATE)"
            ).execute();

            tx.commit();

            System.out.println(instanceName + ": JOB DONE");

        } catch (Exception e) {
            System.out.println(instanceName + ": ERROR " + e.getMessage());
            rollbackQuiet(tx);
        } finally {
            releaseLock(tx);
        }
    }

    private static boolean tryAcquireLock(Connection tx) {
        try {
            PreparedStatement stmt = tx.prepareStatement(TRY_LOCK);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getBoolean(1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void releaseLock(Connection tx) {
        try {
            tx.prepareStatement(UNLOCK).execute();
            System.out.println("LOCK RELEASED");
        } catch (Exception ignored) {}
    }

    private static void rollbackQuiet(Connection tx) {
        try {
            tx.rollback();
        } catch (Exception ignored) {}
    }
}