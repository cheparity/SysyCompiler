package middleEnd.visitor.llvm.ir;

public class IntVariable extends DataValue implements Countable {
    IntVariable(IrType type, String name) {
        super(type, name);
    }
}
