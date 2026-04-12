package de.uzl.its.tests.helper;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ReferenceDTO {
    public String s;
    public String t = "bar";
    public LinkedList<String> list = new LinkedList<String>();
    public ArrayList<String> arrayList = new ArrayList<String>();
    public List<String> genericList = new ArrayList<String>();
    public NestedDTO ref;

    public ReferenceDTO() {
        s = "bar";
        list.add("bar");
        list.add("baz");
        list.add("bar");
        arrayList.add("bar");
        arrayList.add("baz");
        genericList.add("bar");
        genericList.add("baz");
        ref = new NestedDTO();
    }
}
