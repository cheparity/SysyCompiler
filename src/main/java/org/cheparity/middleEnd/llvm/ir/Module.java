package middleEnd.llvm.ir;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Module extends Value {
    private final static String HEADER = "declare i32 @getint()\n" +
            "declare void @putint(i32)\n" +
            "declare void @putch(i32)\n" +
            "declare void @putstr(i8*)\n\n";
    /**
     * Module's Type is considered as VoidTyID
     */
    List<Function> functions = new ArrayList<>();
    List<GlobalValue> globalValues = new ArrayList<>();
    List<Instruction> globalInstructions = new ArrayList<>();

    Module() {
        super(IrType.create(IrType.IrTypeID.VoidTyID));
    }

    void insertGlobal(GlobalValue globalValue) {
        globalValues.add(globalValue);
    }

    Module insertGlobalInst(Instruction... instruction) {
        this.globalInstructions.addAll(Arrays.asList(instruction));
        return this;
    }

    void insertFunc(Function function) {
        functions.add(function);
    }

    @Override
    public String toIrCode() {
        StringBuilder sb = new StringBuilder();
//        sb.append(HEADER);
        for (var gloInst : globalInstructions) {
            sb.append(gloInst.toIrCode()).append("\n");
        }
        sb.append("\n");
        for (Function function : functions) {
            sb.append(function.toIrCode()).append("\n");
        }
        return sb.toString();
    }

}
