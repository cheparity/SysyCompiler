package middleEnd.llvm;

public final class SSARegisterAllocator implements RegisterAllocator {
    int register = -1;

    @Override
    public String allocate() {
        register++;
        return "%" + register;
    }

}
