// SPDX-FileCopyrightText: 2006 Benjamin Livshits livshits@cs.stanford.edu
// SPDX-License-Identifier: Apache-2.0

// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks

/**
 * @author Benjamin Livshits <livshits@cs.stanford.edu>
 *     <p>$Id: Pred4.java,v 1.4 2006/04/04 20:00:40 livshits Exp $
 */
package securibench.micro.pred;

import java.io.IOException;
import java.io.PrintWriter;
import mockx.servlet.http.HttpServletRequest;
import mockx.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/**
 * @servlet description="correlated test with an int variable"
 * @servlet vuln_count = "1"
 */
public class Pred4 extends BasicTestCase implements MicroTestCase {
  private static final String FIELD_NAME = "name";

  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    int x = 3;

    String name = req.getParameter(FIELD_NAME);

    if (x == 3) { // should always be taken
      PrintWriter writer = resp.getWriter();
      writer.println(name); /* BAD */
    }
  }

  public String getDescription() {
    return "correlated test with an int variable";
  }

  public int getVulnerabilityCount() {
    return 1;
  }
}
