package backend.mips.dataStruct;

import backend.mips.os.MipsPrintable;

public class DataDeclInst implements MipsPrintable {
    final String name;
    final PseudoInst type;
    final String value;

    public DataDeclInst(String name, String type, String value) {
        this.name = name;
        this.type = PseudoInst.create(type);
        this.value = value;
    }

    @Override
    public String toMipsCode() {
        return String.format("%s: %s %s", name, type.toMipsCode(), value);
    }
}