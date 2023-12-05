package middleEnd.llvm;

import frontEnd.parser.dataStruct.ASTNode;
import middleEnd.llvm.ir.IrBuilder;
import middleEnd.llvm.ir.IrContext;
import middleEnd.llvm.ir.Module;
import middleEnd.llvm.visitor.FuncVisitor;
import middleEnd.llvm.visitor.GlobalVarVisitor;
import utils.LoggerUtil;

import java.util.logging.Logger;

public class IrTranslator {
    public static final IrContext context = new IrContext();
    private final static Logger LOGGER = LoggerUtil.getLogger();
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

    public IrContext translate2LlvmIr(ASTNode node) {
        var errors = node.getErrors();
        if (!errors.isEmpty()) {
            var sb = new StringBuilder();
            sb.append("There are ").append(errors.size()).append(" error(s) in the source code:");
            for (var error : errors) {
                sb.append("\n").append(error);
            }
            LOGGER.warning(sb.toString());
        }
        Module module = new IrBuilder().buildModule(context);
        node.accept(new GlobalVarVisitor(module));
        node.accept(new FuncVisitor(module));
        return context;
    }

}

