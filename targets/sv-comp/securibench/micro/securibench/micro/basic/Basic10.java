// SPDX-FileCopyrightText: 2006 Benjamin Livshits livshits@cs.stanford.edu
// SPDX-License-Identifier: Apache-2.0

// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks

/*
   @author Benjamin Livshits <livshits@cs.stanford.edu>

   $Id: Basic10.java,v 1.5 2006/04/04 20:00:40 livshits Exp $
*/
package securibench.micro.basic;

import java.io.IOException;
import java.io.PrintWriter;
import mockx.servlet.http.HttpServletRequest;
import mockx.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/**
 * @servlet description="chains of value assignments"
 * @servlet vuln_count = "1"
 */
public class Basic10 extends BasicTestCase implements MicroTestCase {
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String s1 = req.getParameter("name");
    String s2 = s1;
    String s3 = s2;
    String s4 = s3;
    StringBuffer b1 = new StringBuffer(s4);
    StringBuffer b3 = b1;
    String s5 = b3.toString();
    String s6 = s5;

    PrintWriter writer = resp.getWriter();
    writer.println(s6); /* BAD */
  }

  public String getDescription() {
    return "chains of value assignments";
  }

  public int getVulnerabilityCount() {
    return 1;
  }
}
