package de.uzl.its.swat.symbolic.trace;

import lombok.Getter;

/**
 * Represents a special element in the symbolic trace.
 * Special elements are used in places where branching might happen that is not currently supported.
 * This includes future features such as exception handling, but also behaviour such as static initializers.
 */
@Getter
class SpecialElement extends Element {

    // String representation of the instruction that caused the special element
    private final String inst;

    /**
     * Constructs a special element.
     * @param iid The unique identifier of the element
     * @param inst The string representation of the instruction that caused the special element
     */
    SpecialElement(int iid, String inst) {
        this.setIid(iid);
        this.inst = inst;
    }
}
