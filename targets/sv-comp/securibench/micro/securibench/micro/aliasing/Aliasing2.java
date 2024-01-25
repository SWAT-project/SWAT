// SPDX-FileCopyrightText: 2006 Benjamin Livshits livshits@cs.stanford.edu
// SPDX-License-Identifier: Apache-2.0

// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks

/*
   @author Benjamin Livshits <livshits@cs.stanford.edu>

   $Id: Aliasing2.java,v 1.1 2006/04/21 17:14:27 livshits Exp $

   changed by Falk Howar: no vulnerability

*/
package securibench.micro.aliasing;

import java.io.IOException;
import java.io.PrintWriter;
import mockx.servlet.http.HttpServletRequest;
import mockx.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/**
 * @servlet description="simple aliasing false positive"
 * @servlet vuln_count = "0"
 */
public class Aliasing2 extends BasicTestCase implements MicroTestCase {
  private static final String FIELD_NAME = "name";

  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String name = req.getParameter(FIELD_NAME);
    String str = "abc";
    name = str;

    PrintWriter writer = resp.getWriter();
    writer.println(str); /* OK */
  }

  public String getDescription() {
    return "simple aliasing false positive";
  }

  public int getVulnerabilityCount() {
    return 0;
  }
}
