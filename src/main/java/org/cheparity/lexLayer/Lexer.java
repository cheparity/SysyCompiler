package lexLayer;

import lexLayer.tokenData.Token;

import java.util.ArrayList;
import java.util.Optional;

public interface Lexer {

    Optional<Token> next();

    ArrayList<Token> getAllTokens();
}
