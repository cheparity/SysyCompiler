package middleEnd.llvm.visitor;

import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.parser.dataStruct.GrammarType;
import middleEnd.ASTNodeVisitor;
import middleEnd.llvm.ir.*;
import middleEnd.symbols.Symbol;
import middleEnd.symbols.SymbolTable;

public final class LocalVarVisitor implements ASTNodeVisitor {
    private final BasicBlock basicBlock;
    private final IrBuilder builder;
    private final SymbolTable symbolTable;

    public LocalVarVisitor(BasicBlock basicBlock, IrBuilder builder) {
        this.basicBlock = basicBlock;
        this.builder = builder;
        this.symbolTable = basicBlock.getSymbolTable();
    }

    @Override
    public void visit(ASTNode varDecl) {
        assert varDecl.getGrammarType() == GrammarType.VAR_DECL;
        //VarDecl -> BType VarDef { ',' VarDef } ';'
        //VarDef -> Ident { '[' ConstExp ']' } ['=' InitVal]
        for (var varDef : varDecl.getChildren()) {
            if (varDef.getGrammarType() != GrammarType.VAR_DEF) continue;
            //查表获得符号 -> 添加到符号表
            var varRawName = varDef.getChild(0).getRawValue();
            assert symbolTable.getSymbol(varRawName).isPresent();
            Symbol symbol = symbolTable.getSymbol(varRawName).get();
            if (varDef.deepDownFind(GrammarType.INIT_VAL, 1).isPresent()) {
                //如果有初值
                var nodeUnion = new IrUtil(builder, basicBlock).calc(varDef.getChild(2).getChild(0));
                if (nodeUnion.isNum) {
                    Variable variable = builder.buildLocalVariable(basicBlock, IrType.IrTypeID.Int32TyID, nodeUnion.getNumber());
                    symbol.setIrVariable(variable);
                } else {
                    //是一个 variable（未知量）
                    PointerValue pointerValue = builder.buildLocalVariable(basicBlock, IrType.IrTypeID.Int32TyID);
                    throw new RuntimeException("Not implement!"); //todo
                }
            } else {
                //没有初值
                PointerValue pointerValue = builder.buildLocalVariable(basicBlock, IrType.IrTypeID.Int32TyID);
                symbol.setPointer(pointerValue);
            }
        }
    }
}
