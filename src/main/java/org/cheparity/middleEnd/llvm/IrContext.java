package middleEnd.llvm;

import middleEnd.llvm.ir.Module;
import middleEnd.os.IrPrintable;

public class IrContext implements IrPrintable {
    Module irModule;

    IrContext() {

    }

    public void setIrModule(Module irModule) {
        this.irModule = irModule;
    }

    @Override
    public String toIrCode() {
        return irModule.toIrCode();
    }
}
