package visitor;

import exception.GrammarError;

import java.util.ArrayList;

public class ErrorHandler {
    private final ArrayList<GrammarError> errorPool = new ArrayList<>();

    public void addError(GrammarError e) {
        errorPool.add(e);
    }

    public ArrayList<GrammarError> getErrors() {
        return errorPool;
    }

}
