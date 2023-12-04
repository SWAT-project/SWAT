import org.sosy_lab.sv_benchmarks.Verifier;

/**
 * Type : Functional Safety Expected Verdict : True Last modified by : Zafer Esen
 * <zafer.esen@it.uu.se> Date : 11 November 2019
 *
 * <p>This benchmark is just like SortedListInsert-FunSat01, but with a stronger sortedness
 * assertion.
 *
 * <p>Original license follows.
 */

/**
 * Copyright (c) 2011, Regents of the University of California All rights reserved.
 *
 * <p>Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * <p>1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * <p>2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 *
 * <p>3. Neither the name of the University of California, Berkeley nor the names of its
 * contributors may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * <p>THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/** @author Jacob Burnim <jburnim@cs.berkeley.edu> */
public class Main {

  private static class List {
    public int x;
    public List next;

    private static final int SENTINEL = Integer.MAX_VALUE;

    private List(int x, List next) {
      this.x = x;
      this.next = next;
    }

    public List() {
      this(SENTINEL, null);
    }

    public void insert(int data) {
      if (data > this.x) {
        next.insert(data);
      } else {
        next = new List(x, next);
        x = data;
      }
    }
  }

  public static void main(String[] args) {
    final int N = Verifier.nondetInt();
    Verifier.assume(N > 1);

    List list = new List();
    for (int i = 0; i < N; i++) {
      list.insert(Verifier.nondetInt());
    }

    for (List l = list; l.next != null; l = l.next) {
      assert (l.x <= l.next.x);
    }
  }
}
