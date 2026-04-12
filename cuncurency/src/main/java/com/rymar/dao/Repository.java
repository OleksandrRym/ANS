package com.rymar.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class Repository extends BaseRepository {

  public static void main(String[] args) throws Exception {

    Connection tx1 = DriverManager.getConnection(URL, USER, PASS);
    Connection tx2 = DriverManager.getConnection(URL, USER, PASS);


    tx1.setAutoCommit(false);
    tx2.setAutoCommit(false);
    getStateDB();
    executorService.execute(
        () -> {
          try {
            System.out.println("\nTX1: UPDATE hits = hits + 1");
            PreparedStatement ps = tx1.prepareStatement("UPDATE website SET hits = hits + 1");
            ps.executeUpdate();
            Thread.sleep(4000); // wait T2.finish
            System.out.println("TX1: COMMIT");
            tx1.commit();
              getStateDB();
          } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
          }
        });

    executorService.execute(
        () -> {
          try {
            System.out.println("TX2: DELETE");
            Thread.sleep(1000); // Wait T1.start
            PreparedStatement ps = tx2.prepareStatement("DELETE FROM website WHERE hits = 14");
            ps.executeUpdate();
            System.out.println("TX2: DELETE END");
            tx2.commit();
          } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
          }
        });
  }
}
