package de.felixstaude.roguelike.core;

import de.felixstaude.roguelike.combat.DamageSystem;
import de.felixstaude.roguelike.entity.*;
import de.felixstaude.roguelike.input.Input;
import de.felixstaude.roguelike.math.Vec2;
import de.felixstaude.roguelike.shop.Shop;
import de.felixstaude.roguelike.ui.HUD;
import de.felixstaude.roguelike.world.Arena;
import de.felixstaude.roguelike.world.EnemySpawner;
import de.felixstaude.roguelike.world.WaveManager;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.List;

public class Engine implements Runnable {
    public static final int WIDTH = 1280;
    public static final int HEIGHT = 720;
    public static final int TARGET_UPS = 60;
    public static final double DT = 1.0 / TARGET_UPS;

    private final GameCanvas canvas;
    private final Input input = new Input();
    private final Camera camera = new Camera();
    private final Player player = new Player();
    private final Arena arena = new Arena(0, 0, WIDTH, HEIGHT);

    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();
    private final EnemySpawner spawner = new EnemySpawner(arena);
    private final DamageSystem damageSystem = new DamageSystem(player, bullets, enemies, particles);
    private final WaveManager waves = new WaveManager(30.0);
    private final Shop shop = new Shop();

    private Thread loopThread;
    private volatile boolean running = false;
    private boolean showDebug = true;
    private double fps, ups;

    private GameState state = GameState.RUNNING;
    private final Rectangle restartButton = new Rectangle(WIDTH/2 - 120, HEIGHT/2 + 20, 240, 48);

    public Engine(GameCanvas canvas) {
        this.canvas = canvas;
        canvas.addKeyListener(input);
        canvas.addMouseListener(input);
        canvas.addMouseMotionListener(input);
        canvas.setFocusable(true);
        canvas.requestFocus();

        camera.viewW = WIDTH; camera.viewH = HEIGHT;
        player.pos.set(WIDTH / 2.0, HEIGHT / 2.0);

        // Initiale Wave-Parameter an den Spawner
        spawner.onWaveStart(waves.getWave());

        input.onKeyPressed = code -> { if (code == KeyEvent.VK_F3) showDebug = !showDebug; };
    }

    public void start() {
        if (running) return;
        running = true;
        loopThread = new Thread(this, "GameLoop");
        loopThread.start();
    }

    @Override public void run() {
        canvas.createBufferStrategy(3);
        BufferStrategy bs = canvas.getBufferStrategy();

        long prev = System.nanoTime();
        double acc = 0.0;
        long fpsTimer = System.currentTimeMillis();
        int frames = 0, updates = 0;

        while (running) {
            long now = System.nanoTime();
            double delta = (now - prev) / 1_000_000_000.0;
            prev = now;
            acc += delta;

            while (acc >= DT) { update(DT); acc -= DT; updates++; }

            do {
                do {
                    Graphics2D g = (Graphics2D) bs.getDrawGraphics();
                    try { render(g); } finally { g.dispose(); }
                } while (bs.contentsRestored());
                bs.show();
            } while (bs.contentsLost());
            frames++;

            if (System.currentTimeMillis() - fpsTimer >= 1000) {
                fps = frames; ups = updates; frames = 0; updates = 0; fpsTimer += 1000;
            }
        }
    }

    private void update(double dt) {
        input.poll();

        if (state == GameState.GAME_OVER) {
            if (input.wasPressed(KeyEvent.VK_R) ||
                    (input.mousePressedL && restartButton.contains(input.mouseX, input.mouseY))) {
                restartGame();
            }
            return;
        }

        if (state == GameState.SHOP) {
            boolean start = shop.handleInput(input, player);
            if (start) {
                waves.nextWave();
                spawner.onWaveStart(waves.getWave()); // <<< neue Wave -> neuen Schwierigkeitsmodus aktivieren
                state = GameState.RUNNING;
            }
            return;
        }

        // RUNNING
        double ax=0, ay=0;
        if (input.isDown(KeyEvent.VK_W)) ay -= 1;
        if (input.isDown(KeyEvent.VK_S)) ay += 1;
        if (input.isDown(KeyEvent.VK_A)) ax -= 1;
        if (input.isDown(KeyEvent.VK_D)) ax += 1;
        Vec2 move = new Vec2(ax, ay).normalized();

        player.update(dt, move, input.mouseX, input.mouseY, bullets, particles);

        player.pos.x = Math.max(arena.x + player.radius, Math.min(arena.x + arena.w - player.radius, player.pos.x));
        player.pos.y = Math.max(arena.y + player.radius, Math.min(arena.y + arena.h - player.radius, player.pos.y));

        waves.update(dt);
        spawner.update(dt, enemies, player);

        for (Enemy e : enemies) {
            e.update(dt, player, bullets);
            e.pos.x = Math.max(arena.x + e.radius, Math.min(arena.x + arena.w - e.radius, e.pos.x));
            e.pos.y = Math.max(arena.y + e.radius, Math.min(arena.y + arena.h - e.radius, e.pos.y));
        }

        for (int i = bullets.size()-1; i>=0; i--) { Bullet b = bullets.get(i); b.update(dt, enemies); if (b.dead) bullets.remove(i); }
        for (int i = particles.size()-1; i>=0; i--) { Particle p = particles.get(i); p.update(dt); if (p.dead) particles.remove(i); }

        damageSystem.update(dt);

        if (player.isDead()) {
            state = GameState.GAME_OVER;
            return;
        }

        if (waves.isFinished()) {
            bullets.clear();
            enemies.clear();
            particles.clear();
            shop.prepareForWave(waves.getWave(), player);
            state = GameState.SHOP;
            return;
        }

        if (input.wasPressed(KeyEvent.VK_K)) player.damage(8);
        if (input.wasPressed(KeyEvent.VK_L)) player.heal(8);
    }

    private void restartGame() {
        bullets.clear();
        particles.clear();
        enemies.clear();
        spawner.reset();
        waves.reset();

        player.hp = player.maxHp;
        player.xp = 0;
        player.gold = 0;
        player.pos.set(WIDTH/2.0, HEIGHT/2.0);

        // Wave 1 Schwierigkeitsparameter aktivieren
        spawner.onWaveStart(waves.getWave());

        state = GameState.RUNNING;
    }

    private void render(Graphics2D g) {
        g.setColor(new Color(18,20,26));
        g.fillRect(0,0,WIDTH,HEIGHT);

        drawGrid(g);

        g.setColor(new Color(60,66,80));
        g.drawRect(arena.x, arena.y, arena.w-1, arena.h-1);

        for (Enemy e: enemies) e.render(g);
        for (Particle p: particles) p.render(g);
        for (Bullet b: bullets) b.render(g);
        player.render(g);

        if (state == GameState.RUNNING) {
            String t = String.format("Wave %d — %02d:%02d", waves.getWave(), (int)(waves.getTimeLeft()/60), (int)(waves.getTimeLeft()%60));
            HUD.drawTopBanner(g, t);
        } else if (state == GameState.SHOP) {
            HUD.drawTopBanner(g, "Shop – Wave " + waves.getWave() + " beendet");
        }

        HUD.drawBars(g, player);
        HUD.drawCrosshair(g, input.mouseX, input.mouseY);
        if (showDebug) HUD.drawDebug(g, fps, ups, bullets.size(), particles.size(), player);

        if (state == GameState.GAME_OVER) {
            HUD.drawGameOverOverlay(g, restartButton);
        }
        if (state == GameState.SHOP) {
            shop.render(g, player, waves.getWave());
        }
    }

    private void drawGrid(Graphics2D g) {
        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(32,36,45));
        int step=32;
        for (int x=0;x<WIDTH;x+=step) g.drawLine(x,0,x,HEIGHT);
        for (int y=0;y<HEIGHT;y+=step) g.drawLine(0,y,WIDTH,y);
    }
}
