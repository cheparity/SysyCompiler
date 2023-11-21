package middleEnd.llvm.visitor;

import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.parser.dataStruct.GrammarType;
import middleEnd.llvm.NodeUnion;
import middleEnd.llvm.ir.BasicBlock;
import middleEnd.llvm.ir.IrBuilder;
import middleEnd.llvm.ir.Variable;
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
     * 自定义的双目运算符（我现在觉得应该用不到了），接受两个variable，分配寄存器，相加
     *
     * @param a       第一个操作数
     * @param b       第二个操作数
     * @param type    运算符类型。目前通过<font color='red'>{@link GrammarType}来区分运算类型。</font>只可能是<font
     *                color='red'>加减乘除模</font>，而<font
     *                color='red'>不能是正负号等单目运算符。</font>
     * @param builder IrBuilder
     * @return 运算结果，可能是数也可能是寄存器
     */
//    private static Variable Op(Variable a, GrammarType type, Variable b, IrBuilder builder, BasicBlock block) {
//        assert type == GrammarType.PLUS | type == GrammarType.MINUS | type == GrammarType.MULTIPLY | type == GrammarType.DIVIDE | type == GrammarType.MOD;
//        boolean aIsRegister = a.getNumber().isEmpty();
//        boolean bIsRegister = b.getNumber().isEmpty();
//        //只要有一个是寄存器，那么就得生成寄存器码
//        //如果两个都不是寄存器，简单！直接转换为int相加
//        if (!aIsRegister && !bIsRegister) {
//            Integer numa = a.getNumber().get();
//            Integer numb = b.getNumber().get();
//            return switch (type) {
//                //这里不能调用buildBinInstruction，因为a，b都不是寄存器
//                case PLUS -> builder.buildLocalVariable(block, a.getType().getBasicType(), numa + numb);
//                case MINUS -> builder.buildLocalVariable(block, a.getType().getBasicType(), numa - numb);
//                case MULTIPLY -> builder.buildLocalVariable(block, a.getType().getBasicType(), numa * numb);
//                case DIVIDE -> builder.buildLocalVariable(block, a.getType().getBasicType(), numa / numb);
//                case MOD -> builder.buildLocalVariable(block, a.getType().getBasicType(), numa % numb);
//                default -> throw new RuntimeException("Unexpected grammar type: " + type);
//            };
//        }
//        //如果有寄存器，两者就都得生成对应的寄存器指令，而且要加减乘除各种指令
//        //a = 10, b = %2
//        //a = %2, b = 10
//        //a = %2, b = %3
//        Operator add = Operator.create(IrType.create(IrType.IrTypeID.Int32TyID), Operator.OpCode.ADD);
//        Operator sub = Operator.create(IrType.create(IrType.IrTypeID.Int32TyID), Operator.OpCode.SUB);
//        Operator mul = Operator.create(IrType.create(IrType.IrTypeID.Int32TyID), Operator.OpCode.MUL);
//        Operator div = Operator.create(IrType.create(IrType.IrTypeID.Int32TyID), Operator.OpCode.SDIV);
//        Operator mod = Operator.create(IrType.create(IrType.IrTypeID.Int32TyID), Operator.OpCode.SREM);
//        var aVar = aIsRegister ? a : builder.buildLocalVariable(block, a.getType().getBasicType(), a.getNumber().get());
//        var bVar = bIsRegister ? b : builder.buildLocalVariable(block, b.getType().getBasicType(), b.getNumber().get());
//        //现在aVar和bVar都是寄存器了
//        return switch (type) {
//            case PLUS -> builder.buildBinInstruction(block, aVar, add, bVar);
//            case MINUS -> builder.buildBinInstruction(block, aVar, sub, bVar);
//            case MULTIPLY -> builder.buildBinInstruction(block, aVar, mul, bVar);
//            case DIVIDE -> builder.buildBinInstruction(block, aVar, div, bVar);
//            case MOD -> builder.buildBinInstruction(block, aVar, mod, bVar);
//            default -> throw new RuntimeException("Unexpected grammar type: " + type);
//        };
//    }

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

    public NodeUnion calc(ASTNode node) {
        NodeUnion union = new NodeUnion(node, builder, block);
        switch (node.getGrammarType()) {
            case CONST_EXP, EXP, NUMBER -> {
                // Exp -> AddExp
                return calc(node.getChild(0));
            }
            case INT_CONST -> {
                return union.setNumber(Integer.parseInt(node.getRawValue()));
            }
            case ADD_EXP -> {
                // AddExp -> MulExp | AddExp '+' MulExp | AddExp '-' MulExp
                // AddExp -> MulExp {('+'|'-') MulExp}
                if (node.getChildren().size() == 1) {
                    return calc(node.getChild(0));
                }
                var addRes = calc(node.getChild(0));
                for (int i = 1; i < node.getChildren().size(); i += 2) {
                    var plusOrMinus = node.getChild(i).getGrammarType();
                    var mulRes = calc(node.getChild(i + 1));
                    addRes = (plusOrMinus == GrammarType.PLUS) ? (addRes.add(mulRes)) : (addRes.sub(mulRes));
                }

                return addRes;
            }
            case MUL_EXP -> {
                // MulExp -> UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
                // MulExp -> UnaryExp { ('*' | '/' | '%') UnaryExp }
                if (node.getChildren().size() == 1) {
                    return calc(node.getChild(0));
                }
                var mulRes = calc(node.getChild(0));
                for (int i = 1; i < node.getChildren().size(); i += 2) {
                    var unaryRes = calc(node.getChild(i + 1));
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
                    return calc(node.getChild(0));
                }
                if (node.getChildren().size() == 2) {
//                    UnaryOp -> '+' | '−' | '!'
                    var unaryOp = node.getChild(0).getChild(0).getGrammarType();
                    var unaryExp = node.getChild(1);
                    var unaryExpRes = calc(unaryExp);
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
                    NodeUnion calc = calc(pNode);
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
                    return calc(node.getChild(0));
                }
                if (node.getChildren().size() == 3) {
                    return calc(node.getChild(1));
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
                    return calc(node.getChild(0));
                }
                throw new RuntimeException("Not implement array!");
            }
            default -> throw new RuntimeException("Unexpected grammar type: " + node.getGrammarType());
        }
        throw new RuntimeException("You shouldn't walk this");
    }
}
