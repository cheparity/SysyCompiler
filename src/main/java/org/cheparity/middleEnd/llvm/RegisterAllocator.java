package middleEnd.llvm;

public interface RegisterAllocator {
    String allocate();

    void rewind();
}
