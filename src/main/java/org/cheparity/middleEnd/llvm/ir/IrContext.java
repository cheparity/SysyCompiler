package middleEnd.llvm.ir;

import middleEnd.os.IrPrintable;
import utils.LoggerUtil;

import java.util.logging.Logger;

public class IrContext implements IrPrintable {
    private static final Logger LOGGER = LoggerUtil.getLogger();
    Module irModule;

    public IrContext() {

    }

    Module getIrModule() {
        return irModule;
    }

    void setIrModule(Module irModule) {
        this.irModule = irModule;
    }

    public IrPrintable[] getGlobalVarDeclInsts() {
        return this.irModule.globalInstructions.stream().filter(irPrintable -> irPrintable instanceof GlobalDeclInstruction).toList().toArray(new IrPrintable[0]);
    }

    public IrPrintable[] getFunctions() {
        return this.irModule.functions.toArray(new IrPrintable[0]);
    }

    @Override
    public String toIrCode() {
        return irModule.toIrCode();
    }
}
