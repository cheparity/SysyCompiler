package middleEnd.visitor.llvm.ir;

public class GlobalIntConstant extends GlobalValue implements GlobalObjects, ReadonlyObjects {
    int number;

    GlobalIntConstant(IrType type, String name, int number) {
        super(type, name);
        this.number = number;
    }

    @Override
    public String toIrCode() {
        return null;
    }
}
