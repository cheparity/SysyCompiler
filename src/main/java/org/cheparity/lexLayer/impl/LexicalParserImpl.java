package lexLayer.impl;

import lexLayer.LexPool;
import lexLayer.LexicalParser;
import lexLayer.dataStruct.Token;
import utils.LoggerUtil;
import utils.RegUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Optional;
import java.util.logging.Logger;

public class LexicalParserImpl implements LexicalParser {
    private final static Logger LOGGER = LoggerUtil.getLogger();
    private final static LexicalParser LEXER_INSTANCE = new LexicalParserImpl();
    private final static String FILENAME = "./testfile.txt";
    private final static String SOURCE_RAW;
    private final static String SOURCE_UNCOMMENT;
    private final static int SOURCE_LEN;

    static {
        try {
            SOURCE_RAW = Files.readString(Paths.get(FILENAME));
            SOURCE_UNCOMMENT = removeComment();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        SOURCE_LEN = SOURCE_UNCOMMENT.length();
    }

    private final LexPool lexPool = new LexPool();
    /**
     * The line number that may be used for error handler.
     */
    private int lineNum = 1;
    private int curPos = 0;

    private LexicalParserImpl() {
//        LOGGER.config(SOURCE_UNCOMMENT);
    }

    public static LexicalParser getInstance() {
        return LEXER_INSTANCE;
    }

    private static String removeComment() {
        /*todo not consider line number*/
        return LexicalParserImpl.SOURCE_RAW
                .replaceAll(RegUtil.REGION_COMMENT_REG, "")
                .replaceAll(RegUtil.SINGLE_LINE_COMMENT_REG, "");
    }

    /**
     * Handle the next word.
     *
     * @return The next word.
     */
    @Override
    public Optional<Token> next() {
        String rawSym = null;
        for (; curPos < SOURCE_LEN; curPos++) {
            char c = SOURCE_UNCOMMENT.charAt(curPos);
            CharaType charaType = judgeChar(c);
            if (charaType == CharaType.BLANK) {
            } else if (charaType == CharaType.ALPHA) {
                rawSym = readAlpha();
                break;
            } else if (charaType == CharaType.DIGIT) {
                rawSym = readNumber();
                break;
            } else if (charaType == CharaType.SPECIAL_CHAR) {
                rawSym = readSpecial();
                break;
            }
        }
        if (rawSym != null) {
            var token = new Token(lineNum, getCol(rawSym), rawSym);
            lexPool.addToken(token);
            return Optional.of(token);
        }
        return Optional.empty();
    }

    @Override
    public ArrayList<Token> getAllTokens() {
        this.curPos = 0;
        this.lineNum = 1;
        this.lexPool.clean();
        while (this.next().isPresent()) ;
        return lexPool.getTokens();
    }

    // look back for the first '\n'
    private int getCol(String rawStr) {
        int i = (curPos >= SOURCE_UNCOMMENT.length()) ? curPos - 1 : curPos;
        for (; i > 0 && SOURCE_UNCOMMENT.charAt(i) != '\n'; i--) ;
        return curPos - i + 1 - rawStr.length();
    }

    private String readNumber() {
        int start = curPos;
        for (curPos++; curPos < SOURCE_LEN; curPos++) {
            char c = SOURCE_UNCOMMENT.charAt(curPos);
            if (judgeChar(c) != CharaType.DIGIT) {
                break;
            }
        }
        return SOURCE_UNCOMMENT.substring(start, curPos);
    }

    private String readAlpha() {
        int start = curPos;
        for (curPos++; curPos < SOURCE_LEN; curPos++) {
            char c = SOURCE_UNCOMMENT.charAt(curPos);
            if (judgeChar(c) != CharaType.ALPHA && judgeChar(c) != CharaType.DIGIT) {
                break;
            }
        }
        return SOURCE_UNCOMMENT.substring(start, curPos);
    }

    private String readSpecial() {
        int start = curPos;
        curPos++;

        //handle "
        if (SOURCE_UNCOMMENT.charAt(start) == '"') {
            //todo add support for \
            for (; curPos < SOURCE_LEN; curPos++) {
                char c = SOURCE_UNCOMMENT.charAt(curPos);
                if (c == '"') {
                    curPos++;
                    return SOURCE_UNCOMMENT.substring(start, curPos);
                }
            }
        }
        //to see whether c1 can LIGATURE or not
        if (curPos + 1 < SOURCE_LEN) {
            var substr = SOURCE_UNCOMMENT.substring(start, curPos + 1);
            if (judgeChar(substr) == CharaType.LIGATURE) {
                curPos++;
            }
        }
        return SOURCE_UNCOMMENT.substring(start, curPos);
    }

    private CharaType judgeChar(char c) {
        String c2str = String.valueOf(c);
        return judgeChar(c2str);
    }

    private CharaType judgeChar(String word) {
        if (RegUtil.digit(word)) {
            return CharaType.DIGIT;
        } else if (RegUtil.alphabet(word)) {
            return CharaType.ALPHA;
        } else if (RegUtil.specialChar(word)) {
            return CharaType.SPECIAL_CHAR;
        } else if (RegUtil.linkableString(word)) {
            return CharaType.LIGATURE;
        } else if (word.isBlank()) {
            if (word.equals("\n")) {
                lineNum++;
            }
        }
        return CharaType.BLANK;
    }

    private enum CharaType {
        DIGIT,
        SPECIAL_CHAR,
        ALPHA,
        BLANK,
        LIGATURE,
    }

}
