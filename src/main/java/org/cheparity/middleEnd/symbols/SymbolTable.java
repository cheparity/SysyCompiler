package middleEnd.symbols;

import exception.DupIdentError;
import frontEnd.parser.dataStruct.ASTNode;
import frontEnd.parser.dataStruct.ErrorHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class SymbolTable {
    private static SymbolTable global;
    /**
     * The outer symbol table of this symbol table. To search outer symbols.
     */
    private final SymbolTable outer;
    /**
     * The inner symbol tables of this symbol table. To store inner symbols.
     */
    private final List<SymbolTable> inners = new ArrayList<>();
    /**
     * The level of this symbol table.
     */
    private final int level;
    /**
     * The directory of this symbol table. To store symbols.
     */
    private final HashMap<String, Symbol> directory = new HashMap<>();

    public SymbolTable(SymbolTable outer, ASTNode belongTo) {
        this.outer = outer;
        if (outer == null) {
            this.level = 0; //global
        } else {
            this.level = outer.level + 1;
            outer.addInnerTable(this);
        }
        belongTo.setSymbolTable(this);
    }

    public static SymbolTable getGlobal() {
        return global;
    }

    public static void setGlobal(SymbolTable global) {
        SymbolTable.global = global;
    }

    private void addInnerTable(SymbolTable inner) {
        inners.add(inner);
    }

    public SymbolTable getOuter() {
        return outer;
    }

    public void addSymbol(Symbol symbol, ErrorHandler errorHandler) {
        String name = symbol.getToken().getRawValue();
        if (!directory.containsKey(name)) {
            this.directory.put(name, symbol);
            return;
        }
        var token = symbol.getToken();
        var preToken = directory.get(name).getToken();
        errorHandler.addError(new DupIdentError(token, preToken));
    }

    /**
     * Lookup a symbol in this symbol table and its outer symbol tables.
     * <p>
     * Both for variable and function.
     *
     * @param name The name of the symbol.(key)
     * @return The symbol if found, otherwise null.(value)
     */
    public Optional<Symbol> getSymbol(String name) {
        /* 注意：有可能出现这种情况：
         * int main() {
         *     int a = 76;
         *     {
         *         printf("%d\n", a);
         *         int a = +-+10;
         *     }
         *     return 0;
         * }
         * 这样在print语句的时候，应该返回的是上层符号表的符号！这种错误应该只会在语法分析中出现，所以语法分析一律调用需要传递token的函数
         */

        if (directory.containsKey(name)) {
            return Optional.of(directory.get(name));
        }
        if (outer != null) {
            return outer.getSymbol(name);
        }
        return Optional.empty();
    }

    private Symbol getSymbolSafely(String ident, int lineNumber) {
        //make assertion
        Symbol candidateResult;
        if (directory.containsKey(ident)) {
            candidateResult = directory.get(ident);
            if (candidateResult.getToken().getLineNum() <= lineNumber) {
                //如果候选符号在定义的符号之前，ok，可以返回
                return candidateResult;
            }
        }
        //否则的话，要找外层符号
        return outer.getSymbolSafely(ident, lineNumber);
    }

    public Symbol getSymbolSafely(String ident, ASTNode visitingNode) {
        //先找到ident的tokenPosition，再定位其位置
        List<ASTNode> idents = visitingNode.getIdentNodes().stream()
                .filter(i -> i.getRawValue().equals(ident)).toList();
        int maxLine = -1;
        for (var i : idents) {
            maxLine = Math.max(i.getLineNumber(), maxLine);
        }
        return getSymbolSafely(ident, maxLine);
    }

    /**
     * Lookup a function variable in this symbol table and its outer symbol tables.
     *
     * @param name The name of the function.(key)
     * @return The function symbol if found, otherwise null.(value)
     */
    public Optional<FuncSymbol> getFuncSymbol(String name) {
        Optional<Symbol> res = getSymbol(name);
        if (res.isPresent() && res.get().getType() == SymbolType.FUNC) return Optional.of((FuncSymbol) res.get());
        return Optional.empty();
    }

}
