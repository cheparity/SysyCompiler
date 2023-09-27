package grammarLayer.dataStruct;

import java.util.ArrayList;
import java.util.List;

public class ASTNode {
    private final List<ASTNode> children = new ArrayList<>();
    private final GrammarType grammarType;
    private ASTNode father;

    public ASTNode(GrammarType grammarType) {
        this.grammarType = grammarType;
    }

    public void addChild(ASTNode node) {
        node.setFather(this);
        this.children.add(node);
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
}
