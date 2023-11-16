package middleEnd.llvm.visitor;

import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.parser.dataStruct.GrammarType;
import frontEnd.symbols.Symbol;
import frontEnd.symbols.SymbolTable;
import middleEnd.ASTNodeVisitor;
import middleEnd.llvm.IrUtil;
import middleEnd.llvm.ir.BasicBlock;
import middleEnd.llvm.ir.IrBuilder;
import middleEnd.llvm.ir.IrType;
import middleEnd.llvm.ir.Variable;

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
    public void visit(ASTNode varDef) {
        assert varDef.getGrammarType() == GrammarType.VAR_DEF;
        //VarDef -> Ident { '[' ConstExp ']' } ['=' InitVal]
        //查表获得符号 -> 添加到符号表
        var varRawName = varDef.getChild(0).getRawValue();
        assert symbolTable.getSymbol(varRawName).isPresent();
        Symbol symbol = symbolTable.getSymbol(varRawName).get();
        Variable variable;
        if (varDef.getChildren().size() == 1) {
            //没有初始值
            //VarDef -> Ident
            variable = builder.buildLocalVariable(basicBlock, IrType.Int32TyID);
        } else {
            //有初始值的情况，此时[value]很可能是一个寄存器的地址（CalculateConst无法得出具体的数字）。这个怎么处理呢？
            //VarDef -> Ident '=' InitVal
            var value = IrUtil.CalculateConst(varDef.getChild(2), symbolTable);
            //注意，在local variables中，即使是const，也是开辟一段空间
            //todo 如果后面有引用const的值，则直接替换为const的值。符号表已经存下来了，所以我们应该不用考虑const？
        }
        symbol.setIrVariable(variable);

    }
}
