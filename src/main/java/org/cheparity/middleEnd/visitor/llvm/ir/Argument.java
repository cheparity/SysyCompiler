package middleEnd.visitor.llvm.ir;

public class Argument extends Value {
    Argument(IrType type, String name) {
        super(type, name);
    }

    @Override
    public String toIrCode() {
        return null;
    }
}
