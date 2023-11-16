package middleEnd.visitor.llvm.ir;

/**
 * GlobalValue下有Global Constant和Global Variable。与局部变量行为不同
 */
public abstract class GlobalValue extends DataValue implements GlobalObjects {
    GlobalValue(IrType type, String name) {
        super(type, name);
    }

}
