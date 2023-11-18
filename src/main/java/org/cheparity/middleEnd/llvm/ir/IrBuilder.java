package middleEnd.llvm.ir;

import middleEnd.llvm.IrContext;
import middleEnd.llvm.RegisterAllocator;
import middleEnd.symbols.SymbolTable;

public class IrBuilder {
    private RegisterAllocator allocator;

    public IrBuilder() {

    }

    public IrBuilder(RegisterAllocator allocator) {
        this.allocator = allocator;
    }

    public Function buildFunction(IrType.IrTypeID type, String name, Module module) {
        String funcName = allocator.allocate(name);
        var func = new Function(IrType.create(type), funcName, module);
        module.insertFunc(func);
        return func;
    }

    public NestBlock buildNestBlock(BasicBlock fatherBlock, SymbolTable symbolTable) {
        NestBlock nestBlock = new NestBlock(fatherBlock.getName() + "-nested", fatherBlock);
        nestBlock.setSymbolTable(symbolTable);
        return nestBlock;
    }

    public BasicBlock buildEntryBlock(Function function, SymbolTable symbolTable) {
        var bb = new BasicBlock("entry");
        function.setEntryBlock(bb);
        bb.setSymbolTable(symbolTable);
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
        var zeroConst = new Variable(IrType.create(IrType.IrTypeID.Int32TyID), "0", true);
        assert zeroConst.getNumber().isEmpty();
        zeroConst.setNumber(0);
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
        var zeroConst = new Variable(IrType.create(IrType.IrTypeID.Int32TyID), "0", true);
        zeroConst.setNumber(0);
        var zero = new Variable(IrType.create(IrType.IrTypeID.Int32TyID), "0", true);
        zero.setNumber(0);
        var op1 = buildCmpInst(block, variable, new IcmpInstruction.Condition(IcmpInstruction.Condition.Cond.NE), zero);
        var trueVariable = new Variable(IrType.create(IrType.IrTypeID.BitTyID), "true", true);
        trueVariable.setNumber(1);
        return buildBinInstruction(block, op1, Operator.create(IrType.create(IrType.IrTypeID.BitTyID), Operator.OpCode.XOR), trueVariable);
    }

    public Variable buildCmpInst(BasicBlock block, Variable a, IcmpInstruction.Condition condition, Variable b) {
        assert a.getType() == b.getType();
        Variable res = new Variable(IrType.create(IrType.IrTypeID.Int32TyID), allocator.allocate());
        IcmpInstruction icmpInstruction = new IcmpInstruction(res, a, b, condition);
        block.addInstruction(icmpInstruction);
        return res;
    }

    public void buildRetInstOfConst(BasicBlock basicBlock, int resultNumber) {
        Variable variable = new Variable(IrType.create(IrType.IrTypeID.Int32TyID), String.valueOf(resultNumber), false);
        variable.setNumber(resultNumber);
        var ret = new RetInstruction(variable);
        basicBlock.addInstruction(ret);
    }

    public Variable buildConstIntNum(int number) {
        return new IntConstValue(number);
    }

    public void buildRetInstOfConst(BasicBlock basicBlock, Variable variable) {
        var ret = new RetInstruction(variable);
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
        assert a.getType() == b.getType(); //可能后续会出现需要强转的情况，之后再看
        assert a.getNumber().isEmpty() || b.getNumber().isEmpty(); //不能出现二者同时有初值的情况
        var result = new Variable(a.getType(), allocator.allocate()); //新建一个result变量
        var bins = new BinInstruction(result, a, b, op);
        basicBlock.addInstruction(bins);
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
        //        buildStoreInst(basicBlock,,pointerValue); //无初值，则只分配一个指针
        return buildAllocaInst(basicBlock, varType);
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
    public Variable buildLocalVariable(BasicBlock basicBlock, IrType.IrTypeID varType, int value) {
        var pointer = buildLocalVariable(basicBlock, varType);
        IntConstValue intConstValue = new IntConstValue(value);
        buildStoreInst(basicBlock, intConstValue, pointer);
        return pointer.pointAt;
    }

    /**
     * 形如store i32 0, i32* %7。会给指针分配指向的值。
     *
     * @param basicBlock   所属块
     * @param value        第一个操作数（数值，或者寄存器）
     * @param pointerValue 第二个操作数（地址）
     */
    private void buildStoreInst(BasicBlock basicBlock, Variable value, PointerValue pointerValue) {
        StoreInstruction storeInstruction = new StoreInstruction(value, pointerValue);
        pointerValue.setPointAt(value);
        basicBlock.addInstruction(storeInstruction);
    }

    /**
     * 形如：%1 = alloca i32，建立的数据是一个<font color='red'>指针类型</font>
     *
     * @param basicBlock 指令所属的块
     */
    private PointerValue buildAllocaInst(BasicBlock basicBlock, IrType.IrTypeID varType) {
        PointerValue pointerValue = new PointerValue(IrType.create(varType), allocator.allocate());
        AllocaInstruction allocaInstruction = new AllocaInstruction(pointerValue);
        basicBlock.addInstruction(allocaInstruction);
        return pointerValue;
    }

    /**
     * 建立全局常量<font color='red'>及其declare声明指令</font>
     * <p>
     * 形如：@a = dso_local constant i32 1
     *
     * @param module 模块
     * @param type   类型，如i32
     * @param name   变量名，如@a。这里需要name，是因为此name不是由allocator分配的
     * @param number 常量值，如1
     * @return 返回的不是指令，而是<font color='red'>Build出的GlobalVariable</font>
     */
    public GlobalVariable buildGlobalConstantValue(Module module, IrType.IrTypeID type, String name, int number) {
        var globalVariable = new GlobalVariable(IrType.create(type), name, true);
        globalVariable.setNumber(number);
        module.insertGlobal(globalVariable);
        var inst = new GlobalDeclInstruction(globalVariable, true);
        module.insertGlobalInst(inst);
        return globalVariable;
    }

    /**
     * 形如：@a = dso_local global i32 0
     *
     * @param module 模块
     * @param type   类型，如i32
     * @param name   变量名，如@a。这里需要name，是因为此name不是由allocator分配的 number – 常量值，如1
     * @return 返回的不是指令，而是<font color='red'>Build出的GlobalVariable</font>
     */
    public GlobalVariable buildGlobalVariable(Module module, IrType.IrTypeID type, String name) {
        var globalVariable = new GlobalVariable(IrType.create(type), "@" + name, false);
        var inst = new GlobalDeclInstruction(globalVariable, false);
        module.insertGlobalInst(inst);
        module.insertGlobal(globalVariable);
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
    public GlobalVariable buildGlobalVariable(Module module, IrType.IrTypeID irTypeID, String name, int number) {
        var globalVariable = new GlobalVariable(IrType.create(irTypeID), "@" + name, false);
        globalVariable.setNumber(number); //设置number
        var inst = new GlobalDeclInstruction(globalVariable, false);
        module.insertGlobalInst(inst);
        module.insertGlobal(globalVariable);
        return globalVariable;
    }


    public Module buildModule(IrContext context) {
        Module module = new Module();
        context.setIrModule(module);
        module
                .insertGlobalInst(new FuncDeclInstruction(IrType.create(IrType.IrTypeID.Int32TyID), "@getint").addArg(new Argument(IrType.create(IrType.IrTypeID.VoidTyID), "a")))
                .insertGlobalInst(new FuncDeclInstruction(IrType.create(IrType.IrTypeID.VoidTyID), "@putint").addArg(new Argument(IrType.create(IrType.IrTypeID.Int32TyID), "a")))
                .insertGlobalInst(new FuncDeclInstruction(IrType.create(IrType.IrTypeID.VoidTyID), "@putch").addArg(new Argument(IrType.create(IrType.IrTypeID.Int32TyID), "a")))
                .insertGlobalInst(new FuncDeclInstruction(IrType.create(IrType.IrTypeID.VoidTyID), "@putst").addArg(new Argument(IrType.create(IrType.IrTypeID.ByteTyID), "a")));

        return module;
    }

    /**
     * 形如 %8 = load i32, i32* %3, align 4
     *
     * @param block   指令所属块。
     * @param result  左操作数。让a = b，<font color='red'>而不分配新变量。</font>
     * @param pointer 右操作数。指针。
     * @return 只是为了重载的一致性，单纯返回a
     */
    public Variable buildLoadInst(BasicBlock block, Variable result, PointerValue pointer) {
        LoadInstruction loadInstruction = new LoadInstruction(result, pointer);
        block.addInstruction(loadInstruction);
        return result;
    }

    public void buildVoidRetInst(BasicBlock block) {
        block.addInstruction(new RetInstruction());
    }
}
