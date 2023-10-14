package parser.dataStruct;

import lexer.dataStruct.Token;

import java.util.NoSuchElementException;

public final class ASTLeaf extends ASTNode {
    private final Token token;

    public ASTLeaf(Token token) throws NoSuchElementException {
        super(GrammarType.ofTerminal(token.getLexType()).orElseThrow());
        this.token = token;
    }

    public Token getToken() {
        return this.token;
    }
}
