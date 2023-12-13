package middleEnd.llvm.ir;

import middleEnd.os.IrPrintable;

public final class IrType implements IrPrintable {
    private final IrTypeID basicType;
    private final IrTypeID derivedType;
    private int size; //如果是数组的话则有size

    private IrType(IrTypeID basicType, IrTypeID derivedType) {
        this.basicType = basicType;
        this.derivedType = derivedType;
    }

    public static IrType create(IrTypeID basicType) {
        return new IrType(basicType, null);
    }

    public static IrType create(IrTypeID basicType, IrTypeID derivedType) {
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
        if (this.isArray()) {
            return "[" + this.getSize() + " x " + this.getBasicType().toIrCode() + "]";
        }
        if (this.isPointer()) {
            return this.getBasicType().toIrCode() + "*";
        }
        return this.getBasicType().toIrCode();
    }

    public boolean isNumber() {
        return !isArray() && !isPointer();
    }

    public boolean isPointer() {
        if (this.getDerivedType() == null) {
            return false;
        }
        return this.getDerivedType() == IrTypeID.PointerTyID;
    }

    public int getSize() {
        assert this.derivedType != null && this.derivedType.equals(IrTypeID.ArrayTyID);
        return size;
    }

    public IrType setDim(int size) {
        this.size = size;
        return this;
    }

    public boolean isArray() {
        if (this.getDerivedType() == null) return false;
        return this.getDerivedType() == IrTypeID.ArrayTyID;
    }

    public enum IrTypeID implements IrPrintable {
        // PrimitiveTypes - make sure LastPrimitiveTyID stays up to date.
        VoidTyID("void"),        //0: type with no size
        BitTyID("i1"),      //1-bit type
        ByteTyID("i8"),
        Int32TyID("i32"),       //2: 32-bit int type
        LabelTyID(""),       //7: Labels
        TokenTyID(""),       // 10: Tokens
        PointerTyID(""),
        // Derived types... see DerivedTypes file.
        // Make sure FirstDerivedTyID stays up to date!
        FunctionTyID(""),    ///< 12: Functions
        StructTyID(""),      ///< 13: Structures(no use in SysY)
        ArrayTyID(""),       ///< 14: Arrays
        VectorTyID(""); ///< 16: SIMD 'packed' format, or other vector type(no use in SysY)
        private final String value;

        IrTypeID(String value) {
            this.value = value;
        }

        boolean superior(IrTypeID other) {
            return this.compareTo(other) > 0;
        }

        @Override
        public String toIrCode() {
            return this.value;
        }
    }

}
