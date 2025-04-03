package dev.cacahuete.minebreach.core;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import java.util.ArrayList;

public class TickScheduler {
    static ArrayList<Entry> entries = new ArrayList<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tick();
        });
    }

    public static void schedule(int delayTicks, Runnable task) {
        entries.add(new Entry(delayTicks, task));
    }

    public static void tick() {
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            if (entry.done) continue;

            entry.ticksRemaining--;
            if (entry.ticksRemaining <= 0) {
                entry.task.run();
                entry.done = true;
            }
        }

        entries.removeIf(entry -> entry.done);
    }

    public static class Entry {
        public int ticksRemaining;
        public Runnable task;
        public boolean done;

        public Entry(int delayTicks, Runnable task) {
            this.ticksRemaining = delayTicks;
            this.task = task;
            this.done = false;
        }
    }
}
