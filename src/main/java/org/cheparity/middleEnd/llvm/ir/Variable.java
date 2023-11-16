package middleEnd.llvm.ir;

import java.util.Optional;

/**
 * 确保Variable和pointer是伴生的关系
 */
public class Variable extends Value implements Countable {
    final boolean readonly;
    private PointerValue pointer;
    private int number;
    private boolean valued = false;


    /**
     * 变量。默认可变
     *
     * @param type 变量类型
     * @param name 变量名
     */
    Variable(IrType type, String name) {
        super(type, name);
        readonly = false;
        this.pointer = new PointerValue(type, name);
        this.pointer.pointAt = this; //将指针变量指向自己
    }


    /**
     * 变量。可变或不可变（申请const变量时用）
     *
     * @param type     变量类型
     * @param name     变量名
     * @param readonly 是否可变
     */
    Variable(IrType type, String name, boolean readonly) {
        super(type, name);
        this.readonly = readonly;
        this.pointer = new PointerValue(type, name);
    }

    /**
     * 如果此变量有初值，则返回初值，否则返回空
     * <p>
     * <font color='red'>注意：此方法并不是根据name来判断是否具有初值的。</font>比如变量%1，其被store指令赋值以后，就具有了初值，但其name仍然叫%1.
     *
     * @return 初值或空
     */
    public Optional<Integer> getNumber() {
        return valued ? Optional.of(number) : Optional.empty();
    }

    /**
     * 设定变量的值。<font color='red'>注意：此方法并不是将name设定为值</font>
     *
     * @param number 值
     */
    public void setNumber(int number) {
        valued = true;
        this.number = number;
    }

    /**
     * 只被<font color='red'>{@link PointerValue}调用</font>。当new一个指针变量的时候，会自动调用此方法。
     *
     * @param pointer 指针变量
     */
    protected void setPointer(PointerValue pointer) {
        this.pointer = pointer;
    }


    PointerValue toPointer() {
        return pointer;
    }

    @Override
    public String toIrCode() {
        var n = getNumber().isEmpty() ? getName() : getNumber().get(); //如果有数值，则输出数值；否则输出数字
        return getType().toIrCode() + " " + n;
    }
}
