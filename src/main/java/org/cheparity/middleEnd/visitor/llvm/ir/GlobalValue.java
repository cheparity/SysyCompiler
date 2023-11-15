package middleEnd.visitor.llvm.ir;

public abstract class GlobalValue extends Value implements GlobalObjects {
    GlobalValue(IrType type, String name) {
        super(type, name);
    }

    GlobalValue(IrType type) {
        super(type);
    }
}
