package middleEnd.llvm.ir;

import middleEnd.symbols.SymbolTable;

import java.util.ArrayList;
import java.util.List;

public class BasicBlock extends Value {
    private final List<Instruction> instructionList = new ArrayList<>();
    private final List<NestBlock> nestBlockList = new ArrayList<>();
    private SymbolTable symbolTable;
    private Function entryFunc;

    BasicBlock(String name) {
        super(IrType.create(IrType.IrTypeID.LabelTyID), name);
    }

    protected void addNestBlock(NestBlock block) {
        this.nestBlockList.add(block);
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

    @Override
    public String toIrCode() {
        var sb = new StringBuilder();
        for (var instruction : instructionList) {
            sb.append("\t").append(instruction.toIrCode()).append("\n");
        }
        return sb.toString();
    }

}
