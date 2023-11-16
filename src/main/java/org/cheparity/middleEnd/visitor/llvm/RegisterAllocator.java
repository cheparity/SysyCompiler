package middleEnd.visitor.llvm;

import frontEnd.symbols.Symbol;

public interface RegisterAllocator {
    String allocate();

    /**
     * 给某个符号分配寄存器
     *
     * @param symbol 符号
     * @return 分配的寄存器名称
     */
    String allocate(Symbol symbol);
}
