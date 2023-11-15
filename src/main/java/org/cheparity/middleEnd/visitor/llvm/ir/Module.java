package middleEnd.visitor.llvm.ir;

import java.util.ArrayList;
import java.util.List;

public class Module extends Value {
    private final static String HEADER = "declare i32 @getint()\n" +
            "declare void @putint(i32)\n" +
            "declare void @putch(i32)\n" +
            "declare void @putstr(i8*)\n";
    /**
     * Module's Type is considered as VoidTyID
     */
    List<Function> functions = new ArrayList<>();
    List<GlobalValue> globalValues = new ArrayList<>();
    List<Instruction> globalInstructions = new ArrayList<>();

    Module() {
        super(IrType.VoidTyID);
    }

    void insertGlobal(GlobalValue globalValue) {
        globalValues.add(globalValue);
    }

    void insertGlobalInst(Instruction instruction) {
        globalInstructions.add(instruction);
    }

    void insertFunc(Function function) {
        functions.add(function);
    }

    @Override
    public String toIrCode() {
        StringBuilder sb = new StringBuilder();
        sb.append(HEADER);

        for (var gloInst : globalInstructions) {
            sb.append(gloInst.toIrCode());
        }

        for (Function function : functions) {
            sb.append(function.toIrCode());
        }
        return sb.toString();
    }

}
