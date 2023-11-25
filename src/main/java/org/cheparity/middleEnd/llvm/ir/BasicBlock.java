package middleEnd.llvm.ir;

import middleEnd.symbols.SymbolTable;

import java.util.ArrayList;
import java.util.List;

public class BasicBlock extends Value {
    final List<Instruction> instructionList = new ArrayList<>();
    final List<BasicBlock> predecessors = new ArrayList<>();
    final List<BasicBlock> successors = new ArrayList<>();
    private SymbolTable symbolTable;

    public BasicBlock(String name) {
        super(IrType.create(IrType.IrTypeID.LabelTyID), name);
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    public void setSymbolTable(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    void addInstruction(Instruction instruction) {
        instructionList.add(instruction);
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
