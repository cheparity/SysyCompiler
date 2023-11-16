package middleEnd.visitor.llvm.ir;

import middleEnd.visitor.os.IrPrintable;

import java.util.ArrayList;

/**
 * A clazz extends Value means that it can be USED somewhere.
 */
public abstract class Value implements IrPrintable {
    /**
     * The type of the value. All values have a type.
     */
    private final IrType type;
    /**
     * The name of the value. NOT all values have names. It CANNOT be an identifier.
     */
    private final String name;
    /**
     * The list of uses. It is empty until the value is added to a module.
     * <p>
     * Corresponding with <strong>def-use-chain</strong>.
     */
    private final ArrayList<Use> useList = new ArrayList<>();

    Value(IrType type, String name) {
        this.type = type;
        this.name = name;
    }

    /**
     * Some value may not have a name.
     *
     * @param type the type of the value
     */
    Value(IrType type) {
        this.type = type;
        this.name = "";
    }

    IrType getType() {
        return type;
    }

    /**
     * Please call `hasName()` before calling this method.
     *
     * @return the name of the value
     */
    String getName() {
        return name;
    }

    void addUse(Use use) {
        useList.add(use);
    }

    @Override
    public String toIrCode() {
        return type + " " + name;
    }

//    void replaceAllUsesWith(Value newValue) {
//        for (Use use : useList) {
//            use.set(newValue);
//        }
//    }

}
