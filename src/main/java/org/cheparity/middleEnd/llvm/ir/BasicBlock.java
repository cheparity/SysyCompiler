package middleEnd.llvm.ir;

import frontEnd.symbols.SymbolTable;

import java.util.ArrayList;
import java.util.List;

public final class BasicBlock extends Value {
    private List<Instruction> instructionList = new ArrayList<>();
    private SymbolTable symbolTable;

    BasicBlock(String name) {
        super(IrType.LabelTyID, name);
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
