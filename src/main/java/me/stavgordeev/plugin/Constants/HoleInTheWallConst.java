package me.stavgordeev.plugin.Constants;

public class HoleInTheWallConst {

    public enum WallDifficulty {
        EASY,
        MEDIUM,
        HARD,
        VERY_HARD
    }


    public static final class Timers {
        public static final int GAME_DURATION = 300; // in seconds
        public static final int[] WALL_SPEED_UP_LANDMARKS = { 30, 60, 90, 120 , 155, 200}; // in seconds
        public static final int[] INCREASE_WALL_DIFFICULTY_LANDMARKS = { 45, 90, 155}; // in seconds
        public static final int[] PLATFORM_SHRINKAGE_LANDMARKS = { 70, 155};

        public static final int[] WALL_SPEED = {20,15,12,10,7,5,4,3,2}; //in ticks
    }

    //public static final class;




    public static final String PLATFORMS_FOLDER = "platforms";
    public static final String WALLPACK_FOLDER = "wallpack";
    public static final String MAP_FOLDER = "map";
    public static final String GAME_FOLDER = "holeinthewall";
}
