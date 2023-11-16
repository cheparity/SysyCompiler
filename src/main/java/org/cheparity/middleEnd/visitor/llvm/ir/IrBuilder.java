package middleEnd.visitor.llvm.ir;

import middleEnd.visitor.llvm.IrContext;

public class IrBuilder {
    public IrBuilder() {
    }

    public IntConstant buildIntConstant(int number) {
        return new IntConstant(null, number);
    }

    public Function buildFunction(IrType irType, String name, Module module) {
        var func = new Function(irType, name, module);
        module.insertFunc(func);
        return func;
    }


    public BasicBlock buildBasicBlock(String name, Function function) {
        var bb = new BasicBlock(name);
        function.setEntryBlock(bb);
        return bb;
    }

    public void buildRetInstOfConst(BasicBlock basicBlock, int number) {
        var intConst = new IntConstant(null, number); //todo constInt是value（不一定是new constant int，也有可能是个寄存器）
        var ret = new RetInstruction(intConst);
        basicBlock.addInstruction(ret);
    }

    /**
     * 建立Binary Instruction。[result] = add [ty] [op1], [op2]
     * <p>
     * 形如：%3 = add i32 %1, %2
     * <p>
     * %4 = sub i32 %3, 1
     *
     * @param basicBlock 所属基本块
     * @param value1     第一个操作数（数值，或者寄存器）
     * @param value2     第二个操作数（数值，或者寄存器）
     * @param opCode     操作码，如add，sub等
     */
    public void buildBinInstruction(BasicBlock basicBlock, String result, int value1,
                                    int value2, OpCode opCode) {
//        new BinInstruction(result, new ConstantInt(value1), new ConstantInt(value2), opCode);
    }

    /**
     * <font color='red'>没有初始值</font>的时候调用这个函数，此时只需要分配地址。注意build会产生一个指针类型的value。
     *
     * @param basicBlock 所属基本块
     * @param varType    类型，如i32
     * @param name       名，如%1。此名字一定是一个寄存器
     */
    public Value buildLocalVariable(BasicBlock basicBlock, IrType varType, String name) {
        // todo 直接创建指令，符号表的更新操作在visitor里做
        PointerValue pointerValue = new PointerValue(varType, name);
        buildAllocaInstruction(basicBlock, pointerValue);

        return null;
    }

    /**
     * <font color='red'>有初始值</font>的时候调用这个函数。除了需要分配地址之外，还需要用store指令将初值放进刚刚分配的地址之内。
     * <p>
     * 此时store指令操作的地址既有可能是一个数字（如3），又有可能是一个寄存器（如%5）
     * <p>
     * 注意build会产生一个指针类型的value。
     *
     * @param basicBlock 所属基本块
     * @param varType    类型，如i32
     * @param name       名，如%1。此名字一定是一个寄存器
     * @param value      值。既有可能是一个数字（如3），又有可能是一个寄存器（如%5），需要通过前面是否有%来区分。
     */
    public void buildLocalVariable(BasicBlock basicBlock, IrType varType, String name, String value) {
        DataValue dataValue = new DataValue(IrType.Int32TyID, name); //请注意这个name，既有可能是一个数字（如3），又有可能是一个寄存器（如%5）
        PointerValue pointerValue = new PointerValue(varType, name);
        buildAllocaInstruction(basicBlock, pointerValue);
        buildStoreInstruction(basicBlock, dataValue, pointerValue);
    }


    /**
     * 形如store i32 0, i32* %7
     *
     * @param basicBlock   所属块
     * @param value        第一个操作数（数值，或者寄存器）
     * @param pointerValue 第二个操作数（地址）
     */
    private void buildStoreInstruction(BasicBlock basicBlock, Value value, PointerValue pointerValue) {
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
     * 形如：@a = dso_local constant i32 1
     *
     * @param module 模块
     * @param type   类型，如i32
     * @param name   变量名，如@a
     * @param number 常量值，如1
     */
    public Value buildGlobalConstantValue(Module module, IrType type, String name, int number) {
        GlobalIntConstant globalIntConstant = new GlobalIntConstant(type, name, number);
        module.insertGlobal(globalIntConstant);
        var inst = new GlobalDeclInstruction(globalIntConstant, OpCode.CONDEF);
        module.insertGlobalInst(inst);
        return globalIntConstant;
    }

    /**
     * 形如：@a = dso_local global i32 0
     *
     * @param module 模块
     * @param irType 类型，如i32
     * @param name   变量名，如@a
     */
    public void buildGlobalVariable(Module module, IrType irType, String name) {
        var globalVariable = new GlobalIntVariable(irType, name);
        var inst = new GlobalDeclInstruction(globalVariable, OpCode.GLODEF);
        module.insertGlobalInst(inst);
        module.insertGlobal(globalVariable);
    }

    public void buildGlobalVariable(Module module, IrType irType, String name, int number) {
        var globalVariable = new GlobalIntVariable(irType, name);
        globalVariable.setNumber(number); //设置number
        var inst = new GlobalDeclInstruction(globalVariable, OpCode.GLODEF);
        module.insertGlobalInst(inst);
        module.insertGlobal(globalVariable);
    }


    public Module buildModule(IrContext context) {
        return new Module();
    }


}
