package de.uzl.its.swat.symbolicRegression;

import lombok.Getter;

public class InputOutputPair<T1, T2> {

    @Getter
    private T1 first;

    @Getter
    private T2 second;

    @Getter
    private String typeFirst;

    @Getter
    private String typeSecond;

    public InputOutputPair(T1 first, T2 second) {
        this.first = first;
        this.second = second;

        // set types
        typeSecond = second.getClass().getTypeName();

        // check if first is an array
        if (first.getClass().isArray()) {
            // first as array
            Object[] array = (Object[]) first;
            if (array.length == 0) {
                typeFirst = Object.class.getTypeName();
            } else {
                typeFirst = array[0].getClass().getTypeName();
            }
        } else {
            typeFirst = first.getClass().getTypeName();
        }
    }
}
