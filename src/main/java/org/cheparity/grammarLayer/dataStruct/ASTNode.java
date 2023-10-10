package grammarLayer.dataStruct;

import java.util.ArrayList;
import java.util.List;

public class ASTNode {
    private final List<ASTNode> children = new ArrayList<>();
    private final GrammarType grammarType;
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

    @Override
    public String toString() {
        return "ASTNode{" +
                "children=" + children +
                ", grammarType=" + grammarType +
                '}';
    }
}
