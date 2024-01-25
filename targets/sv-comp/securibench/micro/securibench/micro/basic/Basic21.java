// SPDX-FileCopyrightText: 2006 Benjamin Livshits livshits@cs.stanford.edu
// SPDX-License-Identifier: Apache-2.0

// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks

/*
   @author Benjamin Livshits <livshits@cs.stanford.edu>

   $Id: Basic21.java,v 1.7 2006/04/04 20:00:40 livshits Exp $
*/
package securibench.micro.basic;

import java.io.IOException;
import java.util.Locale;
import mock.sql.Connection;
import mock.sql.DriverManager;
import mock.sql.SQLException;
import mock.sql.Statement;
import mockx.servlet.http.HttpServletRequest;
import mockx.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/**
 * @servlet description="SQL injection with less commonly used methods"
 * @servlet vuln_count = "4"
 */
public class Basic21 extends BasicTestCase implements MicroTestCase {
  private static final String FIELD_NAME = "name";

  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String s = req.getParameter(FIELD_NAME);
    String name = s.toLowerCase(Locale.UK);

    Connection con = null;
    try {
      con = DriverManager.getConnection(MicroTestCase.CONNECTION_STRING);
      Statement stmt = con.createStatement();
      stmt.executeUpdate("select * from Users where name=" + name); /* BAD */
      stmt.executeUpdate("select * from Users where name=" + name, 0); /* BAD */
      stmt.executeQuery("select * from Users where name=" + name); /* BAD */
    } catch (SQLException e) {
      System.err.println("An error occurred");
    } finally {
      try {
        if (con != null) con.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
  }

  public String getDescription() {
    return "SQL injection with less commonly used methods";
  }

  public int getVulnerabilityCount() {
    return 4;
  }
}
