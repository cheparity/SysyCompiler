package middleEnd.visitor.llvm.ir;

/**
 * 注意！这个IntConstant是局部的，不是全局的。全局的IntConstant是GlobalConstant
 */
public class IntConstant extends Constant {
    private final int number;

    IntConstant(String name, int number) {
        super(IrType.Int32TyID, name);
        this.number = number;
    }

    IntConstant(int number) {
        super(IrType.Int32TyID);
        this.number = number;
    }

    int getNumber() {
        return number;
    }

    @Override
    public String toIrCode() {
        return getType().toIrCode() + " " + number;
    }
}
