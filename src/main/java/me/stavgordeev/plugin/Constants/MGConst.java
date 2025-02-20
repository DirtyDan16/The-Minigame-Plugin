// src/main/java/me/stavgordeev/plugin/MinigameConstants.java
package me.stavgordeev.plugin.Constants;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;


public class MGConst {
    // Constants that define boundaries for random values for changing floor logic.
    public static class FloorLogic {
        // Constants that define boundaries for where a new floor can spawn.
        public static class NewFloorSpawnBoundaries {
            public static final int UPPER_BOUND_X_CENTER = 10;
            public static final int LOWER_BOUND_X_CENTER = 5;
            public static final int UPPER_BOUND_Z_CENTER = 10;
            public static final int LOWER_BOUND_Z_CENTER = 5;
            public static final int UPPER_BOUND_Y_CENTER = 1;
            public static final int LOWER_BOUND_Y_CENTER = -3;
        }
        public static class FloorSize {
            public static final int UPPER_BOUND_X_RADIUS = 7;
            public static final int LOWER_BOUND_X_RADIUS = 2;
            public static final int UPPER_BOUND_Z_RADIUS = 7;
            public static final int LOWER_BOUND_Z_RADIUS = 2;
        }

        // Constants that define boundaries for how often a certain floor changes its materials.
        public static class ChangingFloor {
            public static final int UPPER_BOUND_START_INTERVAL = 25;
            public static final int LOWER_BOUND_START_INTERVAL = 22;
            public static final int UPPER_BOUND_STOP_INTERVAL = 15;
            public static final int LOWER_BOUND_STOP_INTERVAL = 10;
        }


        public static final int DELAY_TO_SELECT_A_FLOOR_MATERIAL = 25;
        public static final int DURATION_OF_STAYING_IN_A_FLOOR_WITH_ONLY_CHOSEN_MATERIAL = 60;

        public static final Material[] DEFAULT_FLOOR_BLOCK_TYPES = new Material[]{Material.RED_WOOL, Material.BLUE_WOOL, Material.GREEN_WOOL,Material.YELLOW_WOOL};
    }

    public static final World WORLD = Bukkit.getWorld("world");
    public static final Location GAME_START_LOCATION = new Location(WORLD, 0, 150, 0);
    public static final int MIN_INTERVAL = 1;
}