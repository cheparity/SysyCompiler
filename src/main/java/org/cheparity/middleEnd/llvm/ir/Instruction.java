package middleEnd.llvm.ir;

import middleEnd.llvm.MipsRegisterAllocator;
import middleEnd.os.MipsPrintable;

abstract class Instruction extends User implements MipsPrintable {
    protected IrFunction function;

    Instruction() {
        super(IrType.create(IrType.IrTypeID.VoidTyID));
    }

    @Override
    public String toString() {
        return this.toIrCode();
    }

    void setFunction(IrFunction function) {
        this.function = function;
    }

    MipsRegisterAllocator getMipsRegisterAllocator() {
        return this.function.mipsRegisterAllocator;
    }
}
