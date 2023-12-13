package middleEnd.llvm.utils;

import frontEnd.parser.dataStruct.ASTNode;
import middleEnd.llvm.ir.*;

/**
 * 一个联合结构，为ASTNode拓展了两个属性：number，variable。当我们不知道一个ASTNode节点是number还是register的时候，就用这个结构来表示。
 */
public final class NodeUnion {
    final ASTNode node;
    private final IrBuilder builder;
    private final BasicBlock block;
    public boolean isNum;
    private Variable variable;
    private int number;

    public NodeUnion() {
        this.isNum = false;
        this.variable = null;
        this.number = 0;
        this.node = null;
        this.builder = null;
        this.block = null;
    }

    public NodeUnion(ASTNode node, IrBuilder builder, BasicBlock block) {
        this.builder = builder;
        this.block = block;
        this.node = node;
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


    /// ======================== begin 四则运算，算数运算 ================================
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
            return this.setNumber(this.number - other.number);
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
            return this.setNumber(this.number * other.number);
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
            return this.setNumber(this.number / other.number);
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
            return this.setNumber(this.number % other.number);
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

    /// ======================== end 四则运算，算数运算 ================================
    public Variable getVariable() {
        return this.variable;
    }

    public NodeUnion setVariable(Variable variable) {
        this.variable = variable;
        this.isNum = false;
        return this;
    }

    /// ======================== begin 位运算，逻辑运算 ================================

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

    public NodeUnion or(NodeUnion other) {
        if (this.isNum && other.isNum) {
            //只要有一个不是0，则结果为1
            if (this.number != 0 || other.number != 0) return this.setNumber(1);
            else return this.setNumber(0);
        }
        if (!this.isNum && !other.isNum) {
            Variable ret = builder.buildLogicInst(
                    block,
                    this.variable,
                    Operator.create(IrType.create(IrType.IrTypeID.BitTyID), Operator.OpCode.OR),
                    other.variable
            );
            return this.setVariable(ret);
        }

        //否则分配指令进行或运算
        Variable ret;
        if (this.isNum) {
            ret = builder.buildLogicInst(
                    block,
                    builder.buildConstValue(this.number, IrType.IrTypeID.BitTyID),
                    Operator.create(IrType.create(IrType.IrTypeID.BitTyID), Operator.OpCode.OR),
                    other.variable
            );
        } else {
            ret = builder.buildLogicInst(
                    block,
                    this.variable,
                    Operator.create(IrType.create(IrType.IrTypeID.BitTyID), Operator.OpCode.OR),
                    builder.buildConstValue(other.number, IrType.IrTypeID.BitTyID)
            );
        }
        return this.setVariable(ret);
    }

    public NodeUnion and(NodeUnion other) {
        if (this.isNum && other.isNum) {
            //只要有一个是0，则结果为0
            if (this.number == 0 || other.number == 0) return this.setNumber(0);
            else return this.setNumber(1);
        }
        if (!this.isNum && !other.isNum) {
            Variable ret = builder.buildLogicInst(
                    block,
                    this.variable,
                    Operator.create(IrType.create(IrType.IrTypeID.BitTyID), Operator.OpCode.AND),
                    other.variable
            );
            return this.setVariable(ret);
        }
        //否则分配指令进行与运算
        Variable ret;
        if (this.isNum) {
            ret = builder.buildLogicInst(
                    block,
                    builder.buildConstValue(this.number, IrType.IrTypeID.BitTyID),
                    Operator.create(IrType.create(IrType.IrTypeID.BitTyID), Operator.OpCode.AND),
                    other.variable
            );
        } else {
            ret = builder.buildLogicInst(
                    block,
                    this.variable,
                    Operator.create(IrType.create(IrType.IrTypeID.BitTyID), Operator.OpCode.AND),
                    builder.buildConstValue(other.number, IrType.IrTypeID.BitTyID)
            );
        }
        return this.setVariable(ret);
    }

    public NodeUnion eq(NodeUnion other) {
        if (this.isNum && other.isNum) {
            if (this.number == other.number) return this.setNumber(1);
            else return this.setNumber(0);
        }
        if (!this.isNum && !other.isNum) {
            Variable ret = builder.buildLogicInst(
                    block,
                    this.variable,
                    IcmpInstruction.Cond.EQ,
                    other.variable
            );
            return this.setVariable(ret);
        }
        Variable ret;
        if (this.isNum) {
            ret = builder.buildLogicInst(
                    block,
                    builder.buildConstValue(this.number, IrType.IrTypeID.Int32TyID),
                    IcmpInstruction.Cond.EQ,
                    other.variable
            );
        } else {
            ret = builder.buildLogicInst(
                    block,
                    this.variable,
                    IcmpInstruction.Cond.EQ,
                    builder.buildConstValue(other.number, IrType.IrTypeID.Int32TyID)
            );
        }
        return this.setVariable(ret);
    }

    /**
     * 逻辑不等于
     *
     * @param other 另一个NodeUnion
     * @return 逻辑不等于的结果
     */
    public NodeUnion ne(NodeUnion other) {
        if (this.isNum && other.isNum) {
            if (this.number != other.number) return this.setNumber(1);
            else return this.setNumber(0);
        }
        if (!this.isNum && !other.isNum) {
            Variable ret = builder.buildLogicInst(
                    block,
                    this.variable,
                    IcmpInstruction.Cond.NE,
                    other.variable
            );
            return this.setVariable(ret);
        }

        Variable ret;
        if (this.isNum) {
            ret = builder.buildLogicInst(
                    block,
                    builder.buildConstValue(this.number, IrType.IrTypeID.Int32TyID),
                    IcmpInstruction.Cond.NE,
                    other.variable
            );
        } else {
            ret = builder.buildLogicInst(
                    block,
                    this.variable,
                    IcmpInstruction.Cond.NE,
                    builder.buildConstValue(other.number, IrType.IrTypeID.Int32TyID)
            );
        }
        return this.setVariable(ret);
    }

    /**
     * 逻辑小于
     *
     * @param other 另一个NodeUnion
     * @return 逻辑小于的结果
     */
    public NodeUnion lt(NodeUnion other) {
        if (this.isNum && other.isNum) {
            if (this.number < other.number) return this.setNumber(1);
            else return this.setNumber(0);
        }
        if (!this.isNum && !other.isNum) {
            Variable ret = builder.buildLogicInst(
                    block,
                    this.variable,
                    IcmpInstruction.Cond.SLT,
                    other.variable
            );
            return this.setVariable(ret);
        }
        Variable ret;
        if (this.isNum) {
            ret = builder.buildLogicInst(
                    block,
                    builder.buildConstValue(this.number, IrType.IrTypeID.Int32TyID),
                    IcmpInstruction.Cond.SLT,
                    other.variable
            );
        } else {
            ret = builder.buildLogicInst(
                    block,
                    this.variable,
                    IcmpInstruction.Cond.SLT,
                    builder.buildConstValue(other.number, IrType.IrTypeID.Int32TyID)
            );
        }
        return this.setVariable(ret);
    }

    /**
     * 逻辑小于等于
     *
     * @param other 另一个NodeUnion
     * @return 逻辑小于等于的结果
     */
    public NodeUnion le(NodeUnion other) {
        if (this.isNum && other.isNum) {
            if (this.number <= other.number) return this.setNumber(1);
            else return this.setNumber(0);
        }
        if (!this.isNum && !other.isNum) {
            Variable ret = builder.buildLogicInst(
                    block,
                    this.variable,
                    IcmpInstruction.Cond.SLE,
                    other.variable
            );
            return this.setVariable(ret);
        }
        Variable ret;
        if (this.isNum) {
            ret = builder.buildLogicInst(
                    block,
                    builder.buildConstValue(this.number, IrType.IrTypeID.Int32TyID),
                    IcmpInstruction.Cond.SLE,
                    other.variable
            );
        } else {
            ret = builder.buildLogicInst(
                    block,
                    this.variable,
                    IcmpInstruction.Cond.SLE,
                    builder.buildConstValue(other.number, IrType.IrTypeID.Int32TyID)
            );
        }
        return this.setVariable(ret);
    }

    /**
     * 逻辑大于
     *
     * @param other 另一个NodeUnion
     * @return 逻辑大于的结果
     */
    public NodeUnion gt(NodeUnion other) {
        if (this.isNum && other.isNum) {
            if (this.number > other.number) return this.setNumber(1);
            else return this.setNumber(0);
        }
        if (!this.isNum && !other.isNum) {
            Variable ret = builder.buildLogicInst(
                    block,
                    this.variable,
                    IcmpInstruction.Cond.SGT,
                    other.variable
            );
            return this.setVariable(ret);
        }
        Variable ret;
        if (this.isNum) {
            ret = builder.buildLogicInst(
                    block,
                    builder.buildConstValue(this.number, IrType.IrTypeID.Int32TyID),
                    IcmpInstruction.Cond.SGT,
                    other.variable
            );
        } else {
            ret = builder.buildLogicInst(
                    block,
                    this.variable,
                    IcmpInstruction.Cond.SGT,
                    builder.buildConstValue(other.number, IrType.IrTypeID.Int32TyID)
            );
        }
        return this.setVariable(ret);
    }

    /**
     * 逻辑大于等于
     *
     * @param other 另一个NodeUnion
     * @return 逻辑大于等于的结果
     */
    public NodeUnion ge(NodeUnion other) {
        if (this.isNum && other.isNum) {
            if (this.number >= other.number) return this.setNumber(1);
            else return this.setNumber(0);
        }
        if (!this.isNum && !other.isNum) {
            Variable ret = builder.buildLogicInst(
                    block,
                    this.variable,
                    IcmpInstruction.Cond.SGE,
                    other.variable
            );
            return this.setVariable(ret);
        }
        Variable ret;
        if (this.isNum) {
            ret = builder.buildLogicInst(
                    block,
                    builder.buildConstValue(this.number, IrType.IrTypeID.Int32TyID),
                    IcmpInstruction.Cond.SGE,
                    other.variable
            );
        } else {
            ret = builder.buildLogicInst(
                    block,
                    this.variable,
                    IcmpInstruction.Cond.SGE,
                    builder.buildConstValue(other.number, IrType.IrTypeID.Int32TyID)
            );
        }
        return this.setVariable(ret);
    }

    /// ======================== end 位运算，逻辑运算 ================================

    @Override
    public String toString() {
        if (this.isNum) {
            return String.valueOf(this.getNumber());
        }
        return this.getVariable().getName();
    }
}