package lexLayer.impl;

import lexLayer.Lexer;
import lexLayer.tokenData.Token;
import utils.LoggerUtil;
import utils.RegUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.logging.Logger;

public class LexerImpl implements Lexer {
    private final static Logger LOGGER = LoggerUtil.getLogger();
    private final static Lexer LEXER_INSTANCE;

    private final static String FILENAME;

    private final static String SOURCE;

    static {
        FILENAME = "./testfile.txt";
        try {
            SOURCE = Files.readString(Paths.get(FILENAME));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        LEXER_INSTANCE = new LexerImpl();
    }

    /**
     * The reserved words' dictionary.
     */
    private final HashSet<String> reserveWords = new HashSet<>();

    /**
     * The line number that may be used for error handler.
     */
    private int lineNum = 1;
    private int curPos = 0;

    private LexerImpl() {
        LOGGER.fine(SOURCE);
    }

    public static Lexer getInstance() {
        return LEXER_INSTANCE;
    }

    private void addReserve(String... strings) {
        reserveWords.addAll(Arrays.asList(strings));
    }

    private boolean reserved(String word) {
        return reserveWords.contains(word);
    }

    /**
     * Handle the next word.
     *
     * @return The next word.
     */
    @Override
    public Optional<Token> next() {
//        int begin = curPos;
//        char[] charArray = SOURCE.toCharArray();
//        for (; i < charArray.length; curPos++) {
//
//        }
        String word;
        while ((word = SOURCE.substring(curPos, curPos + 1)).isBlank()) {
            if (curPos >= SOURCE.length()) {
                LOGGER.finest("The last character.");
                return Optional.empty();
            }
            if (word.equals("\n")) {
                lineNum++;
            }
            curPos++;
        }
        curPos++;
        String substr = word.substring(begin, curPos);
        // now curPos is at the first non-blank character, and start is the first blank character
        Optional<String> str = switch (judgeChar(substr)) {
            case DIGIT -> readNumber(substr);
            case ALPHA -> readVarOrKeyword(substr);
            case SPECIAL_CHAR -> readOperator(substr);
            case NONE -> Optional.empty();
        };
        str.ifPresent(s -> LOGGER.info("get str: " + s));
        return Optional.empty();
    }


    private Optional<String> readNumber(String word) {
        String substr;
        int begin = curPos;
        while (!(substr = word.substring(curPos, curPos + 1)).isBlank()) {
            if (curPos >= SOURCE.length()) {
                LOGGER.finest("The last character.");
                return Optional.empty();
            }
            if (judgeChar(substr) != CharaType.DIGIT) {
                curPos++;
                return Optional.of(word.substring(begin, curPos));
            }
            curPos++;
        }
        return Optional.empty();
    }

    private Optional<String> readVarOrKeyword(String word) {
        String substr;
        int begin = curPos;
        while (!(substr = word.substring(curPos, curPos + 1)).isBlank()) {
            if (curPos >= SOURCE.length()) {
                LOGGER.finest("The last character.");
                return Optional.empty();
            }
            if (judgeChar(substr) != CharaType.ALPHA) {
                curPos++;
                return Optional.of(word.substring(begin, curPos));
            }
            curPos++;
        }
        return Optional.empty();
    }

    private Optional<String> readOperator(String word) {
        String substr;
        int begin = curPos;
        while (!(substr = word.substring(curPos, curPos + 1)).isBlank()) {
            if (curPos >= SOURCE.length()) {
                LOGGER.finest("The last character.");
                return Optional.empty();
            }
            if (judgeChar(substr) != CharaType.ALPHA) {
                curPos++;
                return Optional.of(word.substring(begin, curPos));
            }
            curPos++;
        }
        return Optional.empty();
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
        } else if (word.isBlank()) {
            return CharaType.BLANK;
        }
    }

    private enum CharaType {
        DIGIT,
        SPECIAL_CHAR,
        ALPHA,
        BLANK,
    }

}
