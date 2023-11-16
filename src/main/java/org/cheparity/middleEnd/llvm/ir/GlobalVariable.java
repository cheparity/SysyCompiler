package middleEnd.llvm.ir;

public final class GlobalVariable extends GlobalValue {
    GlobalVariable(IrType type, String name, boolean readonly) {
        super(type, name, readonly);
    }
}
