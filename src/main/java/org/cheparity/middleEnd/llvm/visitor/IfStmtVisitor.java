package middleEnd.llvm.visitor;

import frontEnd.parser.dataStruct.ASTNode;
import middleEnd.ASTNodeVisitor;

public final class IfStmtVisitor implements ASTNodeVisitor {
    @Override
    //Stmt -> 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
    public void visit(ASTNode ifStmt) {
//        BasicBlock ifTrueBlk, finalBlk, elseBlk = null;
//        final boolean hasElseStmt = ifStmt.deepDownFind(GrammarType.ELSE, 1).isPresent();
//        final ASTNode condNode = ifStmt.getChild(2);
//        final ASTNode ifTrueNodeStmt = IrUtil.wrapStmtAsBlock(ifStmt.getChild(4), symbolTable);
//        //处理Cond
//        NodeUnion condUnion = new IrUtil(builder, basicBlock).calcLogicExp(condNode);
//        if (condUnion.isNum) {
//            int number = condUnion.getNumber();
//            LOGGER.fine("Meet [ " + number + " ]. Jump to ifTrueBlk or elseBlk directly!");
//            //如果是1，则直接接着解析ifTrueBlk里的东西；如果是0且有else，直接建立ElseBlk，如果是0且无else，直接跳过（return）
//            if (number == 1) {
//                visitAnonymousBlock(ifTrueNodeStmt);
//                return;
//            }
//            if (hasElseStmt) {
//                var elseStmt = ifStmt.getChild(-1);
//                visitAnonymousBlock(elseStmt);
//            }
//            return;
//        }
//        //处理各个block
//        Variable cond = condUnion.getVariable();
//        //需要把stmt包装为block => 需要新建一个block
//        ifTrueBlk = visitControlFlowBlock(ifTrueNodeStmt);
//        if (hasElseStmt) {
//            ASTNode elseStmt = IrUtil.wrapStmtAsBlock(ifStmt.getChild(-1), symbolTable);
//            elseBlk = visitControlFlowBlock(elseStmt);
//            //需要给elseBlk增加br语句
//        }
//        finalBlk = builder.buildBasicBlock(basicBlock, basicBlock.getSymbolTable());//新建一个基本块
//
//        //在全部解析完ifTrueBlk, finalBlk, elseBlk之后，才可以buildBrInst
//        if (elseBlk != null) {
//            builder.buildBrInst(basicBlock, cond, ifTrueBlk, elseBlk); //entryBlock 根据 条件 -> ifTrueBlk | elseBlk
//            builder.buildBrInst(elseBlk, finalBlk); //elseBlk -> finalBlk
//        } else {
//            builder.buildBrInst(basicBlock, cond, ifTrueBlk, finalBlk); //entryBlock 根据 条件 -> ifTrueBlk | finalBlk
//            assert ifTrueBlk != null;
//            builder.buildBrInst(ifTrueBlk, finalBlk); //ifTrueBlk -> finalBlk
//        }
//        //这里应该是调用者的basicBlock = finalBlock？
//        // 其实可以通过传递callee的方式传递过来
//        callee.setBasicBlock(finalBlk);
    }
}

