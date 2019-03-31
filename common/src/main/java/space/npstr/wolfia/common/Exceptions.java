package space.npstr.wolfia.common;

import java.util.concurrent.CompletionException;

public class Exceptions {

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
