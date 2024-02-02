// SPDX-FileCopyrightText: 2006 Benjamin Livshits livshits@cs.stanford.edu
// SPDX-License-Identifier: Apache-2.0

// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks

/*
   @author Benjamin Livshits <livshits@cs.stanford.edu>

   $Id: Basic18.java,v 1.4 2006/04/04 20:00:40 livshits Exp $
*/
package securibench.micro.basic;

import java.io.IOException;
import java.io.PrintWriter;
import mockx.servlet.http.HttpServletRequest;
import mockx.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/**
 * @servlet description="protect agains simple loop unrolling"
 * @servlet vuln_count = "1"
 */
public class Basic18 extends BasicTestCase implements MicroTestCase {
  private static final String FIELD_NAME = "name";

  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String s = req.getParameter(FIELD_NAME);

    for (int i = 0; i < 100; i++) {
      PrintWriter writer = resp.getWriter();
      if (i > 5 && (i % 17 == 0)) {
        writer.println(s); /* BAD */
      }
    }
  }

  public String getDescription() {
    return "protect agains simple loop unrolling";
  }

  public int getVulnerabilityCount() {
    return 1;
  }
}
