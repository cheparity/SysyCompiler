package grammarLayer.dataStruct;

import java.util.ArrayList;
import java.util.List;

public class ASTNode {
    private final ASTNode father;
    private final List<ASTNode> children = new ArrayList<>();
    private final GrammarType grammarType;

    public ASTNode(GrammarType grammarType, ASTNode father) {
        this.grammarType = grammarType;
        this.father = father;
    }

    protected ASTNode(ASTNode father, GrammarType type) {
        this.father = father;
        this.grammarType = type;
    }


    public void addChild(ASTNode node) {
        this.children.add(node);
    }

    public ASTNode getFather() {
        return this.father;
    }

    public List<ASTNode> getChildren() {
        return children;
    }

    public GrammarType getGrammarType() {
        return grammarType;
    }
}
