package de.felixstaude.roguelike.core;

import de.felixstaude.roguelike.util.Time;

import java.awt.Graphics2D;
import java.awt.image.BufferStrategy;

/**
 * Generic fixed-timestep game loop. Delegates update/render to a handler and exposes FPS/UPS.
 */
public final class GameLoop implements Runnable {
    public interface Handler {
        void onUpdate(double dt);
        void onRender(Graphics2D g);
    }

    private final GameCanvas canvas;
    private final Handler handler;
    private final double step;

    private Thread thread;
    private volatile boolean running = false;
    private double fps;
    private double ups;

    public GameLoop(GameCanvas canvas, int targetUps, Handler handler) {
        this.canvas = canvas;
        this.handler = handler;
        this.step = 1.0 / targetUps;
    }

    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        thread = new Thread(this, "GameLoop");
        thread.start();
    }

    public synchronized void stop() {
        if (!running) {
            return;
        }
        running = false;
        if (thread != null) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public double getFps() {
        return fps;
    }

    public double getUps() {
        return ups;
    }

    @Override
    public void run() {
        canvas.createBufferStrategy(3);
        BufferStrategy strategy = canvas.getBufferStrategy();

        long previous = Time.nowNanos();
        double accumulator = 0.0;
        long fpsTimer = Time.nowMillis();
        int frames = 0;
        int updates = 0;

        while (running) {
            long now = Time.nowNanos();
            double delta = Time.deltaSeconds(previous, now);
            previous = now;
            accumulator += delta;

            while (accumulator >= step) {
                handler.onUpdate(step);
                accumulator -= step;
                updates++;
            }

            do {
                do {
                    Graphics2D g = (Graphics2D) strategy.getDrawGraphics();
                    try {
                        handler.onRender(g);
                    } finally {
                        g.dispose();
                    }
                } while (strategy.contentsRestored());
                strategy.show();
            } while (strategy.contentsLost());
            frames++;

            long millis = Time.nowMillis();
            if (millis - fpsTimer >= 1000) {
                fps = frames;
                ups = updates;
                frames = 0;
                updates = 0;
                fpsTimer += 1000;
            }
        }
    }
}
