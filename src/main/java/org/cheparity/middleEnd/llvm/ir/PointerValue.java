package middleEnd.llvm.ir;

/**
 * 指针变量。可以通过Variable找到对应的PointerValue，但是不能通过PointerValue找到对应的Variable（除非指针变量拥有对应的Variable）
 */
public final class PointerValue extends Variable {
    Variable pointAt;

    /**
     * 指针。默认可变。
     *
     * @param type 指针类型
     * @param name 指针名
     */
    PointerValue(IrType type, String name) {
        super(type, name);
        super.setPointer(this); //将Variable的pointer设为自己（其实好像不是和优雅，等待想清楚）
    }

    @Override
    public String toIrCode() {
        return getType().toIrCode() + "* " + getName();
    }

    /**
     * 获取指针指向的变量（可能为空）
     *
     * @return 指针指向的变量
     */
    Variable getPointAt() {
        return pointAt;
    }
}
