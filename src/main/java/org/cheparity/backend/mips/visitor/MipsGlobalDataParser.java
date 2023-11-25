package backend.mips.visitor;

import backend.mips.LlvmParser;
import utils.LoggerUtil;

import java.util.logging.Logger;

public final class MipsGlobalDataParser implements LlvmParser {
    private final static Logger logger = LoggerUtil.getLogger();

    // 0  1 2         3        4   5
    // @a = dso_local constant i32 5
    // <name> = dso_local <op Code> <type> <value> (name一定由@打头）
    @Override
    public void parse(String code) {

    }

}
