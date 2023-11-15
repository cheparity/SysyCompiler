package middleEnd.visitor.llvm.ir;

import java.util.ArrayList;
import java.util.List;

public class BasicBlock extends Value {
    private List<Instruction> instructionList = new ArrayList<>();

    BasicBlock(String name) {
        super(IrType.LabelTyID, name);
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
