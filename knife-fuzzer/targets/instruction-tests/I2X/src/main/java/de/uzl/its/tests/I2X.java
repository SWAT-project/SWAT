package de.uzl.its.tests;

public class I2X {


    /**
     * Test function for I2S
     *
     * @param i (symbolic) input
     * @return the converted value
     */
    public int felixtest1(int i) {
        String s = Integer.toString(i);
        return s.length() > 3 ? 1 : 0;
    }

    public int felixtest2(char i) {
        Character c = i;
        return Character.isDigit(c) ? 1 : 0;
    }

    public int felixtest3(char i) {
        return Character.isDefined(i) ? 1 : 0;
    }

    public int felixtest4(char i) {
        //
        return Character.isDigit(i) ? 1 : 0;
    }

    public int felixtest5(char i) {
        //
        return Character.isLetter(i) ? 1 : 0;
    }

    public int felixtest6(int i) {
        //
        return Integer.toString(i).equals("1234") ? 1 : 0;
    }

    /**
     * Main function
     *
     * @param args command line arguments - ignored
     */
    public static void main(String[] args) {
        I2X test = new I2X();
        char testValue = 1234;

        test.felixtest6(testValue);
    }
}
