package de.uzl.its.swat.symbolic.invoke;

import de.uzl.its.swat.symbolic.value.ValueFactory;
import de.uzl.its.swat.symbolic.value.PlaceHolder;
import de.uzl.its.swat.symbolic.value.Value;
import de.uzl.its.swat.symbolic.value.reference.lang.StringValue;
import org.objectweb.asm.Type;

public class DynamicInvocation {

    public static Value<?, ?> invokeMethod(
            String owner, String name, Value<?, ?>[] args, String bsm) {
        if (owner.equals("java/lang/invoke/StringConcatFactory")
                && name.equals("makeConcatWithConstants")) {
            String[] constants = bsm.substring(1, bsm.length() - 1).split("\u0001", -1);

            if (constants.length > 0) {
                if (constants.length - 1 == args.length) {
                    // The expected case
                    StringValue res = ValueFactory.createStringValue(constants[0], -1);
                    for (int i = 0; i < args.length; i++) {
                        String desc = "(Ljava.lang.String;)";
                        Type[] type = Type.getArgumentTypes(desc);
                        res =
                                (StringValue)
                                        res.invokeMethod(
                                                "concat",
                                                type,
                                                new StringValue[] {args[i].asStringValue()});
                        res =
                                (StringValue)
                                        res.invokeMethod(
                                                "concat",
                                                type,
                                                new StringValue[] {
                                                    ValueFactory.createStringValue(
                                                            constants[i + 1], -1)
                                                });
                    }
                    return res;
                }
            }
            return null;
        }

        return PlaceHolder.instance;
    }
}
