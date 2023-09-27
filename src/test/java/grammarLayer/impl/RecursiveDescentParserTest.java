package grammarLayer.impl;

import junit.framework.TestCase;

public class RecursiveDescentParserTest extends TestCase {

    private final RecursiveDescentParser parser = new RecursiveDescentParser();

    public void testParse() {
        System.out.println("=========parse result:=========");
        System.out.println(parser.peekAST());
        System.out.println("=========parse end=========");
    }

}