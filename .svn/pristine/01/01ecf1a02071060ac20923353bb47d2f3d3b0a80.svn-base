package se.su.dsv.maryam;

import android.content.Context;

public class Global {
    private static Global instance = null;
    private Context context;

    public Global( Context context ) {
        this.context = context.getApplicationContext();
    }

    public static Global getInstance( Context context ) {
        if( instance == null )
            instance = new Global( context );
        return instance;
    }

    public LocalDatabase getDatabase() {
        return new LocalDatabase(context);
    }
}