package com.nimbusbase.tpcsltd.twozerogame;

import android.content.Context;
import android.preference.SwitchPreference;
import android.view.View;

import com.nimbusbase.nimbusbase.Server;
import com.nimbusbase.tpcstld.twozerogame.R;

/**
 * Created by Will on 11/10/14.
 */
public class ListItemServer extends SwitchPreference {

    protected final Server
        mServer;
    private boolean selected;

    public ListItemServer(Context context, Server server)  {
        super(context);
        this.mServer = server;
        String defaultServer = Singleton.getDefaultServer();
        if (defaultServer.equals(server.getCloud())) {
            setTitle(server.getCloud()+"✓");
            selected = true;
        } else {
            setTitle(server.getCloud());
        }

        setSwitchTextOff("Out");
        setSwitchTextOn("In");
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        if (selected) {
            view.setBackgroundColor(view.getResources().getColor(R.color.list_background_selected));
        }
    }

    public Server getServer() {
        return mServer;
    }

    @Override
    protected void onClick() {
    }
}
