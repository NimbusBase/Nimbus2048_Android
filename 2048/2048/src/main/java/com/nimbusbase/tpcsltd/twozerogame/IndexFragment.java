package com.nimbusbase.tpcsltd.twozerogame;

import android.app.ActionBar;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nimbusbase.nimbusbase.Base;
import com.nimbusbase.nimbusbase.Server;
import com.nimbusbase.tpcstld.twozerogame.R;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Will on 11/7/14.
 */
public class IndexFragment extends PreferenceFragment {

    private Map<Server.AuthState, String> sAuthStateText;
    private Map<Boolean, String>  sInitStateText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        sAuthStateText = new HashMap<Server.AuthState, String>(4) {{
            put(Server.AuthState.In, getString(R.string.state_in));
            put(Server.AuthState.Out, getString(R.string.state_out));
            put(Server.AuthState.SigningIn, getString(R.string.state_signing_in));
            put(Server.AuthState.SigningOut, getString(R.string.state_signing_out));
        }};
        sInitStateText = new HashMap<Boolean, String>(2) {{
            put(true, getString(R.string.state_initialized));
            put(false, getString(R.string.state_initializing));
        }};
        final Base
                base = getBase();
        bindEvents(base);
        initiatePreferenceScreen(base, R.xml.fragment_index);
    }

    @Override
    public void onDestroy() {
        final Base
                base = getBase();
        unbindEvents(base);
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final ActionBar
                actionBar = getActivity().getActionBar();
        if (actionBar != null)
            actionBar.setTitle(R.string.app_name);
   }

    protected void bindEvents(Base base) {
        final Server[]
                servers = base.getServers();
        for (int index = 0; index < servers.length; index ++) {
            final Server
                    server = servers[index];
            final PropertyChangeSupport
                    support = server.propertyChangeSupport;
            final int
                    innerIndex = index;
            support.addPropertyChangeListener(
                    Server.Property.authState,
                    new PropertyChangeListener() {
                        @Override
                        public void propertyChange(PropertyChangeEvent event) {
                            final Server
                                    innerServer = (Server) event.getSource();
                            onServerStateChange(innerServer, innerIndex);
                        }
                    }
            );
            support.addPropertyChangeListener(
                    Server.Property.isInitialized,
                    new PropertyChangeListener() {
                        @Override
                        public void propertyChange(PropertyChangeEvent event) {
                            final Server
                                    innerServer = (Server) event.getSource();
                            onServerStateChange(innerServer, innerIndex);
                        }
                    }
            );
        }
    }

    protected void unbindEvents(Base base) {
        final Server[]
                servers = base.getServers();
        for (final Server server : servers) {
            final PropertyChangeSupport
                    support = server.propertyChangeSupport;
            for (final PropertyChangeListener listener : support.getPropertyChangeListeners()) {
                support.removePropertyChangeListener(listener);
            }
        }
    }

    protected PreferenceScreen initiatePreferenceScreen(Base base, int preferencesResID) {
        addPreferencesFromResource(preferencesResID);
        final PreferenceScreen
                preferenceScreen = getPreferenceScreen();

        final PreferenceCategory
                serverCate = getServerCategory(preferenceScreen);
        serverCate.setOrderingAsAdded(true);

        final Server[]
                servers =  base.getServers();
        for (int index = 0; index < servers.length; index++) {
            final Server
                    server = servers[index];

            final ListItemServer
                    item = new ListItemServer(getActivity(), server);

            item.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    onServerItemStateChange((ListItemServer) preference, (Boolean) newValue);
                    return false;
                }
            });
            item.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    onServerItemClick((ListItemServer) preference);
                    return true;
                }
            });
            serverCate.addPreference(item);

            onServerStateChange(server, index);
        }
        EditTextPreference autoSyncIntervalEditText = new EditTextPreference(getActivity());
        autoSyncIntervalEditText.getEditText().setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL);
        int seconds = Singleton.getAutoSyncInterval();
        try {
            int config = Integer.valueOf(Singleton.getAutoSyncInterval());
            if ( config>0 ) {
                seconds = config;
            }
        } catch (Throwable e) {

        }
        autoSyncIntervalEditText.setDefaultValue(String.valueOf(seconds));
        autoSyncIntervalEditText.setTitle(getString(R.string.auto_sync_interval));
        autoSyncIntervalEditText.setSummary(seconds + " "+getString(R.string.seconds));
        autoSyncIntervalEditText.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Singleton.setAutoSyncInterval(Integer.valueOf(newValue.toString()));
                setPreferenceScreen(null);
                initiatePreferenceScreen(getBase(), R.xml.fragment_index);
                return true;
            }
        });
        serverCate.addPreference(autoSyncIntervalEditText);
        return preferenceScreen;
    }

    protected void onServerItemClick(ListItemServer item) {
        final Server
                server = item.getServer();
        String defaultServer = Singleton.getDefaultServer();
        if (defaultServer.equals(server.getCloud())) {
            Singleton.setDefaultServer(null);
        } else if (server.isInitialized()) {
            Singleton.setDefaultServer(server.getCloud());
        }
        setPreferenceScreen(null);
        initiatePreferenceScreen(getBase(), R.xml.fragment_index);
        /*final Server
                server = item.getServer();
        if (server.isSynchronizing()) {
            server.getRunningSync().cancel();
        }
        else if (server.canSynchronize()) {
            final int
                    index = Arrays.asList(getBase().getServers()).indexOf(server);
            startSyncOnServer(server, index);
        }*/
    }

    protected void onServerItemStateChange(ListItemServer item, Boolean newValue) {
        final Server
                server = item.getServer();
        final Server.AuthState
                authState = server.getAuthState();
        if (newValue && Server.AuthState.Out == authState) {
            server.authorize(getActivity());
        }
        else if (!newValue && Server.AuthState.In == authState) {
            server.signOut();
        }
    }

    protected void onServerStateChange(Server server, int index) {
        final Server.AuthState
                authState = server.getAuthState();
        final boolean
                initialized = server.isInitialized();
        final  boolean
                syncing = server.isSynchronizing();

        final ListItemServer
                item = (ListItemServer) getServerCategory(getPreferenceScreen()).getPreference(index);
        if (item == null) return;

        if (Server.AuthState.In == authState) {
            item.setChecked(true);
        }
        else if (Server.AuthState.Out == authState) {
            item.setChecked(false);
        }

        if (!syncing) {
            if (Server.AuthState.In == authState) {
                item.setSummary(sInitStateText.get(initialized));
            }
            else {
                item.setSummary(sAuthStateText.get(authState));
            }
        }
    }

    private PreferenceCategory getServerCategory(PreferenceScreen preferenceScreen) {
        return (PreferenceCategory) preferenceScreen.findPreference(getString(R.string.group_servers));
    }

    private static Base getBase() {
        return Singleton.base();
    }
}
