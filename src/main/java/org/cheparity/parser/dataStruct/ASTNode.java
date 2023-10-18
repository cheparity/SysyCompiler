package parser.dataStruct;

import exception.GrammarError;
import lexer.dataStruct.Token;
import utils.LoggerUtil;
import visitor.ErrorHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.logging.Logger;

public class ASTNode {
    private final static Logger LOGGER = LoggerUtil.getLogger();
    private final List<ASTNode> children = new ArrayList<>();
    private final GrammarType grammarType;
    private final ErrorHandler errorHandler = new ErrorHandler();
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
     * Find the first node with the given type in the tree
     *
     * @param type  the type to find
     * @param layer the max layer to find. `layer = 0` means that only find in the current node
     * @return the first node with the given type
     */
    public Optional<ASTNode> deepFind(GrammarType type, int layer) {
        if (layer < 0) {
            return Optional.empty();
        }
        if (this.getGrammarType() == type) {
            return Optional.of(this);
        }
        for (ASTNode child : this.getChildren()) {
            Optional<ASTNode> res = child.deepFind(type, layer - 1);
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

    public List<Token> getTokens() {
        List<Token> tokens = new ArrayList<>();
        for (ASTNode child : this.getChildren()) {
            if (child instanceof ASTLeaf) {
                tokens.add(((ASTLeaf) child).getToken());
            } else {
                tokens.addAll(child.getTokens());
            }
        }
        return tokens;
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

    @Override
    public String toString() {
        return "ASTNode{" + "children=" + children + ", grammarType=" + grammarType + '}';
    }
}
