package middleEnd.llvm;

import java.util.HashMap;
import java.util.Map;

public final class MipsRegisterAllocator {
    /**
     * 变量名->相对于$sp的内存偏移
     */
    private final Map<String, Integer> memOffsetDirectory = new HashMap<>();
    /**
     * 变量名->寄存器
     */
    private final Map<String, MipsRegister> registerAllocatedDirectory = new HashMap<>();
    public int maxSpOffset = 0;
    private int spOffset = 0;

    public void allocaMem(String valName, int size) {
        //肯定不含的
        spOffset -= size;
        memOffsetDirectory.put(valName, spOffset);
        maxSpOffset = Math.min(maxSpOffset, spOffset);
    }

    public void appointMem(String valName, int offset) {
        memOffsetDirectory.put(valName, offset);
    }

    public Integer getMemOff(String valName) {
        if (memOffsetDirectory.containsKey(valName)) {
            return memOffsetDirectory.get(valName);
        } else {
            // 从栈顶开始分配内存（默认为4字节）
            spOffset -= 4;
            memOffsetDirectory.put(valName, spOffset);
            maxSpOffset = Math.min(maxSpOffset, spOffset);
            return spOffset;
        }
    }

    /**
     * 分配临时寄存器，并将其加入到寄存器分配表中
     *
     * @param name 变量名
     * @return 寄存器名
     */
    public String allocaTempReg(String name) {
        //循环赋予t寄存器
        for (int i = 8; i < 16; i++) { //临时寄存器
            String register = MipsRegister.values()[i].getName();
            if (!registerAllocatedDirectory.containsKey(register)) {
                registerAllocatedDirectory.put(name, MipsRegister.values()[i]);
                return register;
            }
        }
        throw new RuntimeException("寄存器不够用了");
    }

    public void freeReg(String... registers) {
        for (String register : registers) {
            var reg = MipsRegister.of(register);
            registerAllocatedDirectory.entrySet().removeIf(entry -> entry.getValue().equals(reg));
        }
    }

    public String getReg(String varName) {
        return registerAllocatedDirectory.get(varName).getName();
    }

    enum MipsRegister {
        ZERO("$zero", "$0"),
        AT("$at", "$1"),
        V0("$v0", "$2"),
        V1("$v1", "$3"),
        A0("$a0", "$4"),
        A1("$a1", "$5"),
        A2("$a2", "$6"),
        A3("$a3", "$7"),
        T0("$t0", "$8"),
        T1("$t1", "$9"),
        T2("$t2", "$10"),
        T3("$t3", "$11"),
        T4("$t4", "$12"),
        T5("$t5", "$13"),
        T6("$t6", "$14"),
        T7("$t7", "$15"),
        S0("$s0", "$16"),
        S1("$s1", "$17"),
        S2("$s2", "$18"),
        S3("$s3", "$19"),
        S4("$s4", "$20"),
        S5("$s5", "$21"),
        S6("$s6", "$22"),
        S7("$s7", "$23"),
        T8("$t8", "$24"),
        T9("$t9", "$25"),
        K0("$k0", "$26"),
        K1("$k1", "$27"),
        GP("$gp", "$28"),
        SP("$sp", "$29"),
        FP("$fp", "$30"),
        RA("$ra", "$31");

        private final String name;
        private final String number;

        MipsRegister(String name, String number) {
            this.name = name;
            this.number = number;
        }

        public static MipsRegister of(String name) {
            for (MipsRegister mipsRegister : MipsRegister.values()) {
                if (mipsRegister.getName().equals(name)) {
                    return mipsRegister;
                }
            }
            return null;
        }

        public String getName() {
            return name;
        }

        public String getNumber() {
            return number;
        }
    }
}
