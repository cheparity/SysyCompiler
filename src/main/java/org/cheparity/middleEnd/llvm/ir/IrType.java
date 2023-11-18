package middleEnd.llvm.ir;

import middleEnd.os.IrPrintable;

public final class IrType implements IrPrintable {
    private final IrTypeID basicType;
    private final IrTypeID derivedType;

    private IrType(IrTypeID basicType, IrTypeID derivedType) {
        this.basicType = basicType;
        this.derivedType = derivedType;
    }

    public static IrType create(IrTypeID basicType) {
        return new IrType(basicType, null);
    }

    static IrType create(IrTypeID basicType, IrTypeID derivedType) {
        return new IrType(basicType, derivedType);
    }

    public IrTypeID getBasicType() {
        return basicType;
    }

    public IrTypeID getDerivedType() {
        return derivedType;
    }

    @Override
    public String toIrCode() {
        return this.basicType.toIrCode();
    }

    public enum IrTypeID implements IrPrintable {
        // PrimitiveTypes - make sure LastPrimitiveTyID stays up to date.
        VoidTyID("void"),        //0: type with no size
        BitTyID("i1"),      //1-bit type
        ByteTyID("i8"),
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

        IrTypeID(String value) {
            this.value = value;
        }

        @Override
        public String toIrCode() {
            return this.value;
        }
    }

}
