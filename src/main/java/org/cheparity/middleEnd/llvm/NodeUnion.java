package middleEnd.llvm;

import frontEnd.parser.dataStruct.ASTNode;
import middleEnd.llvm.ir.Variable;

/**
 * 一个联合结构，为ASTNode拓展了两个属性：number，variable。当我们不知道一个ASTNode节点是number还是register的时候，就用这个结构来表示。
 */
public class NodeUnion {
    private final ASTNode node;
    private Variable variable;
    private int number;

    public NodeUnion(ASTNode node) {
        this.node = node;
    }

    public NodeUnion setVariable(Variable variable) {
        this.variable = variable;
        return this;
    }

    public NodeUnion setNumber(int number) {
        this.number = number;
        return this;
    }

    public NodeUnion nag() {
        return this.setNumber(-this.number);
    }

    public NodeUnion not() {
        return this.setNumber(this.number == 0 ? 1 : 0);
    }

    public NodeUnion add(NodeUnion other) {
    }

    public NodeUnion sub(NodeUnion other) {
    }

    public NodeUnion mul(NodeUnion other) {

    }

    public NodeUnion div(NodeUnion other) {

    }

    public NodeUnion mod(NodeUnion other) {

    }
}