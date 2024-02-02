// SPDX-FileCopyrightText: 2006 Benjamin Livshits livshits@cs.stanford.edu
// SPDX-License-Identifier: Apache-2.0

// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks

/*
   @author Benjamin Livshits <livshits@cs.stanford.edu>

   $Id: Inter6.java,v 1.3 2006/04/04 20:00:40 livshits Exp $
*/
package securibench.micro.inter;

import java.io.IOException;
import java.io.PrintWriter;
import mockx.servlet.http.HttpServletRequest;
import mockx.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/**
 * @servlet description = "bug in class initializer"
 * @servlet vuln_count = "1"
 */
public class Inter6 extends BasicTestCase implements MicroTestCase {
  private static final String FIELD_NAME = "name";
  private static String name;
  private static PrintWriter writer;

  static class ReflectivelyCreated {
    static {
      writer.println(name); /* BAD */
    }
  }

  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    name = req.getParameter(FIELD_NAME);
    writer = resp.getWriter();

    new ReflectivelyCreated();
  }

  public String getDescription() {
    return "bug in class initializer";
  }

  public int getVulnerabilityCount() {
    return 1;
  }
}
