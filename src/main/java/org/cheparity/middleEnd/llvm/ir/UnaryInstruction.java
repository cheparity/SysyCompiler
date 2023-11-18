package middleEnd.llvm.ir;

/**
 * [result] = fneg [(optional)fast-math flags]* [ty] [op1]; yields ty:result
 */
public class UnaryInstruction extends Instruction {
    Variable op;
    Operator operator;

}
