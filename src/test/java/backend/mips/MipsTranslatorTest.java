package backend.mips;

import frontEnd.lexer.impl.LexerImpl;
import frontEnd.parser.SysYParser;
import frontEnd.parser.impl.RecursiveDescentParser;
import junit.framework.TestCase;
import middleEnd.llvm.IrTranslator;
import middleEnd.llvm.ir.IrContext;

public class MipsTranslatorTest extends TestCase {
    SysYParser sysYParser = RecursiveDescentParser.getInstance().setTokens(LexerImpl.getInstance().getAllTokens());
    IrTranslator irTranslator = IrTranslator.getInstance();
    IrContext irContext = irTranslator.translate2LlvmIr(sysYParser.getAST());
    MipsTranslator mipsTranslator = MipsTranslator.getInstance(irContext);

    public void testMips() {
        System.out.println(mipsTranslator.translate2Mips());
    }
}