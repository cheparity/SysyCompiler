package backend.mips.dataStruct;

import backend.mips.os.MipsPrintable;

public class PseudoInst implements MipsPrintable {
    public final PseudoInstId instId;

    private PseudoInst(PseudoInstId instId) {
        this.instId = instId;
    }

    public static PseudoInst create(PseudoInstId id) {
        return new PseudoInst(id);
    }

    public static PseudoInst create(String llvmType) {
        return new PseudoInst(PseudoInstId.ofLlvmType(llvmType));
    }

    @Override
    public String toMipsCode() {
        return instId.value;
    }

    public enum PseudoInstId {
        DataTyID(".data"),
        TextTyID(".text"),
        GlobalTyID(".global"),
        WordTyID(".word"),
        ByteTyID(".byte"),
        AsciiTyID(".ascii"),
        AsciizTyID(".asciiz"),
        SpaceTyID(".space"),
        ;

        final String value;

        PseudoInstId(String value) {
            this.value = value;
        }

        public static PseudoInstId ofLlvmType(String typeStr) {
            return switch (typeStr) {
                case "i32" -> WordTyID;
                default -> throw new IllegalArgumentException("No such pseudo instruction: " + typeStr);
            };
        }
    }
}
