package grammarLayer.dataStruct;

import lexLayer.dataStruct.Token;

import java.util.NoSuchElementException;

public final class ASTLeaf extends ASTNode {
    private final Token token;

    public ASTLeaf(ASTNode father, Token token) throws NoSuchElementException {
        super(father, GrammarType.ofTerminal(token.getLexType()).orElseThrow());
        this.token = token;
    }

    public Token getToken() {
        return this.token;
    }
}
