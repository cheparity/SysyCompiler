package middleEnd.visitor.llvm;

import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.parser.dataStruct.GrammarType;

public class IrUtil {
    static int CalculateConst(ASTNode node) {
        switch (node.getGrammarType()) {
            case EXP, NUMBER -> {
                // Exp -> AddExp
                return CalculateConst(node.getChild(0));
            }
            case INT_CONST -> {
                return Integer.parseInt(node.getRawValue());
            }
            case ADD_EXP -> {
                // AddExp -> MulExp | AddExp '+' MulExp | AddExp '-' MulExp
                // AddExp -> MulExp {('+'|'-') MulExp}
                if (node.getChildren().size() == 1) {
                    return CalculateConst(node.getChild(0));
                }
                var addRes = CalculateConst(node.getChild(0));
                for (int i = 1; i < node.getChildren().size(); i += 2) {
                    var plusOrMinus = node.getChild(i).getGrammarType();
                    var mulRes = CalculateConst(node.getChild(i + 1));
                    addRes = (plusOrMinus == GrammarType.PLUS) ? (addRes + mulRes) : (addRes - mulRes);
                }
                return addRes;
            }
            case MUL_EXP -> {
                // MulExp -> UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
                // MulExp -> UnaryExp { ('*' | '/' | '%') UnaryExp }
                if (node.getChildren().size() == 1) {
                    return CalculateConst(node.getChild(0));
                }
                var mulRes = CalculateConst(node.getChild(0));
                for (int i = 1; i < node.getChildren().size(); i += 2) {
                    var mulOrDivOrMod = node.getChild(i).getGrammarType();
                    var unaryRes = CalculateConst(node.getChild(i + 1));
                    switch (mulOrDivOrMod) {
                        case MULTIPLY -> {
                            mulRes *= unaryRes;
                        }
                        case DIVIDE -> {
                            mulRes /= unaryRes;
                        }
                        case MOD -> {
                            mulRes %= unaryRes;
                        }
                        default -> throw new RuntimeException("Unexpected grammar type: " + mulOrDivOrMod);
                    }
                }
                return mulRes;
            }
            case UNARY_EXP -> {
                //UnaryExp -> Ident '(' [FuncRParams] ')' | PrimaryExp | UnaryOp UnaryExp
                if (node.getChildren().size() == 1) {
                    return CalculateConst(node.getChild(0));
                }
                if (node.getChildren().size() == 2) {
//                    UnaryOp -> '+' | '−' | '!'
                    var unaryOp = node.getChild(0).getChild(0).getGrammarType();
                    var unaryExp = node.getChild(1);
                    var unaryExpRes = CalculateConst(unaryExp);
                    switch (unaryOp) {
                        case PLUS -> {
                            return unaryExpRes;
                        }
                        case MINUS -> {
                            return -unaryExpRes;
                        }
                        case NOT -> {
                            return unaryExpRes == 0 ? 1 : 0;
                        }
                        default -> throw new RuntimeException("Unexpected grammar type: " + unaryOp);
                    }
                }
                if (node.getChildren().size() == 3) {
                    //Ident '(' [FuncRParams] ')'
                    var funcName = node.getChild(0).getRawValue();
                    var funcRParams = node.getChild(2);
                    //todo 查表找到函数，计算参数，返回结果
                }
            }
            case PRIMARY_EXP -> {
//                PrimaryExp ->  LVal | Number | '(' Exp ')'
                if (node.getChildren().size() == 1) {
                    return CalculateConst(node.getChild(0));
                }
                if (node.getChildren().size() == 3) {
                    return CalculateConst(node.getChild(1));
                }
            }
            case LVAL -> {
                //Ident {'[' Exp ']'}
                // todo 查表找到变量，计算偏移量，返回结果
            }
            default -> throw new RuntimeException("Unexpected grammar type: " + node.getGrammarType());

        }
        return 0;
    }
}
