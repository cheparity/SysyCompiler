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


    public Token(int lineNum, int colNum, LexType type) {
        this.lineNum = lineNum;
        this.colNum = colNum;
        this.lexType = type;
    }


    public int getLineNum() {
        return lineNum;
    }

    public void setLineNum(int lineNum) {
        this.lineNum = lineNum;
    }

    public int getColNum() {
        return colNum;
    }

    public void setColNum(int colNum) {
        this.colNum = colNum;
    }

    public LexType getLexType() {
        return lexType;
    }

    public void setLexType(LexType lexType) {
        this.lexType = lexType;
    }


    public String getRawValue() {
        return rawValue;
    }

    public void setRawValue(String str) {
        this.rawValue = str;
    }
}
