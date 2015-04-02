package com.android.fun.snakesurface;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by zhang.la on 2015/3/27.
 */
public class SnakeView extends SurfaceView implements SurfaceHolder.Callback {
    public static final String TAG = "SnakeView";

    private SnakeThread mThread;

    /** 游戏状态 */
    public enum GAME_STATE implements IEnum{
        STATE_NULL(-1),
        /** 准备就绪 */
        STATE_READY(0),
        /** 运行状态 */
        STATE_RUNNING(1),
        /** 暂停状态 */
        STATE_PAUSE(2),
        /** 失败 */
        STATE_LOSE(3),
        /** 胜利 */
        STATE_WIN(4);


        private int value;
        private GAME_STATE(int value){
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    /** 游戏的级别 */
    public enum GAME_LEVEL implements IEnum{
        LEVEL_EASY(600),
        LEVEL_MEDIUM(450),
        LEVEL_HARD(250);


        private int value;
        private GAME_LEVEL(int value){
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    /** 游戏的级别 */
    public enum GAME_STAR_COLOR implements IEnum {
        NULL_STAR(-1),
        RED_STAR(0),
        YELLOW_STAR(1),
        GREEN_STAR(2);


        private int value;
        private GAME_STAR_COLOR(int value){
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    /** 方向 */
    public enum DIRECTION implements IEnum {
        UP(1),
        LEFT(2),
        RIGHT(3),
        DOWN(4);


        private int value;
        private DIRECTION(int value){
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    class SnakeThread extends Thread {

        private Context mContext;
        private SurfaceHolder mSurfaceHolder;
        private Handler mHandler;
        private GAME_STATE mState = GAME_STATE.STATE_NULL;

        /** Indicate whether the surface has been created & is ready to draw */
        private boolean mRun = false;

        private final Paint mPaint = new Paint();

        /**
         * A hash that maps integer handles specified by the subclasser to the drawable that will be
         * used for that reference
         */
        private Bitmap[] mTileArray;

        /**
         * A two-dimensional array of integers in which the number represents the index of the tile that
         * should be drawn at that locations
         */
        private GAME_STAR_COLOR[][] mTileGrid;

        /**
         * mSnakeTrail: A list of Coordinates that make up the snake's body mAppleList: The secret
         * location of the juicy apples the snake craves.
         */
        private ArrayList<Coordinate> mSnakeTrail = new ArrayList<Coordinate>();
        private ArrayList<Coordinate> mAppleList = new ArrayList<Coordinate>();

        private final Object mRunLock = new Object();
        /**
         * Current direction the snake is headed.
         */
        private DIRECTION mDirection = DIRECTION.UP;
        private DIRECTION mNextDirection = DIRECTION.UP;
        /**
         * mScore: Used to track the number of apples captured mMoveDelay: number of milliseconds
         * between snake movements. This will decrease as apples are captured.
         */
        private int mScore = 0;
        private long mMoveDelay = GAME_LEVEL.LEVEL_EASY.getValue();
        /**
         * mLastMove: Tracks the absolute time when the snake last moved, and is used to determine if a
         * move should be made based on mMoveDelay.
         */
        private long mLastMove = 0;

        /**
         * Create a simple handler that we can use to cause animation to happen. We set ourselves as a
         * target and we can use the sleep() function to cause an update/invalidate to occur at a later
         * date.
         */
        private RefreshHandler mRedrawHandler = new RefreshHandler();

        class RefreshHandler extends Handler {

            @Override
            public void handleMessage(Message msg) {
                SnakeThread.this.update();
                SnakeView.this.invalidate();
            }

            public void sleep(long delayMillis) {
                this.removeMessages(0);
                sendMessageDelayed(obtainMessage(0), delayMillis);
            }
        };

        public SnakeThread(SurfaceHolder surfaceHolder, Context context,
                           Handler handler) {
            mSurfaceHolder = surfaceHolder;
            mHandler = handler;
            mContext = context;
        }

        @Override
        public void run() {
            while (mRun) {
                Canvas c = null;
                try {
                    c = mSurfaceHolder.lockCanvas(null);
                    synchronized (mSurfaceHolder) {
                        if (mState == GAME_STATE.STATE_RUNNING) update();
                        // Critical section. Do not allow mRun to be set false until
                        // we are sure all canvas draw operations are complete.
                        //
                        // If mRun has been toggled false, inhibit canvas operations.
                        synchronized (mRunLock) {
                            if (c != null && mRun) doDraw(c);
                        }
                    }
                } finally {
                    // do this in a finally so that if an exception is thrown
                    // during the above, we don't leave the Surface in an
                    // inconsistent state
                    if (c != null) {
                        mSurfaceHolder.unlockCanvasAndPost(c);
                    }
                }
            }
        }

        /******************************************************private function(start)*****************************************************/

        /**
         * Draws the snake, and background to the provided
         * Canvas.
         */
        private void doDraw(Canvas canvas) {
            clearCanvas(canvas);
            for (int x = 0; x < mXTileCount; x += 1) {
                for (int y = 0; y < mYTileCount; y += 1) {
                    if ((mTileGrid[x][y]).getValue() >= 0) {
                        canvas.drawBitmap(mTileArray[mTileGrid[x][y].getValue()], mXOffset + x * mTileSize,
                                mYOffset + y * mTileSize, mPaint);
                    }
                }
            }
        }

        private void clearCanvas(Canvas canvas) {
            Paint paint = new Paint();
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            canvas.drawPaint(paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        }

        /**
         * Selects a random location within the garden that is not currently covered by the snake.
         * Currently _could_ go into an infinite loop if the snake currently fills the garden, but we'll
         * leave discovery of this prize to a truly excellent snake-player.
         */
        private void addRandomApple() {
            Coordinate newCoord = null;
            boolean found = false;
            while (!found) {
                // Choose a new location for our apple
                int newX = 1 + RNG.nextInt(mXTileCount - 2);
                int newY = 1 + RNG.nextInt(mYTileCount - 2);
                newCoord = new Coordinate(newX, newY);

                // Make sure it's not already under the snake
                boolean collision = false;
                int snakelength = mSnakeTrail.size();
                for (int index = 0; index < snakelength; index++) {
                    if (mSnakeTrail.get(index).equals(newCoord)) {
                        collision = true;
                    }
                }
                // if we're here and there's been no collision, then we have
                // a good location for an apple. Otherwise, we'll circle back
                // and try again
                found = !collision;
            }
            if (newCoord == null) {
                Log.e(TAG, "Somehow ended up with a null newCoord!");
            }
            mAppleList.add(newCoord);
        }

        private void initNewGame() {
            mSnakeTrail.clear();
            mAppleList.clear();

            // For now we're just going to load up a short default eastbound snake
            // that's just turned north

            mSnakeTrail.add(new Coordinate(7, 7));
            mSnakeTrail.add(new Coordinate(6, 7));
            mSnakeTrail.add(new Coordinate(5, 7));
            mSnakeTrail.add(new Coordinate(4, 7));
            mSnakeTrail.add(new Coordinate(3, 7));
            mSnakeTrail.add(new Coordinate(2, 7));
            mNextDirection = DIRECTION.UP;

            // Two apples to start with
            addRandomApple();
            addRandomApple();

            mMoveDelay = GAME_LEVEL.LEVEL_HARD.getValue();
            mScore = 0;
        }

        /**
         * Given a ArrayList of coordinates, we need to flatten them into an array of ints before we can
         * stuff them into a map for flattening and storage.
         *
         * @param cvec : a ArrayList of Coordinate objects
         * @return : a simple array containing the x/y values of the coordinates as
         *         [x1,y1,x2,y2,x3,y3...]
         */
        private int[] coordArrayListToArray(ArrayList<Coordinate> cvec) {
            int[] rawArray = new int[cvec.size() * 2];

            int i = 0;
            for (Coordinate c : cvec) {
                rawArray[i++] = c.x;
                rawArray[i++] = c.y;
            }

            return rawArray;
        }

        /**
         * Draws some walls.
         */
        private void updateWalls() {
            for (int x = 0; x < mXTileCount; x++) {
                setTile(GAME_STAR_COLOR.GREEN_STAR, x, 0);
                setTile(GAME_STAR_COLOR.GREEN_STAR, x, mYTileCount - 1);
            }
            for (int y = 1; y < mYTileCount - 1; y++) {
                setTile(GAME_STAR_COLOR.GREEN_STAR, 0, y);
                setTile(GAME_STAR_COLOR.GREEN_STAR, mXTileCount - 1, y);
            }
        }

        /**
         * Draws some apples.
         */
        private void updateApples() {
            for (Coordinate c : mAppleList) {
                setTile(GAME_STAR_COLOR.YELLOW_STAR, c.x, c.y);
            }
        }

        /**
         * Figure out which way the snake is going, see if he's run into anything (the walls, himself,
         * or an apple). If he's not going to die, we then add to the front and subtract from the rear
         * in order to simulate motion. If we want to grow him, we don't subtract from the rear.
         */
        private void updateSnake() {
            boolean growSnake = false;

            // Grab the snake by the head
            Coordinate head = mSnakeTrail.get(0);
            Coordinate newHead = new Coordinate(1, 1);

            mDirection = mNextDirection;

            switch (mDirection) {
                case RIGHT: {
                    newHead = new Coordinate(head.x + 1, head.y);
                    break;
                }
                case LEFT: {
                    newHead = new Coordinate(head.x - 1, head.y);
                    break;
                }
                case UP: {
                    newHead = new Coordinate(head.x, head.y - 1);
                    break;
                }
                case DOWN: {
                    newHead = new Coordinate(head.x, head.y + 1);
                    break;
                }
            }

            // Collision detection
            // For now we have a 1-square wall around the entire arena
            if ((newHead.x < 1) || (newHead.y < 1) || (newHead.x > mXTileCount - 2)
                    || (newHead.y > mYTileCount - 2)) {
                setGameState(GAME_STATE.STATE_LOSE);
                return;
            }

            // Look for collisions with itself
            int snakelength = mSnakeTrail.size();
            for (int snakeindex = 0; snakeindex < snakelength; snakeindex++) {
                Coordinate c = mSnakeTrail.get(snakeindex);
                if (c.equals(newHead)) {
                    setGameState(GAME_STATE.STATE_LOSE);
                    return;
                }
            }

            // Look for apples
            int applecount = mAppleList.size();
            for (int appleindex = 0; appleindex < applecount; appleindex++) {
                Coordinate c = mAppleList.get(appleindex);
                if (c.equals(newHead)) {
                    mAppleList.remove(c);
                    addRandomApple();

                    mScore++;
//                    mMoveDelay *= 0.9;

                    growSnake = true;
                }
            }

            // push a new head onto the ArrayList and pull off the tail
            mSnakeTrail.add(0, newHead);
            // except if we want the snake to grow
            if (!growSnake) {
                mSnakeTrail.remove(mSnakeTrail.size() - 1);
            }

            int index = 0;
            for (Coordinate c : mSnakeTrail) {
                if (index == 0) {
                    setTile(GAME_STAR_COLOR.YELLOW_STAR, c.x, c.y);
                } else {
                    setTile(GAME_STAR_COLOR.RED_STAR, c.x, c.y);
                }
                index++;
            }

        }

        /**
         * Resets all tiles to 0 (empty)
         *
         */
        public void clearTiles() {
            for (int x = 0; x < mXTileCount; x++) {
                for (int y = 0; y < mYTileCount; y++) {
                    setTile(GAME_STAR_COLOR.NULL_STAR, x, y);
                }
            }
        }

        /**
         * Function to set the specified Drawable as the tile for a particular integer key.
         *
         * @param key
         * @param tile
         */
        public void loadTile(GAME_STAR_COLOR key, Drawable tile) {
            Bitmap bitmap = Bitmap.createBitmap(mTileSize, mTileSize, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            tile.setBounds(0, 0, mTileSize, mTileSize);
            tile.draw(canvas);

            mTileArray[key.getValue()] = bitmap;
        }

        /**
         * Rests the internal array of Bitmaps used for drawing tiles, and sets the maximum index of
         * tiles to be inserted
         *
         * @param tilecount
         */

        public void resetTiles(int tilecount) {
            mTileArray = new Bitmap[tilecount];
        }

        /**
         * Used to indicate that a particular tile (set with loadTile and referenced by an integer)
         * should be drawn at the given x/y coordinates during the next invalidate/draw cycle.
         *
         * @param tileindex
         * @param x
         * @param y
         */
        public void setTile(GAME_STAR_COLOR tileindex, int x, int y) {
            mTileGrid[x][y] = tileindex;
        }
        /*******************************************************private function(end)****************************************************/


        /*******************************************************public function(start)****************************************************/
        /**
         * Pauses the physics update & animation.
         */
        public void pause() {
            synchronized (mSurfaceHolder) {
                if (mState == GAME_STATE.STATE_RUNNING) setGameState(GAME_STATE.STATE_PAUSE);
            }
        }

        /**
         * Used to signal the thread whether it should be running or not.
         * Passing true allows the thread to run; passing false will shut it
         * down if it's already running. Calling start() after this was most
         * recently called with false will result in an immediate shutdown.
         *
         * @param b true to run, false to shut down
         */
        public void setRunning(boolean b) {
            // Do not allow mRun to be modified while any canvas operations
            // are potentially in-flight. See doDraw().
            synchronized (mRunLock) {
                mRun = b;
            }
        }

        public boolean getRunning() {
            return mRun;
        }

        /**
         * Sets the game mode. That is, whether we are running, paused, in the
         * failure state, in the victory state, etc.
         *
         * @see #setGameState(com.android.fun.snakesurface.SnakeView.GAME_STATE, CharSequence)
         * @param mode one of the STATE_* constants
         */
        public void setGameState(GAME_STATE mode) {
            synchronized (mSurfaceHolder) {
                setGameState(mode, null);
            }
        }

        /**
         * Sets the game mode. That is, whether we are running, paused, in the
         * failure state, in the victory state, etc.
         *
         * @param mode one of the STATE_* constants
         * @param message string to add to screen or null
         */
        public void setGameState(GAME_STATE mode, CharSequence message) {
            /*
             * This method optionally can cause a text message to be displayed
             * to the user when the mode changes. Since the View that actually
             * renders that text is part of the main View hierarchy and not
             * owned by this thread, we can't touch the state of that View.
             * Instead we use a Message + Handler to relay commands to the main
             * thread, which updates the user-text View.
             */
            synchronized (mSurfaceHolder) {
                GAME_STATE oldState = mState;
                mState = mode;

                Resources res = getContext().getResources();
                CharSequence str = "";
                int backgroundVisible = 0;
                int statusVisible = 0;
                switch (mState) {
                    case STATE_RUNNING:
                        if (oldState != GAME_STATE.STATE_RUNNING) {
                            statusVisible = View.INVISIBLE;
                            backgroundVisible = View.VISIBLE;
                        }
                        break;
                    case STATE_PAUSE:
                        statusVisible = View.VISIBLE;
                        backgroundVisible = View.GONE;
                        str = res.getText(R.string.mode_pause);
                        break;
                    case STATE_READY:
                        statusVisible = View.VISIBLE;
                        backgroundVisible = View.GONE;
                        str = res.getText(R.string.mode_ready);
                        break;
                    case STATE_LOSE:
                        statusVisible = View.VISIBLE;
                        backgroundVisible = View.GONE;
                        DBAdapter.getInstance(mContext).saveHistoryRecords(System.currentTimeMillis()+"",mScore);
                        str = res.getString(R.string.mode_lose, mScore);
                        break;
                    default:
                        break;
                }

                Message msg = mHandler.obtainMessage();
                Bundle b = new Bundle();
                b.putString("text", str.toString());
                b.putInt("status_visible", statusVisible);
                b.putInt("background_visible", backgroundVisible);
                msg.setData(b);
                mHandler.sendMessage(msg);

            }
        }

        /**
         * Sets the current difficulty.
         *
         * @param difficulty
         */
        public void setGameLevel(GAME_LEVEL difficulty) {
            synchronized (mSurfaceHolder) {
                mMoveDelay = difficulty.getValue();
            }
        }

        /**
         * Handles snake movement triggers from Snake Activity and moves the snake accordingly. Ignore
         * events that would cause the snake to immediately turn back on itself.
         *
         * @param direction The desired direction of movement
         */
        public void moveSnake(DIRECTION direction) {

            switch (direction) {
                case UP:
                    if (mState == GAME_STATE.STATE_READY || mState == GAME_STATE.STATE_LOSE) {
                /*
                 * At the beginning of the game, or the end of a previous one,
                 * we should start a new game if UP key is clicked.
                 */
                        initNewGame();
                        setGameState(GAME_STATE.STATE_RUNNING);
                        update();
                        return;
                    }

                    if (mState == GAME_STATE.STATE_PAUSE) {
                /*
                 * If the game is merely paused, we should just continue where we left off.
                 */
                        setGameState(GAME_STATE.STATE_RUNNING);
                        update();
                        return;
                    }

                    if (mDirection != DIRECTION.DOWN) {
                        mNextDirection = DIRECTION.UP;
                    }
                    break;
                case DOWN:
                    if (mDirection != DIRECTION.DOWN) {
                        mNextDirection = DIRECTION.DOWN;
                    }
                    return;
                case LEFT:
                    if (mDirection != DIRECTION.RIGHT) {
                        mNextDirection = DIRECTION.LEFT;
                    }
                    break;
                case RIGHT:
                    if (mDirection != DIRECTION.LEFT) {
                        mNextDirection = DIRECTION.RIGHT;
                    }
                    break;
                default:
                    break;
            }

        }

        /**
         * Handles the basic update loop, checking to see if we are in the running state, determining if
         * a move should be made, updating the snake's location.
         */
        public void update() {
            if (mState == GAME_STATE.STATE_RUNNING) {
                long now = System.currentTimeMillis();

                if (now - mLastMove > mMoveDelay) {
                    clearTiles();
                    updateWalls();
                    updateSnake();
                    updateApples();
                    mLastMove = now;
                }
                mRedrawHandler.sleep(mMoveDelay);
            }

        }

        public GAME_STATE getGameState() {
            return mState;
        }

        /*******************************************************public function(end)****************************************************/

    }

    /**
     * Parameters controlling the size of the tiles and their range within view. Width/Height are in
     * pixels, and Drawables will be scaled to fit to these dimensions. X/Y Tile Counts are the
     * number of tiles that will be drawn.
     */
    protected static int mTileSize = 50;

    /** 边框x轴数量 */
    protected static int mXTileCount;
    /** 边框Y轴数量 */
    protected static int mYTileCount;

    private static int mXOffset;
    private static int mYOffset;

    /**
     * Everyone needs a little randomness in their life
     */
    private static final Random RNG = new Random();

    /**
     * mStatusText: Text shows to the user in some run states
     */
    private TextView mStatusText;

    /**
     * mBackgroundView: Background View which shows 4 different colored triangles pressing which
     * moves the snake
     */
    private View mBackgroundView;

    public SnakeView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // register our interest in hearing about changes to our surface
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        mThread = new SnakeThread(holder, context, new Handler(){
            @Override
            public void handleMessage(Message msg) {
                mStatusText.setVisibility(msg.getData().getInt("status_visible"));
                mStatusText.setText(msg.getData().getString("text"));
                mBackgroundView.setVisibility(msg.getData().getInt("background_visible"));
            }
        });

        initSnakeView(context);

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int width = wm.getDefaultDisplay().getWidth();
        int height = wm.getDefaultDisplay().getHeight();
        initWidthAndHeight(width,height);

        setFocusable(true); // make sure we get key events
    }

    /**
     * Given a flattened array of ordinate pairs, we reconstitute them into a ArrayList of
     * Coordinate objects
     *
     * @param rawArray : [x1,y1,x2,y2,...]
     * @return a ArrayList of Coordinates
     */
    private ArrayList<Coordinate> coordArrayToArrayList(int[] rawArray) {
        ArrayList<Coordinate> coordArrayList = new ArrayList<Coordinate>();

        int coordCount = rawArray.length;
        for (int index = 0; index < coordCount; index += 2) {
            Coordinate c = new Coordinate(rawArray[index], rawArray[index + 1]);
            coordArrayList.add(c);
        }
        return coordArrayList;
    }

    /**
     * Given a ArrayList of coordinates, we need to flatten them into an array of ints before we can
     * stuff them into a map for flattening and storage.
     *
     * @param cvec : a ArrayList of Coordinate objects
     * @return : a simple array containing the x/y values of the coordinates as
     *         [x1,y1,x2,y2,x3,y3...]
     */
    private int[] coordArrayListToArray(ArrayList<Coordinate> cvec) {
        int[] rawArray = new int[cvec.size() * 2];

        int i = 0;
        for (Coordinate c : cvec) {
            rawArray[i++] = c.x;
            rawArray[i++] = c.y;
        }

        return rawArray;
    }

    private void initWidthAndHeight(int width, int height) {
        mXTileCount = (int) Math.floor(width / mTileSize);
        mYTileCount = (int) Math.floor(height / mTileSize);

        mXOffset = ((width - (mTileSize * mXTileCount)) / 2);
        mYOffset = ((height - (mTileSize * mYTileCount)) / 2);

        mThread.mTileGrid = new GAME_STAR_COLOR[mXTileCount][mYTileCount];
        mThread.clearTiles();
    }

    private void initSnakeView(Context context) {

        setFocusable(true);

        Resources r = this.getContext().getResources();

        mThread.resetTiles(4);
        mThread.loadTile(GAME_STAR_COLOR.RED_STAR, r.getDrawable(R.drawable.redstar));
        mThread.loadTile(GAME_STAR_COLOR.YELLOW_STAR, r.getDrawable(R.drawable.yellowstar));
        mThread.loadTile(GAME_STAR_COLOR.GREEN_STAR, r.getDrawable(R.drawable.greenstar));

    }
    /******************************************************public function(start)*****************************************************/
    /**
     * Fetches the animation thread corresponding to this LunarView.
     *
     * @return the animation thread
     */
    public SnakeThread getThread() {
        return mThread;
    }

    public void moveSnake(DIRECTION direction) {
        mThread.moveSnake(direction);
    }

    public void setGameLevel(GAME_LEVEL level) {
        mThread.setGameLevel(level);
    }

    public void setGameState(GAME_STATE state) {
        mThread.setGameState(state);
    }

    /**
     * Restore game state if our process is being relaunched
     *
     * @param icicle a Bundle containing the game state
     */
    public void restoreState(Bundle icicle) {
        setGameState(GAME_STATE.STATE_PAUSE);

        mThread.mAppleList = coordArrayToArrayList(icicle.getIntArray("mAppleList"));
        mThread.mDirection = (DIRECTION) Utils.getEnumByValue(icicle.getInt("mDirection"),DIRECTION.class);
        mThread.mNextDirection = (DIRECTION) Utils.getEnumByValue(icicle.getInt("mNextDirection"),DIRECTION.class);
        mThread.mMoveDelay = icicle.getLong("mMoveDelay");
        mThread.mScore = icicle.getInt("mScore");
        mThread.mSnakeTrail = coordArrayToArrayList(icicle.getIntArray("mSnakeTrail"));
    }

    /**
     * Save game state so that the user does not lose anything if the game process is killed while
     * we are in the background.
     *
     * @return a Bundle with this view's state
     */
    public Bundle saveState() {
        Bundle map = new Bundle();

        map.putIntArray("mAppleList", coordArrayListToArray(mThread.mAppleList));
        map.putInt("mDirection", Integer.valueOf(mThread.mDirection.getValue()));
        map.putInt("mNextDirection", Integer.valueOf(mThread.mNextDirection.getValue()));
        map.putLong("mMoveDelay", Long.valueOf(mThread.mMoveDelay));
        map.putLong("mScore", Integer.valueOf(mThread.mScore));
        map.putIntArray("mSnakeTrail", coordArrayListToArray(mThread.mSnakeTrail));

        return map;
    }

    /**
     * Sets the Dependent views that will be used to give information (such as "Game Over" to the
     * user and also to handle touch events for making movements
     *
     */
    public void setDependentViews(TextView msgView, View backgroundView) {
        mStatusText = msgView;
        mBackgroundView = backgroundView;
    }
    /******************************************************public function(end)*****************************************************/

    /*******************************************************override function(start)****************************************************/
    /**
     * Standard window-focus override. Notice focus lost so we can pause on
     * focus lost. e.g. user switches to take a call.
     */
    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        if (!hasWindowFocus) mThread.pause();
    }

    public static boolean bStartActivity = false;
    /**
     * Callback invoked when the Surface has been created and is ready to be
     * used.
     * @param holder
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // start the thread here so that we don't busy-wait in run()
        // waiting for the surface to be created
        if (!bStartActivity) {
            mThread.setRunning(true);
            mThread.start();
        } else {
            bStartActivity = false;
        }
    }

    /**
     * Callback invoked when the surface dimensions change
     * used.
     * @param holder
     * @param format
     * @param width
     * @param height
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mXTileCount = (int) Math.floor(width / mTileSize);
        mYTileCount = (int) Math.floor(height / mTileSize);

        mXOffset = ((width - (mTileSize * mXTileCount)) / 2);
        mYOffset = ((height - (mTileSize * mYTileCount)) / 2);

        mThread.mTileGrid = new GAME_STAR_COLOR[mXTileCount][mYTileCount];
        mThread.clearTiles();
    }

    /**
     * Callback invoked when the Surface has been destroyed and must no longer
     * be touched. WARNING: after this method returns, the Surface/Canvas must
     * never be touched again!
     * @param holder
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (!bStartActivity) {
            // we have to tell thread to shut down & wait for it to finish, or else
            // it might touch the Surface after we return and explode
            boolean retry = true;
            mThread.setRunning(false);
            while (retry) {
                try {
                    mThread.join();
                    retry = false;
                } catch (InterruptedException e) {
                }
            }
        }
    }

    /*******************************************************override function(end)****************************************************/

    /******************************************************inner class(start)*****************************************************/
    /**
     * Simple class containing two integer values and a comparison function. There's probably
     * something I should use instead, but this was quick and easy to build.
     */
    private class Coordinate {
        public int x;
        public int y;

        public Coordinate(int newX, int newY) {
            x = newX;
            y = newY;
        }

        public boolean equals(Coordinate other) {
            if (x == other.x && y == other.y) {
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return "Coordinate: [" + x + "," + y + "]";
        }
    }
    /******************************************************inner class(end)*****************************************************/
}
