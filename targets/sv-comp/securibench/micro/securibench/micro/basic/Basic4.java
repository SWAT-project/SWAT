// SPDX-FileCopyrightText: 2006 Benjamin Livshits livshits@cs.stanford.edu
// SPDX-License-Identifier: Apache-2.0

// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks

/*
   @author Benjamin Livshits <livshits@cs.stanford.edu>

   $Id: Basic4.java,v 1.4 2006/04/04 20:00:40 livshits Exp $
*/
package securibench.micro.basic;

import java.io.IOException;
import java.io.PrintWriter;
import mockx.servlet.http.HttpServletRequest;
import mockx.servlet.http.HttpServletResponse;
import org.sosy_lab.sv_benchmarks.Verifier;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/**
 * @servlet description="test path sensitivity just a bit"
 * @servlet vuln_count = "1"
 */
public class Basic4 extends BasicTestCase implements MicroTestCase {
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String str = req.getParameter("name");
    int choice = Verifier.nondetInt();
    PrintWriter writer = resp.getWriter();

    switch (choice) {
      case 1:
        break;
      case 2:
        break;
      case 3:
        writer.println(str); /* BAD */
        break;
      default:
    }
  }

  public String getDescription() {
    return "test path sensitivity just a bit";
  }

  public int getVulnerabilityCount() {
    return 1;
  }
}
