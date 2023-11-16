package middleEnd.visitor.llvm.ir;

import java.util.ArrayList;
import java.util.List;

public class Function extends GlobalValue implements GlobalObjects {
    private final List<Argument> arguments = new ArrayList<>();
    private final IrType returnType;
    private BasicBlock entryBlock;
    private Module module;

    Function(IrType type, String name, Module module) {
        super(type, name);
        this.returnType = type;
        this.module = module;
    }

    void insertParam(Argument argument) {
        arguments.add(argument);
    }

    List<Argument> getArguments() {
        return arguments;
    }

    void setEntryBlock(BasicBlock entryBlock) {
        this.entryBlock = entryBlock;
    }

    @Override
    public String toIrCode() {
        var sb = new StringBuilder();
        sb
                .append("define dso_local ")
                .append(returnType.toIrCode())
                .append(" ")
                .append(getName())
                .append("(");
        for (int i = 0; i < arguments.size(); i++) {
            var arg = arguments.get(i);
            sb.append(arg.getType()).append(" ").append(arg.getName());
            if (i != arguments.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(")").append(" {");
        sb.append("\n");
        sb.append(entryBlock.toIrCode());
        sb.append("}");
        return sb.toString();
    }

}
