import grammarLayer.dataStruct.ASTLeaf;
import grammarLayer.dataStruct.ASTNode;
import grammarLayer.dataStruct.GrammarType;
import grammarLayer.impl.RecursiveDescentParser;
import lexLayer.LexicalParser;
import lexLayer.dataStruct.Token;
import lexLayer.impl.LexicalParserImpl;
import utils.LoggerUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.logging.Logger;

public class Compiler {
    private static final Logger LOGGER = LoggerUtil.getLogger();
    private static final FileOutputStream fos;

    static {
        File f = new File("output.txt");
        if (f.exists()) f.delete();
        try {
            fos = new FileOutputStream("output.txt");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        printGrammarAnswer();
    }

    private static void printLexAnswer() {
        LexicalParser l = LexicalParserImpl.getInstance();
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
        RecursiveDescentParser grammarParser = new RecursiveDescentParser();
        grammarParser.parse();
        printGrammarAnswer(grammarParser.getAST());
    }

    private static void printGrammarAnswer(ASTNode tree) {
        PrintStream ps = new PrintStream(fos);
//        if (tree.getChildren().isEmpty()) return;

        for (var child : tree.getChildren()) {
            printGrammarAnswer(child);
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
}