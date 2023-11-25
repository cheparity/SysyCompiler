package middleEnd.llvm.ir;

public final class EntryBlock extends BasicBlock {
    private Function entryFunc;

    EntryBlock(String name) {
        super(name);
    }

    public Function getEntryFunc() {
        return this.entryFunc;
    }

    void setEntryFunc(Function function) {
        this.entryFunc = function;
    }

    Instruction getLastInstruction() {
        return instructionList.get(instructionList.size() - 1);
    }

}
