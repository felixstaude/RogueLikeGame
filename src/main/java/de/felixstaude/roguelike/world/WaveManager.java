package de.felixstaude.roguelike.world;

public class WaveManager {
    private final double waveDuration; // Sekunden
    private double timeLeft;
    private int wave = 1;
    private boolean finished = false;

    public WaveManager(double waveDurationSeconds) {
        this.waveDuration = waveDurationSeconds;
        this.timeLeft = waveDurationSeconds;
    }

    public void reset() {
        wave = 1;
        timeLeft = waveDuration;
        finished = false;
    }

    /** tickt nur während RUNNING; bei 0 bleibt stehen und setzt finished=true */
    public void update(double dt) {
        if (finished) return;
        timeLeft -= dt;
        if (timeLeft <= 0) {
            timeLeft = 0;
            finished = true;
        }
    }

    /** vom Engine/Shop aufrufen, wenn Spieler die nächste Wave startet */
    public void nextWave() {
        wave++;
        timeLeft = waveDuration;
        finished = false;
    }

    public boolean isFinished() { return finished; }
    public int getWave() { return wave; }
    public double getTimeLeft() { return Math.max(0, timeLeft); }
}
