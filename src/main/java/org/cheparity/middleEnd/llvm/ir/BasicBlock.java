package middleEnd.llvm.ir;

import middleEnd.symbols.SymbolTable;

import java.util.ArrayList;
import java.util.LinkedList;

public class BasicBlock extends Value {
    private final LinkedList<BasicBlock> predecessors = new LinkedList<>();
    private final LinkedList<BasicBlock> successors = new LinkedList<>();
    private final LinkedList<Instruction> instructionList = new LinkedList<>();
    /**
     * tag是为了方便查找某些块。比如，在for循环里需要查找forStmt2块等
     */
    private final ArrayList<String> tags = new ArrayList<>();
    /**
     * block所属的function。如果不是entry block，则为null
     */
    private IrFunction irFunction;
    /**
     * 应该在{@link middleEnd.llvm.visitor.BlockVisitor}里设置
     */
    private SymbolTable symbolTable;

    BasicBlock(String name) {
        super(IrType.create(IrType.IrTypeID.LabelTyID), name);
    }

    public boolean withTag(String... tag) {
        for (String t : tag) {
            if (!tags.contains(t)) return false;
        }
        return true;
    }

    /**
     * 递归查找满足tag的前驱块
     *
     * @param tag 标签
     * @return <font color='red'>第一个</font>满足tag的前驱块
     */
    public BasicBlock findPreWithTag(String tag) {
        if (getPredecessors().isEmpty()) return null;
        for (BasicBlock blk : getPredecessors()) {
            if (blk.withTag(tag)) return blk;
            BasicBlock res = blk.findPreWithTag(tag);
            if (res != null) return res;
        }
        return null;
    }

    /**
     * 递归查找满足tag的后继块
     *
     * @param tag 标签
     * @return <font color='red'>第一个</font>满足tag的后继块
     */
    public BasicBlock findSucWithTag(String tag) {
        if (getSuccessors().isEmpty()) return null;
        for (BasicBlock blk : getSuccessors()) {
            if (blk.withTag(tag)) return blk;
            BasicBlock res = blk.findSucWithTag(tag);
            if (res != null) return res;
        }
        return null;
    }

    public BasicBlock setTag(String tag) {
        this.tags.add(tag);
        return this;
    }

    public IrFunction getFunction() {
        return irFunction;
    }

    void setFunction(IrFunction irFunction) {
        this.irFunction = irFunction;
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

    public LinkedList<BasicBlock> getPredecessors() {
        return predecessors;
    }

    public LinkedList<BasicBlock> getSuccessors() {
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


    LinkedList<Instruction> getInstructionList() {
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
        if (!isEntryBlock()) {
            sb.append(getName().substring(1)).append(":");
            if (!tags.isEmpty()) {
                sb.append("\t; ");
                for (String tag : this.tags) {
                    sb.append(tag).append(" ");
                }
            }
            sb.append("\n");
        }
        getInstructionList().forEach(inst -> sb.append('\t').append(inst.toIrCode()).append("\n"));
        sb.append("\n");
        return sb.toString();
    }

    /**
     * 从后继块中删除块
     *
     * @param blk2drop 待删除的块
     */
    public void removeBlock(BasicBlock blk2drop) {
        getFunction().getBlockList().remove(blk2drop);
        successors.remove(blk2drop);
    }

    public boolean endWithBr() {
        if (this.getInstructionList().isEmpty()) return false;
        return this.getInstructionList().getLast() instanceof BrInstruction;
    }

    public boolean endWithRet() {
        if (this.getInstructionList().isEmpty()) return false;
        return this.getInstructionList().getLast() instanceof RetInstruction;
    }
}
