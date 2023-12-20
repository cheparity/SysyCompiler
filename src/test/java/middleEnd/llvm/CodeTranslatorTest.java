package middleEnd.llvm;

import frontEnd.lexer.impl.LexerImpl;
import frontEnd.parser.SysYParser;
import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.parser.impl.RecursiveDescentParser;
import junit.framework.TestCase;
import middleEnd.CodeTranslator;
import middleEnd.llvm.ir.IrContext;
import utils.LoggerUtil;

import java.util.logging.Level;

public class CodeTranslatorTest extends TestCase {

    SysYParser sysYParser = RecursiveDescentParser.getInstance();
    CodeTranslator codeTranslator = CodeTranslator.getInstance();


    public void testLlvmIr() {
        LoggerUtil.setLoggerLevel(Level.FINE);
        sysYParser.setTokens(LexerImpl.getInstance().getAllTokens());
        ASTNode ast = sysYParser.getAST();
        IrContext irContext = codeTranslator.translate2LlvmIr(ast);
        System.out.println(irContext.toIrCode());
    }

    public void testMips() {
//        LoggerUtil.setLoggerLevel(Level.FINE);
        sysYParser.setTokens(LexerImpl.getInstance().getAllTokens());
        ASTNode ast = sysYParser.getAST();
        IrContext irContext = codeTranslator.translate2LlvmIr(ast);
        System.out.println(irContext.toMipsCode());
    }

    public void testUnwrap() {
        sysYParser.setTokens(LexerImpl.getInstance().getAllTokens());
        ASTNode ast = sysYParser.getAST();
        IrContext irContext = codeTranslator.translate2LlvmIr(ast);
//        IrUtil.unwrapAllLogicNodes(ast);
    }
}