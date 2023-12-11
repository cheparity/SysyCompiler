package middleEnd.llvm.visitor;

import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.parser.dataStruct.GrammarType;
import middleEnd.ASTNodeVisitor;
import middleEnd.llvm.ir.*;
import middleEnd.symbols.Symbol;
import middleEnd.symbols.SymbolTable;
import utils.Message;

import java.util.ArrayList;

public final class LocalVarVisitor implements ASTNodeVisitor {
    private final BasicBlock basicBlock;
    private final IrBuilder builder;
    private final SymbolTable symbolTable;

    public LocalVarVisitor(BlockVisitor callee) {
        this.basicBlock = callee.getBasicBlock();
        this.builder = callee.getBuilder();
        this.symbolTable = basicBlock.getSymbolTable();
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

    @Override
    public IrBuilder getBuilder() {
        return this.builder;
    }

    @Override
    public void emit(Message message, ASTNodeVisitor sender) {
        //do nothing
    }

    private void visitConstDecl(ASTNode constDecl) {
        for (var constDef : constDecl.getChildren()) { //ConstDef -> Ident { '[' ConstExp ']' } '=' ConstInitVal
            if (constDef.getGrammarType() != GrammarType.CONST_DEF) continue;
            //查表获得符号 -> 添加到符号表
            var constRawName = constDef.getChild(0).getRawValue();
            assert symbolTable.getSymbol(constRawName).isPresent();
            Symbol symbol = symbolTable.getSymbol(constRawName).get();

            boolean isArr = constDef.deepDownFind(GrammarType.LEFT_BRACKET, 1).isPresent();
            if (!isArr) {
                var nodeUnion = new IrUtil(builder, basicBlock).calcAloExp(constDef.getChild(-1));
                if (nodeUnion.isNum) {
                    PointerValue pointer = builder.buildLocalVariable(basicBlock, IrType.IrTypeID.Int32TyID, nodeUnion.getNumber());
                    symbol.setPointer(pointer);
                    continue;
                }
                //是一个 variable
                Variable variable = nodeUnion.getVariable();
                PointerValue pointerValue = builder.buildLocalVariable(basicBlock, IrType.IrTypeID.Int32TyID);
                builder.buildStoreInst(basicBlock, variable, pointerValue);
                symbol.setPointer(pointerValue);
                continue;
            }

            //是一个数组
            //先build alloca指令分配指针
            int dim = symbol.getDim();
            int arrSize = 0;
            for (int i = 0; i < dim; i++) {
                //计算数组大小
                //constExp 出现于 2,5,8...，即 2+3i
                var constExp = constDef.getChild(2 + 3 * i);
                var num = IrUtil.calculateConst4Global(constExp);
                symbol.setDimSize(i + 1, num);
                arrSize = (arrSize == 0) ? num : num * arrSize;
            }
            var pointer = builder.buildLocalArray(basicBlock, IrType.IrTypeID.Int32TyID, arrSize);
            symbol.setPointer(pointer);
            //必有initVal ConstInitVal -> ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
            var inits = new ArrayList<Integer>();
            IrUtil.unwrapArrayInitVal4Global(constDef.getChild(-1), inits); //这里好像不能用，因为可能会出现变量值？
            builder.buildArrayStoreInsts(basicBlock, pointer, inits.toArray(new Integer[0]));
            pointer.setNumber(inits.toArray(new Integer[0]));
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

            boolean isArr = varDef.deepDownFind(GrammarType.LEFT_BRACKET, 1).isPresent();
            if (!isArr) {
                if (varDef.deepDownFind(GrammarType.INIT_VAL, 1).isEmpty()) {
                    //没有初值，只alloca不store
                    PointerValue pointerValue = builder.buildLocalVariable(basicBlock, IrType.IrTypeID.Int32TyID);
                    symbol.setPointer(pointerValue);
                    continue;
                }
                //如果有初值，建立的也是指针
                var nodeUnion = new IrUtil(builder, basicBlock).calcAloExp(varDef.getChild(2).getChild(0));
                if (nodeUnion.isNum) {
                    PointerValue pointer = builder.buildLocalVariable(basicBlock, IrType.IrTypeID.Int32TyID, nodeUnion.getNumber());
                    symbol.setPointer(pointer);
                    continue;
                }
                Variable variable = nodeUnion.getVariable();
                PointerValue pointerValue = builder.buildLocalVariable(basicBlock, variable.getType().getBasicType());
                builder.buildStoreInst(basicBlock, variable, pointerValue);
                symbol.setPointer(pointerValue);
                continue;
            }

            //是一个数组 VarDef -> Ident { '[' ConstExp ']' } ['=' InitVal]
            //先build alloca指令分配指针
            int dim = symbol.getDim();
            int arrSize = 0;
            for (int i = 0; i < dim; i++) {
                //计算数组大小
                //constExp 出现于 2,5,8...，即 2+3i
                var constExp = varDef.getChild(2 + 3 * i);
                var num = IrUtil.calculateConst4Global(constExp);
                symbol.setDimSize(i + 1, num);
                arrSize = (arrSize == 0) ? num : num * arrSize;
            }
            var pointer = builder.buildLocalArray(basicBlock, IrType.IrTypeID.Int32TyID, arrSize);
            symbol.setPointer(pointer);

            //不一定有initVal
            boolean hasInitVal = varDef.getChild(-1).getGrammarType() == GrammarType.INIT_VAL;
            if (!hasInitVal) continue;

            //如果还有initVal
            var inits = new ArrayList<Integer>();
            IrUtil.unwrapArrayInitVal4Global(varDef.getChild(-1), inits); //这里好像不能用，因为可能会出现变量值？但是如果都是数字就没关系
            builder.buildArrayStoreInsts(basicBlock, pointer, inits.toArray(new Integer[0]));
            pointer.setNumber(inits.toArray(new Integer[0]));
        }
    }
}
