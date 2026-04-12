package de.uzl.its.targets.simple_list_endpoint.dto;

public class SomeObject {

    private int a;
    private int b;
    private int c;

    public SomeObject(int a, int b, int c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    public SomeObject() {
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
}
