package com.nimbusbase.tpcsltd.twozerogame;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.nimbusbase.nimbusbase.Base;
import com.nimbusbase.nimbusbase.Constant.Config;
import com.nimbusbase.nimbusbase.Server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Will on 11/12/14.
 */
public class Singleton {
    public static Context CONTEXT;

    public static String getDefaultServer() {
        return PreferenceManager.getDefaultSharedPreferences(CONTEXT).getString("defaultServer","");
    }

    public static void setDefaultServer(String defaultServer) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(CONTEXT).edit();
        editor.putString("defaultServer", defaultServer);
        editor.commit();
    }

    public static Base base() {
        return SingletonHolder.sBaseInstance;
    }

    private static class SingletonHolder {

        private final static Base sBaseInstance = new Base(
                CONTEXT,
                new NimbusStorage(CONTEXT),
                getBaseConfigs()
        );

        public static Map<String, Object> getBaseConfigs() {
            final String
                    appName = "game2048";
            return
                    new HashMap<String, Object>() {{
                        put(Config.SERVERS,
                                new ArrayList<Map<String,Object>>() {{
                                    add(new HashMap<String, Object>() {{
                                            put(Config.CLOUD, Config.DROPBOX);
                                            put(Config.APP_NAME, appName);
                                            put(Config.APP_ID, "sz3df7p1dr9tq7g");
                                            put(Config.APP_SECRET, "rwy8f452n0b16da");
                                            put(Config.AUTH_SCOPE, Config.AuthScope.APP_DATA);
                                        }}
                                    );
                                    add(new HashMap<String, Object>() {{
                                            put(Config.CLOUD, Config.BOX);
                                            put(Config.APP_NAME, appName);
                                            put(Config.APP_ID, "2xhcxhtuouujye1mjbc70c2h04mmnd9y");
                                            put(Config.APP_SECRET, "ae3s2pAFqmYAVcZ8IGOwRvM57Whqd6Zm");
                                            put(Config.AUTH_SCOPE, Config.AuthScope.ROOT);
                                        }}
                                    );
                                }}
                        );
                    }};
        }
    }
}
