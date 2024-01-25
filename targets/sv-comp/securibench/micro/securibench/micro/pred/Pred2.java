// SPDX-FileCopyrightText: 2006 Benjamin Livshits livshits@cs.stanford.edu
// SPDX-License-Identifier: Apache-2.0

// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks

/*
   @author Benjamin Livshits <livshits@cs.stanford.edu>

   $Id: Pred2.java,v 1.4 2006/04/04 20:00:40 livshits Exp $
*/
package securibench.micro.pred;

import java.io.IOException;
import java.io.PrintWriter;
import mockx.servlet.http.HttpServletRequest;
import mockx.servlet.http.HttpServletResponse;
import org.sosy_lab.sv_benchmarks.Verifier;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/**
 * @servlet description="simple correlated tests"
 * @servlet vuln_count = "1"
 */
public class Pred2 extends BasicTestCase implements MicroTestCase {
  private static final String FIELD_NAME = "name";

  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    boolean choice = Verifier.nondetBoolean();
    String name = "abc";

    if (choice) {
      name = req.getParameter(FIELD_NAME);
    }

    if (choice) {
      PrintWriter writer = resp.getWriter();
      writer.println(name); /* BAD */
    }
  }

  public String getDescription() {
    return "simple correlated tests";
  }

  public int getVulnerabilityCount() {
    return 1;
  }
}
