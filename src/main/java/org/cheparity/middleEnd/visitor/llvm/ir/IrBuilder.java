package middleEnd.visitor.llvm.ir;

import middleEnd.visitor.llvm.IrContext;

public class IrBuilder {
    public IrBuilder() {
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
        var constInt = new ConstantInt(number);
        var ret = new RetInstruction(constInt);
        basicBlock.addInstruction(ret);
        //result type: null
        //operand: i32 [number]
    }

    public void buildLocalVariable(Function function, IrType varType, String name) {
        // todo 直接创建指令
        // alloca instruction
        // store instruction
    }

    public void buildGlobalConstantValue(Module module, IrType type, String name, int number) {
        GlobalConstantValue globalConstantValue = new GlobalConstantValue(type, name, number);
        module.insertGlobal(globalConstantValue);
        var inst = new GlobalDeclInstruction(globalConstantValue, OpCode.CONDEF);
        module.insertGlobalInst(inst);
    }

    public void buildGlobalVariable(Module module, IrType irType, String name) {
        var globalVariable = new GlobalVariable(irType, name);
        var inst = new GlobalDeclInstruction(globalVariable, OpCode.GLODEF);
        module.insertGlobalInst(inst);
        module.insertGlobal(globalVariable);
    }


    public Module buildModule(IrContext context) {
        return new Module();
    }


}
