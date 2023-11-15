package frontEnd.parser.dataStruct.utils;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides the logger.
 */
public class LoggerUtil {

    /**
     * SEVERE
     * WARNING
     * INFO
     * CONFIG
     * FINE
     * FINER
     * FINEST
     */
    private static final Level LEVEL = Level.OFF;
    private static final Handler CONSOLE_HANDLER = new ConsoleHandler();
    private static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    public static Logger getLogger() {
        var logger = Logger.getLogger(STACK_WALKER.getCallerClass().getName());
        logger.setUseParentHandlers(false);
        CONSOLE_HANDLER.setLevel(LEVEL);
        logger.addHandler(CONSOLE_HANDLER);
        logger.setLevel(LEVEL);
        return logger;
    }
}
