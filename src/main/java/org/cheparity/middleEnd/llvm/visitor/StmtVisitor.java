package middleEnd.llvm.visitor;

import frontEnd.parser.dataStruct.ASTLeaf;
import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.parser.dataStruct.GrammarType;
import middleEnd.ASTNodeVisitor;
import middleEnd.llvm.NodeUnion;
import middleEnd.llvm.ir.BasicBlock;
import middleEnd.llvm.ir.IrBuilder;
import middleEnd.llvm.ir.PointerValue;
import middleEnd.llvm.ir.Variable;
import middleEnd.symbols.Symbol;
import middleEnd.symbols.SymbolTable;

import java.util.List;

public final class StmtVisitor implements ASTNodeVisitor {
    private final BasicBlock basicBlock;
    private final IrBuilder builder;
    private final SymbolTable symbolTable;

    public StmtVisitor(BasicBlock basicBlock, IrBuilder builder) {
        this.basicBlock = basicBlock;
        this.builder = builder;
        this.symbolTable = basicBlock.getSymbolTable();
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
            //焯！如果没有return，还要补上，否则过不了llvm的编译。那就默认ret void / ret i32 0（不在这里做，在Function里做）
            if (stmt.getChildren().size() == 2) {
                //没有exp的情况，直接build空返回语句。
                builder.buildVoidRetInst(basicBlock);
                return;
            }
            var res = new IrUtil(builder, basicBlock).calc(stmt.getChild(1));
            if (res.isNum) builder.buildRetInstOfConst(basicBlock, res.getNumber());
            else builder.buildRetInstOfConst(basicBlock, res.getVariable());
        }
        //Stmt -> Block
        else if (stmt.getChild(0).getGrammarType() == GrammarType.BLOCK) {
            //语法检查阶段时已经确保语法没问题了，即不会出现引用块内变量的情况。
            stmt.accept(new BlockVisitor(basicBlock, builder));
        }
        //Stmt -> LVal '=' 'getint''('')'';'
        else if (stmt.deepDownFind(GrammarType.GETINT, 1).isPresent()) {
            var lval = stmt.getChild(0);
            String lvalName = lval.getChild(0).getRawValue();
            assert basicBlock.getSymbolTable().getSymbol(lvalName).isPresent();
            Symbol symbol = basicBlock.getSymbolTable().getSymbol(lvalName).get();
            //给symbol重新赋值
            Variable variable = builder.buildCallInst(basicBlock, "getint");
            builder.buildStoreInst(basicBlock, variable, symbol.getPointer());
        }
        //stmt -> 'printf''('FormatString{','Exp}')'';'
        else if (stmt.getChild(0).getGrammarType() == GrammarType.PRINTF) {
            var formatString = ((ASTLeaf) stmt.getChild(2)).getToken().getRawValue();
            List<ASTNode> exps = stmt.getChildren().stream().filter(node -> node.getGrammarType() == GrammarType.EXP).toList();
            var args = new Variable[exps.size()];
            for (int i = 0; i < exps.size(); i++) {
                var res = new IrUtil(builder, basicBlock).calc(exps.get(i));
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
        //Stmt -> LVal '=' Exp ';'
        else if (stmt.getChild(0).getGrammarType() == GrammarType.LVAL) {
            //LVal -> Ident {'[' Exp ']'}
            assert symbolTable.getSymbol(stmt.getChild(0).getRawValue()).isPresent();
            Symbol symbol = symbolTable.getSymbol(stmt.getChild(0).getRawValue()).get();
            PointerValue pointer = symbol.getPointer(); //a:%1
            assert pointer != null;
            NodeUnion result = new IrUtil(builder, basicBlock).calc(stmt.getChild(2));
            if (result.isNum) {
                builder.buildStoreInst(basicBlock, builder.buildConstIntNum(result.getNumber()), pointer);
            } else {
                builder.buildStoreInst(basicBlock, result.getVariable(), pointer);
            }
        }
        //Stmt -> Exp ';'
        else if (stmt.getChild(0).getGrammarType() == GrammarType.EXP) {
            new IrUtil(builder, basicBlock).calc(stmt.getChild(0));
        }
    }
}
