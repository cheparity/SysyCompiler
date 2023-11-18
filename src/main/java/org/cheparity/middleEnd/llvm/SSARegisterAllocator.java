package middleEnd.llvm;

public final class SSARegisterAllocator implements RegisterAllocator {
    int register = 0;

    @Override
    public String allocate() {
        register++;
        return "%" + register;
    }

    /**
     * 单纯将函数名称加到"%"后面
     *
     * @param funcName 函数名
     * @return 分配的寄存器名称
     */
    @Override
    public String allocate(String funcName) {
        return "@" + funcName;
    }


}
