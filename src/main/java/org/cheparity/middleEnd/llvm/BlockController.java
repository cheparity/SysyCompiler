package middleEnd.llvm;

import frontEnd.parser.dataStruct.ASTNode;
import middleEnd.llvm.ir.BasicBlock;

public interface BlockController {
    void updateVisitingBlk(BasicBlock basicBlock);

    ASTNode getVisitingNode();
}
