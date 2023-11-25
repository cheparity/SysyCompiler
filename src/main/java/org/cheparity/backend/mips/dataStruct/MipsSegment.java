package backend.mips.dataStruct;

import backend.mips.os.MipsPrintable;

public abstract class MipsSegment implements MipsPrintable {
    public final PseudoInst pseudoInst;
    public final MipsContext context;

    MipsSegment(MipsContext context, PseudoInst.PseudoInstId pseudoInst) {
        this.context = context;
        this.pseudoInst = PseudoInst.create(pseudoInst);
    }
}
