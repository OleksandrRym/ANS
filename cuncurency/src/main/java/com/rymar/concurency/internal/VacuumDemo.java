package com.rymar.concurency.internal;

import com.rymar.repository.BaseRepository;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import lombok.SneakyThrows;

///
/// Демонстрація команди `Vacuum` та як вона очищає бд
///
/// Для демрнстрації Vacuum потрібно розкоментувати метод `showVacuum()`
///
/// Vacuum має оптимізацію яка демонструється в `showDefaultVacuumOptimizations()`
///
/// Tail truncate - назва оптимізації коли вакум обрізає кінець масиву pages
///
public class VacuumDemo extends BaseRepository {

  private static final String SIZE_QUERY =
      "SELECT pg_size_pretty(pg_relation_size('bloat_test')) as size_bytes;";

  @SneakyThrows
  public static void main(String[] args) {
    setupBaseRepo();

    // showDefaultVacuumOptimizations();

    // showVacuum();

    executorService.shutdown();
  }

  @SneakyThrows
  private static void showDefaultVacuumOptimizations() {
    try (Connection conn = HIKARI_POOL.getConnection()) {
      conn.setAutoCommit(true);
      Statement st = conn.createStatement();

      st.execute("DROP TABLE IF EXISTS bloat_test;");
      st.execute("CREATE TABLE bloat_test (id int, data text);");
      System.out.println("--- Етап 1: Нова таблиця ---");
      printSize(st);

      System.out.println("\n--- Етап 2: Вставка 1_000_000 рядків ---");
      st.execute(
          "INSERT INTO bloat_test SELECT i, repeat('text-', 100) FROM generate_series(1, 1000000) s(i);");
      printSize(st);

      System.out.println("\n--- Етап 3: Видалення даних (сміття) ---");
      st.execute("DELETE FROM bloat_test WHERE id > 200000;");
      printSize(st);

      System.out.println("\n--- Етап 4: Звичайний VACUUM ---");
      st.execute("VACUUM bloat_test;");
      printSize(st);

      System.out.println("\n--- Етап 5: VACUUM FULL ---");
      st.execute("VACUUM FULL bloat_test;");
      printSize(st);
    }
  }

  @SneakyThrows
  private static void showVacuum() {
    try (Connection conn = HIKARI_POOL.getConnection()) {
      conn.setAutoCommit(true);
      Statement st = conn.createStatement();

      st.execute("DROP TABLE IF EXISTS bloat_test;");
      st.execute("CREATE TABLE bloat_test (id int, data text);");
      System.out.println("--- Етап 1: Нова таблиця ---");
      printSize(st);

      System.out.println("\n--- Етап 2: Вставка 1_000_000 рядків ---");
      st.execute(
          "INSERT INTO bloat_test SELECT i, repeat('text-', 100) FROM generate_series(1, 1000000) s(i);");
      printSize(st);

      System.out.println("\n--- Етап 3: Видалення даних ---");
      st.execute("DELETE FROM bloat_test WHERE id % 2 = 1 AND id > 200000;");
      printSize(st);

      System.out.println("\n--- Етап 4: VACUUM ---");
      st.execute("VACUUM bloat_test;");
      printSize(st);

      System.out.println("\n--- Етап 5: VACUUM FULL ---");
      st.execute("VACUUM FULL bloat_test;");
      printSize(st);
    }
  }

  @SneakyThrows
  private static void printSize(Statement st) {
    ResultSet rs = st.executeQuery(SIZE_QUERY);
    if (rs.next()) {
      System.out.println("Поточний розмір таблиці на диску: " + rs.getString("size_bytes"));
    }
  }
}
