package middleEnd.llvm.ir;

/**
 * 单纯的数字数值，如1，2等。
 */
public final class ConstValue extends Variable {
    ConstValue(Integer num, IrType.IrTypeID typeID) {
        super(IrType.create(typeID), num.toString(), true);
        super.setNumber(num);
    }
}
