package middleEnd.llvm.ir;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Module extends Value {
    private final static String HEADER = "declare i32 @getint()\n" +
            "declare void @putint(i32)\n" +
            "declare void @putch(i32)\n" +
            "declare void @putstr(i8*)\n\n";
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

    Module insertFunc(Function function) {
        functions.add(function);
        return this;
    }

    Function getFunc(String name) {
        for (var func : functions) {
            if (func.getName().equals(name)) {
                return func;
            }
        }
        return null;
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
            if (function.getEntryBlock() == null) continue; //decl的函数
            sb.append(function.toIrCode()).append("\n");
        }
        return sb.toString();
    }

}
