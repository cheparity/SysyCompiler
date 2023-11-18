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

    @Override
    public String toIrCode() {
        StringBuilder sb = new StringBuilder();
        sb.append("declare ").append(retType.toIrCode()).append(" ").append(funcName).append("(");
        for (int i = 0; i < arguments.size(); i++) {
            var type = arguments.get(i);
            if (type.getType().getBasicType() == IrType.IrTypeID.VoidTyID) continue;
            sb.append(type.toIrCode());
            if (i != arguments.size() - 1) sb.append(", ");
        }
        sb.append(")");
        return sb.toString();
    }
}
