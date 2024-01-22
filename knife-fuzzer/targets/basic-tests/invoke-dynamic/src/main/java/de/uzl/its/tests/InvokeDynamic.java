package de.uzl.its.tests;

interface Comparable {
    boolean cmp(int a);
}

public class InvokeDynamic {
    /*
    int check = 42;

    private String test3(int param) {

        Function<Integer, Boolean> c = (a) -> (a==42);
        check = 11;
        boolean res = c.apply(param);
        if(res){
            return "path_0";
        }
        else{
            return "path_1";
        }
    }


        private String test4(int param) {
            int offset = 10;
            Comparable c1 = (a1) -> (a1 < check - offset);
            boolean res1 = c1.cmp(param);


            Comparable c2 = (a2) -> (a2 == 5);
            boolean res2 = c2.cmp(param);

            if(res1 && res2){
                return "path_0";
            }
            else{
                return "path_1";
            }
        }

    private String test5(int param) {
        int offset = 10;
        Comparable c1 = (a1) -> (a1 < param);
        boolean res1 = c1.cmp(42);


        Comparable c2 = (a2) -> (a2 == 5);
        boolean res2 = c2.cmp(param);

        if(res1 && res2){
            return "path_0";
        }
        else{
            return "path_1";
        }
    }
    private String test6(int param) {
        Function<Integer, Boolean> c1 = (a1) -> (a1 < param);
        boolean res = c1.apply(42);


        if(res){
            return "path_0";
        }
        else{
            return "path_1";
        }
    }

    private String test1(int param) {
        Function<Integer, Boolean> c1 = (a1) -> (a1 == check);
        boolean res = c1.apply(param);


        if(res){
            return "path_0";
        }
        else{
            return "path_1";
        }
    }

    private String test2(int param) {
        AtomicBoolean res = new AtomicBoolean(false);
        List<Integer> list = new ArrayList<Integer>();
        list.add(42);
        list.add(420);
        list.stream().forEach((n) -> res.set(n - check == param));
        // list.stream().forEach((n) -> res.set(n == param));


        if(res.get()){
            return "path_0";
        }
        else{
            return "path_1";
        }
    }
    private String test2(String param) {
        String x = "XXX";
        int y = 12;
        String concatenated = param + y;
        // String concatinated = param + "XXX";
        //String concatenated = param.concat("XXX");//param + "XXX";
        boolean result = concatenated.equals("XXYYXXX");


        if(result){
            return "path_0";
        }
        else{
            return "path_1";
        }
    }*/
    private String test(String param) {
        String y = "?";
        String z = "^";
        String x = z + "!!" + "||" + param + "WW" + y + "XX";

        boolean result = x.equals("^!!||::::WW?XX");

        if (result) {
            return "path_0";
        } else {
            return "path_1";
        }
    }

    public static void main(String[] args) {
        // System.out.println(new InvokeDynamic().test(10));
        System.out.println(new InvokeDynamic().test("XXX"));
    }
}
