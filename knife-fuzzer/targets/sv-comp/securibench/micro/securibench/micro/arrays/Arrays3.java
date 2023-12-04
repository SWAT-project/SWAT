// SPDX-FileCopyrightText: 2006 Benjamin Livshits livshits@cs.stanford.edu
// SPDX-License-Identifier: Apache-2.0

// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks

/*
   @author Benjamin Livshits <livshits@cs.stanford.edu>

   $Id: Arrays3.java,v 1.3 2006/04/04 20:00:40 livshits Exp $
*/
package securibench.micro.arrays;

import java.io.IOException;
import java.io.PrintWriter;
import mockx.servlet.http.HttpServletRequest;
import mockx.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/**
 * @servlet description = "a more complex array test"
 * @servlet vuln_count = "1"
 */
public class Arrays3 extends BasicTestCase implements MicroTestCase {
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String s1 = req.getParameter("name");
    String[] array1 = new String[10];
    String[] array2 = new String[10];

    array1[0] = s1;
    array2[0] = "abc";

    PrintWriter writer = resp.getWriter();
    writer.println(array1[0]); /* BAD */
    writer.println(array2[0]); /* OK */
  }

  public String getDescription() {
    return "a more complex array test";
  }

  public int getVulnerabilityCount() {
    return 1;
  }
}
