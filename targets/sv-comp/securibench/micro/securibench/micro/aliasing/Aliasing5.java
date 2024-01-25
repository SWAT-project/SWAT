// SPDX-FileCopyrightText: 2006 Benjamin Livshits livshits@cs.stanford.edu
// SPDX-License-Identifier: Apache-2.0

// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks
/*
   @author Benjamin Livshits <livshits@cs.stanford.edu>

   $Id: Aliasing5.java,v 1.1 2006/04/21 17:14:27 livshits Exp $
*/
package securibench.micro.aliasing;

import java.io.IOException;
import java.io.PrintWriter;
import mockx.servlet.http.HttpServletRequest;
import mockx.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/**
 * @servlet description="interprocedural argument aliasing"
 * @servlet vuln_count = "1"
 */
public class Aliasing5 extends BasicTestCase implements MicroTestCase {
  private static final String FIELD_NAME = "name";

  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    StringBuffer buf = new StringBuffer("abc");
    foo(buf, buf, resp, req);
  }

  void foo(StringBuffer buf, StringBuffer buf2, HttpServletResponse resp, HttpServletRequest req)
      throws IOException {
    String name = req.getParameter(FIELD_NAME);
    buf.append(name);
    PrintWriter writer = resp.getWriter();
    writer.println(buf2.toString()); /* BAD */
  }

  public String getDescription() {
    return "interprocedural argument aliasing";
  }

  public int getVulnerabilityCount() {
    return 1;
  }
}
