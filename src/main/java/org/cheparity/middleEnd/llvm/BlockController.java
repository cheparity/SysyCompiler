package middleEnd.llvm;

import middleEnd.llvm.ir.BasicBlock;

public interface BlockController {
    void updateVisitingBlk(BasicBlock basicBlock);
}
