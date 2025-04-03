package dev.cacahuete.minebreach.core;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

public class DelayedTaskScheduler {
    private int ticksRemaining;
    private final Runnable task;
    boolean done;

    public static DelayedTaskScheduler seconds(double seconds, Runnable task) {
        return new DelayedTaskScheduler((int)(seconds * 20), task);
    }

    public DelayedTaskScheduler(int delayTicks, Runnable task) {
        this.ticksRemaining = delayTicks;
        this.task = task;
        ServerTickEvents.END_SERVER_TICK.register(this::onEndTick);
    }

    private void onEndTick(MinecraftServer server) {
        if (done) return;

        ticksRemaining--;
        if (ticksRemaining <= 0) {
            task.run();
            done = true;
        }
    }
}