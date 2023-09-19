package lexLayer.tokenData.dataStructure;

public class SysInt implements SysDataContainer<Integer> {
    private final Integer value;

    public SysInt(String str) {
        this.value = Integer.parseInt(str);
    }

    @Override
    public Integer getValue() {
        return this.value;
    }


    /**
     * String or Int.
     *
     * @return The value type.
     */
    @Override
    public SysValueType getType() {
        return SysValueType.INT;
    }
}
