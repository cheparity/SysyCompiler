package lexLayer.dataStruct;

public final class Token {
    private final int lineNum;
    private final int colNum;
    private final LexType lexType;
    private final String rawValue;


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
