// SPDX-FileCopyrightText: 2006 Benjamin Livshits livshits@cs.stanford.edu
// SPDX-License-Identifier: Apache-2.0

// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks

/*
   @author Benjamin Livshits <livshits@cs.stanford.edu>

   $Id: StrongUpdates2.java,v 1.4 2006/04/04 20:00:41 livshits Exp $
*/
package securibench.micro.strong_updates;

import java.io.IOException;
import java.io.PrintWriter;
import mockx.servlet.http.HttpServletRequest;
import mockx.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/**
 * @servlet description="simple test of strong updates"
 * @servlet vuln_count = "0"
 */
public class StrongUpdates2 extends BasicTestCase implements MicroTestCase {
  private static final String FIELD_NAME = "name";

  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String name = req.getParameter(FIELD_NAME);
    name = "abc";

    PrintWriter writer = resp.getWriter();
    writer.println(name); /* OK */
  }

  public String getDescription() {
    return "simple test of strong updates";
  }

  public int getVulnerabilityCount() {
    return 0;
  }
}
