package utils;

public class RegUtil {
    public static final String NUM_REG = "^-?[1-9]\\d*|0$";
    public static final String STR_REG = "^\\\".*\\\"$";
    public static final String IDENT_REG = "^[a-zA-Z_][a-zA-Z_0-9]*$";
    public static final String ALPHA_REG = "^[_a-zA-Z]$";
    public static final String DIGIT_REG = "^\\d$";
    public static final String SPECIAL_CHAR_REG = "^[\"'!@#$%^&(){}\\[\\]|:;<>?,./\\-+=]$";
    public static final String LINKABLE_OPERATOR_REG = "^[>=]|[<=]|[=]{2}|[!=]$";
    public static final String SINGLE_LINE_COMMENT_REG = "[\\/]{2,}.*"; //may remove the \n
    public static final String REGION_COMMENT_REG = "(?<!\\/)\\/\\*((?:(?!\\*\\/).|\\s)*)\\*\\/";


    public static boolean digit(String str) {
        return str.matches(DIGIT_REG);
    }

    public static boolean number(String str) {
        return str.matches(NUM_REG);
    }

    public static boolean specialChar(String str) {
        return str.matches(SPECIAL_CHAR_REG);
    }

    public static boolean alphabet(String str) {
        return str.matches(ALPHA_REG);
    }

    public static boolean linkableString(String str) {
        return str.matches(LINKABLE_OPERATOR_REG) || str.matches(SINGLE_LINE_COMMENT_REG) || str.matches(REGION_COMMENT_REG);
    }
}
