package visitor;

import exception.GrammarErrorException;
import parser.dataStruct.ASTNode;

import java.util.ArrayList;

public class ErrorHandler {
    private static final ErrorHandler INSTANCE = new ErrorHandler();
    private static final ArrayList<GrammarErrorException> errorPool = new ArrayList<>();
    private ASTNode AST_ROOT;

    private ErrorHandler() {
    }

    public static ErrorHandler getInstance() {
        return INSTANCE;
    }

    public void setASTRoot(ASTNode astRoot) {
        errorPool.clear();
        this.AST_ROOT = astRoot;
    }

    public void addError(GrammarErrorException e) {
        errorPool.add(e);
    }

    public ArrayList<GrammarErrorException> getErrors() {
        return errorPool;
    }

}
