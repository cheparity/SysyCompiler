package middleEnd.llvm.ir;

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
}
