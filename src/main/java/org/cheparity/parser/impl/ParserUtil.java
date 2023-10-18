package parser.impl;

import parser.dataStruct.ASTLeaf;
import parser.dataStruct.ASTNode;
import parser.dataStruct.GrammarType;
import parser.dataStruct.symbol.*;

import java.util.Optional;

public class ParserUtil {

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
        if (child.getGrammarType().equals(GrammarType.IDENTIFIER)) {
            Optional<Symbol> symbol = table.lookup(((ASTLeaf) child).getToken().getRawValue());
            if (symbol.isPresent() && symbol.get().getType().equals(SymbolType.FUNC)) {
                return ((FuncSymbol) symbol.get()).getDim();
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
        assert ident.getGrammarType().equals(GrammarType.IDENTIFIER);
        Optional<Symbol> symbol = table.lookup(ident.getToken().getRawValue());
        if (symbol.isPresent()) {
            if (isArr) return ((VarSymbol) symbol.get()).getDim() - 1;
            else return ((VarSymbol) symbol.get()).getDim();
        }
        return -1;
    }
}
