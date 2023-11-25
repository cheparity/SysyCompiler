package middleEnd.llvm;

import frontEnd.lexer.impl.LexerImpl;
import frontEnd.parser.SysYParser;
import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.parser.impl.RecursiveDescentParser;
import junit.framework.TestCase;
import middleEnd.llvm.ir.IrContext;

public class IrTranslatorTest extends TestCase {

    SysYParser sysYParser = RecursiveDescentParser.getInstance();
    IrTranslator irTranslator = IrTranslator.getInstance();


    public void testLlvmIr() {
        sysYParser.setTokens(LexerImpl.getInstance().getAllTokens());
        ASTNode ast = sysYParser.getAST();
        IrContext irContext = irTranslator.translate2LlvmIr(ast);
        System.out.println(irContext.toIrCode());
    }
}