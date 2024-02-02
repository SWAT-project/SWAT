// SPDX-FileCopyrightText: 2006 Benjamin Livshits livshits@cs.stanford.edu
// SPDX-License-Identifier: Apache-2.0

// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks

/*
    @author Benjamin Livshits <livshits@cs.stanford.edu>

    $Id: Pred1.java,v 1.5 2006/04/21 17:14:26 livshits Exp $
*/
package securibench.micro.pred;

import java.io.IOException;
import java.io.PrintWriter;
import mockx.servlet.http.HttpServletRequest;
import mockx.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/**
 * @servlet description="simple if(false) test"
 * @servlet vuln_count = "0"
 */
public class Pred1 extends BasicTestCase implements MicroTestCase {
  private static final String FIELD_NAME = "name";

  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String name = req.getParameter(FIELD_NAME);
    if (false) {
      PrintWriter writer = resp.getWriter();
      writer.println(name); /* OK */
    }
  }

  public String getDescription() {
    return "simple if(false) test";
  }

  public int getVulnerabilityCount() {
    return 0;
  }
}
