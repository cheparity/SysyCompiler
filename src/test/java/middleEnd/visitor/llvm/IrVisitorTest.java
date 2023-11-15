package middleEnd.visitor.llvm;

import frontEnd.lexer.impl.LexerImpl;
import frontEnd.parser.SysYParser;
import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.parser.impl.RecursiveDescentParser;
import junit.framework.TestCase;

public class IrVisitorTest extends TestCase {

    SysYParser sysYParser = RecursiveDescentParser.getInstance();
    IrVisitor irVisitor = IrVisitor.getInstance();


    public void testVisit() {
        sysYParser.setTokens(LexerImpl.getInstance().getAllTokens());
        ASTNode ast = sysYParser.getAST();
        irVisitor.visit(ast);
        IrContext irContext = irVisitor.getContext();
        System.out.println(irContext.toIrCode());
    }
}