package backend.mips.dataStruct;

import java.util.ArrayList;
import java.util.List;

public class MipsDataSeg extends MipsSegment {
    private static MipsDataSeg instance;
    final List<DataDeclInst> dataDeclInsts = new ArrayList<>();

    private MipsDataSeg(MipsContext context) {
        super(context, PseudoInst.PseudoInstId.DataTyID);
    }

    public static MipsDataSeg getInstance(MipsContext context) {
        if (instance == null) {
            instance = new MipsDataSeg(context);
            context.segments.add(instance);
            return instance;
        }
        return instance;
    }

    public void addDataDeclInst(DataDeclInst dataDeclInst) {
        dataDeclInsts.add(dataDeclInst);
    }

    @Override
    public String toMipsCode() {
        StringBuilder sb = new StringBuilder();
        sb.append(".data\n");
        for (DataDeclInst dataDeclInst : dataDeclInsts) {
            sb.append('\t').append(dataDeclInst.toMipsCode()).append('\n');
        }
        return sb.toString();
    }


}
