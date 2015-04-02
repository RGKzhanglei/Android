package com.android.fun.snakesurface;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;


public class SnakeActivity extends ActionBarActivity {

    private SnakeView mSnakeView;

    public static final String BUNDDLE_KEY = "BUNDDLE_KEY";

    private GestureDetector mGestureDetector;
    private static final int FLING_MIN_DISTANCE = 50;
    private static final int FLING_MIN_VELOCITY = 0;

    public static final int ACTIVITY_RESULT_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_snake);

        mSnakeView = (SnakeView) findViewById(R.id.snake);
        mSnakeView.setDependentViews((TextView) findViewById(R.id.text), findViewById(R.id.background));

        mGestureDetector = new GestureDetector(this,onGestureListener);

        if (savedInstanceState == null) {
            // We were just launched -- set up a new game
            mSnakeView.setGameState(SnakeView.GAME_STATE.STATE_READY);
            mSnakeView.setGameLevel(SnakeView.GAME_LEVEL.LEVEL_EASY);
        } else {
            // We are being restored
            Bundle map = savedInstanceState.getBundle(BUNDDLE_KEY);
            if (map != null) {
                mSnakeView.restoreState(map);
            } else {
                mSnakeView.setGameState(SnakeView.GAME_STATE.STATE_PAUSE);
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_snake, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.show_history_records) {
            mSnakeView.bStartActivity = true;
            Intent intent = new Intent(this,ScoreHistoryListAcivity.class);
            startActivityForResult(intent, ACTIVITY_RESULT_REQUEST_CODE);
            return true;
        } else if (id == R.id.delete_history_records) {
            if (DBAdapter.getInstance(this).deleteHistoryRecords()) {
                Toast.makeText(this,"Delete records success",Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this,"Delete records fail",Toast.LENGTH_LONG).show();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSnakeView.setGameState(SnakeView.GAME_STATE.STATE_PAUSE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle(BUNDDLE_KEY, mSnakeView.saveState());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    GestureDetector.OnGestureListener onGestureListener = new GestureDetector.SimpleOnGestureListener(){
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                               float velocityY) {
            float x = e2.getX() - e1.getX();
            float y = e2.getY() - e1.getY();
            float x_abs = Math.abs(x);
            float y_abs = Math.abs(y);
            if (x_abs >= y_abs) {
                // gesture left or right
                if (x > FLING_MIN_DISTANCE || x < -FLING_MIN_DISTANCE) {
                    if (x > 0) {
                        // Fling right
                        if (mSnakeView.getThread().getGameState() == SnakeView.GAME_STATE.STATE_RUNNING) {
                            // Direction is same as the quadrant which was clicked
                            mSnakeView.moveSnake(SnakeView.DIRECTION.RIGHT);

                        } else {
                            // If the game is not running then on touching any part of the screen
                            // we start the game by sending MOVE_UP signal to SnakeView
                            mSnakeView.moveSnake(SnakeView.DIRECTION.UP);
                        }
                    } else if (x <= 0) {
                        // Fling left
                        if (mSnakeView.getThread().getGameState() == SnakeView.GAME_STATE.STATE_RUNNING) {
                            // Direction is same as the quadrant which was clicked
                            mSnakeView.moveSnake(SnakeView.DIRECTION.LEFT);

                        } else {
                            // If the game is not running then on touching any part of the screen
                            // we start the game by sending MOVE_UP signal to SnakeView
                            mSnakeView.moveSnake(SnakeView.DIRECTION.UP);
                        }
                    }
                }
            } else {
                // gesture down or up
                if (y > FLING_MIN_DISTANCE || y < -FLING_MIN_DISTANCE) {
                    if (y > 0) {
                        // Fling down
                        if (mSnakeView.getThread().getGameState() == SnakeView.GAME_STATE.STATE_RUNNING) {
                            // Direction is same as the quadrant which was clicked
                            mSnakeView.moveSnake(SnakeView.DIRECTION.DOWN);

                        } else {
                            // If the game is not running then on touching any part of the screen
                            // we start the game by sending MOVE_UP signal to SnakeView
                            mSnakeView.moveSnake(SnakeView.DIRECTION.UP);
                        }
                    } else if (y <= 0) {
                        // Fling up
                        if (mSnakeView.getThread().getGameState() == SnakeView.GAME_STATE.STATE_RUNNING) {
                            // Direction is same as the quadrant which was clicked
                            mSnakeView.moveSnake(SnakeView.DIRECTION.UP);

                        } else {
                            // If the game is not running then on touching any part of the screen
                            // we start the game by sending MOVE_UP signal to SnakeView
                            mSnakeView.moveSnake(SnakeView.DIRECTION.UP);
                        }
                    }
                }
            }
            return true;
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case ACTIVITY_RESULT_REQUEST_CODE:
                    mSnakeView.bStartActivity = true;
                    break;
                default:
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
