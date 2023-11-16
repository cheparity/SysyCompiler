package middleEnd.llvm.ir;

import frontEnd.symbols.SymbolTable;
import middleEnd.llvm.IrContext;
import middleEnd.llvm.RegisterAllocator;

public class IrBuilder {
    private RegisterAllocator allocator;

    public IrBuilder() {

    }

    public IrBuilder(RegisterAllocator allocator) {
        this.allocator = allocator;
    }

    public Function buildFunction(IrType irType, String name, Module module) {
        String funcName = allocator.allocate(name);
        var func = new Function(irType, funcName, module);
        module.insertFunc(func);
        return func;
    }

    public BasicBlock buildEntryBlock(Function function, SymbolTable symbolTable) {
        var bb = new BasicBlock("entry");
        function.setEntryBlock(bb);
        bb.setSymbolTable(symbolTable);
        return bb;
    }

    public BasicBlock buildBasicBlock(String name, Function function) {
        var bb = new BasicBlock(name);
        function.setEntryBlock(bb);
        return bb;
    }

    //todo 思路：需要Util.Cal方法，返回一个ret的Variable值，传递给该函数。Variable包含了ret的寄存器或数值信息
    public void buildRetInstOfConst(BasicBlock basicBlock, int number) {
//        var intConst = new IntConstant(null, number); //todo constInt是value（不一定是new constant int，也有可能是个寄存器）
//        var ret = new RetInstruction(intConst);
//        basicBlock.addInstruction(ret);
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
     * @param name       名，如%1。此名字一定是一个寄存器
     */
    public Variable buildLocalVariable(BasicBlock basicBlock, IrType varType) {
        var name = allocator.allocate();
        Variable variable = new Variable(varType, name);
        buildAllocaInstruction(basicBlock, variable.toPointer());
        return variable;
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
     * @param name       名，如%1。此名字一定是一个寄存器
     * @param value      值。既有可能是一个数字（如3），又有可能是一个寄存器（如%5），需要通过前面是否有%来区分。
     */
    public Variable buildLocalVariable(BasicBlock basicBlock, IrType varType, int value) {
        var variable = buildLocalVariable(basicBlock, varType);
        buildAllocaInstruction(basicBlock, variable.toPointer());
        variable.setNumber(value); //给variable赋值
        return variable;
    }


    /**
     * 形如store i32 0, i32* %7
     *
     * @param basicBlock   所属块
     * @param value        第一个操作数（数值，或者寄存器）
     * @param pointerValue 第二个操作数（地址）
     */
    private void buildStoreInstruction(BasicBlock basicBlock, Variable value, PointerValue pointerValue) {
        StoreInstruction storeInstruction = new StoreInstruction(value, pointerValue);
        basicBlock.addInstruction(storeInstruction);
    }

    /**
     * 形如：%1 = alloca i32，建立的数据是一个<font color='red'>指针类型</font>
     *
     * @param basicBlock 指令所属的块
     */
    private void buildAllocaInstruction(BasicBlock basicBlock, PointerValue pointerValue) {
        AllocaInstruction allocaInstruction = new AllocaInstruction(pointerValue);
        basicBlock.addInstruction(allocaInstruction);
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
    public GlobalVariable buildGlobalConstantValue(Module module, IrType type, String name, int number) {
        var globalVariable = new GlobalVariable(type, name, true);
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
     * @param irType 类型，如i32
     * @param name   变量名，如@a。这里需要name，是因为此name不是由allocator分配的 number – 常量值，如1
     * @return 返回的不是指令，而是<font color='red'>Build出的GlobalVariable</font>
     */
    public GlobalVariable buildGlobalVariable(Module module, IrType irType, String name) {
        var globalVariable = new GlobalVariable(irType, name, false);
        var inst = new GlobalDeclInstruction(globalVariable, false);
        module.insertGlobalInst(inst);
        module.insertGlobal(globalVariable);
        return globalVariable;
    }

    /**
     * 形如：@a = dso_local global i32 0
     *
     * @param module 模块
     * @param irType 类型，如i32
     * @param name   变量名，如@a。这里需要name，是因为此name不是由allocator分配的 number – 常量值，如1
     * @return 返回的不是指令，而是<font color='red'>Build出的GlobalVariable</font>
     */
    public GlobalVariable buildGlobalVariable(Module module, IrType irType, String name, int number) {
        var globalVariable = new GlobalVariable(irType, name, false);
        globalVariable.setNumber(number); //设置number
        var inst = new GlobalDeclInstruction(globalVariable, false);
        module.insertGlobalInst(inst);
        module.insertGlobal(globalVariable);
        return globalVariable;
    }


    public Module buildModule(IrContext context) {
        Module module = new Module();
        context.setIrModule(module);
        return module;
    }


}
