package lexLayer.tokenData;

import lexLayer.LexType;
import utils.LoggerUtil;

import java.util.logging.Logger;

public final class Token {
    private final static Logger LOGGER = LoggerUtil.getLogger();
    private int lineNum;
    private int colNum;
    private LexType lexType;
    private String rawValue;


    public Token(int lineNum, int colNum, String rawValue) {
        this.lineNum = lineNum;
        this.colNum = colNum;
        this.lexType = LexType.ofValue(rawValue);
        this.rawValue = rawValue;
    }

    public int getLineNum() {
        return lineNum;
    }

    public LexType getLexType() {
        return this.lexType;
    }

    public int getColNum() {
        return colNum;
    }

    public String getRawValue() {
        return rawValue;
    }

    @Override
    public String toString() {
        return String.format("%-20s\t%-20s\t%-20s", lexType, rawValue, "(" + lineNum + "," + colNum + ")");
    }
}
