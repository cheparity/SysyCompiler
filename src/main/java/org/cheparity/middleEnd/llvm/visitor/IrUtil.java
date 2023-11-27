package middleEnd.llvm.visitor;

import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.parser.dataStruct.GrammarType;
import middleEnd.llvm.ir.BasicBlock;
import middleEnd.llvm.ir.IrBuilder;
import middleEnd.llvm.ir.Variable;
import middleEnd.llvm.utils.NodeUnion;
import middleEnd.symbols.FuncType;
import middleEnd.symbols.Symbol;
import middleEnd.symbols.SymbolTable;
import middleEnd.symbols.VarSymbol;

import java.util.ArrayList;
import java.util.List;

class IrUtil {
    private final SymbolTable table;
    private final IrBuilder builder;
    private final BasicBlock block;

    IrUtil(IrBuilder builder, BasicBlock block) {
        this.block = block;
        this.table = block.getSymbolTable();
        this.builder = builder;
    }

    /**
     * @param node 节点
     * @return 整数数值
     */
    public static int CalculateConst4Global(ASTNode node) {
        switch (node.getGrammarType()) {
            case CONST_INIT_VAL -> {
                //ConstInitVal -> ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
                if (node.getChildren().size() == 1) {
                    return CalculateConst4Global(node.getChild(0));
                }
                throw new RuntimeException("Not implement array!");
            }
            case CONST_EXP, EXP, NUMBER -> {
                return CalculateConst4Global(node.getChild(0));
            }
            // Exp -> AddExp
            case INT_CONST -> {
                return Integer.parseInt(node.getRawValue());
            }
            case ADD_EXP -> {
                // AddExp -> MulExp | AddExp '+' MulExp | AddExp '-' MulExp
                // AddExp -> MulExp {('+'|'-') MulExp}
                if (node.getChildren().size() == 1) {
                    return CalculateConst4Global(node.getChild(0));
                }
                var addRes = CalculateConst4Global(node.getChild(0));
                for (int i = 1; i < node.getChildren().size(); i += 2) {
                    var plusOrMinus = node.getChild(i).getGrammarType();
                    var mulRes = CalculateConst4Global(node.getChild(i + 1));
                    addRes = (plusOrMinus == GrammarType.PLUS) ? (addRes + mulRes) : (addRes - mulRes);
                }
                return addRes;
            }
            case MUL_EXP -> {
                // MulExp -> UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
                // MulExp -> UnaryExp { ('*' | '/' | '%') UnaryExp }
                if (node.getChildren().size() == 1) {
                    return CalculateConst4Global(node.getChild(0));
                }
                var mulRes = CalculateConst4Global(node.getChild(0));
                for (int i = 1; i < node.getChildren().size(); i += 2) {
                    var mulOrDivOrMod = node.getChild(i).getGrammarType();
                    var unaryRes = CalculateConst4Global(node.getChild(i + 1));
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
                //UnaryExp -> PrimaryExp | UnaryOp UnaryExp | Ident '(' [FuncRParams] ')'
                if (node.getChildren().size() == 1) {
                    return CalculateConst4Global(node.getChild(0));
                }
                if (node.getChildren().size() == 2) {
//                    UnaryOp -> '+' | '−' | '!'
                    var unaryOp = node.getChild(0).getChild(0).getGrammarType();
                    var unaryExp = node.getChild(1);
                    var unaryExpRes = CalculateConst4Global(unaryExp);
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
                    return CalculateConst4Global(node.getChild(0));
                }
                if (node.getChildren().size() == 3) {
                    return CalculateConst4Global(node.getChild(1));
                }
            }
            case LVAL -> {
                //Ident {'[' Exp ']'}
                //查表找到变量，计算偏移量，返回结果。要考虑到Ident无值的情况，此时需要分配空间，返回寄存器
                var name = node.getChild(0).getRawValue();
//                assert table.getSymbol(name).isPresent();
                var table = SymbolTable.getGlobal();
                assert table.getSymbol(name).isPresent();
                Symbol symbol = table.getSymbol(name).get();
                //todo 没有考虑数组的情况
                //如果可以获得值，那就返回
                var num = symbol.getNumber();
                if (num.isPresent()) return num.get();
                //todo 如果没有赋初值，寄！要分配新的寄存器进行相加
                throw new RuntimeException("Should alloca register");
            }
            default -> throw new RuntimeException("Unexpected grammar type: " + node.getGrammarType());
        }
        return 0;
    }

    public NodeUnion calcAloExp(ASTNode node) {
        NodeUnion union = new NodeUnion(node, builder, block);
        switch (node.getGrammarType()) {
            case CONST_EXP, EXP, NUMBER -> {
                // Exp -> AddExp
                return calcAloExp(node.getChild(0));
            }
            case INT_CONST -> {
                return union.setNumber(Integer.parseInt(node.getRawValue()));
            }
            case ADD_EXP -> {
                // AddExp -> MulExp | AddExp '+' MulExp | AddExp '-' MulExp
                // AddExp -> MulExp {('+'|'-') MulExp}
                if (node.getChildren().size() == 1) {
                    return calcAloExp(node.getChild(0));
                }
                var addRes = calcAloExp(node.getChild(0));
                for (int i = 1; i < node.getChildren().size(); i += 2) {
                    var plusOrMinus = node.getChild(i).getGrammarType();
                    var mulRes = calcAloExp(node.getChild(i + 1));
                    addRes = (plusOrMinus == GrammarType.PLUS) ? (addRes.add(mulRes)) : (addRes.sub(mulRes));
                }

                return addRes;
            }
            case MUL_EXP -> {
                // MulExp -> UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
                // MulExp -> UnaryExp { ('*' | '/' | '%') UnaryExp }
                if (node.getChildren().size() == 1) {
                    return calcAloExp(node.getChild(0));
                }
                var mulRes = calcAloExp(node.getChild(0));
                for (int i = 1; i < node.getChildren().size(); i += 2) {
                    var unaryRes = calcAloExp(node.getChild(i + 1));
                    var mulOrDivOrMod = node.getChild(i).getGrammarType();
                    mulRes = switch (mulOrDivOrMod) {
                        case MULTIPLY -> mulRes.mul(unaryRes);
                        case DIVIDE -> mulRes.div(unaryRes);
                        case MOD -> mulRes.mod(unaryRes);
                        default -> throw new RuntimeException("Unexpected grammar type: " + mulOrDivOrMod);
                    };
                }
                return mulRes;
            }
            case UNARY_EXP -> {
                //UnaryExp -> PrimaryExp | UnaryOp UnaryExp | Ident '(' [FuncRParams] ')'
                if (node.getChildren().size() == 1) {
                    return calcAloExp(node.getChild(0));
                }
                if (node.getChildren().size() == 2) {
//                    UnaryOp -> '+' | '−' | '!'
                    var unaryOp = node.getChild(0).getChild(0).getGrammarType();
                    var unaryExp = node.getChild(1);
                    var unaryExpRes = calcAloExp(unaryExp);
                    switch (unaryOp) {
                        case PLUS -> {
                            return unaryExpRes;
                        }
                        case MINUS -> {
                            return unaryExpRes.nag();
                        }
                        case NOT -> {
                            return unaryExpRes.not();
                        }
                        default -> throw new RuntimeException("Unexpected grammar type: " + unaryOp);
                    }
                }
                //Ident '(' [FuncRParams] ')'
                //FuncRParams -> Exp { ',' Exp }
                String funcName = node.getChild(0).getRawValue();
                //1.符号表里查函数，函数里获得参数符号 2.获取符号pointer 3.将pointer load进具体的variable里 4.将寄存器variable存进symbol
                assert SymbolTable.getGlobal().getFuncSymbol(funcName).isPresent();
                List<VarSymbol> fparams = SymbolTable.getGlobal().getFuncSymbol(funcName).get().getParams();
                ArrayList<Variable> paramVariables = new ArrayList<>(); //新建一个variable列表，用于存放实参

                //for循环是在构建实参列表paramVariables
                for (int i = 0; i < fparams.size(); i++) {
//                    var pSymbol = fparams.get(i);
                    var pNode = node.getChild(2).getChild(2 * i); //0->0, 1->2, 2->4, .. i->2*i
                    NodeUnion calc = calcAloExp(pNode);
                    //如果传参是数字：%2 = call i32 @foo(i32 1) 直接call
                    if (calc.isNum) {
                        paramVariables.add(builder.buildConstIntNum(calc.getNumber()));
                        continue;
                    }
                    //如果传参是变量
//                    PointerValue pointer = pSymbol.getPointer();
//                    Variable register = builder.buildLoadInst(block, pointer); //此时variable为load出的寄存器
                    paramVariables.add(calc.getVariable()); //将寄存器存入列表
//                    pSymbol.setIrVariable(calc.getVariable()); //将寄存器存入符号表 todo 想清楚
                }
                //5.build call inst
                //如果是void，直接call
                if (SymbolTable.getGlobal().getFuncSymbol(funcName).get().getFuncType() == FuncType.VOID) {
                    builder.buildCallInst(block, funcName, paramVariables.toArray(new Variable[0]));
                    return union.setNumber(0);
                }
                //如果是int，call后再load
                Variable variable = builder.buildCallInst(block, funcName, paramVariables.toArray(new Variable[0]));
                return union.setVariable(variable);
            }
            case PRIMARY_EXP -> {
//                PrimaryExp ->  LVal | Number | '(' Exp ')'
                if (node.getChildren().size() == 1) {
                    return calcAloExp(node.getChild(0));
                }
                if (node.getChildren().size() == 3) {
                    return calcAloExp(node.getChild(1));
                }
            }
            case LVAL -> {
                //Ident {'[' Exp ']'}
                //查表找到变量，计算偏移量，返回结果。要考虑到Ident无值的情况，此时需要分配空间，返回寄存器
                var name = node.getChild(0).getRawValue();
                assert table.getSymbol(name).isPresent();
                Symbol symbol = table.getSymbol(name).get();
                //todo 没有考虑数组的情况
                //如果可以获得值，那就返回
                var num = symbol.getNumber();
                if (num.isPresent()) return union.setNumber(num.get());
                //如果没有赋初值
                assert symbol.getPointer() != null;//这里symbol有可能只是分配了寄存器，没有load。则需要把指针load为一个寄存器，再赋值给union
                Variable variable = builder.buildLoadInst(block, symbol.getPointer()); //此时variable为load出的寄存器
                return union.setVariable(variable);
            }
            case CONST_INIT_VAL -> {
                //ConstInitVal -> ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
                if (node.getChildren().size() == 1) {
                    return calcAloExp(node.getChild(0));
                }
                throw new RuntimeException("Not implement array!");
            }
            default -> throw new RuntimeException("Unexpected grammar type: " + node.getGrammarType());
        }
        throw new RuntimeException("You shouldn't walk this");
    }

    public NodeUnion calcLogicExp(ASTNode node) {
        //Cond -> LOrExp
        //LOrExp -> LAndExp | LOrExp '||' LAndExp
        //LAndExp -> EqExp | LAndExp '&&' EqExp
        //EqExp -> RelExp | EqExp ('==' | '!=') RelExp
        //RelExp -> AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
        switch (node.getGrammarType()) {
            case COND -> {
                return calcLogicExp(node.getChild(0));
            }
            case LOR_EXP -> {
                if (node.getChildren().size() == 1) {
                    return calcLogicExp(node.getChild(0));
                }
                var lOrExp = calcLogicExp(node.getChild(0));
                var lAndExp = calcLogicExp(node.getChild(2));
                var op = node.getChild(1).getGrammarType();
                return switch (op) {
                    case LOGICAL_OR -> lOrExp.or(lAndExp);
                    default -> throw new RuntimeException("Unexpected grammar type: " + op);
                };
            }
            case LAND_EXP -> {
                if (node.getChildren().size() == 1) {
                    return calcLogicExp(node.getChild(0));
                }
                var lAndExp = calcLogicExp(node.getChild(0));
                var eqExp = calcLogicExp(node.getChild(2));
                var op = node.getChild(1).getGrammarType();
                return switch (op) {
                    case LOGICAL_AND -> lAndExp.and(eqExp);
                    default -> throw new RuntimeException("Unexpected grammar type: " + op);
                };
            }
            case EQ_EXP -> {
                if (node.getChildren().size() == 1) {
                    return calcLogicExp(node.getChild(0));
                }
                var eqExp = calcLogicExp(node.getChild(0));
                var relExp = calcLogicExp(node.getChild(2));
                var op = node.getChild(1).getGrammarType();
                return switch (op) {
                    case EQUAL -> eqExp.eq(relExp);
                    case NOT_EQUAL -> eqExp.ne(relExp);
                    default -> throw new RuntimeException("Unexpected grammar type: " + op);
                };
            }
            case REL_EXP -> {
                if (node.getChildren().size() == 1) {
                    return calcLogicExp(node.getChild(0));
                }
                var relExp = calcLogicExp(node.getChild(0));
                var addExp = calcLogicExp(node.getChild(2));
                var op = node.getChild(1).getGrammarType();
                return switch (op) {
                    case LESS_THAN -> relExp.lt(addExp);
                    case LESS_THAN_EQUAL -> relExp.le(addExp);
                    case GREATER_THAN -> relExp.gt(addExp);
                    case GREATER_THAN_EQUAL -> relExp.ge(addExp);
                    default -> throw new RuntimeException("Unexpected grammar type: " + op);
                };
            }
            default -> {
                return calcAloExp(node); //剩余的情况就是算术表达式
            }
        }
    }
}
