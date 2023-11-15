package middleEnd.visitor.llvm.ir;

public abstract class Constant extends Value {
    Constant(IrType type, String name) {
        super(type, name);
    }

    Constant(IrType type) {
        super(type);
    }
}
