package middleEnd.llvm.ir;

import middleEnd.os.MipsPrintable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Module extends Value implements MipsPrintable {
    List<IrFunction> irFunctions = new ArrayList<>();
    List<GlobalValue> globalValues = new ArrayList<>();
    List<Instruction> globalInstructions = new ArrayList<>();

    Module() {
        super(IrType.create(IrType.IrTypeID.VoidTyID));
    }

    void insertGlobal(GlobalValue globalValue) {
        globalValues.add(globalValue);
    }

    Module insertGlobalInst(Instruction... instruction) {
        this.globalInstructions.addAll(Arrays.asList(instruction));
        return this;
    }

    Module insertFunc(IrFunction irFunction) {
        irFunctions.add(irFunction);
        return this;
    }

    IrFunction getFunc(String name) {
        for (var func : irFunctions) {
            if (func.getName().equals(name)) {
                return func;
            }
        }
        return null;
    }

    @Override
    public String toIrCode() {
        StringBuilder sb = new StringBuilder();
        for (var gloInst : globalInstructions) {
            sb.append(gloInst.toIrCode()).append("\n");
        }
        sb.append("\n");
        for (IrFunction irFunction : irFunctions) {
            if (irFunction.getEntryBlock() == null) continue; //decl的函数
            sb.append(irFunction.toIrCode()).append("\n");
        }
        return sb.toString();
    }

    @Override
    public String toMipsCode() {
        StringBuilder sb = new StringBuilder();
        sb.append(".data:\n");
        for (var gloInst : globalInstructions) {
            if (gloInst instanceof GlobalDeclInstruction) {
                sb.append('\t').append(gloInst.toMipsCode()).append("\n");
            }
        }
        sb.append('\n');
        for (IrFunction irFunction : irFunctions) {
            if (irFunction.getEntryBlock() == null) continue; //decl的函数
            sb.append(irFunction.toMipsCode()).append("\n");
        }
        sb.append('\n');
        sb.append(".text\n\tjal main\n\tli $v0 10\n\tsyscall");
        return sb.toString();
    }
}
