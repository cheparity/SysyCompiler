package lexLayer;

import lexLayer.dataStruct.Token;

import java.util.ArrayList;
import java.util.Optional;

public interface LexicalParser {

    Optional<Token> next();

    ArrayList<Token> getAllTokens();
}
