package backend.mips;

import middleEnd.llvm.RegisterAllocator;

public class MipsRegisterAllocator implements RegisterAllocator {
    private int registerCount = 0;

    @Override
    public String allocate() {
        return "$t" + registerCount++;
    }
}
