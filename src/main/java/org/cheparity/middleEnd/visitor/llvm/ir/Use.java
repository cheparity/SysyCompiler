package middleEnd.visitor.llvm.ir;

public class Use {
    private Use prev;
    private Use next;
    private User user;
    private Value value;

    Use(Value value, User user) {
        this.value = value;
        this.user = user;
    }
}
