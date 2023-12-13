package middleEnd.llvm.ir;

import middleEnd.llvm.IrTranslator;
import middleEnd.llvm.RegisterAllocator;
import middleEnd.llvm.utils.NodeUnion;
import middleEnd.symbols.FuncSymbol;
import middleEnd.symbols.SymbolTable;
import middleEnd.symbols.VarSymbol;
import utils.LoggerUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class IrBuilder {
    private static final Logger LOGGER = LoggerUtil.getLogger();
    private RegisterAllocator allocator;

    public IrBuilder() {

    }

    public IrBuilder(RegisterAllocator allocator) {
        this.allocator = allocator;
    }

    public IrFunction buildFunction(IrType.IrTypeID type, String name, Module module) {
        String funcName = "@" + name;
        var func = new IrFunction(IrType.create(type, IrType.IrTypeID.FunctionTyID), funcName, module);
        module.insertFunc(func);
        LOGGER.fine("build function: " + funcName);
        return func;
    }

    /**
     * 建立一个条件跳转指令，形如：br i1 %4, label %5, label %6
     * <p>
     *
     * <font color='red'>注意continue和break进来的是替换，其他都是保留原有！</font>
     *
     * @param belonging 所属基本块
     * @param cond      条件
     * @param ifTrue    如果条件为真，跳转到的块
     * @param ifFalse   如果条件为假，跳转到的块
     */
    public void buildBrInst(Boolean sudo, BasicBlock belonging, Variable cond, BasicBlock ifTrue, BasicBlock ifFalse) {
        var br = new BrInstruction(cond, ifTrue, ifFalse);
        if (belonging.endWithRet()) {
            return; //不要构建语句
        }
        if (belonging.endWithBr()) {
            LOGGER.warning("Already has br instruction " + belonging.getLastInstruction().toIrCode() + "!");
            if (sudo) {
                LOGGER.warning("REPLACED!");
                belonging.getInstructionList().removeLast();
            } else {
                LOGGER.warning("Skipped!");
                return;
            }
        }
        belonging.addInstruction(br);
        LOGGER.fine("build br instruction: " + br.toIrCode() + " at block: " + belonging.getName());
    }

    /**
     * 直接跳转到dest，形如 br label [dest]
     * <p>
     *
     * <font color='red'>注意continue和break进来的是替换，其他都是保留原有！</font>
     *
     * @param belonging 所属基本块
     * @param dest      目的块
     */
    public void buildBrInst(Boolean sudo, BasicBlock belonging, BasicBlock dest) {
        var br = new BrInstruction(dest);
        if (belonging.endWithRet()) {
            return; //不要构建语句
        }
        if (belonging.endWithBr()) {
            LOGGER.warning("Already has br instruction " + belonging.getLastInstruction().toIrCode() + "!");
            if (sudo) {
                LOGGER.warning("REPLACED!");
                belonging.getInstructionList().removeLast();
            } else {
                LOGGER.warning("Skipped!");
                return;
            }
        }
        belonging.addInstruction(br);
        LOGGER.fine("build br instruction: " + br.toIrCode() + " at block: " + belonging.getName());
    }

    /**
     * 负责把argument new出来，并添加到function中。
     *
     * @param irFunction 所属函数
     * @param type       参数类型
     */
    public void buildArg(IrFunction irFunction, IrType type) {
        Argument argument = new Argument(type, allocator.allocate());
        irFunction.insertArgument(argument);
        LOGGER.fine("build argument: " + argument.getName() + " at irFunction: " + irFunction.getName());
    }

    /**
     * 建造基本块（区别于函数入口块）
     * <p>
     * 1. 新建基本块
     * <p>
     * 2. 设定基本块的前驱和后继
     * <p>
     * 3. 把基本块加入到函数的blockList中
     *
     * @param predecessor 前驱块
     * @return 返回新建立的基本块
     */
    public BasicBlock buildBasicBlock(BasicBlock predecessor) {
        var bb = new BasicBlock(allocator.allocate()).setSymbolTable(predecessor.getSymbolTable());
        predecessor.addSuccessor(bb);
        bb.setFunction(predecessor.getFunction());
        bb.getFunction().addBlock(bb);
        bb.addPredecessor(predecessor);
        LOGGER.fine("build basic block: " + bb.getName() + " in function: " + bb.getFunction().getName() + " " +
                "predecessor: " + predecessor.getName());
        return bb;
    }

    public BasicBlock buildBasicBlock(BasicBlock predecessor, SymbolTable symbolTable) {
        LOGGER.fine("build basic block: " + predecessor.getName() + " in function: " + predecessor.getFunction().getName() + " " +
                "predecessor: " + predecessor.getName() + " and set symbol table");

        return buildBasicBlock(predecessor).setSymbolTable(symbolTable);
    }


    /**
     * 建立函数入口块。与buildBasicBlock的区别是，还需要把函数的参数存进符号表中。
     *
     * @param irFunction 函数
     * @return 返回一个函数入口块
     */
    public BasicBlock buildEntryBlock(IrFunction irFunction) {
        var bb = new BasicBlock(allocator.allocate()); //每个临时寄存器和基本块占用一个编号
        //entry block的前驱是没有的
        irFunction.setEntryBlock(bb);
        bb.setFunction(irFunction);

        //为了从符号表中找到函数参数，并更新其指针。1.注意是从全局符号表查找function 2.注意function的name之前有个@，所以要substring(1)
        assert SymbolTable.getGlobal().getFuncSymbol(irFunction.getName().substring(1)).isPresent();
        FuncSymbol funcSymbol = SymbolTable.getGlobal().getFuncSymbol(irFunction.getName().substring(1)).get();
        List<VarSymbol> params = funcSymbol.getParams();
        //将function的所有arguments，先alloca一个新pointer，再将arg里的寄存器store进point中
        for (int i = 0; i < irFunction.getArguments().size(); i++) {
            Argument arg = irFunction.getArguments().get(i);
            VarSymbol argSymbol = params.get(i);
            PointerValue pointer = buildAllocaInst(bb, arg.getType());
            buildStoreInst(bb, new Variable(pointer.getType(), arg.getName()), pointer);
            //还应该把这个指针存进符号表中
            argSymbol.setPointer(pointer);
        }
        LOGGER.fine("build entry block: " + bb.getName() + " in irFunction: " + bb.getFunction().getName());
        return bb;
    }

    /**
     * 形如：%5 = sub nsw i32 0, %4，表示%5寄存器是%4寄存器的取反
     *
     * @param block    所属基本块
     * @param register 要取反的寄存器。<font color='red'>不能是一个数值</font>
     * @return 返回一个result的Variable，这个Variable是<font color='red'>寄存器</font>，里面存放了结果
     */
    public Variable buildNegInst(BasicBlock block, Variable register) {
        var zeroConst = new ConstValue(0, IrType.IrTypeID.Int32TyID);
        assert zeroConst.getNumber().isEmpty();
        zeroConst.setNumber(0);
        LOGGER.fine("build neg instruction: " + zeroConst.getName() + " in block: " + block.getName());
        return buildBinInstruction(block, zeroConst, Operator.create(IrType.create(IrType.IrTypeID.Int32TyID), Operator.OpCode.SUB),
                register);
    }


    /**
     * 形如：%5 = sub nsw i32 0, %4，表示%5寄存器是%4寄存器的取反
     * <p>
     * 对%4取反就是：
     * <p>
     * %5 = icmp ne i32 %4, 0 ;如果%4不等于0，那么%5就是true
     * <p>
     * %6 = xor i1 %5, true
     *
     * @param block    所属基本块
     * @param variable 要取反的寄存器。<font color='red'>不能是一个数值</font>
     * @return 返回一个result的Variable，这个Variable是<font color='red'>寄存器</font>，里面存放了结果
     */
    public Variable buildNotInst(BasicBlock block, Variable variable) {
        assert variable.getNumber().isEmpty();
        var zeroConst = new ConstValue(0, IrType.IrTypeID.Int32TyID);
        var op1 = buildCmpInst(block, variable, IcmpInstruction.Cond.NE, zeroConst);
        var trueVariable = new ConstValue(1, IrType.IrTypeID.BitTyID);
        LOGGER.fine("build not instruction: " + trueVariable.getName() + " in block: " + block.getName());
        return buildBinInstruction(block, op1, Operator.create(IrType.create(IrType.IrTypeID.BitTyID), Operator.OpCode.XOR), trueVariable);
    }

    /**
     * 专门构建与或非等逻辑运算的函数。
     * <p>
     * 1. <font color='red'>等和不等运算</font>：直接build icmp instruction，得到的结果即为返回结果
     * <p>
     * 2. 与或运算：与0进行icmp之后，得到bit变量，再进行buildBinInst（and/or）得到bit的变量结果
     *
     * @param block 所属基本块
     * @param a     左操作数
     * @param b     右操作数
     * @param op    操作码（{@link IcmpInstruction.Cond}）
     * @return 返回一个result的Variable，这个Variable是<font color='red'>寄存器</font>，里面存放了结果
     */
    public Variable buildLogicInst(BasicBlock block, Variable a, IcmpInstruction.Cond op, Variable b) {
        LOGGER.fine("build logic instruction: " + op + " in block: " + block.getName());
        return buildCmpInst(block, a, op, b);
    }

    /**
     * 专门构建与或非等逻辑运算的函数。
     * <p>
     * 1. 等和不等运算：直接build icmp instruction，得到的结果即为返回结果
     * <p>
     * 2. <font color='red'>与或运算</font>：与0进行icmp之后，得到bit变量，再进行buildBinInst（and/or）得到bit的变量结果
     *
     * @param block 所属基本块
     * @param a     左操作数
     * @param b     右操作数
     * @param op    操作码（{@link Operator.OpCode}）
     * @return 返回一个result的Variable，这个Variable是<font color='red'>寄存器</font>，里面存放了结果
     */
    public Variable buildLogicInst(BasicBlock block, Variable a, Operator op, Variable b) {
        LOGGER.fine(a.getType().toIrCode() + " " + a.getName() + ", " + b.getType().toIrCode() + " " + b.getName() + ", " + op.toIrCode());
        assert op.opCode == Operator.OpCode.AND || op.opCode == Operator.OpCode.OR;
        Variable aBit = a, bBit = b;
        //第一层封装：
        //如果a或者b是0/1的const常量，则需要先把它们转换成bit类型的变量
        if (a instanceof ConstValue) {
            assert a.getNumber().isPresent();
            Integer num = a.getNumber().get();
            aBit = num == 0 ? buildConstValue(0, IrType.IrTypeID.BitTyID) : buildConstValue(1, IrType.IrTypeID.BitTyID);
        }

        if (b instanceof ConstValue) {
            assert b.getNumber().isPresent();
            Integer num = b.getNumber().get();
            bBit = num == 0 ? buildConstValue(0, IrType.IrTypeID.BitTyID) : buildConstValue(1, IrType.IrTypeID.BitTyID);
        }

        //第二层封装：
        //如果a或者b已经是bit类型了，则不需要再进行与0的比较了
        //否则，需要进行与0的比较，得到bit类型的变量
        Variable zero = buildConstValue(0, IrType.IrTypeID.BitTyID);
        if (aBit.getType().getBasicType() != IrType.IrTypeID.BitTyID) {
            aBit = buildCmpInst(block, aBit, IcmpInstruction.Cond.NE, zero);
        }
        if (bBit.getType().getBasicType() != IrType.IrTypeID.BitTyID) {
            bBit = buildCmpInst(block, bBit, IcmpInstruction.Cond.NE, zero);
        }

        //第三层封装：
        //如果a和b都是constValue，则直接进行与或运算，不用buildBinInstruction
        if (aBit instanceof ConstValue && bBit instanceof ConstValue) {
            assert aBit.getNumber().isPresent() && bBit.getNumber().isPresent();
            Integer num1 = aBit.getNumber().get();
            Integer num2 = bBit.getNumber().get();
            int result = op.opCode == Operator.OpCode.AND ? num1 & num2 : num1 | num2;
            return buildConstValue(result, IrType.IrTypeID.BitTyID);
        }

        LOGGER.fine("build logic instruction: " + op.toIrCode() + " in block: " + block.getName());
        return buildBinInstruction(block, aBit, op, bBit);
    }


    /**
     * 形如：  %4 = icmp slt i32 %3, 2      ; ===> if %3 < 2
     *
     * @param block     所属块
     * @param a         左值
     * @param condition 比较条件
     * @param b         右值
     * @return 结果variable。是一个bit类型的变量
     */
    public Variable buildCmpInst(BasicBlock block, Variable a, IcmpInstruction.Cond condition, Variable b) {
        LOGGER.fine("compare " + a.getType().toIrCode() + " " + a.toIrCode() + " " + condition.toIrCode() + " " + b.getType().toIrCode() + " " +
                b.toIrCode());
        IrType.IrTypeID at = a.getType().getBasicType();
        IrType.IrTypeID bt = b.getType().getBasicType();
        if (at.superior(bt)) {
            b = buildZextInst(block, b, a);
        } else if (bt.superior(at)) {
            a = buildZextInst(block, a, b);
        }

        Variable res = new Variable(IrType.create(IrType.IrTypeID.BitTyID), allocator.allocate());
        IcmpInstruction icmpInstruction = new IcmpInstruction(res, a, b, condition);
        block.addInstruction(icmpInstruction);
        LOGGER.fine("build icmp instruction: " + icmpInstruction.toIrCode() + " in block: " + block.getName());
        return res;
    }

    private Variable buildZextInst(BasicBlock block, Variable inferior, Variable superior) {
        Variable result = new Variable(superior.getType(), allocator.allocate());
        ZextInstruction zextInstruction = new ZextInstruction(inferior, result);
        block.addInstruction(zextInstruction);
        return result;
    }

    public void buildRetInstOfConst(BasicBlock basicBlock, int resultNumber) {
        Variable variable = new Variable(IrType.create(IrType.IrTypeID.Int32TyID), String.valueOf(resultNumber), false);
        variable.setNumber(resultNumber);
        var ret = new RetInstruction(variable);
        LOGGER.fine("build ret instruction: " + ret.toIrCode() + " in block: " + basicBlock.getName());
        basicBlock.addInstruction(ret);
    }

    public Variable buildConstIntNum(int number) {
        LOGGER.fine("build const int number: " + number);
        return new ConstValue(number, IrType.IrTypeID.Int32TyID);
    }

    public Variable buildConstValue(int number, IrType.IrTypeID typeID) {
        assert number == 0 || number == 1;
        LOGGER.fine("build const number: " + number + " of type: " + typeID.toIrCode());
        return new ConstValue(number, typeID);
    }

    public void buildRetInstOfConst(BasicBlock basicBlock, Variable variable) {
        var ret = new RetInstruction(variable);
        LOGGER.fine("build ret instruction: " + ret.toIrCode() + " in block: " + basicBlock.getName());
        basicBlock.addInstruction(ret);
    }

    /**
     * 建立{@link BinInstruction}。[result] = add [ty] [op1], [op2]
     * <p>
     * <font color='red'>涵盖了所有情况：op1、op2都为寄存器；只有一个为寄存器，等等。因为输出是在{@link BinInstruction}里异构的，于是屏蔽了这些细节。</font>
     * <p>
     * 形如：%3 = add i32 %1, %2
     * <p>
     * %3 = add i32 1, %2
     * <p>
     * %3 = add i32 1, 2
     * <p>
     * %4 = sub i32 %3, 1
     *
     * @param basicBlock 所属基本块
     * @param a          第一个操作数（数值，或者寄存器）
     * @param b          第二个操作数（数值，或者寄存器）
     * @param op         操作码，如add，sub等。通过Operator获得
     * @return 返回一个result的Variable，这个Variable是<font color='red'>寄存器或者值</font>，里面存放了结果
     */
    public Variable buildBinInstruction(BasicBlock basicBlock, Variable a, Operator op, Variable b) {
        assert a.getType().getBasicType() == b.getType().getBasicType(); //可能后续会出现需要强转的情况，之后再看
        assert a.getNumber().isEmpty() || b.getNumber().isEmpty(); //不能出现二者同时有初值的情况
        var result = new Variable(a.getType(), allocator.allocate()); //新建一个result变量
        var bins = new BinInstruction(result, a, b, op);
        basicBlock.addInstruction(bins);
        LOGGER.fine("build bin instruction: " + bins.toIrCode() + " in block: " + basicBlock.getName());
        return result;
    }

    /**
     * <font color='red'>没有初始值</font>的时候调用这个函数，此时只需要分配地址。注意build会产生一个指针类型的value。
     *
     * @param basicBlock 所属基本块
     * @param varType    类型，如i32
     * @return 一个指针（pointerValue）
     */
    public PointerValue buildLocalVariable(BasicBlock basicBlock, IrType.IrTypeID varType) {
        LOGGER.fine("build local variable: " + varType + " in block: " + basicBlock.getName());
        return buildAllocaInst(basicBlock, IrType.create(varType));
    }


    public PointerValue buildLocalArray(BasicBlock basicBlock, IrType.IrTypeID basicType, int arrSize) {
        LOGGER.fine("build local array: " + basicType + " in block: " + basicBlock.getName());
        return buildAllocaInst(basicBlock, IrType.create(basicType, IrType.IrTypeID.ArrayTyID).setDim(arrSize));
    }

    public PointerValue buildElementPointer(BasicBlock basicBlock, PointerValue arrayPointer, NodeUnion nodeUnion) {
        if (nodeUnion.isNum) {
            return buildElementPointer(basicBlock, arrayPointer, nodeUnion.getNumber());
        } else {
            return buildElementPointer(basicBlock, arrayPointer, nodeUnion.getVariable());
        }
    }

    //是要从pointer里取出偏移
    public PointerValue buildElementPointer(BasicBlock basicBlock, PointerValue arrayPointer, Variable offIndex) {
        LOGGER.fine("build element arrayPointer: " + arrayPointer.getName() + " in block: " + basicBlock.getName());
        PointerValue result = new PointerValue(IrType.create(IrType.IrTypeID.Int32TyID), allocator.allocate());
        GetElementPtrInstruction inst;
        if (arrayPointer.getType().isArray()) {
            ConstValue off0 = new ConstValue(0, IrType.IrTypeID.Int32TyID);
            inst = new GetElementPtrInstruction(result, arrayPointer, off0, offIndex);
        } else {
            inst = new GetElementPtrInstruction(result, arrayPointer, offIndex);
        }
        basicBlock.addInstruction(inst);
        LOGGER.fine("build get element ptr: " + inst.toIrCode());
        return result;
    }

    public PointerValue buildElementPointer(BasicBlock basicBlock, PointerValue arrayPointer, int offIndex) {
        ConstValue constValue = new ConstValue(offIndex, IrType.IrTypeID.Int32TyID);
        return buildElementPointer(basicBlock, arrayPointer, constValue);
    }

    /**
     * 如果我的%1是一个（一维）数组指针，我要把某一个值从中load出来，就要调用这个指令
     *
     * @param basicBlock   所属块
     * @param arrayPointer 数组指针
     * @param offset       偏移
     * @return 寄存器
     */
    public Variable buildLoadArrayInsts(BasicBlock basicBlock, PointerValue arrayPointer, NodeUnion offset) {
        if (offset.isNum) {
            ConstValue offConst = new ConstValue(offset.getNumber(), IrType.IrTypeID.Int32TyID);
            return buildLoadArrayInsts(basicBlock, arrayPointer, offConst);
        } else {
            return buildLoadArrayInsts(basicBlock, arrayPointer, offset.getVariable());
        }
    }

    private Variable buildLoadArrayInsts(BasicBlock basicBlock, PointerValue arrayPointer, Variable offset) {
        if (arrayPointer.getType().isArray()) {
            //array不需要load出来
            PointerValue elementPointer = buildElementPointer(basicBlock, arrayPointer, offset);
            return pointerToVariable(elementPointer);
        }
        //如果是指针，则首先要从arrayPointer里load出来
        Variable arrayPosition = buildLoadInst(basicBlock, arrayPointer);
        //然后要构建getelementptr
        PointerValue elementPointer = buildElementPointer(basicBlock, variableToPointer(arrayPosition), offset);
        //最后return todo 这里想清楚是return什么，需不需要load出来。如果需要值就要load出来（在调用的地方load），否则不需要
        return pointerToVariable(elementPointer);
    }

    public void buildArrayStoreInsts(BasicBlock basicBlock, PointerValue arrayPointer, NodeUnion... inits) {
        //先从pointer里取出偏移，然后再build store inst
        for (var i = 0; i < inits.length; i++) {
            ConstValue constValue = new ConstValue(i, IrType.IrTypeID.Int32TyID);
            PointerValue pointerValue = buildElementPointer(basicBlock, arrayPointer, constValue);
            Variable variable;
            if (inits[i].isNum) {
                variable = new ConstValue(inits[i].getNumber(), IrType.IrTypeID.Int32TyID);
                arrayPointer.setNumber(inits.length, i, inits[i].getNumber());
            } else {
                variable = inits[i].getVariable();
            }
            buildStoreInst(basicBlock, variable, pointerValue);
        }
    }

    /**
     * <font color='red'>有初始值</font>的时候调用这个函数。除了需要分配地址之外，还需要用store指令将初值放进刚刚分配的地址之内。
     * <p>
     * 此时store指令操作的地址既有可能是一个数字（如3），又有可能是一个寄存器（如%5）
     * <p>
     * 例如：
     * <p>
     * int i = 1;
     * <p>
     * %1 = alloca i32
     * <p>
     * store i32 1, i32* %1
     * <p>
     * 注意build会产生一个指针类型的value。
     *
     * @param basicBlock 所属基本块
     * @param varType    类型，如i32
     * @param value      值。既有可能是一个数字（如3），又有可能是一个寄存器（如%5），需要通过前面是否有%来区分。
     */
    public PointerValue buildLocalVariable(BasicBlock basicBlock, IrType.IrTypeID varType, int value) {
        var pointer = buildLocalVariable(basicBlock, varType);
        ConstValue constValue = new ConstValue(value, varType);
        buildStoreInst(basicBlock, constValue, pointer);
        LOGGER.fine("build local variable: " + varType + " in block: " + basicBlock.getName() + " with initial value: " + value);
        return pointer;
    }


    /**
     * 形如：%1 = alloca i32，建立的数据是一个<font color='red'>指针类型</font>
     *
     * @param basicBlock 指令所属的块
     */
    private PointerValue buildAllocaInst(BasicBlock basicBlock, IrType varType) {
        PointerValue pointerValue = new PointerValue(varType, allocator.allocate());
        AllocaInstruction allocaInstruction = new AllocaInstruction(pointerValue);
        basicBlock.addInstruction(allocaInstruction);
        LOGGER.fine("build alloca instruction: " + allocaInstruction.toIrCode() + " in block: " + basicBlock.getName());
        return pointerValue;
    }

    /**
     * 建立全局常量<font color='red'>及其declare声明指令</font>。其实和global variable一样，会出现符号表中存放的是值还是指针的问题。只不过const
     * value在使用时就替换掉了，所以可能不用仔细考虑。
     * <p>
     * 形如：@a = dso_local constant i32 1
     *
     * @param module 模块
     * @param type   类型，如i32
     * @param name   变量名，如@a。这里需要name，是因为此name不是由allocator分配的
     * @param number 常量值，如1
     * @return 返回的不是指令，而是<font color='red'>Build出的GlobalVariable</font>
     */
    public GlobalValue buildGlobalConstantValue(Module module, IrType.IrTypeID type, String name, int number) {
        var globalVariable = new GlobalValue(IrType.create(type), "@" + name, true);
        globalVariable.setNumber(number);
        module.insertGlobal(globalVariable);
        var inst = new GlobalDeclInstruction(globalVariable, true);
        module.insertGlobalInst(inst);
        LOGGER.fine("build global constant value: " + globalVariable.getName() + " in module: " + module.getName());
        return globalVariable;
    }

    /**
     * 形如 int b[10][20]; => @b = dso_local global [200 i32] zeroinitializer
     * <p>
     * int a[1+2+3+4]={1,1+1,1+3-1,0,0,0,0,0,0,0};
     * <p>
     * => @a = dso_local global [10 x i32] [i32 1, i32 2, i32 3, i32 0, i32 0, i32 0, i32 0, i32 0, i32 0, i32 0]
     * <p>
     * 我们全部按照<font color=red>一维数组</font>处理，所以不用dim，默认为1
     *
     * @param module   所属模块
     * @param irTypeID 类型，如i32
     * @param arrSize  数组大小
     * @param name     变量名，如@a。这里需要name，是因为此name不是由allocator分配的
     * @return 返回的不是指令，而是<font color='red'>Build出的GlobalVariable</font>
     */
    public GlobalValue buildGlobalArray(Module module, IrType.IrTypeID irTypeID, boolean isConst, int arrSize,
                                        String name, Integer... initNums) {
        var globalArr = new GlobalValue(IrType.create(irTypeID, IrType.IrTypeID.ArrayTyID).setDim(arrSize),
                "@" + name, isConst);
        if (initNums == null || initNums.length == 0) {
            Integer[] zeros = new Integer[arrSize];
            Arrays.fill(zeros, 0);
            globalArr.setNumber(zeros);
        } else {
            globalArr.setNumber(initNums);
        }

        module.insertGlobal(globalArr);
        var inst = new GlobalDeclInstruction(globalArr, isConst);
        module.insertGlobalInst(inst);
        LOGGER.fine("build global array: " + globalArr.getName() + " in module: " + module.getName());
        return globalArr;
    }

    /**
     * 形如：@a = dso_local global i32 0
     *
     * @param module 模块
     * @param type   类型，如i32
     * @param name   变量名，如@a。这里需要name，是因为此name不是由allocator分配的 number – 常量值，如1
     * @return 返回的不是指令，而是<font color='red'>Build出的GlobalVariable</font>
     */
    public GlobalValue buildGlobalVariable(Module module, IrType.IrTypeID type, String name) {
        var globalVariable = new GlobalValue(IrType.create(type), "@" + name, false);
        var inst = new GlobalDeclInstruction(globalVariable, false);
        module.insertGlobalInst(inst);
        module.insertGlobal(globalVariable);
        LOGGER.fine("build global variable: " + globalVariable.getName() + " in module: " + module.getName());
        return globalVariable;
    }

    /**
     * 形如：@a = dso_local global i32 0
     *
     * @param module   模块
     * @param irTypeID 类型，如i32
     * @param name     变量名，如@a。这里需要name，是因为此name不是由allocator分配的 number – 常量值，如1
     * @return 返回的不是指令，而是<font color='red'>Build出的GlobalVariable</font>
     */
    public GlobalValue buildGlobalVariable(Module module, IrType.IrTypeID irTypeID, String name, int number) {
        var globalVariable = new GlobalValue(IrType.create(irTypeID), "@" + name, false);
        globalVariable.setNumber(number); //设置number
        var inst = new GlobalDeclInstruction(globalVariable, false);
        module.insertGlobalInst(inst);
        module.insertGlobal(globalVariable);
        LOGGER.fine("build global variable: " + globalVariable.getName() + " in module: " + module.getName());
        return globalVariable;
    }


    public Module buildModule(IrContext context) {
        Module module = new Module();
        context.setIrModule(module);
        module
                .insertGlobalInst(new FuncDeclInstruction(IrType.create(IrType.IrTypeID.Int32TyID), "@getint").addArg(new Argument(IrType.create(IrType.IrTypeID.VoidTyID), "a")))
                .insertGlobalInst(new FuncDeclInstruction(IrType.create(IrType.IrTypeID.VoidTyID), "@putint").addArg(new Argument(IrType.create(IrType.IrTypeID.Int32TyID), "a")))
                .insertGlobalInst(new FuncDeclInstruction(IrType.create(IrType.IrTypeID.VoidTyID), "@putch").addArg(new Argument(IrType.create(IrType.IrTypeID.Int32TyID), "a")))
                .insertGlobalInst(new FuncDeclInstruction(IrType.create(IrType.IrTypeID.VoidTyID), "@putstr").addArg(new Argument(IrType.create(IrType.IrTypeID.ByteTyID), "a")));
        module
                .insertFunc(new IrFunction(IrType.create(IrType.IrTypeID.Int32TyID, IrType.IrTypeID.FunctionTyID), "@getint", module))
                .insertFunc(new IrFunction(IrType.create(IrType.IrTypeID.VoidTyID, IrType.IrTypeID.FunctionTyID), "@putint", module))
                .insertFunc(new IrFunction(IrType.create(IrType.IrTypeID.VoidTyID, IrType.IrTypeID.FunctionTyID), "@putch", module))
                .insertFunc(new IrFunction(IrType.create(IrType.IrTypeID.VoidTyID, IrType.IrTypeID.FunctionTyID), "@putstr", module));
        LOGGER.fine("build module: " + module.getName());
        return module;
    }

    /**
     * 形如 %8 = load i32, i32* %3, align 4。%8由allocator自动分配。
     *
     * @param block   指令所属块。
     * @param pointer 右操作数。指针。
     * @return 返回load进的寄存器（左操作数）
     */
    public Variable buildLoadInst(BasicBlock block, PointerValue pointer) {
        var result = new Variable(pointer.getType(), allocator.allocate());
        LoadInstruction loadInstruction = new LoadInstruction(result, pointer);
        block.addInstruction(loadInstruction);
        LOGGER.fine("build load instruction: " + loadInstruction.toIrCode() + " in block: " + block.getName());
        return result;
    }

    public void buildVoidRetInst(BasicBlock block) {
        RetInstruction retInst = new RetInstruction();
        block.addInstruction(retInst);
        LOGGER.fine("build void ret instruction: " + retInst.toIrCode() + " in block: " + block.getName());
    }

    public Variable buildCallInst(BasicBlock block, String funName, NodeUnion... paramVariables) {
        assert SymbolTable.getGlobal().getSymbol(funName).isPresent();
        var symbol = (FuncSymbol) SymbolTable.getGlobal().getSymbol(funName).get();
        List<VarSymbol> fparams = symbol.getParams();
        List<Variable> rparams = new ArrayList<>();

        for (int i = 0; i < symbol.getParamCount(); i++) {
            var fp = fparams.get(i);
            var rp = paramVariables[i];

            if (fp.getDim() == 0 && rp.isNum) {
                //需要数字，结果也是数字
                rparams.add(new ConstValue(rp.getNumber(), IrType.IrTypeID.Int32TyID));
            } else if (fp.getDim() == 0 && !rp.isNum) {
                //需要数字，结果不是数字
                Variable variable = rp.getVariable();
                if (variable.getType().isNumber()) {
                    rparams.add(variable);
                } else {
                    var numberVar = buildLoadInst(block, variableToPointer(variable));
                    rparams.add(numberVar);
                }
            } else if (!rp.isNum) {
                //不需要数字，结果肯定不会是数字
                rparams.add(rp.getVariable());
            }
        }

        IrContext context = IrTranslator.context;
        Module module = context.getIrModule();
        IrFunction func = module.getFunc("@" + funName);
        if (func.getReturnType().getBasicType() == IrType.IrTypeID.VoidTyID) {
            buildVoidCallInst(block, funName, rparams.toArray(new Variable[0]));
            return null;
        }
        Variable variable = new Variable(func.getType(), allocator.allocate());
        CallInstruction callInstruction = new CallInstruction(func, variable, rparams.toArray(new Variable[0]));
        block.addInstruction(callInstruction);
        LOGGER.fine("build call instruction: " + callInstruction.toIrCode() + " in block: " + block.getName());
        return variable;
    }

    public Variable buildCallCoreInst(BasicBlock block, String funName, Variable... paramVariables) {
        IrContext context = IrTranslator.context;
        Module module = context.getIrModule();
        IrFunction func = module.getFunc("@" + funName);
        if (func.getReturnType().getBasicType() == IrType.IrTypeID.VoidTyID) {
            buildVoidCallInst(block, funName, paramVariables);
            return null;
        }
        Variable variable = new Variable(func.getType(), allocator.allocate());
        CallInstruction callInstruction = new CallInstruction(func, variable, paramVariables);
        block.addInstruction(callInstruction);
        LOGGER.fine("build call instruction: " + callInstruction.toIrCode() + " in block: " + block.getName());
        return variable;
    }

    private void buildVoidCallInst(BasicBlock block, String funName, Variable... paramVariables) {
        IrContext context = IrTranslator.context;
        Module module = context.getIrModule();
        IrFunction func = module.getFunc("@" + funName);
        CallInstruction callInstruction = new CallInstruction(func, paramVariables);
        block.addInstruction(callInstruction);
        LOGGER.fine("build void call instruction: " + callInstruction.toIrCode() + " in block: " + block.getName());
    }

    /**
     * 把一个寄存器的值赋给一个变量，形如：store i32 %5, i32* %4
     *
     * @param basicBlock   所属块
     * @param variable     变量
     * @param pointerValue 寄存器
     */
    public void buildStoreInst(BasicBlock basicBlock, Variable variable, PointerValue pointerValue) {
        StoreInstruction storeInstruction = new StoreInstruction(variable, pointerValue);
        basicBlock.addInstruction(storeInstruction);
        LOGGER.fine("build store instruction: " + storeInstruction.toIrCode() + " in block: " + basicBlock.getName());
    }

    public Variable pointerToVariable(PointerValue pointer) {
        return new Variable(
                IrType.create(pointer.getType().getBasicType(), IrType.IrTypeID.PointerTyID),
                pointer.getName(),
                false);
    }

    public PointerValue variableToPointer(Variable variable) {
        return new PointerValue(
                IrType.create(variable.getType().getBasicType()),
                variable.getName()
        );
    }

    public Variable toBitVariable(BasicBlock basicBlock, Variable rawVariable) {
        if (rawVariable.getType().getBasicType() == IrType.IrTypeID.BitTyID) {
            return rawVariable;
        }
        //先cmp
        IrType.IrTypeID typeID = rawVariable.getType().getBasicType();
        return buildCmpInst(basicBlock, new ConstValue(0, typeID), IcmpInstruction.Cond.NE, rawVariable);
    }

    public void removeBlock(BasicBlock belonging, BasicBlock removed, String reason) {
        LOGGER.info("Remove block " + removed.getName() + " from block " + belonging.getName() + " because " + reason);
        belonging.removeBlock(removed);
        allocator.rewind();
    }
}
