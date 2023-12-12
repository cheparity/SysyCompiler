package frontEnd.parser.dataStruct;

import exception.GrammarError;
import frontEnd.lexer.dataStruct.Token;
import frontEnd.parser.ASTNodeElement;
import middleEnd.ASTNodeVisitor;
import middleEnd.llvm.visitor.*;
import middleEnd.symbols.SymbolTable;
import utils.LoggerUtil;

import java.util.*;
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

    public ASTNode addChild(ASTNode node) {
        node.setFather(this);
        this.children.add(node);
        return this;
    }

    public ErrorHandler getErrorHandler() {
        return this.errorHandler;
    }

    public ASTNode getFather() {
        return this.father;
    }

    public ASTNode setFather(ASTNode father) {
        assert !(father instanceof ASTLeaf);
        this.father = father;
        return this;
    }

    public ASTNode replaceItselfAs(ASTNode as) {
        if (this.father == null) {
            LOGGER.warning("Cannot find the father node");
            return null;
        }
        this.father.replaceChildAs(this, as);
        return as;
    }

    public void replaceChildAs(ASTNode child, ASTNode as) {
        int index = this.children.indexOf(child);
        if (index == -1) {
            LOGGER.warning("Cannot find the child node");
            return;
        }
        this.children.set(index, as);
        as.setFather(this);
    }

    public List<ASTNode> getChildren() {
        return children;
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    public ASTNode setSymbolTable(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        return this;
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
        while (index < 0) {
            index = index + children.size();
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

    public Optional<ASTNode> deepDownFind(GrammarType type) {
        if (this.getGrammarType() == type) {
            return Optional.of(this);
        }
        for (ASTNode child : this.getChildren()) {
            Optional<ASTNode> res = child.deepDownFind(type);
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

    public int getLineNumber() {
        Token fstLeaveTk = getLeaves().get(0);
        return fstLeaveTk.getLineNum();
    }

    public String getIdent() {
        if (this.getGrammarType() == GrammarType.IDENT) {
            return this.getRawValue();
        }
        for (var child : children) {
            String res = child.getIdent();
            if (res != null) {
                return res;
            }
        }
        return null;
    }

    public Set<String> getIdents() {
        if (this.getGrammarType() == GrammarType.IDENT) {
            return Set.of(this.getRawValue());
        }
        Set<String> res = new HashSet<>();
        for (var child : children) {
            res.addAll(child.getIdents());
        }
        return res;
    }

    public String getRawValue() {
        if (this.getGrammarType().isTerminal()) {
            return ((ASTLeaf) this).getToken().getRawValue();
        }
        List<Token> leaves = getLeaves();
        StringBuilder sb = new StringBuilder();
        for (Token leaf : leaves) {
            sb.append(leaf.getRawValue()).append(" ");
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


    @Override
    public void accept(ASTNodeVisitor visitor) {
        if (visitor instanceof GlobalVarVisitor) {
            //过滤出全局变量定义
            children.stream()
                    .filter(node -> node.getGrammarType() == GrammarType.DECL)
                    .forEach(visitor::visit);
        } else if (visitor instanceof FuncVisitor) {
            //过滤出函数定义
            children.stream()
                    .filter(node -> node.getGrammarType() == GrammarType.FUNC_DEF | node.getGrammarType() == GrammarType.MAIN_FUNC_DEF)
                    .forEach(visitor::visit);
        } else if (visitor instanceof BlockVisitor) {
            if (getGrammarType() == GrammarType.BLOCK) {
                visitor.visit(this);
                return;
            }
            //过滤出函数体
            children.stream()
                    .filter(node -> node.getGrammarType() == GrammarType.BLOCK)
                    .forEach(visitor::visit);
        } else if (visitor instanceof LocalVarVisitor) {
            //过滤出局部变量定义（特殊，因为调用者node是blockItem，而我们没有blockItemVisitor）
            assert this.getGrammarType() == GrammarType.BLOCK_ITEM;
            // BlockItem -> Decl | Stmt
            // Decl -> ConstDecl | VarDecl
            // 所以我们直接过滤出ConstDecl && VarDecl。
            if (getChild(0).getGrammarType() == GrammarType.DECL) {
                for (var node : getChild(0).getChildren()) {
                    visitor.visit(node); //传递过去的是constDecl && varDecl
                }
            }
        } else if (visitor instanceof StmtVisitor) {
            // BlockItem -> Decl | Stmt
            if (getGrammarType() == GrammarType.STMT || getGrammarType() == GrammarType.FOR_STMT) {
                visitor.visit(this);
            } else if (getChild(0).getGrammarType() == GrammarType.STMT || getChild(0).getGrammarType() == GrammarType.FOR_STMT) {
                visitor.visit(getChild(0));
            }
        } else {
            //默认行为，不应该到这里
            LOGGER.severe("ASTNode accept error");
//            visitor.visit(children, this);
        }
    }
}
