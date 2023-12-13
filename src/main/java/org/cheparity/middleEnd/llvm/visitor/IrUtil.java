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
import utils.LoggerUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

class IrUtil {
    private final static Logger LOGGER = LoggerUtil.getLogger();
    private final SymbolTable table;
    private final IrBuilder builder;
    private final BasicBlock block;

    IrUtil(IrBuilder builder, BasicBlock block) {
        this.block = block;
        this.table = block.getSymbolTable();
        this.builder = builder;
    }

    public static void unwrapArrayInitVal4Global(ASTNode node, ArrayList<Integer> inits) {
        //ConstInitVal → ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
        //InitVal -> Exp | '{' [ InitVal { ',' InitVal } ] '}'
        //递归解析
        switch (node.getChild(0).getGrammarType()) {
            case CONST_EXP, EXP -> {
                inits.add(calculateConst4Global(node.getChild(0)));
            }
            case LEFT_BRACE -> {
                node.getChildren().stream()
                        .filter(child ->
                                child.getGrammarType().equals(GrammarType.CONST_INIT_VAL) ||
                                        child.getGrammarType().equals(GrammarType.INIT_VAL)
                        )
                        .forEach(v -> unwrapArrayInitVal4Global(v, inits));
            }
        }
    }

    /**
     * @param node 节点
     * @return 整数数值
     */
    public static int calculateConst4Global(ASTNode node) {
        switch (node.getGrammarType()) {
            case CONST_INIT_VAL -> {
                //ConstInitVal -> ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
                if (node.getChildren().size() == 1) {
                    return calculateConst4Global(node.getChild(0));
                }
                throw new RuntimeException("Not implement array!");
            }
            case CONST_EXP, EXP, NUMBER -> {
                return calculateConst4Global(node.getChild(0));
            }
            // Exp -> AddExp
            case INT_CONST -> {
                return Integer.parseInt(node.getRawValue());
            }
            case ADD_EXP -> {
                // AddExp -> MulExp | AddExp '+' MulExp | AddExp '-' MulExp
                // AddExp -> MulExp {('+'|'-') MulExp}
                if (node.getChildren().size() == 1) {
                    return calculateConst4Global(node.getChild(0));
                }
                var addRes = calculateConst4Global(node.getChild(0));
                for (int i = 1; i < node.getChildren().size(); i += 2) {
                    var plusOrMinus = node.getChild(i).getGrammarType();
                    var mulRes = calculateConst4Global(node.getChild(i + 1));
                    addRes = (plusOrMinus == GrammarType.PLUS) ? (addRes + mulRes) : (addRes - mulRes);
                }
                return addRes;
            }
            case MUL_EXP -> {
                // MulExp -> UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
                // MulExp -> UnaryExp { ('*' | '/' | '%') UnaryExp }
                if (node.getChildren().size() == 1) {
                    return calculateConst4Global(node.getChild(0));
                }
                var mulRes = calculateConst4Global(node.getChild(0));
                for (int i = 1; i < node.getChildren().size(); i += 2) {
                    var mulOrDivOrMod = node.getChild(i).getGrammarType();
                    var unaryRes = calculateConst4Global(node.getChild(i + 1));
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
                    return calculateConst4Global(node.getChild(0));
                }
                if (node.getChildren().size() == 2) {
//                    UnaryOp -> '+' | '−' | '!'
                    var unaryOp = node.getChild(0).getChild(0).getGrammarType();
                    var unaryExp = node.getChild(1);
                    var unaryExpRes = calculateConst4Global(unaryExp);
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
                if (node.getChildren().size() >= 3) {
                    //应该不用考虑？
                    throw new RuntimeException("Not considering call func in the global decl!");
                }
            }
            case PRIMARY_EXP -> {
//                PrimaryExp ->  LVal | Number | '(' Exp ')'
                if (node.getChildren().size() == 1) {
                    return calculateConst4Global(node.getChild(0));
                }
                if (node.getChildren().size() == 3) {
                    return calculateConst4Global(node.getChild(1));
                }
            }
            case LVAL -> {
                //Ident {'[' Exp ']'} todo
                //查表找到变量，计算偏移量，返回结果。要考虑到Ident无值的情况，此时需要分配空间，返回寄存器
                var name = node.getChild(0).getIdent();
                var table = SymbolTable.getGlobal();
                Symbol symbol = table.getSymbolSafely(name, node);
                int dim = symbol.getDim();
                if (dim == 0) {
                    var num = symbol.getNumber();
                    assert num.isPresent(); //全局变量一定有确定的num
                    return num.get();
                }
                AtomicBoolean actualDimSame = new AtomicBoolean();
                var offset = calcOffsetForGlobal(node, symbol, actualDimSame);
                //数组，dim至少为1维
                //应该可以直接load
                Optional<Integer> number = symbol.getNumber(offset);
                assert number.isPresent();
                return number.get();
            }
            default -> throw new RuntimeException("Unexpected grammar type: " + node.getGrammarType());
        }
        return 0;
    }

    private static int calcOffsetForGlobal(ASTNode node, Symbol symbol, AtomicBoolean actualDimEqualsDefinedDim) throws NoSuchElementException {
        //否则的话，形如a[] 或者 a[][]
        //先拿最后一个dim的数值。calculateConst4Global(node.getChild(3*dim-1)) 能获取到dim对应的ASTNode值
        //计算两个值，一个是symbol的dim（已经传递过来了），另一个是node的实际dim
        var dim = symbol.getDim();
        int nodeDim = node.getChildren().stream().filter(child ->
                child.getGrammarType().equals(GrammarType.LEFT_BRACKET)).toList().size();

        int offset = 0;
        actualDimEqualsDefinedDim.set(false);
        if (nodeDim == dim) {
            //如果实际dim!=nodeDim，就是a[5][6]这种数组，取了a[2]这种情况，或者a[8]这种数组取了a这种情况，不可以常规计算，要计算地址
            //那么按照上面的例子，a[2] => a[2][0]，a => a[0]，即自动补0 => 只有offset的初始值有不同
            offset = calculateConst4Global(node.getChild(3 * dim - 1));
            actualDimEqualsDefinedDim.set(true);
        } else if (nodeDim == 0) {
            //如果nodeDim为0，就是b[2][3]传递了b这种情况，直接返回offset（也就是0）即可
            return offset;
        }

        for (int nowDim = dim - 1; nowDim > 0; nowDim--) {
            int num = calculateConst4Global(node.getChild(3 * nowDim - 1));
            int expand = 1;
            for (int i = 1; i < symbol.getDim(); i++) {
                expand *= symbol.getDimSize(nowDim + i);
            }
            offset += num * expand;
        }
        return offset;
    }

    /**
     * 如果stmt本身就是一个块了，就不用包；如果是单语句的形式，就包装成块，并把<font color='red'>原来所属块的符号表</font>传递给新块
     *
     * @param stmt        单语句/块
     * @param symbolTable 原来所属块的符号表
     * @return 包装好的块
     */
    public static ASTNode wrapStmtAsBlock(ASTNode stmt, SymbolTable symbolTable) {
        GrammarType grammarType = stmt.getGrammarType();
        assert grammarType == GrammarType.STMT || grammarType == GrammarType.FOR_STMT;
        //如果本身的第一个child就是块，就不用包装了
        if (stmt.getChild(0).getGrammarType() == GrammarType.BLOCK) return stmt;
        //由：stmt(old)
        //到：stmt（new） -> Block -> BlockItem -> stmt(old)
        ASTNode blk = new ASTNode(GrammarType.BLOCK);
        ASTNode blkItm = new ASTNode(GrammarType.BLOCK_ITEM);
        blk
                .setSymbolTable(symbolTable)
                .addChild(blkItm)
                .setFather(stmt.getFather());
        blkItm
                .addChild(stmt)
                .setFather(blk);

        return stmt.replaceItselfAs(blk);
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
                int symbolCnt = SymbolTable.getGlobal().getFuncSymbol(funcName).get().getParamCount();
                List<NodeUnion> rparams = new ArrayList<>(); //新建一个variable列表，用于存放实参

                //for循环是在构建实参列表paramVariables
                for (int i = 0; i < symbolCnt; i++) {
                    var pNode = node.getChild(2).getChild(2 * i); //0->0, 1->2, 2->4, .. i->2*i
                    rparams.add(calcAloExp(pNode));
                }
                //5.build call inst
                //如果是void，直接call
                if (SymbolTable.getGlobal().getFuncSymbol(funcName).get().getFuncType() == FuncType.VOID) {
                    builder.buildCallInst(block, funcName, rparams.toArray(new NodeUnion[0]));
                    return union.setNumber(0);
                }
                //如果是int，call后再load
                Variable variable = builder.buildCallInst(block, funcName, rparams.toArray(new NodeUnion[0]));
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
                var name = node.getChild(0).getRawValue();//"aaa"
                Symbol symbol = table.getSymbolSafely(name, node);

                int dim = symbol.getDim();
                if (dim == 0) {
                    //如果可以获得值，那就返回
                    var num = symbol.getNumber();
                    if (num.isPresent()) return union.setNumber(num.get());
                    //如果没有赋初值
                    assert symbol.getPointer() != null;//这里symbol有可能只是分配了寄存器，没有load。则需要把指针load为一个寄存器，再赋值给union
                    Variable variable = builder.buildLoadInst(block, symbol.getPointer()); //此时variable为load出的寄存器
                    symbol.setIrVariable(variable);
                    return union.setVariable(variable);
                }
                //先要计算偏移量，用getelementptr取出来偏移指针；再将偏移指针内的值load出来
                //数组是a[5][6], 取a[2][3] => a[3+2*6] => 关键是第二维的dim2
                //数组是a[5][6][7]，取a[2][3][4] => 4 + 3*7 + 2*6*7
                AtomicBoolean actualDimSame = new AtomicBoolean();
                NodeUnion offsetUnion = this.calcOffset(node, symbol, actualDimSame); //不应该调用global的！！
                if (!actualDimSame.get()) {
                    //但如果实际dim!=nodeDim，就是a[5][6]这种数组，取了a[2]这种情况，或者a[8]这种数组取了a这种情况，不可以常规计算，要计算地址
//                    PointerValue loadPointer = builder.buildElementPointer(block, symbol.getPointer(), offsetUnion);
                    Variable loadPointer = builder.buildLoadArrayInsts(block, symbol.getPointer(), offsetUnion);
                    return union.setVariable(loadPointer);
                }
                //否则需要load出来
                Variable variable = builder.buildLoadArrayInsts(block, symbol.getPointer(), offsetUnion);
                Variable p = builder.buildLoadInst(block, builder.variableToPointer(variable));
                return union.setVariable(p);
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

    public NodeUnion calcOffset(ASTNode node, Symbol symbol) {
        AtomicBoolean atomicBoolean = new AtomicBoolean();
        NodeUnion nodeUnion = calcOffset(node, symbol, atomicBoolean);
        assert atomicBoolean.get();
        return nodeUnion;
    }

    public NodeUnion calcOffset(ASTNode node, Symbol symbol, AtomicBoolean actualDimEqualsDefinedDim) throws NoSuchElementException {
        //否则的话，形如a[] 或者 a[][]
        //先拿最后一个dim的数值。calculateConst4Global(node.getChild(3*dim-1)) 能获取到dim对应的ASTNode值
        //计算两个值，一个是symbol的dim（已经传递过来了），另一个是node的实际dim
        NodeUnion offsetUnion = new NodeUnion(node, builder, block);
        var dim = symbol.getDim();
        int nodeDim = node.getChildren().stream().filter(child ->
                child.getGrammarType().equals(GrammarType.LEFT_BRACKET)).toList().size();

        actualDimEqualsDefinedDim.set(dim == nodeDim);
        if (nodeDim == dim) {
            //如果实际dim!=nodeDim，就是a[5][6]这种数组，取了a[2]这种情况，或者a[8]这种数组取了a这种情况，不可以常规计算，要计算地址
            //那么按照上面的例子，a[2] => a[2][0]，a => a[0]，即自动补0 => 只有offset的初始值有不同
            offsetUnion = calcAloExp(node.getChild(3 * dim - 1)); //如果是a[i]这种，就要计算表达式了
        } else if (nodeDim == 0) {
            //如果nodeDim为0，就是b[2][3]传递了b这种情况，直接返回offset=0即可
            return offsetUnion.setNumber(0);
        } else {
            offsetUnion.setNumber(0);
        }
        // a[2][2] => a[1][1] => 1*2+1=3
        for (int nowDim = dim - 1; nowDim > 0; nowDim--) {
            NodeUnion num = calcAloExp(node.getChild(3 * nowDim - 1));
            NodeUnion expand = new NodeUnion(node, builder, block).setNumber(1);
            for (int i = 1; i < symbol.getDim(); i++) {
                NodeUnion dimSize = new NodeUnion(node, builder, block).setNumber(symbol.getDimSize(nowDim + i));
                expand = expand.mul(dimSize);
            }
//            offset += num * expand;
            offsetUnion = offsetUnion.add(num.mul(expand));
        }
        return offsetUnion;
    }

    /**
     * 你能得到一堆加在block里的指令，和一个nodeUnion，表示condition。可以用这个condition进行判断
     *
     * @param node 条件节点
     * @return condition
     */
    public NodeUnion calcLogicExp(ASTNode node) {
        //Cond -> LOrExp
        //LOrExp -> LAndExp | LOrExp '||' LAndExp
        //LAndExp -> EqExp | LAndExp '&&' EqExp
        //EqExp -> RelExp | EqExp ('==' | '!=') RelExp
        //RelExp -> AddExp | RelExp ('<' | '<=' | '>' | '>=') AddExp
        switch (node.getGrammarType()) {
            case COND -> {
                return calcLogicExp(node.getChild(0));
            }
            case LOR_EXP -> {
                if (node.getChildren().size() == 1) {
                    return calcLogicExp(node.getChild(0));
                }
                var lOrExp = calcLogicExp(node.getChild(0));
                if (lOrExp.isNum && lOrExp.getNumber() == 1) {
                    //短路求值
                    return lOrExp;
                }
                var lAndExp = calcLogicExp(node.getChild(2));
                if (lAndExp.isNum && lAndExp.getNumber() == 1) {
                    return lAndExp;
                }
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
                if (lAndExp.isNum && lAndExp.getNumber() == 0) {
                    return lAndExp;
                }
                var eqExp = calcLogicExp(node.getChild(2));
                if (eqExp.isNum && eqExp.getNumber() == 0) {
                    return eqExp;
                }
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
                return calcAloExp(node); //这里就是应该直接返回，但是要在最终的出口返回的地方（函数调用的地方）判断
            }
        }
    }


    public void unwrapArrayInitVal(ASTNode node, ArrayList<NodeUnion> inits) {
        //ConstInitVal -> ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
        //InitVal -> Exp | '{' [ InitVal { ',' InitVal } ] '}'
        //递归解析
        switch (node.getChild(0).getGrammarType()) {
            case CONST_EXP, EXP -> {
                inits.add(calcAloExp(node.getChild(0)));
            }
            case LEFT_BRACE -> {
                node.getChildren()
                        .stream()
                        .filter(child ->
                                child.getGrammarType().equals(GrammarType.CONST_INIT_VAL) ||
                                        child.getGrammarType().equals(GrammarType.INIT_VAL)
                        )
                        .forEach(v -> unwrapArrayInitVal(v, inits));
            }
        }
    }
}
