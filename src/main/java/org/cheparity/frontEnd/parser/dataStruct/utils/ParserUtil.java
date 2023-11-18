package frontEnd.parser.dataStruct.utils;

import frontEnd.parser.dataStruct.ASTLeaf;
import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.parser.dataStruct.GrammarType;
import middleEnd.symbols.*;

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

}
