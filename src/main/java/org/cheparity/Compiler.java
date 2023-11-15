import exception.GrammarError;
import frontEnd.lexer.SysYLexer;
import frontEnd.lexer.impl.LexerImpl;
import frontEnd.parser.SysYParser;
import frontEnd.parser.dataStruct.ASTLeaf;
import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.parser.dataStruct.GrammarType;
import frontEnd.parser.dataStruct.utils.LoggerUtil;
import frontEnd.parser.impl.RecursiveDescentParser;

import java.io.*;
import java.util.TreeSet;
import java.util.logging.Logger;

public class Compiler {
    private static final Logger LOGGER = LoggerUtil.getLogger();
    private static final SysYLexer lexer = LexerImpl.getInstance();
    private static final SysYParser parser = RecursiveDescentParser.getInstance();
    private static final FileOutputStream fos;

    static {
        File f = new File("error.txt");
        if (f.exists()) f.delete();
        try {
            fos = new FileOutputStream("error.txt");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        printLexAnswer();
        printGrammarAnswer();
        printErrorAnswer();
    }

    private static void printLexAnswer() {
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
        PrintStream ps = new PrintStream(fos);
//        if (tree.getChildren().isEmpty()) return;

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
        RecursiveDescentParser parser = RecursiveDescentParser.getInstance();
        TreeSet<GrammarError> errors = parser.getAST().getErrors();
        PrintStream ps = new PrintStream(fos);
        errors.forEach(e -> ps.println(e.getToken().getLineNum() + " " + e.getCode().getValue()));
    }

}