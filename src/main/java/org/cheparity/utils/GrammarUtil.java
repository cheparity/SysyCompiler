package utils;

import frontEnd.lexer.dataStruct.Token;
import frontEnd.parser.dataStruct.ASTLeaf;
import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.parser.dataStruct.GrammarType;
import middleEnd.symbols.*;

import java.util.List;
import java.util.Optional;

public final class GrammarUtil {

    //Exp → AddExp
    public static int getExpDim(ASTNode exp, SymbolTable table) {
        assert exp.getGrammarType().equals(GrammarType.EXP);
        ASTNode child = exp.getChildren().get(0);
        if (child.getGrammarType().equals(GrammarType.ADD_EXP)) {
            return getAddExpDim(child, table);
        }
        return -1;
    }

    //AddExp → MulExp | AddExp ('+' | '−') MulExp
    private static int getAddExpDim(ASTNode addExp, SymbolTable table) {
        assert addExp.getGrammarType().equals(GrammarType.ADD_EXP);
        ASTNode child = addExp.getChildren().get(0);
        if (child.getGrammarType().equals(GrammarType.MUL_EXP)) {
            return getMulExpDim(child, table);
        } else if (child.getGrammarType().equals(GrammarType.ADD_EXP)) {
            return getAddExpDim(child, table);
        }
        return -1;
    }

    //MulExp -> UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
    private static int getMulExpDim(ASTNode mulExp, SymbolTable table) {
        assert mulExp.getGrammarType().equals(GrammarType.MUL_EXP);
        ASTNode child = mulExp.getChildren().get(0);
        if (child.getGrammarType().equals(GrammarType.UNARY_EXP)) {
            return getUnaryExpDim(child, table);
        } else if (child.getGrammarType().equals(GrammarType.MUL_EXP)) {
            return getMulExpDim(child, table);
        }
        return -1;
    }

    //UnaryExp → Ident '(' [FuncRParams] ')' | PrimaryExp | UnaryOp UnaryExp
    private static int getUnaryExpDim(ASTNode unaryExp, SymbolTable table) {
        assert unaryExp.getGrammarType().equals(GrammarType.UNARY_EXP);
        ASTNode child = unaryExp.getChildren().get(0);
        if (child.getGrammarType().equals(GrammarType.IDENT)) {
            Optional<Symbol> symbol = table.getSymbol(((ASTLeaf) child).getToken().getRawValue());
            if (symbol.isPresent() && symbol.get().getType().equals(SymbolType.FUNC)) {
                var s = (FuncSymbol) symbol.get();
                if (s.getFuncType() == FuncType.VOID) return -1;
                else return s.getDim();
            }
        } else if (child.getGrammarType().equals(GrammarType.PRIMARY_EXP)) {
            return getPrimaryExpDim(child, table);
        } else if (child.getGrammarType().equals(GrammarType.UNARY_OP)) {
            return getUnaryExpDim(child, table);
        }
        return -1;
    }

    //PrimaryExp → LVal | Number | '(' Exp ')'
    private static int getPrimaryExpDim(ASTNode primaryExp, SymbolTable table) {
        assert primaryExp.getGrammarType().equals(GrammarType.PRIMARY_EXP);
        ASTNode child = primaryExp.getChildren().get(0);
        if (child.getGrammarType().equals(GrammarType.LVAL)) {
            return getLValDim(child, table);
        } else if (child.getGrammarType().equals(GrammarType.NUMBER)) {
            return 0;
        } else if (child.getGrammarType().equals(GrammarType.EXP)) {
            return getExpDim(child, table);
        }
        return -1;
    }

    //LVal → Ident {'[' Exp ']'}
    private static int getLValDim(ASTNode lval, SymbolTable table) {
        assert lval.getGrammarType().equals(GrammarType.LVAL);
        ASTLeaf ident = (ASTLeaf) lval.getChildren().get(0);
        boolean isArr = lval.getChildren().size() > 1;
        assert ident.getGrammarType().equals(GrammarType.IDENT);
        Optional<Symbol> symbol = table.getSymbol(ident.getToken().getRawValue());
        if (symbol.isPresent() && symbol.get().getType().equals(SymbolType.VAR)) {
            if (isArr) return ((VarSymbol) symbol.get()).getDim() - 1;
            else return ((VarSymbol) symbol.get()).getDim();
        } else if (symbol.isPresent() && symbol.get().getType().equals(SymbolType.CONST)) {
            if (isArr) return ((ConstSymbol) symbol.get()).getDim() - 1;
            else return ((ConstSymbol) symbol.get()).getDim();
        }
        return -1;
    }

    // VarDef → Ident { '[' ConstExp ']' } ['=' InitVal]
    // ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal
    public static int getDim4Def(ASTNode def) {
        assert def.getGrammarType().equals(GrammarType.VAR_DEF) || def.getGrammarType().equals(GrammarType.CONST_DEF) || def.getGrammarType().equals(GrammarType.FUNC_FPARAM);
        //judge the number of '['
        int dim = 0;
        for (ASTNode child : def.getChildren()) {
            if (child.getGrammarType().equals(GrammarType.LEFT_BRACKET)) {
                dim++;
            }
        }
        return dim;
    }

    /**
     * 判断一个节点是否在for循环中：递归在父节点中查找，找到一个 child[0]是for的blockItem
     *
     * @param token 节点的token
     * @return 是否在for循环中
     */
    public static boolean isInForBlk(Token token, List<Token> tokens) {
        //毁灭吧！通过文本判断
        int cnt = 0;
        //右进左出，空则不判断
        for (var i = tokens.indexOf(token); i > 0; i--) {
            var nowTk = tokens.get(i);
            switch (nowTk.getLexType()) {
                case RBRACE -> cnt++;
                case LBRACE -> cnt--;
                case FORTK -> {
                    if (cnt != 0) return true; //括号都匹配则什么都不做
                }
            }
        }
        return false;
    }
//    public static boolean isInForBlk(ASTNode node) {
//        if (node.getGrammarType() == GrammarType.STMT &&
//                node.getChild(0) != null &&
//                node.getChild(0).getGrammarType() == GrammarType.FOR) {
//            return true;
//        }
//        if (node.getFather() == null) {
//            return false;
//        }
//        return isInForBlk(node.getFather());
//    }

}
