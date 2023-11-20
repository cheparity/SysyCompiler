package middleEnd.llvm;

import frontEnd.parser.dataStruct.ASTNode;
import middleEnd.llvm.ir.*;

/**
 * 一个联合结构，为ASTNode拓展了两个属性：number，variable。当我们不知道一个ASTNode节点是number还是register的时候，就用这个结构来表示。
 */
public final class NodeUnion {
    private final ASTNode node;
    private final IrBuilder builder;
    private final BasicBlock block;
    public boolean isNum;
    private Variable variable;
    private int number;

    public NodeUnion(ASTNode node, IrBuilder builder, BasicBlock block) {
        this.node = node;
        this.builder = builder;
        this.block = block;
    }

    public int getNumber() {
        return number;
    }

    public NodeUnion setNumber(int number) {
        this.number = number;
        this.isNum = true;
        return this;
    }

    /**
     * 取负数。如果是数值，直接取反；如果是寄存器，分配指令进行取负数。
     *
     * @return 取负数后的NodeUnion
     */
    public NodeUnion nag() {
        if (this.isNum) {
            return this.setNumber(-this.number);
        }
        //否则分配指令进行取反
        Variable ret = builder.buildNegInst(block, variable);
        return this.setVariable(ret);
    }

    /**
     * 取反。如果是数值，直接取反；如果是寄存器，分配指令进行取反。
     * <p>
     * %4 = load i32, i32* %2, align 4
     * <p>
     * %5 = icmp ne i32 %4, 0
     * <p>
     * %6 = xor i1 %5, true
     * <p>
     *
     * @return 取反后的NodeUnion
     */
    public NodeUnion not() {
        if (this.isNum) {
            return this.setNumber(this.number == 0 ? 1 : 0);
        }
        //否则分配指令进行取反
        var ret = builder.buildNotInst(block, variable);
        return this.setVariable(ret);
    }

    public NodeUnion add(NodeUnion other) {
        if (this.isNum && other.isNum) { //二者都是数字
            return this.setNumber(this.number + other.number);
        }

        if (!this.isNum && !other.isNum) { //二者都不是数字
            Variable ret = builder.buildBinInstruction(block, this.variable, Operator.create(IrType.create(IrType.IrTypeID.Int32TyID),
                    Operator.OpCode.ADD), other.variable);
            return this.setVariable(ret);
        }
        //有一个是数字，是数字的就需要build const variable与另一个相加
        Variable ret;
        if (this.isNum) {
            ret = builder.buildBinInstruction(block, builder.buildConstIntNum(this.number),
                    Operator.create(IrType.create(IrType.IrTypeID.Int32TyID),
                            Operator.OpCode.ADD), other.variable);
        } else {
            ret = builder.buildBinInstruction(block, this.variable, Operator.create(IrType.create(IrType.IrTypeID.Int32TyID),
                    Operator.OpCode.ADD), builder.buildConstIntNum(other.number));
        }
        return this.setVariable(ret);

    }

    public NodeUnion sub(NodeUnion other) {
        if (this.isNum && other.isNum) { //二者都是数字
            return this.setNumber(this.number + other.number);
        }

        if (!this.isNum && !other.isNum) { //二者都不是数字
            Variable ret = builder.buildBinInstruction(block, this.variable, Operator.create(IrType.create(IrType.IrTypeID.Int32TyID),
                    Operator.OpCode.SUB), other.variable);
            return this.setVariable(ret);
        }
        //有一个是数字，是数字的就需要build const variable与另一个相加
        Variable ret;
        if (this.isNum) {
            ret = builder.buildBinInstruction(block, builder.buildConstIntNum(this.number),
                    Operator.create(IrType.create(IrType.IrTypeID.Int32TyID),
                            Operator.OpCode.SUB), other.variable);
        } else {
            ret = builder.buildBinInstruction(block, this.variable, Operator.create(IrType.create(IrType.IrTypeID.Int32TyID),
                    Operator.OpCode.SUB), builder.buildConstIntNum(other.number));
        }
        return this.setVariable(ret);
    }

    public NodeUnion mul(NodeUnion other) {
        if (this.isNum && other.isNum) { //二者都是数字
            return this.setNumber(this.number + other.number);
        }

        if (!this.isNum && !other.isNum) { //二者都不是数字
            Variable ret = builder.buildBinInstruction(block, this.variable, Operator.create(IrType.create(IrType.IrTypeID.Int32TyID),
                    Operator.OpCode.MUL), other.variable);
            return this.setVariable(ret);
        }
        //有一个是数字，是数字的就需要build const variable与另一个相加
        Variable ret;
        if (this.isNum) {
            ret = builder.buildBinInstruction(block, builder.buildConstIntNum(this.number),
                    Operator.create(IrType.create(IrType.IrTypeID.Int32TyID),
                            Operator.OpCode.MUL), other.variable);
        } else {
            ret = builder.buildBinInstruction(block, this.variable, Operator.create(IrType.create(IrType.IrTypeID.Int32TyID),
                    Operator.OpCode.MUL), builder.buildConstIntNum(other.number));
        }
        return this.setVariable(ret);
    }

    public NodeUnion div(NodeUnion other) {
        if (this.isNum && other.isNum) { //二者都是数字
            return this.setNumber(this.number + other.number);
        }

        if (!this.isNum && !other.isNum) { //二者都不是数字
            Variable ret = builder.buildBinInstruction(block, this.variable, Operator.create(IrType.create(IrType.IrTypeID.Int32TyID),
                    Operator.OpCode.SDIV), other.variable);
            return this.setVariable(ret);
        }
        //有一个是数字，是数字的就需要build const variable与另一个相加
        Variable ret;
        if (this.isNum) {
            ret = builder.buildBinInstruction(block, builder.buildConstIntNum(this.number),
                    Operator.create(IrType.create(IrType.IrTypeID.Int32TyID),
                            Operator.OpCode.SDIV), other.variable);
        } else {
            ret = builder.buildBinInstruction(block, this.variable, Operator.create(IrType.create(IrType.IrTypeID.Int32TyID),
                    Operator.OpCode.SDIV), builder.buildConstIntNum(other.number));
        }
        return this.setVariable(ret);
    }

    public NodeUnion mod(NodeUnion other) {
        if (this.isNum && other.isNum) { //二者都是数字
            return this.setNumber(this.number + other.number);
        }

        if (!this.isNum && !other.isNum) { //二者都不是数字
            Variable ret = builder.buildBinInstruction(block, this.variable, Operator.create(IrType.create(IrType.IrTypeID.Int32TyID),
                    Operator.OpCode.SREM), other.variable);
            return this.setVariable(ret);
        }
        //有一个是数字，是数字的就需要build const variable与另一个相加
        Variable ret;
        if (this.isNum) {
            ret = builder.buildBinInstruction(block, builder.buildConstIntNum(this.number),
                    Operator.create(IrType.create(IrType.IrTypeID.Int32TyID),
                            Operator.OpCode.SREM), other.variable);
        } else {
            ret = builder.buildBinInstruction(block, this.variable, Operator.create(IrType.create(IrType.IrTypeID.Int32TyID),
                    Operator.OpCode.SREM), builder.buildConstIntNum(other.number));
        }
        return this.setVariable(ret);
    }

    public Variable getVariable() {
        return this.variable;
    }

    public NodeUnion setVariable(Variable variable) {
        this.variable = variable;
        this.isNum = false;
        return this;
    }
}