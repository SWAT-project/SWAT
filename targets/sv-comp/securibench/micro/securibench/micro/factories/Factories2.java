// SPDX-FileCopyrightText: 2006 Benjamin Livshits livshits@cs.stanford.edu
// SPDX-License-Identifier: Apache-2.0

// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks

/*
   @author Benjamin Livshits <livshits@cs.stanford.edu>

   $Id: Factories2.java,v 1.3 2006/04/04 20:00:41 livshits Exp $
*/
package securibench.micro.factories;

import java.io.IOException;
import java.io.PrintWriter;
import mockx.servlet.http.HttpServletRequest;
import mockx.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/**
 * @servlet description="simple factory problem with String.toString"
 * @servlet vuln_count = "1"
 */
public class Factories2 extends BasicTestCase implements MicroTestCase {
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String s1 = req.getParameter("name");
    String s2 = s1.toString();
    String s3 = "abc".toString();

    PrintWriter writer = resp.getWriter();

    writer.println(s2); /* BAD */
    writer.println(s3); /* OK */
  }

  public String getDescription() {
    return "simple factory problem with String.toString";
  }

  public int getVulnerabilityCount() {
    return 1;
  }
}
