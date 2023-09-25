import lexLayer.LexicalParser;
import lexLayer.dataStruct.Token;
import lexLayer.impl.LexicalParserImpl;
import utils.LoggerUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

public class Compiler {
    private static final Logger LOGGER = LoggerUtil.getLogger();

    public static void main(String[] args) {
        final LexicalParser lexer = LexicalParserImpl.getInstance();
        ArrayList<Token> allTokens = lexer.getAllTokens();
        File f = new File("output.txt");
        if (f.exists()) f.delete();
        allTokens.forEach(token -> {
            try (FileWriter fw = new FileWriter("output.txt", true)) {
                fw.append(String.valueOf(token.getLexType())).append(" ").append(token.getRawValue()).append("\n").flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        });
    }

}