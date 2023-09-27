package grammarLayer.dataStruct;

import exception.NotATerminalException;
import lexLayer.dataStruct.Token;

public final class ASTLeaf extends ASTNode {
    private final Token token;

    public ASTLeaf(Token token) throws NotATerminalException {
        super(GrammarType.ofTerminal(token.getLexType()).orElseThrow());
        this.token = token;
    }

    public Token getToken() {
        return this.token;
    }
}
