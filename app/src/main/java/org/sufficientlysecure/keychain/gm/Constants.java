package org.sufficientlysecure.keychain.gm;

public class Constants {

    public static final String TAG = "Keychain_gm";

    public static final boolean DEBUG = BuildConfig.DEBUG;
    public static final String OPEN_KEYCHAIN_PACKAGE_NAME =
            DEBUG ? "org.sufficientlysecure.keychain.debug" : "org.sufficientlysecure.keychain";


    public static final String TEMPSTORAGE_AUTHORITY = BuildConfig.APPLICATION_ID + ".tempstorage";
    public static final int TEMPFILE_TTL = 24 * 60 * 60 * 1000; // 1 day

}
