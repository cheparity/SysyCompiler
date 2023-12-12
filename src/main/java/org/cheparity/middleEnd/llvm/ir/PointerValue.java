package middleEnd.llvm.ir;

/**
 * 指针变量。可以通过Variable找到对应的PointerValue，但是不能通过PointerValue找到对应的Variable（除非指针变量拥有对应的Variable）
 */
public class PointerValue extends Value implements TokenContainer {
    /**
     * 所指对象。<font color='red'>可以为空</font>
     */
    Integer[] number; //初始化为0；
    private String token = "";

    /**
     * 指针。默认可变。
     *
     * @param type 指针类型
     * @param name 指针名
     */
    PointerValue(IrType type, String name) {
        super(type, name);
    }

    public Integer[] getNumber() {
        return number;
    }

    public void setNumber(Integer... number) {
        this.number = number;
    }

    public void resetNumber() {
        this.number = null;
    }

    public void setNumber(int size, Integer position, Integer number) {
        if (this.number == null) {
            this.number = new Integer[size];
        }
        this.number[position] = number;
    }

    @Override
    public String getToken() {
        return token;
    }

    @Override
    public PointerValue setToken(String token) {
        this.token = token;
        return this;
    }

    @Override
    public String toIrCode() {
        return getType().toIrCode() + "* " + getName();
    }

}
