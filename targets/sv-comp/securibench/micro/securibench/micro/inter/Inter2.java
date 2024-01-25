// SPDX-FileCopyrightText: 2006 Benjamin Livshits livshits@cs.stanford.edu
// SPDX-License-Identifier: Apache-2.0

// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks

/*
   @author Benjamin Livshits <livshits@cs.stanford.edu>

   $Id: Inter2.java,v 1.4 2006/04/04 20:00:40 livshits Exp $
*/
package securibench.micro.inter;

import java.io.IOException;
import java.io.PrintWriter;
import mockx.servlet.http.HttpServletRequest;
import mockx.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/**
 * @servlet description="simple id method call"
 * @servlet vuln_count = "2"
 */
public class Inter2 extends BasicTestCase implements MicroTestCase {
  private static final String FIELD_NAME = "name";

  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String s1 = req.getParameter(FIELD_NAME);

    PrintWriter writer = resp.getWriter();
    String s2 = id(s1, writer);
    String s3 = id("abc", writer);
    writer.println(s2); /* BAD */ // Is this double-reporting?..
    writer.println(s3); /* OK */
  }

  private String id(String string, PrintWriter writer) {
    writer.println(string); /* BAD */

    return string;
  }

  public String getDescription() {
    return "simple id method call";
  }

  public int getVulnerabilityCount() {
    return 2;
  }
}
