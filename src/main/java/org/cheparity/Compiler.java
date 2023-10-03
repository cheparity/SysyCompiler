import grammarLayer.dataStruct.ASTLeaf;
import grammarLayer.dataStruct.ASTNode;
import grammarLayer.dataStruct.GrammarType;
import grammarLayer.impl.RecursiveDescentParser;
import utils.LoggerUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
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
        RecursiveDescentParser grammarParser = new RecursiveDescentParser();
        grammarParser.parse();
        printAnswer(grammarParser.getAST());
    }

    private static void printAnswer(ASTNode tree) {
        PrintStream ps = new PrintStream(fos);
//        if (tree.getChildren().isEmpty()) return;

        for (var child : tree.getChildren()) {
            printAnswer(child);
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