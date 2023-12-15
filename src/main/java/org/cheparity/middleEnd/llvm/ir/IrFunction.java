package middleEnd.llvm.ir;

import utils.LoggerUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public final class IrFunction extends GlobalValue implements GlobalObjects {
    private final static Logger LOGGER = LoggerUtil.getLogger();
    private final List<Argument> arguments = new ArrayList<>();
    private final IrType returnType;
    private final Module module; //所属module
    private final List<BasicBlock> blockList = new ArrayList<>();
    private BasicBlock entryBlock;

    IrFunction(IrType type, String name, Module module) {
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
        return this.entryBlock;
    }

    void setEntryBlock(BasicBlock entryBlock) {
        assert getBlockList().isEmpty();
        this.entryBlock = entryBlock;
        addBlock(entryBlock);
    }

    List<BasicBlock> getBlockList() {
        return this.blockList;
    }

    BasicBlock getBlock(int i) {
        return this.blockList.get(i);
    }

    BasicBlock getLastBlock() {
        return getBlockList().get(getBlockList().size() - 1);
    }

    public void addBlock(BasicBlock block) {
        this.getBlockList().add(block);
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
        if (getLastBlock().getLastInstruction() == null || !(getLastBlock().getLastInstruction() instanceof RetInstruction)) {
            if (returnType.getBasicType() == IrType.IrTypeID.VoidTyID) {
                getLastBlock().addInstruction(new RetInstruction());
            } else {
                getLastBlock().addInstruction(new RetInstruction(new ConstValue(0, IrType.IrTypeID.Int32TyID)));
            }
        }
        //检查br
        for (int i = 0; i < getBlockList().size(); i++) {
            var now = getBlock(i);
            if (i < getBlockList().size() - 1 && !now.endWithRet() && !now.endWithBr()) {
                //补上br语句
                var nextBlk = getBlock(i + 1);
//                now.addInstruction(new BrInstruction(nextBlk));
                LOGGER.warning("Basic block " + now.getName() + " isn't end with br. Add br instruction to " + nextBlk.getName());
            }
            sb.append(now.toIrCode());
        }

        //如果没有ret，则需要补上ret，否则过不了llvm的编译。那就默认ret void / ret i32 0
        sb.append("}");
        return sb.toString();
    }

}
