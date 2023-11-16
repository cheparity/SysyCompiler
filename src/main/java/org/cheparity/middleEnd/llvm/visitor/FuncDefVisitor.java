package middleEnd.llvm.visitor;

import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.parser.dataStruct.GrammarType;
import middleEnd.ASTNodeVisitor;
import middleEnd.llvm.SSARegisterAllocator;
import middleEnd.llvm.ir.Function;
import middleEnd.llvm.ir.IrBuilder;
import middleEnd.llvm.ir.IrType;
import middleEnd.llvm.ir.Module;

public final class FuncDefVisitor implements ASTNodeVisitor {
    private final Module module;
    private final IrBuilder builder = new IrBuilder(new SSARegisterAllocator());

    public FuncDefVisitor(Module module) {
        this.module = module;
    }

    @Override
    public void visit(ASTNode func) {
        //所有的node都是FuncDef. FuncDef -> FuncType Ident '(' [FuncFParams] ')' Block
        assert func.getGrammarType() == GrammarType.FUNC_DEF
                || func.getGrammarType() == GrammarType.MAIN_FUNC_DEF;
        IrType funcType = func.getChild(0).getGrammarType().equals(GrammarType.VOID) ? IrType.VoidTyID :
                IrType.Int32TyID;
        String funcName = func.getChild(1).getRawValue();
        Function function = builder.buildFunction(funcType, funcName, module);
        for (var child : func.getChildren()) {
            if (child.getGrammarType() == GrammarType.BLOCK) {
                child.accept(new BlockVisitor(function, builder));
            } else if (child.getGrammarType() == GrammarType.FUNC_RPARAMS) {
                //todo 解析参数
            }
        }

    }
}
