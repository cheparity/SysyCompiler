package middleEnd.visitor.llvm.ir;

public class GlobalVariable extends GlobalValue implements GlobalObjects {
    int number;
    boolean init = false;

    GlobalVariable(IrType type, String name) {
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
