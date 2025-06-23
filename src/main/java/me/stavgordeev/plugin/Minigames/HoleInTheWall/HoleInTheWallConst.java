package me.stavgordeev.plugin.Minigames.HoleInTheWall;

public final class HoleInTheWallConst {

    public final class WallDifficulty {
        public static final int EASY = 0;
        public static final int MEDIUM = 1;
        public static final int HARD = 2;
        public static final int VERY_HARD = 3;
    }

    // Directions in which the wall can come from. i.e South means that the wall will come from the south side of the arena.
    // When a new Wall Object is created, it will be assigned a direction which it'll remember for various logic.
    public enum WallDirection {
        SOUTH, NORTH, WEST, EAST;
    }

    public static final class Timers {
        public static final int GAME_DURATION = 300; // in seconds
        public static final int[] WALL_SPEED_UP_LANDMARKS = {30, 60, 90, 120 , 155, 200}; // in seconds
        public static final int[] INCREASE_WALL_DIFFICULTY_LANDMARKS = {45, 90, 155}; // in seconds
        public static final int[] PLATFORM_SHRINKAGE_LANDMARKS = {70, 155};

        public static final int[] WALL_SPEED = {20,15,12,10,7,5,4,3,2}; //in ticks
    }

    //public static final class;




    public static final String PLATFORMS_FOLDER = "platforms";
    public static final String WALLPACK_FOLDER = "wallpack";
    public static final String MAP_FOLDER = "map";
    public static final String GAME_FOLDER = "holeinthewall";
}
