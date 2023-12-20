package middleEnd.llvm.ir;

// <result> = getelementptr <ty>, <ty>* <ptrval>, {<ty> <index>}*
// 对于1维数组而言，其实非常固定。几个ty都是一样的
// %3 = getelementptr [(arrSize) x i32], <ptrVal.toIrCode()>, ty 0, ty index
public final class GetElementPtrInstruction extends Instruction {
    final PointerValue resultPointer;
    final PointerValue ptrVal;
    final Variable[] index;

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
                ptrVal.toIrCode()
        );
        StringBuilder str2 = new StringBuilder();
        for (var i : index) {
            str2.append(", ").append(i.getType().toIrCode()).append(" ").append(i.getName());
        }
        return str1 + str2;
    }

    //    li $t0, 1
    //    sw $t0, -16($fp)
    //    li $t0, 2
    //    sw $t0, -12($fp)
    //    li $t0, 3
    //    sw $t0, -8($fp)
    //    li $t0, 4
    //    sw $t0, -4($fp)
    @Override
    public String toMipsCode() {
        Integer arrMemOff = getMipsRegisterAllocator().getMemOff(ptrVal.getName());
        if (index[index.length - 1].getNumber().isPresent()) {
            Integer indexNum = index[index.length - 1].getNumber().get();
            getMipsRegisterAllocator().appointMem(resultPointer.getName(), indexNum * 4 + arrMemOff);
        } else {
            throw new RuntimeException("not implemented when index is not a number");
        }
        return null;
    }
}
