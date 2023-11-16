package middleEnd.visitor.llvm;

import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.parser.dataStruct.GrammarType;
import frontEnd.symbols.ConstSymbol;
import frontEnd.symbols.Symbol;
import frontEnd.symbols.SymbolTable;
import middleEnd.visitor.llvm.ir.IrBuilder;

public class IrUtil {
    static String CalculateConst(ASTNode node, SymbolTable table) {
        switch (node.getGrammarType()) {
            case EXP, NUMBER -> {
                // Exp -> AddExp
                return CalculateConst(node.getChild(0), table);
            }
            case INT_CONST -> {
                return Integer.parseInt(node.getRawValue());
            }
            case ADD_EXP -> {
                // AddExp -> MulExp | AddExp '+' MulExp | AddExp '-' MulExp
                // AddExp -> MulExp {('+'|'-') MulExp}
                if (node.getChildren().size() == 1) {
                    return CalculateConst(node.getChild(0), table);
                }
                var addRes = CalculateConst(node.getChild(0), table);
                for (int i = 1; i < node.getChildren().size(); i += 2) {
                    var plusOrMinus = node.getChild(i).getGrammarType();
                    var mulRes = CalculateConst(node.getChild(i + 1), table);
                    addRes = (plusOrMinus == GrammarType.PLUS) ? (addRes + mulRes) : (addRes - mulRes);
                }
                return addRes;
            }
            case MUL_EXP -> {
                // MulExp -> UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
                // MulExp -> UnaryExp { ('*' | '/' | '%') UnaryExp }
                if (node.getChildren().size() == 1) {
                    return CalculateConst(node.getChild(0), table);
                }
                var mulRes = CalculateConst(node.getChild(0), table);
                for (int i = 1; i < node.getChildren().size(); i += 2) {
                    var mulOrDivOrMod = node.getChild(i).getGrammarType();
                    var unaryRes = CalculateConst(node.getChild(i + 1), table);
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
                    return CalculateConst(node.getChild(0), table);
                }
                if (node.getChildren().size() == 2) {
//                    UnaryOp -> '+' | '−' | '!'
                    var unaryOp = node.getChild(0).getChild(0).getGrammarType();
                    var unaryExp = node.getChild(1);
                    var unaryExpRes = CalculateConst(unaryExp, table);
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
                    return CalculateConst(node.getChild(0), table);
                }
                if (node.getChildren().size() == 3) {
                    return CalculateConst(node.getChild(1), table);
                }
            }
            case LVAL -> {
                //Ident {'[' Exp ']'}
                // todo 查表找到变量，计算偏移量，返回结果。要考虑到Ident无值的情况，此时需要分配空间，返回寄存器
                var name = node.getChild(0).getRawValue();
                assert table.lookup(name).isPresent();
                Symbol symbol = table.lookup(name).get();
                //todo 没有考虑数组的情况
                //如果是const，直接就可以获得返回值
                if (symbol instanceof ConstSymbol) {
                    return ((ConstSymbol) symbol).getValue();
                }
                //如果不是const，寄！要分配新的寄存器进行相加，从此之后返回的都是string了
                throw new RuntimeException("You should call [static int CalculateConst(ASTNode node, SymbolTable " + "table, RegisterAllocator allocator)]");
            }
            default -> throw new RuntimeException("Unexpected grammar type: " + node.getGrammarType());
        }
        return 0;
    }

    /**
     * 自定义的双目运算符，接受两个string，通过%判定是否需要用到寄存器相加。
     *
     * @param a         第一个操作数
     * @param b         第二个操作数
     * @param type      运算符类型，只可能是加减乘除模，而<font color='red'>不能是正负号等单目运算符。</font>
     * @param allocator 寄存器分配器
     * @return 运算结果，可能是数也可能是寄存器
     */
    private static String r2Op(String a, String b, GrammarType type, RegisterAllocator allocator) {

        assert type == GrammarType.PLUS | type == GrammarType.MINUS | type == GrammarType.MULTIPLY | type == GrammarType.DIVIDE | type == GrammarType.MOD;
        boolean aIsRegister = a.charAt(0) == '%';
        boolean bIsRegister = b.charAt(0) == '%';
        //只要有一个是寄存器，那么就得生成寄存器码
        boolean hasRegister = aIsRegister | bIsRegister;
        if (!hasRegister) {
            //简单！直接转换为int相加
            int aInt = Integer.parseInt(a);
            int bInt = Integer.parseInt(b);
            return switch (type) {
                case PLUS -> String.valueOf(aInt + bInt);
                case MINUS -> String.valueOf(aInt - bInt);
                case MULTIPLY -> String.valueOf(aInt * bInt);
                case DIVIDE -> String.valueOf(aInt / bInt);
                case MOD -> String.valueOf(aInt % bInt);
                default -> throw new RuntimeException("Unexpected grammar type: " + type);
            };
        }
        //如果有寄存器，就得生成对应的寄存器指令，而且要加减乘除各种指令
        IrBuilder builder = new IrBuilder();
        //a = 10 , b = %2
        //a = %2, b = 10
        //a = %2, b = %3
        switch (type) {
            case PLUS -> {
                
            }
            case MINUS -> {

            }
            case MULTIPLY -> {

            }
            case DIVIDE -> {

            }
            case MOD -> {

            }
            case NOT -> {

            }
        }
    }

}
