package middleEnd.llvm.ir;

public final class Argument extends Value {
    Argument(IrType type, String name) {
        super(type, name);
    }

    @Override
    public String toIrCode() {
        return null;
    }
}
