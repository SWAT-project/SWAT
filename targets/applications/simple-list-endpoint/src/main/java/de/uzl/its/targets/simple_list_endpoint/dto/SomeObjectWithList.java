package de.uzl.its.targets.simple_list_endpoint.dto;

import java.util.List;

public class SomeObjectWithList {

    public static class SomeSubObject {
        private int d;

        public SomeSubObject(int d) {
            this.d = d;
        }

        public SomeSubObject() {
        }

        public int getD() {
            return d;
        }

        public void setD(int d) {
            this.d = d;
        }
    }

    private int a;
    private int b;
    private int c;
    private List<String> list;
    private SomeSubObject someSubObject;

    public SomeObjectWithList(int a, int b, int c, List<String> list, SomeSubObject someSubObject) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.list = list;
        this.someSubObject = someSubObject;
    }

    private SomeObjectWithList() {
    }

    public int getA() {
        return a;
    }

    public void setA(int a) {
        this.a = a;
    }

    public int getB() {
        return b;
    }

    public void setB(int b) {
        this.b = b;
    }

    public int getC() {
        return c;
    }

    public void setC(int c) {
        this.c = c;
    }

    public List<String> getList() {
        return list;
    }

    public void setList(List<String> list) {
        this.list = list;
    }

    public SomeSubObject getSomeSubObject() {
        return someSubObject;
    }

    public void setSomeSubObject(SomeSubObject someSubObject) {
        this.someSubObject = someSubObject;
    }
}
