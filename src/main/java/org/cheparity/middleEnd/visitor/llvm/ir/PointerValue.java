package middleEnd.visitor.llvm.ir;

public class PointerValue extends DataValue {
    PointerValue(IrType type, String name) {
        super(type, name);
    }

    @Override
    public String toIrCode() {
        return getType().toIrCode() + "* " + getName();
    }
}
