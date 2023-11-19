package middleEnd.llvm.ir;

/**
 * GlobalValue下有Global Constant和Global Variable。与局部变量行为不同
 */
public abstract class GlobalValue extends PointerValue implements GlobalObjects {
    boolean readonly;
    int number = 0; //初始化为0；

    GlobalValue(IrType type, String name) {
        super(type, name);
    }

    GlobalValue(IrType type, String name, boolean readonly) {
        super(type, name);
        this.readonly = readonly;
    }

    int getNumber() {
        return number;
    }

    void setNumber(int number) {
        this.number = number;
    }

    @Override
    public String toIrCode() {
        return this.getType().toIrCode() + " " + this.number;
    }
}
