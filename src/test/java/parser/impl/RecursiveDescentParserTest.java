package parser.impl;

import exception.GrammarError;
import junit.framework.TestCase;
import parser.dataStruct.ASTLeaf;
import parser.dataStruct.ASTNode;

import java.util.TreeSet;

public class RecursiveDescentParserTest extends TestCase {

    private final RecursiveDescentParser parser = RecursiveDescentParser.getInstance();

    public void testAST() {
        System.out.println("=========parse result:=========");
        parser.parse();
        peekAST(parser.getAST(), 0);
        System.out.println("=========parse end=========");
    }


    private void peekAST(ASTNode tree, int deep) {
        for (int i = 0; i < deep; i++) {
            System.out.print("\t");
        }
        System.out.print("|__");
        if (tree instanceof ASTLeaf) {
            System.out.println("\033[36;4m" + tree.getGrammarType() + "\033[0m" + ": " + "\033[31;4m" + ((ASTLeaf) tree).getToken().getRawValue() + "\033[0m");
        } else {
            System.out.println("\033[36;4m" + tree.getGrammarType() + "\033[0m");
        }

        if (tree.getChildren().isEmpty()) return;

        for (var child : tree.getChildren()) {
            peekAST(child, deep + 1);
        }

    }

    public void testErrorHandler() {
        parser.parse();
        var ast = parser.getAST();
        System.out.println("=========error handler result:=========");
        TreeSet<GrammarError> errors = ast.getErrors();
        for (var error : errors) {
            System.out.println(error);
        }
        System.out.println("=========error handler end=========");
    }
}