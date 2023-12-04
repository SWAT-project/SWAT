// SPDX-FileCopyrightText: 2006 Benjamin Livshits livshits@cs.stanford.edu
// SPDX-License-Identifier: Apache-2.0

// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks

/*
   @author Benjamin Livshits <livshits@cs.stanford.edu>

   $Id: Arrays10.java,v 1.4 2006/04/04 20:00:40 livshits Exp $
*/
package securibench.micro.arrays;

import java.io.IOException;
import java.io.PrintWriter;
import mockx.servlet.http.HttpServletRequest;
import mockx.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/**
 * @servlet description = "comple multidimentional array test"
 * @servlet vuln_count = "1"
 */
public class Arrays10 extends BasicTestCase implements MicroTestCase {
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String name = req.getParameter("name");
    String[][] array = new String[3][5];
    array[0] = new String[] {name, "abc"};

    PrintWriter writer = resp.getWriter();
    writer.println(array[0][0]); /* BAD */
    writer.println(array[0][2]); /* OK */
  }

  public String getDescription() {
    return "comple multidimentional array test";
  }

  public int getVulnerabilityCount() {
    return 1;
  }
}
