package dev.cacahuete.minebreach.core;

import net.minecraft.util.math.random.Random;

public class Shuffler {
    public static void shuffle(int[] input) {
        Random rand = Random.createLocal();

        for (int i = input.length - 1; i > 0; i--) {
            int j = rand.nextInt(i + 1); // 0 to i (inclusive)
            // Swap array[i] with array[j]
            int temp = input[i];
            input[i] = input[j];
            input[j] = temp;
        }
    }
}
