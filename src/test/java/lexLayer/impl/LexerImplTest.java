package lexLayer.impl;

import lexLayer.LexType;
import lexLayer.Lexer;
import org.junit.Assert;
import org.junit.Test;

public class LexerImplTest {
    Lexer l = LexerImpl.getInstance();

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

    }

    @Test
    public void testLexType() {
        Assert.assertEquals(LexType.ofValue("main"), LexType.MAINTK);
        Assert.assertEquals(LexType.ofValue("["), LexType.LBRACK);
    }
}