package middleEnd.llvm.visitor;

import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.parser.dataStruct.GrammarType;
import middleEnd.ASTNodeVisitor;
import middleEnd.llvm.ir.*;
import middleEnd.llvm.utils.NodeUnion;
import middleEnd.symbols.Symbol;
import middleEnd.symbols.SymbolTable;
import utils.LoggerUtil;
import utils.Message;

import java.util.ArrayList;
import java.util.logging.Logger;

public final class LocalVarVisitor implements ASTNodeVisitor {
    private final static Logger LOGGER = LoggerUtil.getLogger();
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
        LOGGER.info("visit const decl: " + constDecl.getRawValue());
        for (var constDef : constDecl.getChildren()) { //ConstDef -> Ident { '[' ConstExp ']' } '=' ConstInitVal
            if (constDef.getGrammarType() != GrammarType.CONST_DEF) continue;
            //查表获得符号 -> 添加到符号表
            var constRawName = constDef.getChild(0).getRawValue();
            Symbol symbol = symbolTable.getSymbolSafely(constRawName, constDecl);

            boolean isArr = constDef.deepDownFind(GrammarType.LEFT_BRACKET, 1).isPresent();
            if (!isArr) {
                var nodeUnion = new IrUtil(builder, basicBlock).calcAloExp(constDef.getChild(-1));
                if (nodeUnion.isNum) {
                    PointerValue pointer = builder
                            .buildLocalVariable(basicBlock, IrType.IrTypeID.Int32TyID, nodeUnion.getNumber())
                            .setToken(constRawName);
                    symbol.setPointer(pointer);
                    continue;
                }
                //是一个 variable
                Variable variable = nodeUnion.getVariable();
                PointerValue pointerValue = builder
                        .buildLocalVariable(basicBlock, IrType.IrTypeID.Int32TyID)
                        .setToken(constRawName);
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
            var pointer = builder.buildLocalArray(basicBlock, IrType.IrTypeID.Int32TyID, arrSize).setToken(constRawName);
            symbol.setPointer(pointer);
            //必有initVal ConstInitVal -> ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
            var inits = new ArrayList<NodeUnion>();
            new IrUtil(builder, basicBlock).unwrapArrayInitVal(constDef.getChild(-1), inits); //这里好像不能用，因为可能会出现变量值？
            builder.buildArrayStoreInsts(basicBlock, pointer, inits.toArray(new NodeUnion[0]));
        }
    }

    private void visitVarDecl(ASTNode varDecl) {
        LOGGER.info("visit var decl: " + varDecl.getRawValue());
        //VarDecl -> BType VarDef { ',' VarDef } ';'
        //VarDef -> Ident { '[' ConstExp ']' } ['=' InitVal]
        for (var varDef : varDecl.getChildren()) {
            if (varDef.getGrammarType() != GrammarType.VAR_DEF) continue;
            //查表获得符号 -> 添加到符号表
            var varRawName = varDef.getChild(0).getRawValue();
            Symbol symbol = symbolTable.getSymbolSafely(varRawName, varDecl);

            boolean isArr = varDef.deepDownFind(GrammarType.LEFT_BRACKET, 1).isPresent();
            if (!isArr) {
                if (varDef.deepDownFind(GrammarType.INIT_VAL, 1).isEmpty()) {
                    //没有初值，只alloca不store
                    PointerValue pointerValue = builder
                            .buildLocalVariable(basicBlock, IrType.IrTypeID.Int32TyID)
                            .setToken(varRawName);
                    symbol.setPointer(pointerValue);
                    continue;
                }
                //如果有初值，建立的也是指针
                var nodeUnion = new IrUtil(builder, basicBlock).calcAloExp(varDef.getChild(2).getChild(0));
                if (nodeUnion.isNum) {
                    PointerValue pointer = builder
                            .buildLocalVariable(basicBlock, IrType.IrTypeID.Int32TyID, nodeUnion.getNumber())
                            .setToken(varRawName);
                    symbol.setPointer(pointer);
                    continue;
                }
                Variable variable = nodeUnion.getVariable();
                PointerValue pointerValue = builder
                        .buildLocalVariable(basicBlock, variable.getType().getBasicType())
                        .setToken(varRawName);
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
            var pointer = builder
                    .buildLocalArray(basicBlock, IrType.IrTypeID.Int32TyID, arrSize)
                    .setToken(varRawName);
            symbol.setPointer(pointer);

            //不一定有initVal
            boolean hasInitVal = varDef.getChild(-1).getGrammarType() == GrammarType.INIT_VAL;
            if (!hasInitVal) continue;

            //如果还有initVal，那就是有初值的数组
            var inits = new ArrayList<NodeUnion>();
            new IrUtil(builder, basicBlock).unwrapArrayInitVal(varDef.getChild(-1), inits); //不能用4Global！
            builder.buildArrayStoreInsts(basicBlock, pointer, inits.toArray(new NodeUnion[0])); //todo
        }
    }
}
