// SPDX-FileCopyrightText: 2006 Benjamin Livshits livshits@cs.stanford.edu
// SPDX-License-Identifier: Apache-2.0

// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks

/*
   @author Benjamin Livshits <livshits@cs.stanford.edu>

   $Id: Session1.java,v 1.3 2006/04/04 20:00:41 livshits Exp $
*/
package securibench.micro.session;

import java.io.IOException;
import java.io.PrintWriter;
import mockx.servlet.http.HttpServletRequest;
import mockx.servlet.http.HttpServletResponse;
import mockx.servlet.http.HttpSession;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/**
 * @servlet description="simple session test"
 * @servlet vuln_count = "1"
 */
public class Session1 extends BasicTestCase implements MicroTestCase {
  private static final String FIELD_NAME = "name";

  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String name = req.getParameter(FIELD_NAME);
    HttpSession session = req.getSession();
    session.setAttribute("name", name);
    String s2 = (String) session.getAttribute("name");

    PrintWriter writer = resp.getWriter();
    writer.println(s2); /* BAD */
  }

  public String getDescription() {
    return "simple session test";
  }

  public int getVulnerabilityCount() {
    return 1;
  }
}
