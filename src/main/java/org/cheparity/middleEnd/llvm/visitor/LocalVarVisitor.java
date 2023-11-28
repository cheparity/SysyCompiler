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

    public LocalVarVisitor(BlockVisitor callee) {
        this.basicBlock = callee.getBasicBlock();
        this.builder = callee.getBuilder();
        this.symbolTable = basicBlock.getSymbolTable();
    }

    /**
     * 为了处理匿名块的情况，需要把<font color='red'>匿名块里的符号表</font>单独传递过来。
     *
     * @param basicBlock  基本块
     * @param builder     IrBuilder
     * @param symbolTable 匿名块里的符号表
     */
    public LocalVarVisitor(BasicBlock basicBlock, IrBuilder builder, SymbolTable symbolTable) {
        this.basicBlock = basicBlock;
        this.builder = builder;
        this.symbolTable = symbolTable;
    }

    @Override
    public void visit(ASTNode node) {
        assert node.getGrammarType() == GrammarType.VAR_DECL || node.getGrammarType() == GrammarType.CONST_DECL;
        //ConstDecl -> 'const' BType ConstDef { ',' ConstDef } ';'
        switch (node.getGrammarType()) {
            case CONST_DECL -> visitConstDecl(node);
            case VAR_DECL -> visitVarDecl(node);
        }
    }

    private void visitConstDecl(ASTNode constDecl) {
        //ConstDecl -> 'const' BType ConstDef { ',' ConstDef } ';'
        //ConstDef -> Ident '=' ConstExp
        for (var constDef : constDecl.getChildren()) {
            if (constDef.getGrammarType() != GrammarType.CONST_DEF) continue;
            //查表获得符号 -> 添加到符号表
            var constRawName = constDef.getChild(0).getRawValue();
            assert symbolTable.getSymbol(constRawName).isPresent();
            Symbol symbol = symbolTable.getSymbol(constRawName).get();
            var nodeUnion = new IrUtil(builder, basicBlock).calcAloExp(constDef.getChild(2));
            if (nodeUnion.isNum) {
                PointerValue pointer = builder.buildLocalVariable(basicBlock, IrType.IrTypeID.Int32TyID, nodeUnion.getNumber());
                symbol.setPointer(pointer);
            } else {
                //是一个 variable
                Variable variable = nodeUnion.getVariable();
                PointerValue pointerValue = builder.buildLocalVariable(basicBlock, IrType.IrTypeID.Int32TyID);
                builder.buildStoreInst(basicBlock, variable, pointerValue);
                symbol.setPointer(pointerValue);
            }
        }
    }

    private void visitVarDecl(ASTNode varDecl) {
        //VarDecl -> BType VarDef { ',' VarDef } ';'
        //VarDef -> Ident { '[' ConstExp ']' } ['=' InitVal]
        for (var varDef : varDecl.getChildren()) {
            if (varDef.getGrammarType() != GrammarType.VAR_DEF) continue;
            //查表获得符号 -> 添加到符号表
            var varRawName = varDef.getChild(0).getRawValue();
            assert symbolTable.getSymbol(varRawName).isPresent();
            Symbol symbol = symbolTable.getSymbol(varRawName).get();
            if (varDef.deepDownFind(GrammarType.INIT_VAL, 1).isPresent()) {
                //如果有初值，建立的也是指针
                var nodeUnion = new IrUtil(builder, basicBlock).calcAloExp(varDef.getChild(2).getChild(0));
                if (nodeUnion.isNum) {
                    PointerValue pointer = builder.buildLocalVariable(basicBlock, IrType.IrTypeID.Int32TyID,
                            nodeUnion.getNumber());
                    symbol.setPointer(pointer);
                } else {
                    Variable variable = nodeUnion.getVariable();
                    PointerValue pointerValue = builder.buildLocalVariable(basicBlock, IrType.IrTypeID.Int32TyID);
                    builder.buildStoreInst(basicBlock, variable, pointerValue);
                    symbol.setPointer(pointerValue);
                }
            } else {
                //没有初值，只alloca不store
                PointerValue pointerValue = builder.buildLocalVariable(basicBlock, IrType.IrTypeID.Int32TyID);
                symbol.setPointer(pointerValue);
            }
        }
    }
}
