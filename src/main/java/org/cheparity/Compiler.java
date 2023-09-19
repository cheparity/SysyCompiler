import lexLayer.Lexer;
import lexLayer.impl.LexerImpl;
import lexLayer.tokenData.Token;
import utils.LoggerUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

public class Compiler {
    private static Logger LOGGER = LoggerUtil.getLogger();

    public static void main(String[] args) {
        final Lexer lexer = LexerImpl.getInstance();
        ArrayList<Token> allTokens = lexer.getAllTokens();
        File f = new File("output.txt");
        if (f.exists()) f.delete();
        allTokens.forEach(token -> {
            LOGGER.info(String.valueOf(token));
            try (FileWriter fw = new FileWriter("output.txt", true)) {
                fw.append(String.valueOf(token.getLexType())).append(" ").append(token.getRawValue()).append("\n").flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        });
    }

}