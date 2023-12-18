package middleEnd.llvm.ir;

import middleEnd.os.IrPrintable;
import middleEnd.os.MipsPrintable;
import utils.LoggerUtil;

import java.util.logging.Logger;

public class IrContext implements IrPrintable, MipsPrintable {
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
        return this.irModule.globalInstructions.stream()
                .filter(irPrintable -> irPrintable instanceof GlobalDeclInstruction)
                .toList()
                .toArray(new IrPrintable[0]);
    }

    public IrPrintable[] getFunctions() {
        return this.irModule.irFunctions.toArray(new IrPrintable[0]);
    }

    @Override
    public String toIrCode() {
        return irModule.toIrCode();
    }

    @Override
    public String toMipsCode() {
        return irModule.toMipsCode();
    }
}
