package middleEnd.visitor.llvm.ir;

public class Operator extends User {
    Operator(IrType type) {
        super(type);
    }

    Operator(IrType type, String name) {
        super(type, name);
    }

    @Override
    public String toIrCode() {
        return null;
    }
}
