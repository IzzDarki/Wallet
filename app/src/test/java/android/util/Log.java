

package android.util;

/** Mock of android.util.Log for unit testing. */
public class Log {
    public static int d(String tag, String msg) {
        System.out.println("DEBUG [" + tag + "] " + msg);
        return 0;
    }

    public static int d(String tag, String msg, Throwable tr) {
        d(tag, msg);

        System.out.print('\t');
        tr.printStackTrace(System.out);
        return 0;
    }

    public static int i(String tag, String msg) {
        System.out.println("INFO [" + tag + "] " + msg);
        return 0;
    }

    public static int i(String tag, String msg, Throwable tr) {
        i(tag, msg);

        System.out.print('\t');
        tr.printStackTrace(System.out);
        return 0;
    }

    public static int w(String tag, String msg) {
        System.out.println("WARN [" + tag + "] " + msg);
        return 0;
    }

    public static int w(String tag, String msg, Throwable tr) {
        w(tag, msg);

        System.out.print('\t');
        tr.printStackTrace(System.out);
        return 0;
    }

    public static int e(String tag, String msg) {
        System.out.println("ERROR [" + tag + "] " + msg);
        return 0;
    }

    public static int e(String tag, String msg, Throwable tr) {
        e(tag, msg);

        System.out.print('\t');
        tr.printStackTrace(System.out);
        return 0;
    }

    public static int v(String tag, String msg) {
        System.out.println("VERBOSE [" + tag + "] " + msg);
        return 0;
    }

    public static int v(String tag, String msg, Throwable tr) {
        v(tag, msg);

        System.out.print('\t');
        tr.printStackTrace(System.out);
        return 0;
    }

    public static int wtf(String tag, String msg) {
        System.out.println("WHAT A TERRIBLE FAILURE [" + tag + "] " + msg);
        return 0;
    }

    public static int wtf(String tag, String msg, Throwable tr) {
        wtf(tag, msg);

        System.out.print('\t');
        tr.printStackTrace(System.out);
        return 0;
    }
}
