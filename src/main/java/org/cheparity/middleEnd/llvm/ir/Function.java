package middleEnd.llvm.ir;

import java.util.ArrayList;
import java.util.List;

public class Function extends GlobalValue implements GlobalObjects {
    private final List<Argument> arguments = new ArrayList<>();
    private final IrType returnType;
    private final Module module; //所属module
    //    private final List<BasicBlock> blockList = new ArrayList<>();
    private EntryBlock entryBlock;

    Function(IrType type, String name, Module module) {
        super(type, name);
        this.returnType = type;
        this.module = module;
    }

    void insertArgument(Argument argument) {
        arguments.add(argument);
    }

    List<Argument> getArguments() {
        return arguments;
    }

    BasicBlock getEntryBlock() {
        return entryBlock;
    }

    void setEntryBlock(EntryBlock entryBlock) {
        this.entryBlock = entryBlock;
        entryBlock.setEntryFunc(this);
    }
    //    public void addBasicBlock(BasicBlock basicBlock) {
//        this.blockList.add(basicBlock);
//    }

    public Module getModule() {
        return module;
    }

    public IrType getReturnType() {
        return returnType;
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
            sb.append(arg.getType().toIrCode()).append(" ").append(arg.getName());
            if (i != arguments.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(")").append(" {");
        sb.append("\n");
        //如果没有ret，则需要补上ret，否则过不了llvm的编译。那就默认ret void / ret i32 0
        if (!(entryBlock.getLastInstruction() instanceof RetInstruction)) {
            if (returnType.getBasicType() == IrType.IrTypeID.VoidTyID) {
                entryBlock.addInstruction(new RetInstruction());
            } else {
                entryBlock.addInstruction(new RetInstruction(new IntConstValue(0)));
            }
        }
        sb.append(entryBlock.toIrCode());
        //如果没有ret，则需要补上ret，否则过不了llvm的编译。那就默认ret void / ret i32 0
        sb.append("}");
        return sb.toString();
    }

}
