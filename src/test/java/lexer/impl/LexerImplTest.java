package lexer.impl;

import frontEnd.lexer.SysYLexer;
import frontEnd.lexer.dataStruct.LexType;
import frontEnd.lexer.impl.LexerImpl;
import org.junit.Assert;
import org.junit.Test;

public class LexerImplTest {
    SysYLexer l = LexerImpl.getInstance();

    @Test
    public void testEmpty() {
        String str1 = "";
        String str2 = "\n";
        String str3 = "\t";
        String str4 = " ";
        String str5 = "\r";
        Assert.assertTrue(str1.isBlank());
        Assert.assertTrue(str2.isBlank());
        Assert.assertTrue(str3.isBlank());
        Assert.assertTrue(str4.isBlank());
        Assert.assertTrue(str5.isBlank());
    }

    @Test
    public void testReadNumber() {

        l.next();
        System.out.println("-----");
        l.next();
        System.out.println("-----");
        l.next();
        System.out.println("-----");
    }

    @Test
    public void testTokens() {
        l.getAllTokens().forEach(System.out::println);
    }

    @Test
    public void testLexType() {
        Assert.assertEquals(LexType.ofValue("main"), LexType.MAINTK);
        Assert.assertEquals(LexType.ofValue("["), LexType.LBRACK);
    }
}