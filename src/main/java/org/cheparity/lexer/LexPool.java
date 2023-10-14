package lexer;

import lexer.dataStruct.Token;

import java.util.ArrayList;

public class LexPool {
    private final ArrayList<Token> tokens = new ArrayList<>();

    public void addToken(Token token) {
        tokens.add(token);
    }

    public ArrayList<Token> getTokens() {
        return this.tokens;
    }

    public void clean() {
        this.tokens.clear();
    }
}
