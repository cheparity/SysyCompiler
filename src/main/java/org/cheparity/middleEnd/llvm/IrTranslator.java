package middleEnd.llvm;

import frontEnd.parser.dataStruct.ASTNode;
import middleEnd.llvm.ir.IrBuilder;
import middleEnd.llvm.ir.Module;
import middleEnd.llvm.visitor.FuncVisitor;
import middleEnd.llvm.visitor.GlobalVarVisitor;

public class IrTranslator {
    public static final IrContext context = new IrContext();
    private static IrTranslator instance;

    private IrTranslator() {
    }

    public static IrTranslator getInstance() {
        if (instance == null) {
            instance = new IrTranslator();
            return instance;
        }
        return instance;
    }

    public IrContext getContext() {
        return context;
    }

    public IrContext translate2LlvmIr(ASTNode node) {
        Module module = new IrBuilder().buildModule(context);
        node.accept(new GlobalVarVisitor(module));
        node.accept(new FuncVisitor(module));
        return context;
    }

}

