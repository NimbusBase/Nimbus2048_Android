package com.nimbusbase.tpcsltd.twozerogame;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.Toast;

import com.nimbusbase.tpcstld.twozerogame.R;

import java.util.Date;
import java.util.List;

public class MainActivity extends ActionBarActivity {

    MainView view;
    boolean screenLocked = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // set up NimbusBase
        Singleton.CONTEXT = getApplicationContext();
        Singleton.base();
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        view = new MainView(getBaseContext());
        view.setMainActivity(this);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        //view.hasSaveState = settings.getBoolean("save_state", false);

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean("hasState")) {
                load();
            }
        }
        setContentView(view);
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (screenLocked) {
            Toast.makeText(this, getString(R.string.screen_lock_hint), Toast.LENGTH_LONG).show();
            super.onKeyDown(keyCode, event);
        }
        if ( keyCode == KeyEvent.KEYCODE_MENU) {
            //Do nothing
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            view.game.move(2);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            view.game.move(0);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            view.game.move(3);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            view.game.move(1);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("hasState", true);
        save();
    }

    protected void onPause() {
        super.onPause();
        save();
    }

    public void save() {
        //SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        //SharedPreferences.Editor editor = settings.edit();
        NimbusStorage settings = new NimbusStorage(this);
        List<NimbusStorage.Snapshot> snapshots = settings.getSnapshots();
        NimbusStorage.Snapshot snapshotInStorage = null;
        if (settings.getSnapshots().size()>=1) {
            snapshotInStorage = snapshots.get(0);
        }
        // Check whether data changed
        NimbusStorage.Snapshot newSnapshot = settings.newSnapshot();
        NimbusStorage.Snapshot undoSnapshot = null;
        if (view.game.canUndo) {
            undoSnapshot = settings.newSnapshot();
        }
        newSnapshot.setSize(view.game.grid.field.length);
        if (view.game.canUndo) {
            undoSnapshot.setSize(view.game.grid.field.length);
        }
        for (int xx = 0; xx < view.game.grid.field.length; xx++) {
            for (int yy = 0; yy < view.game.grid.field[0].length; yy++) {
                Tile point = view.game.grid.field[xx][yy];
                int value = point == null ? 0 : point.getValue();
                newSnapshot.putPoint(xx + " " + yy, value);
                if (view.game.canUndo) {
                    Tile undoPoint = view.game.grid.undoField[xx][yy];
                    int undoValue = undoPoint == null ? 0 : undoPoint.getValue();
                    undoSnapshot.putPoint(xx + " " + yy, undoValue);
                }
            }
        }
        long now = new Date().getTime();
        if (view.game.canUndo) {
            undoSnapshot.setCreateAt(now);
            undoSnapshot.setScore(view.game.lastScore);
            undoSnapshot.setState(view.game.lastGameState);
        }
        newSnapshot.setCreateAt(now+1);
        newSnapshot.setScore(view.game.score);
        newSnapshot.setState(view.game.gameState);

        // only save if current data is different from the storage
        if (view.game.canUndo && !newSnapshot.equals(snapshotInStorage)) {
            //if (view.game.canUndo) {
                undoSnapshot.create();
            //}
            newSnapshot.create();
            settings.clearSnapshotTable();

        }
        long highScoreInStorage = settings.getHighScore(0);
        if (view.game.highScore > highScoreInStorage) {
            settings.recordHighScore(view.game.highScore);
            settings.clearHighScoreTable();
        }
        settings.close();
    }

    protected void onResume() {
        super.onResume();
        load();
    }

    public void startSyncOptionActivity() {
        Intent intent = new Intent(this, SyncOptionActivity.class);
        startActivity(intent);
    }

    public void load() {
        //Stopping all animations
        view.game.aGrid.cancelAnimations();
        NimbusStorage settings = new NimbusStorage(this);
        List<NimbusStorage.Snapshot> snapshots = settings.getSnapshots();
        NimbusStorage.Snapshot latest = null,undoSnapshot = null;
        if (snapshots.size()>=1) {
            latest = snapshots.get(0);
        }
        if (snapshots.size()>=2) {
            undoSnapshot = snapshots.get(1);
        }
        NimbusStorage.CoordinateReader coordinateReader = null;
        if (latest != null) {
            coordinateReader = latest.pointsReader();
        }
        NimbusStorage.CoordinateReader undoCoordinateReader = null;
        if (undoSnapshot != null) {
            undoCoordinateReader = undoSnapshot.pointsReader();
        }
        //SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        for (int xx = 0; xx < view.game.grid.field.length; xx++) {
            for (int yy = 0; yy < view.game.grid.field[0].length; yy++) {
                int value = -1;
                if (coordinateReader != null) {
                    value = coordinateReader.getCoordinate(xx + " " + yy, -1);
                }
                if (value > 0) {
                    view.game.grid.field[xx][yy] = new Tile(xx, yy, value);
                } else if (value == 0) {
                    view.game.grid.field[xx][yy] = null;
                }

                int undoValue = -1;
                if (undoCoordinateReader != null) {
                    undoValue = undoCoordinateReader.getCoordinate(xx + " " + yy, -1);
                }
                if (undoValue > 0) {
                    view.game.grid.undoField[xx][yy] = new Tile(xx, yy, undoValue);
                } else if (undoValue == 0) {
                    view.game.grid.undoField[xx][yy] = null;
                }
            }
        }

        view.game.score = latest == null?view.game.score:latest.getScore();
        // update high score
        long highScore = view.game.getHighScore();
        if (view.game.highScore > highScore) {
            view.game.recordHighScore();
        } else {
            view.game.highScore = highScore;
        }
        view.game.lastScore = undoSnapshot == null?view.game.lastScore:undoSnapshot.getScore();
        view.game.canUndo = undoSnapshot == null? view.game.canUndo:true;
        view.game.gameState = latest == null?view.game.gameState:latest.getState();
        view.game.lastGameState = undoSnapshot == null? view.game.lastGameState:undoSnapshot.getState();
        view.invalidate();
        settings.close();
    }
}
