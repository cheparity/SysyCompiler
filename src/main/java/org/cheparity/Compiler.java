import grammarLayer.GrammarParser;
import grammarLayer.impl.RecursiveDescentParser;
import utils.LoggerUtil;

import java.io.File;
import java.util.logging.Logger;

public class Compiler {
    private static final Logger LOGGER = LoggerUtil.getLogger();

    public static void main(String[] args) {
        GrammarParser grammarParser = new RecursiveDescentParser();
        File f = new File("output.txt");
        if (f.exists()) f.delete();

//        allTokens.forEach(token -> {
//            try (FileWriter fw = new FileWriter("output.txt", true)) {
//                fw.append(String.valueOf(token.getLexType())).append(" ").append(token.getRawValue()).append("\n").flush();
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//
//        });
    }

}