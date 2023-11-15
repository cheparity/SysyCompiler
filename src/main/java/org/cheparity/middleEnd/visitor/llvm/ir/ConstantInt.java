package middleEnd.visitor.llvm.ir;

public class ConstantInt extends Constant {
    int value;

    ConstantInt(int value) {
        super(IrType.Int32TyID);
        this.value = value;
    }

    @Override
    public String toIrCode() {
        return getType().toIrCode() + " " + value;
    }
}
