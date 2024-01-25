// SPDX-FileCopyrightText: 2006 Benjamin Livshits livshits@cs.stanford.edu
// SPDX-License-Identifier: Apache-2.0

// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks
/**
 * @author Benjamin Livshits <livshits@cs.stanford.edu>
 *     <p>$Id: Basic28.java,v 1.4 2006/04/04 20:00:40 livshits Exp $
 *     <p>changed by Falk Howar: no vulnerabilty. boolean default value is false
 */
package securibench.micro.basic;

import java.io.IOException;
import java.io.PrintWriter;
import mockx.servlet.http.HttpServletRequest;
import mockx.servlet.http.HttpServletResponse;
import securibench.micro.BasicTestCase;
import securibench.micro.MicroTestCase;

/**
 * @servlet description="complicated control flow"
 * @servlet vuln_count = "0"
 */
public class Basic28 extends BasicTestCase implements MicroTestCase {
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String name = req.getParameter("name");
    boolean b[] = new boolean[3];
    PrintWriter writer = resp.getWriter();
    if (b[0]) {
      if (b[0]) {
        if (b[0]) {
          if (b[0]) {
            if (b[0]) {
              if (b[0]) {
                if (b[0]) {
                  if (b[0]) {
                    if (b[0]) {}
                  } else {
                  }
                } else {
                }
                if (b[0]) {}
              } else {
              }
            } else {
              if (b[0]) {
                if (b[0]) {
                  if (b[0]) {}
                } else {
                }
              } else {
              }
            }
          } else {
            if (b[0]) {
              if (b[0]) {
                if (b[0]) {}
                writer.println(name); /* BAD */
              } else {
              }
            } else {
            }
          }
        } else {
          if (b[0]) {
            if (b[0]) {
              if (b[0]) {}
            } else {
            }
          } else {
          }
        }
      } else {
        if (b[0]) {
          if (b[0]) {
            if (b[0]) {
              if (b[0]) {
                if (b[0]) {}
              } else {
              }
            } else {
              if (b[0]) {
                if (b[0]) {
                  if (b[0]) {}
                } else {
                }
              } else {
              }
            }
          } else {
            if (b[0]) {
              if (b[0]) {
                if (b[0]) {}
              } else {
              }
            } else {
            }
          }
        } else {
          if (b[0]) {
            if (b[0]) {
              if (b[0]) {}
            } else {
            }
          } else {
            if (b[0]) {
              if (b[0]) {
                if (b[0]) {}
              } else {
              }
            } else {
            }
          }
        }
      }
    } else {
      if (b[0]) {
        if (b[0]) {
          if (b[0]) {
            writer.println(name); /* BAD */
          }
        } else {
        }
      } else {
      }
    }
  }

  public String getDescription() {
    return "complicated control flow";
  }

  public int getVulnerabilityCount() {
    return 0;
  }
}
