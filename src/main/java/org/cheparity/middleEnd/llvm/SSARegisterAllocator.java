package middleEnd.llvm;

import middleEnd.RegisterAllocator;

public final class SSARegisterAllocator implements RegisterAllocator {
    int register = -1;

    @Override
    public String allocate() {
        register++;
        return "%" + register;
    }

    @Override
    public void reset() {
        register--;
    }

}
