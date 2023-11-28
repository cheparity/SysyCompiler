package middleEnd.llvm.ir;

import middleEnd.symbols.SymbolTable;

import java.util.ArrayList;
import java.util.List;

public class BasicBlock extends Value {
    private final List<BasicBlock> predecessors = new ArrayList<>();
    private final List<BasicBlock> successors = new ArrayList<>();
    private final List<Instruction> instructionList = new ArrayList<>();
    /**
     * block所属的function。如果不是entry block，则为null
     */
    private Function function;
    /**
     * 应该在{@link middleEnd.llvm.visitor.BlockVisitor}里设置
     */
    private SymbolTable symbolTable;

    BasicBlock(String name) {
        super(IrType.create(IrType.IrTypeID.LabelTyID), name);
    }

    public Function getFunction() {
        return function;
    }

    void setFunction(Function function) {
        this.function = function;
    }


    boolean isEntryBlock() {
        if (getFunction() == null) return false;
        return getFunction().getEntryBlock() == this;
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    /**
     * <font color='red'>不应该在IrBuilder里调用！</font>应该在{@link middleEnd.llvm.visitor.BlockVisitor}里设置
     *
     * @param symbolTable 符号表
     */
    public BasicBlock setSymbolTable(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        return this;
    }

    public List<BasicBlock> getPredecessors() {
        return predecessors;
    }

    public List<BasicBlock> getSuccessors() {
        return successors;
    }

    void addPredecessor(BasicBlock predecessor) {
        getPredecessors().add(predecessor);
    }

    void addSuccessor(BasicBlock successor) {
        getSuccessors().add(successor);
    }

    void addInstruction(Instruction instruction) {
        getInstructionList().add(instruction);
    }


    List<Instruction> getInstructionList() {
        return this.instructionList;
    }

    Instruction getLastInstruction() {
        if (getInstructionList().isEmpty()) return null;
        return getInstructionList().get(getInstructionList().size() - 1);
    }

    @Override
    public String toIrCode() {
        var sb = new StringBuilder();
        //如果不是entryBlock，则需要打印label
        if (!isEntryBlock())
            sb.append(getName().substring(1)).append(":\n");
        getInstructionList().forEach(inst -> sb.append('\t').append(inst.toIrCode()).append("\n"));
        sb.append("\n");
        return sb.toString();
    }

}
