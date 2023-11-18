package middleEnd.llvm.ir;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FuncDeclInstruction extends Instruction {
    private final IrType retType;
    private final String funcName;
    private final List<Argument> arguments = new ArrayList<>();

    FuncDeclInstruction(IrType retType, String funcName) {
        this.retType = retType;
        this.funcName = funcName;
    }

    FuncDeclInstruction addArg(Argument... arguments) {
        this.arguments.addAll(Arrays.asList(arguments));
        return this;
    }
}
