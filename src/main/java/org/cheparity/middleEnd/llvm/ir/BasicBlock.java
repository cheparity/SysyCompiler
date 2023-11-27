package middleEnd.llvm.ir;

import middleEnd.symbols.SymbolTable;

import java.util.ArrayList;
import java.util.List;

public class BasicBlock extends Value {
    private final List<BasicBlock> predecessors = new ArrayList<>();
    private final List<BasicBlock> successors = new ArrayList<>();
    private final List<Instruction> instructionList = new ArrayList<>();


    /**
     * 应该在{@link middleEnd.llvm.visitor.BlockVisitor}里设置
     */
    private SymbolTable symbolTable;

    BasicBlock(String name) {
        super(IrType.create(IrType.IrTypeID.LabelTyID), name);
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    /**
     * <font color='red'>不应该在IrBuilder里调用！</font>应该在{@link middleEnd.llvm.visitor.BlockVisitor}里设置
     *
     * @param symbolTable 符号表
     */
    public void setSymbolTable(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
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
        return getInstructionList().get(getInstructionList().size() - 1);
    }

    @Override
    public String toIrCode() {
        var sb = new StringBuilder();
        getInstructionList().forEach(inst -> {


            if (inst instanceof LabelInstruction) {
                //todo 如果是，则应该跳转到这个label的successor里打印？
                sb.append(inst.toIrCode()).append("\n");
            } else {
                sb.append('\t').append(inst.toIrCode()).append("\n");
            }
        });
        return sb.toString();
    }

}
