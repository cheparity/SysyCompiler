package middleEnd.llvm;

public interface RegisterAllocator {
    String allocate();

    /**
     * 给函数分配寄存器
     *
     * @param funcName 函数名
     * @return 分配的寄存器名称
     */
    String allocate(String funcName);
}
