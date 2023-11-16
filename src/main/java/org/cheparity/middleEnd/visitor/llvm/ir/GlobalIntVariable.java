package middleEnd.visitor.llvm.ir;

public class GlobalIntVariable extends GlobalValue implements GlobalObjects {
    int number;
    boolean init = false;

    GlobalIntVariable(IrType type, String name) {
        super(type, name);
    }

    void setNumber(int number) {
        this.number = number;
        this.init = true;
    }

    @Override
    public String toIrCode() {
        return null;
    }
}
