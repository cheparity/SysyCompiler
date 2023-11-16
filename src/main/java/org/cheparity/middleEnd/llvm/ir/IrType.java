package middleEnd.llvm.ir;

import middleEnd.os.IrPrintable;

public enum IrType implements IrPrintable {
    // PrimitiveTypes - make sure LastPrimitiveTyID stays up to date.
    VoidTyID(""),        //0: type with no size
    Int32TyID("i32"),       //2: 32-bit int type
    LabelTyID(""),       //7: Labels
    TokenTyID(""),       // 10: Tokens

    // Derived types... see DerivedTypes file.
    // Make sure FirstDerivedTyID stays up to date!
    FunctionTyID(""),    ///< 12: Functions
    StructTyID(""),      ///< 13: Structures(no use in SysY)
    ArrayTyID(""),       ///< 14: Arrays
    PointerTyID(""),     ///< 15: Pointers(no use in SysY)
    VectorTyID(""); ///< 16: SIMD 'packed' format, or other vector type(no use in SysY)
    private final String value;

    IrType(String value) {
        this.value = value;
    }

    @Override
    public String toIrCode() {
        return this.value;
    }
}
