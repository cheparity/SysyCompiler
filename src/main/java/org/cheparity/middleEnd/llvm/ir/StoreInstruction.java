package middleEnd.llvm.ir;

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
    private final Variable value;
    private final PointerValue pointerValue;

    StoreInstruction(Variable value, PointerValue pointerValue) {
        super();
        this.value = value;
        this.pointerValue = pointerValue;
    }

    @Override
    public String toIrCode() {
        return "store " + value.getType().toIrCode() + " " + value.toIrCode() + ", " + pointerValue.getType().toIrCode() + "* " + pointerValue.getName();
    }

    @Override
    public String toMipsCode() {
        var sb = new StringBuilder();
        String pointer = pointerValue.getName();
        Integer memOff = getMipsRegisterAllocator().getMemOff(pointer);
        String register = "$t1";
        if (value.getNumber().isPresent() && !value.getType().isArray()) {
            sb
                    .append(String.format("li\t\t%s, %s", register, value.getNumber().get()))
                    .append("\n\t");

        } else {
            //load出来
            sb
                    .append(String.format("lw\t\t%s, %s($fp)", register, getMipsRegisterAllocator().getMemOff(value.getName())))
                    .append("\n\t");

        }
        sb.append(String.format("sw\t\t%s, %s($fp)", register, memOff));
        return sb.toString();
    }
}
