package middleEnd.llvm.ir;

abstract class Instruction extends User {

    Instruction() {
        super(IrType.create(IrType.IrTypeID.VoidTyID));
    }

}
