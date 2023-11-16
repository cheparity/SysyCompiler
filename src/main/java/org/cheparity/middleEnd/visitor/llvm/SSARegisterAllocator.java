package middleEnd.visitor.llvm;

import frontEnd.symbols.Symbol;

public final class SSARegisterAllocator implements RegisterAllocator {
    int register = -1;

    @Override
    public String allocate() {
        register++;
        return "%" + register;
    }

    /**
     * 给某个符号分配寄存器
     *
     * @param symbol 符号
     * @return 分配的寄存器名称
     */
    @Override
    public String allocate(Symbol symbol) {
        String ret = allocate();
        //更新符号的寄存器
        symbol.setRegister(ret);
        return ret;
    }
}
