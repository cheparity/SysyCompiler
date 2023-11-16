package middleEnd.visitor.llvm.ir;

public abstract class DataValue extends Value {
    DataValue(IrType type, String name) {
        super(type, name);
    }

    DataValue(IrType type) {
        super(type);
    }
}
