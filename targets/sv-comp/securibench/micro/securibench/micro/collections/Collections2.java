// SPDX-FileCopyrightText: 2006 Benjamin Livshits livshits@cs.stanford.edu
// SPDX-License-Identifier: Apache-2.0

// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks

/*
   @author Benjamin Livshits <livshits@cs.stanford.edu>

   $Id: Collections2.java,v 1.5 2006/04/04 20:00:41 livshits Exp $
*/
package securibench.micro.collections;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import mockx.servlet.http.HttpServletRequest;
import mockx.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/**
 * @servlet description = "collection deposit/retrieve, check for false positives"
 * @servlet vuln_count = "1"
 */
public class Collections2 extends BasicTestCase implements MicroTestCase {
  private static final String FIELD_NAME = "name";

  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String s1 = req.getParameter(FIELD_NAME);
    LinkedList ll1 = new LinkedList();
    ll1.addLast(s1);

    LinkedList ll2 = new LinkedList();
    ll2.addLast("abc");

    String s2 = (String) ll1.getLast();
    String s3 = (String) ll2.getLast();

    PrintWriter writer = resp.getWriter();
    writer.println(s2); /* BAD */
    writer.println(s3); /* OK */
  }

  public String getDescription() {
    return "simple collection deposit/retrieve";
  }

  public int getVulnerabilityCount() {
    return 1;
  }
}
