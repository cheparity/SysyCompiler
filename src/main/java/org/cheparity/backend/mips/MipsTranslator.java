package backend.mips;

import backend.mips.dataStruct.DataDeclInst;
import backend.mips.dataStruct.MipsContext;
import backend.mips.dataStruct.MipsDataSeg;
import middleEnd.llvm.ir.IrContext;
import middleEnd.os.IrPrintable;
import utils.LoggerUtil;

import java.util.Arrays;
import java.util.logging.Logger;

/**
 * 为了确保可移植性，MipsTranslator<font color='red'>不应该依赖于IrContext，而是应该依赖于IrCode</font>。所以我将其完全设计成了String的解析器。
 */
public final class MipsTranslator {
    private final static Logger LOGGER = LoggerUtil.getLogger();
    private static MipsTranslator instance;
    public final IrContext irContext;
    private final MipsContext mipsContext = new MipsContext();

    private MipsTranslator(IrContext irContext) {
        this.irContext = irContext;
    }

    public static MipsTranslator getInstance(IrContext context) {
        if (instance == null) {
            instance = new MipsTranslator(context);
            return instance;
        }
        context.toIrCode(); //确保全部转化为irCode
        return instance;
    }

    public String translate2Mips() {
        transGlobalData();

        return mipsContext.toMipsCode();
    }

    //@a = dso_local constant i32 5
    //<name> = dso_local <op Code> <type value>
    private void transGlobalData() {
        IrPrintable[] globalInstructions = irContext.getGlobalVarDeclInsts();
        if (globalInstructions == null) return;
        MipsDataSeg dataSeg = MipsDataSeg.getInstance(mipsContext);
        for (IrPrintable instruction : globalInstructions) {
            String[] split = instruction.toIrCode().split(" ");
            LOGGER.info(Arrays.toString(split));
            var name = split[0].substring(1);
            var type = split[4];
            var value = split[5];
            dataSeg.addDataDeclInst(new DataDeclInst(name, type, value));
        }
    }

}
