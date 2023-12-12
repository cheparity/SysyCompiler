package middleEnd.llvm.ir;

/**
 * GlobalValue下有Global Constant和Global Variable。与局部变量行为不同
 */
public class GlobalValue extends PointerValue implements GlobalObjects {

    GlobalValue(IrType type, String name) {
        super(type, name);
    }

    GlobalValue(IrType type, String name, boolean readonly) {
        super(type, name, readonly);
    }

    @Override
    public String toIrCode() {
        if (this.getType().isArray()) {
            return this.getType().toIrCode() + "* " + this.getName();
        }
        return this.getType().toIrCode() + " " + this.number[0];
    }
}
