package com.nimbusbase.tpcsltd.twozerogame;

import android.os.AsyncTask;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Toast;

import com.nimbusbase.nimbusbase.Server;
import com.nimbusbase.nimbusbase.promise.Callback;
import com.nimbusbase.nimbusbase.promise.NMBError;
import com.nimbusbase.nimbusbase.promise.Promise;
import com.nimbusbase.nimbusbase.promise.Response;

import java.util.Objects;

public class InputListener implements View.OnTouchListener {

    private static final int SWIPE_MIN_DISTANCE = 0;
    private static final int SWIPE_THRESHOLD_VELOCITY = 25;
    private static final int MOVE_THRESHOLD = 250;
    private static final int RESET_STARTING = 10;

    private float x;
    private float y;
    private float lastdx;
    private float lastdy;
    private float previousX;
    private float previousY;
    private float startingX;
    private float startingY;
    private int previousDirection = 1;
    private int veryLastDirection = 1;
    private boolean hasMoved = false;

    MainView mView;

    public InputListener(MainView view) {
        super();
        this.mView = view;
    }

    public boolean onTouch(View view, MotionEvent event) {
        if (mView.getMainActivity().screenLocked) {
            Toast.makeText(mView.getMainActivity(), "Screen is locked during sync.", Toast.LENGTH_LONG).show();
            return true;
        }
        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                x = event.getX();
                y = event.getY();
                startingX = x;
                startingY = y;
                previousX = x;
                previousY = y;
                lastdx = 0;
                lastdy = 0;
                hasMoved = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                x = event.getX();
                y = event.getY();
                if (mView.game.isActive()) {
                    float dx = x - previousX;
                    if (Math.abs(lastdx + dx) < Math.abs(lastdx) + Math.abs(dx) && Math.abs(dx) > RESET_STARTING
                            &&  Math.abs(x - startingX) > SWIPE_MIN_DISTANCE) {
                        startingX = x;
                        startingY = y;
                        lastdx = dx;
                        previousDirection = veryLastDirection;
                    }
                    if (lastdx == 0) {
                        lastdx = dx;
                    }
                    float dy = y - previousY;
                    if (Math.abs(lastdy + dy) < Math.abs(lastdy) + Math.abs(dy) && Math.abs(dy) > RESET_STARTING
                            && Math.abs(y - startingY) > SWIPE_MIN_DISTANCE) {
                        startingX = x;
                        startingY = y;
                        lastdy = dy;
                        previousDirection = veryLastDirection;
                    }
                    if (lastdy == 0) {
                        lastdy = dy;
                    }
                    if (pathMoved() > SWIPE_MIN_DISTANCE * SWIPE_MIN_DISTANCE && !hasMoved) {
                        boolean moved = false;
                        //Vertical
                        if (((dy >= SWIPE_THRESHOLD_VELOCITY && Math.abs(dy) >= Math.abs(dx)) || y - startingY >= MOVE_THRESHOLD) && previousDirection % 2 != 0) {
                            moved = true;
                            previousDirection = previousDirection * 2;
                            veryLastDirection = 2;
                            mView.game.move(2);
                        } else if (((dy <= -SWIPE_THRESHOLD_VELOCITY && Math.abs(dy) >= Math.abs(dx)) || y - startingY <= -MOVE_THRESHOLD ) && previousDirection % 3 != 0) {
                            moved = true;
                            previousDirection = previousDirection * 3;
                            veryLastDirection = 3;
                            mView.game.move(0);
                        }
                        //Horizontal
                        if (((dx >= SWIPE_THRESHOLD_VELOCITY && Math.abs(dx) >= Math.abs(dy)) || x - startingX >= MOVE_THRESHOLD) && previousDirection % 5 != 0) {
                            moved = true;
                            previousDirection = previousDirection * 5;
                            veryLastDirection = 5;
                            mView.game.move(1);
                        } else if (((dx <= -SWIPE_THRESHOLD_VELOCITY  && Math.abs(dx) >= Math.abs(dy)) || x - startingX <= -MOVE_THRESHOLD) && previousDirection % 7 != 0) {
                            moved = true;
                            previousDirection = previousDirection * 7;
                            veryLastDirection = 7;
                            mView.game.move(3);
                        }
                        if (moved) {
                            hasMoved = true;
                            startingX = x;
                            startingY = y;
                        }
                    }
                }
                previousX = x;
                previousY = y;
                return true;
            case MotionEvent.ACTION_UP:
                x = event.getX();
                y = event.getY();
                previousDirection = 1;
                veryLastDirection = 1;
                //"Menu" inputs
                if (!hasMoved) {
                    if (iconPressed(mView.sXNewGame, mView.sYIcons)) {
                        mView.game.newGame();
                    } else if (iconPressed(mView.sXUndo, mView.sYIcons)) {
                        mView.game.revertUndoState();
                    } else if (iconPressed(mView.sXSetting, mView.sYIcons)) {
                        // new view for sync setting
                        if (!mView.isAutoSyncOn) {
                            mView.getMainActivity().startSyncOptionActivity();
                        } else {
                            Toast.makeText(mView.getMainActivity(), "Please turn off the AUTO SYNC first.", Toast.LENGTH_LONG).show();
                        }
                    } else if (iconPressed(mView.sXSync, mView.sYIcons)) {
                        String cloudName = Singleton.getDefaultServer();
                        if (!"".equals(cloudName)) {
                            Server defaultServer = null;
                            for (Server server : Singleton.base().getServers()) {
                                if (server.getCloud().equals(cloudName)&&server.isInitialized()) {
                                    defaultServer = server;
                                    break;
                                }
                            }
                            if (defaultServer != null) {
                                startSync(defaultServer);
                            } else {
                                Toast.makeText(mView.getMainActivity(),
                                        "Please select a default server first",
                                        Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(mView.getMainActivity(),
                                    "Please select a default server first",
                                    Toast.LENGTH_LONG).show();
                        }
                    } else if (iconPressed(mView.sXAuto, mView.sYIcons)){
                        if (mView.isAutoSyncOn) {
                            mView.isAutoSyncOn = false;
                            //
                        } else {
                            String cloudName = Singleton.getDefaultServer();
                            if (!"".equals(cloudName)) {
                                Server defaultServer = null;
                                for (Server server : Singleton.base().getServers()) {
                                    if (server.getCloud().equals(cloudName) && server.isInitialized()) {
                                        defaultServer = server;
                                        break;
                                    }
                                }
                                if (defaultServer != null) {
                                    mView.isAutoSyncOn = true;
                                    //start timer
                                    startTimer(defaultServer);
                                } else {
                                    Toast.makeText(mView.getMainActivity(),
                                            "Please select a default server first",
                                            Toast.LENGTH_LONG).show();
                                }
                            } else {
                                Toast.makeText(mView.getMainActivity(),
                                        "Please select a default server first",
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                        mView.invalidate();
                    }else if (isTap(2) && inRange(mView.startingX, x, mView.endingX)
                        && inRange(mView.startingY, x, mView.endingY) && mView.continueButtonEnabled) {
                        mView.game.setEndlessMode();
                    }
                }
        }
        return true;
    }

    public void startSync(final Server defaultServer) {
        mView.getMainActivity().screenLocked = true;
        if (defaultServer.isSynchronizing()) {
            defaultServer.getRunningSync().cancel();
        } else if (defaultServer.canSynchronize()) {
            mView.getMainActivity().save();
            mView.syncPercentage = 0;
            mView.isSyncing = true;
            final Promise
                    promise = defaultServer.synchronize(null);
            AsyncTask syncTask = new AsyncTask<Object, Object, Object>() {

                @Override
                protected Object doInBackground(Object... params) {
                    promise
                            .onProgress(new Callback.ProgressListener() {
                                @Override
                                public void onProgress(double v) {
                                    mView.syncPercentage = (int)Math.ceil(v*100);
                                    mView.invalidate();
                                }
                            })
                            .onAlways(new Callback.AlwaysListener() {
                                @Override
                                public void onAlways(Response response) {
                                    mView.getMainActivity().screenLocked = false;
                                    mView.isSyncing = false;
                                    if (!response.isSuccess()) {
                                        final NMBError
                                                error = response.error;
                                        if (error != null)
                                            Toast.makeText(mView.getMainActivity(), error.toString(), Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(mView.getMainActivity(), "Sync is successful", Toast.LENGTH_LONG).show();
                                    }
                                    mView.getMainActivity().load();
                                    if (mView.isAutoSyncOn) {
                                        startTimer(defaultServer);
                                    }
                                }
                            });
                    return null;
                }
            };
            syncTask.execute();
            mView.invalidate();
        }
    }

    private class TimerForSync extends Thread {
        Server server;
        public TimerForSync(Server defaultServer) {
            server = defaultServer;
        }
        @Override
        public void run() {
            if (mView.isAutoSyncOn) {
                if (!mView.isSyncing) {
                    mView.secondsRemain--;
                    mView.h.postDelayed(this, 1000);
                }
                if (mView.secondsRemain < 1) {
                    //start sync & reset timer
                    mView.secondsRemain = 60;
                    startSync(server);
                }
            }
            mView.invalidate();
        }
    };

    public void startTimer(Server defaultServer) {
        mView.secondsRemain = 60;
        mView.secondsRemain = 60;
        mView.h.postDelayed(new TimerForSync(defaultServer), 1000);
    }

    private float pathMoved() {
        return (x - startingX) * (x - startingX) + (y - startingY) * (y - startingY);
    }

    private boolean iconPressed(int sx, int sy) {
        return isTap(1) && inRange(sx, x, sx + mView.iconSize)
                && inRange(sy, y, sy + mView.iconSize);
    }

    private boolean inRange(float starting, float check, float ending) {
        return (starting <= check && check <= ending);
    }

    private boolean isTap(int factor) {
        return pathMoved() <= mView.iconSize * factor;
    }
}
