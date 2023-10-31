import exception.GrammarError;
import lexer.SysYLexer;
import lexer.dataStruct.Token;
import lexer.impl.LexerImpl;
import parser.dataStruct.ASTLeaf;
import parser.dataStruct.ASTNode;
import parser.dataStruct.GrammarType;
import parser.impl.RecursiveDescentParser;
import utils.LoggerUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.logging.Logger;

public class Compiler {
    private static final Logger LOGGER = LoggerUtil.getLogger();
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
        SysYLexer l = LexerImpl.getInstance();
        ArrayList<Token> allTokens = l.getAllTokens();
        allTokens.forEach(token -> {
            try {
                fos.write((token.getLexType() + " " + token.getRawValue() + "\n").getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void printGrammarAnswer() {
        RecursiveDescentParser grammarParser = RecursiveDescentParser.getInstance();
        grammarParser.parse();
        ast2String(grammarParser.getAST());
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