// SPDX-FileCopyrightText: 2006 Benjamin Livshits livshits@cs.stanford.edu
// SPDX-License-Identifier: Apache-2.0

// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks

/*
   @author Benjamin Livshits <livshits@cs.stanford.edu>

   $Id: Basic3.java,v 1.4 2006/04/04 20:00:40 livshits Exp $
*/
package securibench.micro.basic;

import java.io.IOException;
import java.io.PrintWriter;
import mockx.servlet.http.HttpServletRequest;
import mockx.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/**
 * @servlet description="simple derived string test"
 * @servlet vuln_count = "1"
 */
public class Basic3 extends BasicTestCase implements MicroTestCase {
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String str = req.getParameter("name");
    PrintWriter writer = resp.getWriter();
    String s2 = str.toLowerCase();

    writer.println(s2); /* BAD */
  }

  public String getDescription() {
    return "simple derived string test";
  }

  public int getVulnerabilityCount() {
    return 1;
  }
}
