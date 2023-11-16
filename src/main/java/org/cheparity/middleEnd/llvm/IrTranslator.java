package middleEnd.llvm;

import frontEnd.parser.dataStruct.ASTNode;
import middleEnd.llvm.ir.IrBuilder;
import middleEnd.llvm.ir.Module;
import middleEnd.llvm.visitor.FuncDefVisitor;
import middleEnd.llvm.visitor.GlobalVarVisitor;

public class IrTranslator {
    private static IrTranslator instance;
    private final IrContext context = new IrContext();

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
        node.accept(new FuncDefVisitor(module));
        return context;
    }

}

