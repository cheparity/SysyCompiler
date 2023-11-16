package middleEnd.visitor.llvm.ir;

/**
 * store [ty] [value], [ty]* [pointer]
 * <p>
 * store 指令是一个 User，因为它使用了其他 Value 对象作为操作数。
 * <p>
 * store 指令有两个 Use 对象，分别表示存储的值和存储的目标地址。
 * <p>
 * 存储的值（[value]）是一个 Value，表示要存储的数据。注意其name<font color='red'>如果有%则表示寄存器，如果没有则表示具体的数字。</font>
 * <p>
 * 存储的目标地址（[pointer]）也是一个 Value，表示存储数据的目标内存地址。
 */
public final class StoreInstruction extends Instruction {
    private final Value value;
    private final Value pointerValue;

    StoreInstruction(Value value, Value pointerValue) {
        super();
        this.value = value;
        this.pointerValue = pointerValue;
    }

    @Override
    public String toIrCode() {
        return "store " + value.toIrCode() + ", " + pointerValue.toIrCode();
    }
}
