package space.npstr.wolfia.common;

import java.util.concurrent.CompletionException;

public class Exceptions {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Exceptions.class);

    public static final Thread.UncaughtExceptionHandler UNCAUGHT_EXCEPTION_HANDLER
            = (thread, throwable) -> log.error("Uncaught exception in thread {}", thread.getName(), throwable);

    //unwrap completion exceptions
    public static Throwable unwrap(Throwable throwable) {
        Throwable realCause = throwable;
        while ((realCause instanceof CompletionException) && realCause.getCause() != null) {
            realCause = realCause.getCause();
        }
        return realCause;
    }

    private Exceptions() {
        //util class
    }
}
