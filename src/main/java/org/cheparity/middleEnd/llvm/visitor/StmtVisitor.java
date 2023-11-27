package middleEnd.llvm.visitor;

import frontEnd.parser.dataStruct.ASTLeaf;
import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.parser.dataStruct.GrammarType;
import middleEnd.ASTNodeVisitor;
import middleEnd.llvm.ir.EntryBlock;
import middleEnd.llvm.ir.IrBuilder;
import middleEnd.llvm.ir.PointerValue;
import middleEnd.llvm.ir.Variable;
import middleEnd.llvm.utils.NodeUnion;
import middleEnd.symbols.Symbol;
import middleEnd.symbols.SymbolTable;

import java.util.List;

public final class StmtVisitor implements ASTNodeVisitor {
    private final EntryBlock basicBlock;
    private final IrBuilder builder;
    private final SymbolTable symbolTable;

    public StmtVisitor(EntryBlock basicBlock, IrBuilder builder) {
        this.basicBlock = basicBlock;
        this.builder = builder;
        this.symbolTable = basicBlock.getSymbolTable();
    }

    /**
     * 为了处理匿名块的情况，需要把<font color='red'>匿名块里的符号表</font>单独传递过来。
     *
     * @param basicBlock  基本块
     * @param builder     IrBuilder
     * @param symbolTable 匿名块里的符号表
     */
    public StmtVisitor(BasicBlock basicBlock, IrBuilder builder, SymbolTable symbolTable) {
        this.basicBlock = basicBlock;
        this.builder = builder;
        this.symbolTable = symbolTable;
    }

    /**
     * Stmt ->
     * <p>
     * LVal '=' Exp ';'
     * <p>
     * | LVal '=' 'getint''('')'';'
     * <p>
     * | [Exp] ';'
     * <p>
     * | Block
     * <p>
     * | 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
     * <p>
     * | 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
     * <p>
     * | 'break' ';'
     * <p>
     * | 'continue' ';'
     * <p>
     * | 'return' [Exp] ';'
     * <p>
     * | 'printf''('FormatString{','Exp}')'';'
     */
    @Override
    public void visit(ASTNode stmt) {
        //stmt -> 'return' Exp ';'
        if (stmt.getChild(0).getGrammarType() == GrammarType.RETURN) {
            visitRetStmt(stmt);
        }
        //Stmt -> LVal '=' 'getint''('')'';'
        else if (stmt.deepDownFind(GrammarType.GETINT, 1).isPresent()) {
            visitGetintStmt(stmt);
        }
        //stmt -> 'printf''('FormatString{','Exp}')'';'
        else if (stmt.getChild(0).getGrammarType() == GrammarType.PRINTF) {
            visitPrintfStmt(stmt);
        }
        //Stmt -> LVal '=' Exp ';'
        else if (stmt.getChild(0).getGrammarType() == GrammarType.LVAL) {
            visitLvalStmt(stmt);
        }
        //Stmt -> Exp ';'
        else if (stmt.getChild(0).getGrammarType() == GrammarType.EXP) {
            new IrUtil(builder, basicBlock).calcAloExp(stmt.getChild(0));
        }
        //Stmt -> 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
        else if (stmt.getChild(0).getGrammarType() == GrammarType.IF) {
            visitIfStmt(stmt);
        }
        //Stmt -> Block 如果是匿名块，不要用blockVisitor解析。如果是控制流的块，则用blockVisitor解析
        else if (stmt.getChild(0).getGrammarType() == GrammarType.BLOCK) {
            //语法检查阶段时已经确保语法没问题了，即不会出现引用块内变量的情况。
            visitBlock(stmt);
        }
    }

    private void visitBlock(ASTNode blockStmt) {
        //build嵌套块（非函数入口块）
        //此时有两种情况：1. 匿名块 2. 从控制流语句过来的块。特点:控制流过来的块，其兄弟必有if或者for
        GrammarType brotherGraTy = blockStmt.getFather().getChild(0).getGrammarType();
        if (brotherGraTy == GrammarType.IF || brotherGraTy == GrammarType.FOR) {
            var newBlk = builder.buildBasicBlock(basicBlock);
            blockStmt.accept(new BlockVisitor(newBlk, builder));
            return;
        }
        // 匿名块。注意调用的**构造函数**是不一样的
        for (var child : blockStmt.getChild(0).getChildren()) {
            if (child.getGrammarType() != GrammarType.BLOCK_ITEM) continue;
            SymbolTable blockST = blockStmt.getChild(0).getSymbolTable();
            assert blockST != null;
            child.accept(new LocalVarVisitor(basicBlock, builder, blockST));
            child.accept(new StmtVisitor(basicBlock, builder, blockST));
        }
    }

    private void visitLvalStmt(ASTNode lvalStmt) {
        //LVal -> Ident {'[' Exp ']'}
        assert symbolTable.getSymbol(lvalStmt.getChild(0).getRawValue()).isPresent();
        Symbol symbol = symbolTable.getSymbol(lvalStmt.getChild(0).getRawValue()).get();
        PointerValue pointer = symbol.getPointer(); //a:%1
        assert pointer != null;
        NodeUnion result = new IrUtil(builder, basicBlock).calcAloExp(lvalStmt.getChild(2));
        if (result.isNum) {
            builder.buildStoreInst(basicBlock, builder.buildConstIntNum(result.getNumber()), pointer);
        } else {
            builder.buildStoreInst(basicBlock, result.getVariable(), pointer);
        }
    }

    private void visitPrintfStmt(ASTNode printStmt) {
        var formatString = ((ASTLeaf) printStmt.getChild(2)).getToken().getRawValue();
        List<ASTNode> exps = printStmt.getChildren().stream().filter(node -> node.getGrammarType() == GrammarType.EXP).toList();
        var args = new Variable[exps.size()];
        for (int i = 0; i < exps.size(); i++) {
            var res = new IrUtil(builder, basicBlock).calcAloExp(exps.get(i));
            if (res.isNum) args[i] = builder.buildConstIntNum(res.getNumber());
            else args[i] = res.getVariable();
        }
        //解析formatString
        char[] chars = formatString.toCharArray();
        int argCnt = 0;
        for (int i = 1; i < chars.length - 1; i++) {
            if (chars[i] == '%' && chars[i + 1] == 'd') {
                builder.buildCallInst(basicBlock, "putint", args[argCnt]);
                argCnt++;
                i++;
            } else if (chars[i] == '\\' && chars[i + 1] == 'n') {
                builder.buildCallInst(basicBlock, "putch", builder.buildConstIntNum(10));
                i++;
            } else {
                builder.buildCallInst(basicBlock, "putch", builder.buildConstIntNum(chars[i]));
            }
        }
    }

    private void visitGetintStmt(ASTNode getintStmt) {
        var lval = getintStmt.getChild(0);
        String lvalName = lval.getChild(0).getRawValue();
        assert basicBlock.getSymbolTable().getSymbol(lvalName).isPresent();
        Symbol symbol = basicBlock.getSymbolTable().getSymbol(lvalName).get();
        //给symbol重新赋值
        Variable variable = builder.buildCallInst(basicBlock, "getint");
        builder.buildStoreInst(basicBlock, variable, symbol.getPointer());
    }

    private void visitRetStmt(ASTNode retStmt) {
        //如果没有return，还要补上，否则过不了llvm的编译。那就默认ret void / ret i32 0（不在这里做，在Function里做）
        if (retStmt.getChildren().size() == 2) {
            //没有exp的情况，直接build空返回语句。
            builder.buildVoidRetInst(basicBlock);
            return;
        }
        var res = new IrUtil(builder, basicBlock).calcAloExp(retStmt.getChild(1));
        if (res.isNum) builder.buildRetInstOfConst(basicBlock, res.getNumber());
        else builder.buildRetInstOfConst(basicBlock, res.getVariable());
    }

    //Stmt -> 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
    private void visitIfStmt(ASTNode ifStmt) {
        //Cond -> LOrExp
        //LOrExp -> LAndExp | LOrExp '||' LAndExp
        //LAndExp -> EqExp | LAndExp '&&' EqExp
        //EqExp -> RelExp | EqExp ('==' | '!=') RelExp
        //RelExp -> AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
        ASTNode condNode = ifStmt.getChild(2);
        ASTNode ifTrueNode = ifStmt.getChild(4);

        NodeUnion union = new IrUtil(builder, basicBlock).calcLogicExp(condNode);
        if (union.isNum) {
            //todo 如果是数字，表明可以直接判断，就不用去建立if语句结构
            throw new RuntimeException("Not implement!");
        }
        Variable variable = union.getVariable();
        visit(ifTrueNode);
        if (ifStmt.deepDownFind(GrammarType.ELSE, 1).isPresent()) {
            ASTNode elseStmt = ifStmt.getChild(-1);
            visit(elseStmt);
        }
    }
}
