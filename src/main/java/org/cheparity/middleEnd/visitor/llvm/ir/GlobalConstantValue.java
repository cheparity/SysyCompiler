package middleEnd.visitor.llvm.ir;

public class GlobalConstantValue extends GlobalValue implements GlobalObjects, ReadonlyObjects {
    int number;

    GlobalConstantValue(IrType type, String name, int number) {
        super(type, name);
    }

    @Override
    public String toIrCode() {
        return null;
    }
}
