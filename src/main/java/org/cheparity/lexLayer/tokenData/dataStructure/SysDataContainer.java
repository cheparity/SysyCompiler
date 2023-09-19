package lexLayer.tokenData.dataStructure;

import java.io.Serializable;

public interface SysDataContainer<T extends Serializable> {

    T getValue();

    /**
     * String or Int.
     *
     * @return The value type.
     */
    SysValueType getType();
}
