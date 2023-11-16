package middleEnd.visitor.llvm.ir;

import java.util.ArrayList;

/**
 * A class extends `User` means that it will use some `Value` objects linked with `Use`.
 */
public abstract class User extends Value {

    /**
     * User's value references are called `operands`. Because the IR is in SSA form, each operand is a `Value`.
     * <p>
     * It is called <strong>use-def-chain</strong>.
     */
    private final ArrayList<Value> operands = new ArrayList<>();

    User(IrType type, String name) {
        super(type, name);
        //set up use relationship
    }

    User(IrType type) {
        super(type);
    }

}
