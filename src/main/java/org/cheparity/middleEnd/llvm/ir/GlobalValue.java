package middleEnd.llvm.ir;

import java.util.Arrays;

/**
 * GlobalValue下有Global Constant和Global Variable。与局部变量行为不同
 */
public class GlobalValue extends PointerValue implements GlobalObjects {
    boolean readonly;
    Integer[] number; //初始化为0；

    GlobalValue(IrType type, String name) {
        super(type, name);
    }

    GlobalValue(IrType type, String name, boolean readonly) {
        super(type, name);
        this.readonly = readonly;
    }

    Integer[] getNumber() {
        return number;
    }

    void setNumber(Integer... number) {
        this.number = number;
    }

    @Override
    public String toIrCode() {
        if (this.getType().getDerivedType() == IrType.IrTypeID.ArrayTyID) {
            return this.getType().toIrCode() + " " + Arrays.toString(this.number);
        }
        return this.getType().toIrCode() + " " + this.number[0];
    }
}
