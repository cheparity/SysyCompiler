package lexLayer.tokenData.dataStructure;

public class SysString implements SysDataContainer<String> {
    private final String value;

    public SysString(String str) {
        this.value = str.substring(1, str.length() - 1);
    }

    @Override
    public String getValue() {
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
