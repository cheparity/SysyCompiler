import exception.GrammarError;
import frontEnd.lexer.SysYLexer;
import frontEnd.lexer.impl.LexerImpl;
import frontEnd.parser.SysYParser;
import frontEnd.parser.dataStruct.ASTLeaf;
import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.parser.dataStruct.GrammarType;
import frontEnd.parser.impl.RecursiveDescentParser;
import middleEnd.CodeTranslator;
import middleEnd.llvm.ir.IrContext;
import utils.LoggerUtil;

import java.io.*;
import java.util.TreeSet;
import java.util.logging.Logger;

public class Compiler {
    private static final Logger LOGGER = LoggerUtil.getLogger();
    private static final SysYLexer lexer = LexerImpl.getInstance();
    private static final SysYParser parser = RecursiveDescentParser.getInstance();
    private static final CodeTranslator CODE_TRANSLATOR = CodeTranslator.getInstance();

    public static void main(String[] args) {
//        printLexAnswer();
//        printGrammarAnswer();
//        printErrorAnswer();
        printLlvmIrAnswer();
        printMipsAnswer();
    }

    private static void printLexAnswer() {
        var fos = getFos("lex.txt");
        lexer.getAllTokens().forEach(token -> {
            try {
                fos.write((token.getLexType() + " " + token.getRawValue() + "\n").getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void printGrammarAnswer() {
        parser.setTokens(lexer.getAllTokens());
        parser.parse();
        ast2String(parser.getAST());
    }

    private static void ast2String(ASTNode tree) {
        var fos = getFos("ast.txt");
        PrintStream ps = new PrintStream(fos);

        for (var child : tree.getChildren()) {
            ast2String(child);
        }
        GrammarType g = tree.getGrammarType();
        if (g == GrammarType.BLOCK_ITEM || g == GrammarType.DECL || g == GrammarType.B_TYPE) {
            return;
        }
        if (tree instanceof ASTLeaf) {
            ps.println(((ASTLeaf) tree).getToken().getLexType() + " " + ((ASTLeaf) tree).getToken().getRawValue());
        } else {
            ps.println("<" + tree.getGrammarType().getValue() + ">");
        }
    }

    private static void printErrorAnswer() {
        var fos = getFos("error.txt");
        RecursiveDescentParser parser = RecursiveDescentParser.getInstance();
        TreeSet<GrammarError> errors = parser.getAST().getErrors();
        PrintStream ps = new PrintStream(fos);
        errors.forEach(e -> ps.println(e.getToken().getLineNum() + " " + e.getCode().getValue()));
    }

    private static void printLlvmIrAnswer() {
        parser.setTokens(LexerImpl.getInstance().getAllTokens());
        ASTNode ast = parser.getAST();
        IrContext irContext = CODE_TRANSLATOR.translate2LlvmIr(ast);
        var fos = getFos("llvm_ir.txt");
//        System.out.println(irContext.toIrCode());
        PrintStream ps = new PrintStream(fos);
        ps.println(irContext.toIrCode());
    }

    private static void printMipsAnswer() {
        parser.setTokens(LexerImpl.getInstance().getAllTokens());
        ASTNode ast = parser.getAST();
        IrContext irContext = CODE_TRANSLATOR.translate2LlvmIr(ast);
        irContext.toIrCode();
        var fos = getFos("mips.txt");
        PrintStream ps = new PrintStream(fos);
        ps.println(irContext.toMipsCode());
    }

    private static OutputStream getFos(String pathname) {
        File f = new File(pathname);
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(pathname, false);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        return fos;
    }

}