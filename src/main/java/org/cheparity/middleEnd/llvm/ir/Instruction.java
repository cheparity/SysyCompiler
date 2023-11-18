package middleEnd.llvm.ir;

public abstract class Instruction extends User {

    Instruction() {
        super(IrType.create(IrType.IrTypeID.VoidTyID));
    }

}
