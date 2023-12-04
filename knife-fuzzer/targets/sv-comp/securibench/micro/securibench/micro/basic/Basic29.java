// SPDX-FileCopyrightText: 2006 Benjamin Livshits livshits@cs.stanford.edu
// SPDX-License-Identifier: Apache-2.0

// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks
/**
 * @author Benjamin Livshits <livshits@cs.stanford.edu>
 *     <p>$Id: Basic29.java,v 1.4 2006/04/04 20:00:40 livshits Exp $
 */
package securibench.micro.basic;

import java.io.IOException;
import java.io.PrintWriter;
import mockx.servlet.http.HttpServletRequest;
import mockx.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/**
 * @servlet description="recursive data structures"
 * @servlet vuln_count = "2"
 */
public class Basic29 extends BasicTestCase implements MicroTestCase {
  class Node {
    String value;
    Node next = null;
  }

  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String name = req.getParameter("name");
    Node head = new Node();
    Node next = new Node();
    head.next = next;
    next.value = name;

    PrintWriter writer = resp.getWriter();
    writer.println(next.value); /* BAD */
    writer.println(head.next.value); /* BAD */
    writer.println(head.value); /* OK */
  }

  public String getDescription() {
    return "recursive data structures";
  }

  public int getVulnerabilityCount() {
    return 2;
  }
}
