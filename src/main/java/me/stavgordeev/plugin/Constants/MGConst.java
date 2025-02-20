// src/main/java/me/stavgordeev/plugin/MinigameConstants.java
package me.stavgordeev.plugin.Constants;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.lang.reflect.Field;


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
            public static final int LOWER_BOUND_X_RADIUS = 3;
            public static final int UPPER_BOUND_Z_RADIUS = 7;
            public static final int LOWER_BOUND_Z_RADIUS = 3;
        }

        // Constants that define boundaries for how often a certain floor changes its materials.
        public static class ChangingFloor {
            public static final int UPPER_BOUND_START_INTERVAL = 20;
            public static final int LOWER_BOUND_START_INTERVAL = 20;
            public static final int UPPER_BOUND_STOP_INTERVAL = 15;
            public static final int LOWER_BOUND_STOP_INTERVAL = 15;

            public static final int DELAY_TO_DECREASE_INTERVAL = 20*10;

            public static final int AMOUNT_OF_CONST = countConstantsInClass(ChangingFloor.class)-1;
        }


        public static final int DELAY_TO_SELECT_A_FLOOR_MATERIAL = 25;
        public static final int DURATION_OF_STAYING_IN_A_FLOOR_WITH_ONLY_CHOSEN_MATERIAL = 60;

        public static final Material[] LIST_OF_FLOOR_MATERIALS = new Material[]{Material.RED_WOOL, Material.BLUE_WOOL, Material.GREEN_WOOL,Material.PURPLE_WOOL,Material.ORANGE_WOOL,Material.YELLOW_WOOL,Material.LIME_WOOL,Material.CYAN_WOOL,Material.LIGHT_BLUE_WOOL};
        public static final Material[] DEFAULT_FLOOR_BLOCK_TYPES = new Material[]{Material.RED_WOOL, Material.BLUE_WOOL, Material.GREEN_WOOL,Material.PURPLE_WOOL};
    }

    public static final World WORLD = Bukkit.getWorld("world");
    public static final Location GAME_START_LOCATION = new Location(WORLD, 0, 150, 0);
    public static final int MIN_INTERVAL = 1;


    public static int countConstantsInClass(Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        int count = 0;
        for (Field field : fields) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) && java.lang.reflect.Modifier.isFinal(field.getModifiers())) {
                count++;
            }
        }
        return count;
    }
}