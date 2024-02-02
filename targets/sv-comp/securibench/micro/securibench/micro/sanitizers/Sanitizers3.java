// SPDX-FileCopyrightText: 2006 Benjamin Livshits livshits@cs.stanford.edu
// SPDX-License-Identifier: Apache-2.0

// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks

/*
   @author Benjamin Livshits <livshits@cs.stanford.edu>

   $Id: Sanitizers3.java,v 1.4 2006/04/21 17:14:27 livshits Exp $
*/
package securibench.micro.sanitizers;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Locale;
import mockx.servlet.http.HttpServletRequest;
import mockx.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/**
 * @servlet description="safe redirect"
 * @servlet vuln_count = "0"
 */
public class Sanitizers3 extends BasicTestCase implements MicroTestCase {
  private static final String FIELD_NAME = "name";

  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String s = req.getParameter(FIELD_NAME);
    String name = s.toLowerCase(Locale.UK);

    resp.sendRedirect(URLEncoder.encode("/user/" + name, "UTF-8")); /* OK */
  }

  public String getDescription() {
    return "safe redirect";
  }

  public int getVulnerabilityCount() {
    return 0;
  }
}
