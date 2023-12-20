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

    /*
     *
     *   全局：
     *   # %16 = getelementptr [5 x i32], [5 x i32]* @C, i32 0, i32 0
     * 	 la      $t0, C(全局数组就直接这么写）
     *   # 计算第一次偏移
     *   la      $t0, [offset]($t0) #从基地址偏移offset的地址
     *   # 计算第二次偏移
     *   la      $t0, [offset]($t0) #从偏移offset的地址偏移offset
     *
     *   lw      $t0, ($t0)         #取数
     *   sw      $t0, (新分配的offset)($fp) #存起来
     *
     *
     *
     *   局部：
     *   # %4 = getelementptr i32, i32* %3, i32 2
     *   la      $t0, [%3的offset]($t0) #拿到%3数组的的基地址
     *   #计算偏移 => 存到t1里
     *
     *   la      $t0, ($t1) #基地址计算一次偏移
     *
     *   lw      $t0, ($t0)         #取数
     *   sw      $t0, (新分配的offset)($fp) #存起来
     *
     * */

    @Override
    public String toMipsCode() {
        var sb = new StringBuilder();
        var ptrName = ptrVal.getName();
        //全局数组
        if (ptrName.startsWith("@")) {
            sb.append("la\t\t$t0, ").append(ptrName.substring(1));
        } else {
            //否则是局部数组
            Integer arrMemOff = getMipsRegisterAllocator().getFpMemOff(ptrName);
            sb.append(String.format("lw\t\t$t0, %s($fp)", arrMemOff));
        }
        //循环计算偏移
        for (var offVariable : this.index) {
            if (offVariable.getNumber().isPresent()) {
                sb
                        .append("\n\t")
                        .append("li\t\t$t1, ")
                        .append(offVariable.getNumber().get());
            } else {
                //否则是个变量，需要先load出来
                String name = offVariable.getName();
                Integer memOff = getMipsRegisterAllocator().getFpMemOff(name);
                sb
                        .append("\n\t")
                        .append("lw\t\t$t1, ").append(memOff).append("$(fp)");
            }
            //然后t1左移2
            sb.append("\n\t").append("sll\t\t$t1, $t1, 2");
            //t0再加上t1(偏移)
            sb.append("\n\t").append("addu\t$t1, $t1, $t0"); //t1就是偏移的地址
        }
        //最后存数
        if (getMipsRegisterAllocator().getFpMemOff(resultPointer.getName()) != null) {
            sb
                    .append("\n\t")
                    .append("lw\t\t$t0, ($t1)")
                    .append("\n\t")
                    .append("sw\t\t$t0, ").append(getMipsRegisterAllocator().getFpMemOff(resultPointer.getName())).append("($fp)");
            return sb.toString();
        }

        sb.append("\n\t").append("addiu\t$sp, $sp, -4");
        getMipsRegisterAllocator().addFpOffset(resultPointer.getName());

        sb
                .append("\n\t")
                .append("sw\t\t$t1, ($sp)")
                .append("\n\t");
        return sb.toString();
    }

//    @Override
//    public String toMipsCode() {
//        var sb = new StringBuilder();
//        if (ptrVal.getName().startsWith("@")) {
//            //全局变量有单独的指针
//            sb.append("la\t\t$t0, ").append(ptrVal.getName().substring(1));
//        }
//
//        Integer arrMemOff = getMipsRegisterAllocator().getMemOff(ptrVal.getName());
//
//
//        if (index[index.length - 1].getNumber().isPresent()) {
//            Integer indexNum = index[index.length - 1].getNumber().get();
//            //直接指定变量所在的内存为数组基地址+偏移量
//            getMipsRegisterAllocator().appointMem(resultPointer.getName(), indexNum * 4 + arrMemOff);
//        } else {
//            throw new RuntimeException("not implemented when index is not a number");
//        }
//        return null;
//    }
}
