package middleEnd.llvm.ir;

// <result> = getelementptr <ty>, <ty>* <ptrval>, {<ty> <index>}*
// 对于1维数组而言，其实非常固定。几个ty都是一样的
// %3 = getelementptr [(arrSize) x i32], <ptrVal.toIrCode()>, ty 0, ty index
public final class GetElementPtrInstruction extends Instruction {
    final PointerValue ptrVal;
    final Variable[] index;
    final PointerValue resultPointer;

    GetElementPtrInstruction(PointerValue resultPointer, PointerValue ptrVal, Variable... index) {
        assert ptrVal != null;
        this.resultPointer = resultPointer;
        this.ptrVal = ptrVal;
        this.index = index;
    }

    @Override
    public String toIrCode() {
        var str1 = String.format("%s = getelementptr %s, %s",
                resultPointer.getName(),
                ptrVal.getType().toIrCode(),
                ptrVal.toIrCode());
        StringBuilder str2 = new StringBuilder();
        for (var i : index) {
            str2.append(", ").append(i.getType().toIrCode()).append(" ").append(i.getName());
        }
        return str1 + str2;
    }

    @Override
    public String toMipsCode() {
        return null;
    }
}
