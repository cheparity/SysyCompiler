package middleEnd.llvm.ir;

/**
 * 单纯的数字数值，如1，2等。
 */
public final class IntConstValue extends Variable {
    IntConstValue(Integer num) {
        super(IrType.create(IrType.IrTypeID.Int32TyID), num.toString(), true);
        super.setNumber(num);
    }
}
