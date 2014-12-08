package com.nimbusbase.tpcsltd.twozerogame;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;

public class MainActivity extends ActionBarActivity {

    MainView view;
    public static final String WIDTH = "width";
    public static final String HEIGHT = "height";
    public static final String SCORE = "score";
    public static final String HIGH_SCORE = "high score";
    public static final String UNDO_SCORE = "undo score";
    public static final String CAN_UNDO = "can undo";
    public static final String UNDO_GRID = "undo";
    public static final String GAME_STATE = "game state";
    public static final String UNDO_GAME_STATE = "undo game state";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        view = new MainView(getBaseContext());
        view.setMainActivity(this);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        view.hasSaveState = settings.getBoolean("save_state", false);

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean("hasState")) {
                load();
            }
        }
        setContentView(view);
        // set up NimbusBase
        Singleton.CONTEXT = getApplicationContext();
        Singleton.base();
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
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

    private void save() {
        //SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        //SharedPreferences.Editor editor = settings.edit();
        NimbusStorage settings = new NimbusStorage(this);
        NimbusStorage.CoordinateReader coordinateReader = settings.coordinateReader();
        // Check whether data changed
        boolean differentFromStorage = false;
        for (int xx = 0; xx < view.game.grid.field.length; xx++) {
            if (differentFromStorage) {
                break;
            }
            for (int yy = 0; yy < view.game.grid.field[0].length; yy++) {
                int value = coordinateReader.getCoordinate(xx + " " + yy, -1);
                if (value > 0 &&
                        (view.game.grid.field[xx][yy] == null ||
                                value != view.game.grid.field[xx][yy].getValue())) {
                    differentFromStorage = true;
                    break;
                } else if (value == 0 && view.game.grid.field[xx][yy] != null) {
                    differentFromStorage = true;
                    break;
                }

                int undoValue = coordinateReader.getUndoCoordinate(UNDO_GRID + xx + " " + yy, -1);
                if (undoValue > 0 &&
                        (view.game.grid.undoField[xx][yy] == null ||
                                undoValue != view.game.grid.undoField[xx][yy].getValue())) {
                    differentFromStorage = true;
                     break;
                } else if (undoValue == 0 && view.game.grid.undoField[xx][yy] != null) {
                    differentFromStorage = true;
                    break;
                }
            }
        }

        if (view.game.score != settings.getLong(SCORE, 0)) {
            differentFromStorage = true;
        }
        if (view.game.highScore != settings.getLong(HIGH_SCORE, 0)) {
            differentFromStorage = true;
        };
        if (!differentFromStorage) {
            return;
        }
        // only save if current data is different from the storage
        NimbusStorage.Editor editor = settings.edit();
        Tile[][] field = view.game.grid.field;
        Tile[][] undoField = view.game.grid.undoField;
        editor.putInt(WIDTH, field.length);
        editor.putInt(HEIGHT, field.length);
        for (int xx = 0; xx < field.length; xx++) {
            for (int yy = 0; yy < field[0].length; yy++) {
                if (field[xx][yy] != null) {
                    editor.putCoordinate(xx + " " + yy, field[xx][yy].getValue());
                } else {
                    editor.putCoordinate(xx + " " + yy, 0);
                }

                if (undoField[xx][yy] != null) {
                    editor.putUndoCoordinate(UNDO_GRID + xx + " " + yy, undoField[xx][yy].getValue());
                } else {
                    editor.putUndoCoordinate(UNDO_GRID + xx + " " + yy, 0);
                }
            }
        }
        editor.putLong(SCORE, view.game.score);
        editor.putLong(HIGH_SCORE, view.game.highScore);
        editor.putLong(UNDO_SCORE, view.game.lastScore);
        editor.putBoolean(CAN_UNDO, view.game.canUndo);
        editor.putInt(GAME_STATE, view.game.gameState);
        editor.putInt(UNDO_GAME_STATE, view.game.lastGameState);
        editor.commit();
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

    private void load() {
        //Stopping all animations
        view.game.aGrid.cancelAnimations();
        // update main game's high score
        long newestHighScore = view.game.getHighScore();
        if (newestHighScore >= view.game.highScore) {
            // remote score is higher
            view.game.highScore = newestHighScore;
        } else {
            // local score is higher, overwrite
            view.game.recordHighScore();
        }
        NimbusStorage settings = new NimbusStorage(this);
        NimbusStorage.CoordinateReader coordinateReader = settings.coordinateReader();
        //SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        for (int xx = 0; xx < view.game.grid.field.length; xx++) {
            for (int yy = 0; yy < view.game.grid.field[0].length; yy++) {
                int value = coordinateReader.getCoordinate(xx + " " + yy, -1);
                if (value > 0) {
                    view.game.grid.field[xx][yy] = new Tile(xx, yy, value);
                } else if (value == 0) {
                    view.game.grid.field[xx][yy] = null;
                }

                int undoValue = coordinateReader.getUndoCoordinate(UNDO_GRID + xx + " " + yy, -1);
                if (undoValue > 0) {
                    view.game.grid.undoField[xx][yy] = new Tile(xx, yy, undoValue);
                } else if (value == 0) {
                    view.game.grid.undoField[xx][yy] = null;
                }
            }
        }

        view.game.score = settings.getLong(SCORE, view.game.score);
        view.game.highScore = settings.getLong(HIGH_SCORE, view.game.highScore);
        view.game.lastScore = settings.getLong(UNDO_SCORE, view.game.lastScore);
        view.game.canUndo = settings.getBoolean(CAN_UNDO, view.game.canUndo);
        view.game.gameState = settings.getInt(GAME_STATE, view.game.gameState);
        view.game.lastGameState = settings.getInt(UNDO_GAME_STATE, view.game.lastGameState);
        view.invalidate();
        settings.close();
    }
}
