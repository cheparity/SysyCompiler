package frontEnd.parser.dataStruct;

import exception.GrammarError;
import frontEnd.lexer.dataStruct.Token;
import frontEnd.parser.ASTNodeElement;
import frontEnd.parser.dataStruct.utils.LoggerUtil;
import frontEnd.symbols.SymbolTable;
import middleEnd.visitor.ASTNodeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.logging.Logger;

public class ASTNode implements ASTNodeElement {
    private final static Logger LOGGER = LoggerUtil.getLogger();
    private final List<ASTNode> children = new ArrayList<>();
    private final GrammarType grammarType;
    private final ErrorHandler errorHandler = new ErrorHandler();
    private SymbolTable symbolTable = null;
    private ASTNode father = null;

    public ASTNode(GrammarType grammarType) {
        this.grammarType = grammarType;
    }

    public void addChild(ASTNode node) {
        node.setFather(this);
        this.children.add(node);
    }

    public void removeLastChild() {
        this.children.get(this.children.size() - 1).setFather(null);
        this.children.remove(this.children.size() - 1);
    }

    public void replaceLastChild(ASTNode node) {
        this.removeLastChild();
        this.addChild(node);
    }

    public ErrorHandler getErrorHandler() {
        return this.errorHandler;
    }

    public ASTNode getFather() {
        return this.father;
    }

    private void setFather(ASTNode father) {
        assert !(father instanceof ASTLeaf);
        this.father = father;
    }

    public List<ASTNode> getChildren() {
        return children;
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    public void setSymbolTable(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    /**
     * Get the child node with the given index.
     * <p>
     * Allow circular access, but not allow the index out of bound.
     *
     * @param index the index of the child node
     * @return the child node with the given index
     */
    public ASTNode getChild(int index) {
        if (index >= children.size()) {
            LOGGER.warning("Index out of bound");
            return null;
        }
        return children.get(index % children.size());
    }

    @Override
    public GrammarType getGrammarType() {
        return grammarType;
    }

    public String peekTree() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getGrammarType().toString());
        if (!this.getChildren().isEmpty()) {
            sb.append("(");
            for (ASTNode child : this.getChildren()) {
                sb.append(child.peekTree());
            }
            sb.append(")");
        }
        return sb.toString();
    }

    public ASTLeaf lastLeaf() {
        if (this.children.isEmpty()) {
            return (ASTLeaf) this;
        }
        return this.children.get(this.children.size() - 1).lastLeaf();
    }

    public Token lastToken() {
        return lastLeaf().getToken();
    }

    public void addGrammarError(GrammarError e) {
        this.errorHandler.addError(e);
    }

    /**
     * Find the <strong>first node</strong> with the given type in the tree
     *
     * @param type  the type to find
     * @param layer the max layer to find. `layer = 0` means that only find in the current node
     * @return the first node with the given type
     */
    public Optional<ASTNode> deepDownFind(GrammarType type, int layer) {
        if (layer < 0) {
            return Optional.empty();
        }
        if (this.getGrammarType() == type) {
            return Optional.of(this);
        }
        for (ASTNode child : this.getChildren()) {
            Optional<ASTNode> res = child.deepDownFind(type, layer - 1);
            if (res.isPresent()) {
                return res;
            }
        }
        return Optional.empty();
    }

    public Optional<ASTNode> deepUpFind(GrammarType type, int layer) {
        if (layer < 0) {
            return Optional.empty();
        }
        if (this.getGrammarType() == type) {
            return Optional.of(this);
        }
        if (this.getFather() == null) {
            return Optional.empty();
        }
        return this.getFather().deepUpFind(type, layer - 1);
    }

    public Optional<ASTNode> deepUpFind(GrammarType type) {
        if (this.getGrammarType() == type) {
            return Optional.of(this);
        }
        if (this.getFather() == null) {
            return Optional.empty();
        }
        return this.getFather().deepUpFind(type);
    }

    /**
     * Cannot invoke if the node is a leaf!
     *
     * @return the leaves of the tree
     */
    public List<Token> getLeaves() {
        assert this.getGrammarType().isNonTerminal();
        List<Token> tokens = new ArrayList<>();
        for (ASTNode child : this.getChildren()) {
            if (child instanceof ASTLeaf) {
                tokens.add(((ASTLeaf) child).getToken());
            } else {
                tokens.addAll(child.getLeaves());
            }
        }
        return tokens;
    }

    public String getRawValue() {
        if (this.getGrammarType().isTerminal()) {
            return ((ASTLeaf) this).getToken().getRawValue();
        }
        List<Token> leaves = getLeaves();
        StringBuilder sb = new StringBuilder();
        for (Token leaf : leaves) {
            sb.append(leaf.getRawValue());
        }
        return sb.toString();
    }

    public TreeSet<GrammarError> getErrors() {
        TreeSet<GrammarError> errStk = new TreeSet<>();
        getErrors(errStk);
        return errStk;
    }

    private void getErrors(TreeSet<GrammarError> errStk) {
        ArrayList<GrammarError> errors = this.getErrorHandler().getErrors();
        errStk.addAll(errors);

        for (var child : children) {
            child.getErrors(errStk);
        }
    }

//    @Override
//    public String toString() {
//        return "ASTNode{" + "rawValue=" + getRawValue() +
//                ", grammarType=" + grammarType +
//                ", children=" + children +
//                '}';
//    }


    @Override
    public void accept(ASTNodeVisitor visitor) {
        visitor.visit(this);
    }
}
