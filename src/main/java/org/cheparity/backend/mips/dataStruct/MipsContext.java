package backend.mips.dataStruct;

import backend.mips.os.MipsPrintable;

import java.util.ArrayList;
import java.util.List;

public final class MipsContext implements MipsPrintable {
    List<MipsSegment> segments = new ArrayList<>();

    @Override
    public String toMipsCode() {
        StringBuilder sb = new StringBuilder();
        for (MipsSegment segment : segments) {
            sb.append(segment.toMipsCode());
        }
        return sb.toString();
    }
}
