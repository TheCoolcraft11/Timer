package de.thecoolcraft11.timer;

public class TimerTarget {
    private final String id;
    private final long time;
    private final String command;
    private boolean executed;

    public TimerTarget(String id, long time, String command) {
        this.id = id;
        this.time = time;
        this.command = command;
        this.executed = false;
    }

    public String getId() {
        return id;
    }

    public long getTime() {
        return time;
    }

    public String getCommand() {
        return command;
    }

    public boolean isExecuted() {
        return executed;
    }

    public void setExecuted(boolean executed) {
        this.executed = executed;
    }

    public void reset() {
        this.executed = false;
    }

    @Override
    public String toString() {
        return id + ": " + time + "s -> " + (command != null ? command : "no command");
    }
}

