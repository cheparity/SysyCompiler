package middleEnd.llvm.ir;

import middleEnd.symbols.SymbolTable;

import java.util.ArrayList;
import java.util.List;

public class BasicBlock extends Value {
    final List<NestedBlock> nestedBlocks = new ArrayList<>();
    private final List<Instruction> instructionList = new ArrayList<>();
    private SymbolTable symbolTable;
    private Function entryFunc;

    BasicBlock(String name) {
        super(IrType.create(IrType.IrTypeID.LabelTyID), name);
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    public void setSymbolTable(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    public Function getEntryFunc() {
        return this.entryFunc;
    }

    void setEntryFunc(Function function) {
        this.entryFunc = function;
    }

    void addInstruction(Instruction instruction) {
        instructionList.add(instruction);
    }

    Instruction getLastInstruction() {
        return instructionList.get(instructionList.size() - 1);
    }

    @Override
    public String toIrCode() {
        var sb = new StringBuilder();
        for (var instruction : instructionList) {
            sb.append("\t").append(instruction.toIrCode()).append("\n");
        }
        return sb.toString();
    }

}
