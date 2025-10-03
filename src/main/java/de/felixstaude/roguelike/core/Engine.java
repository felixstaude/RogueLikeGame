package de.felixstaude.roguelike.core;

import de.felixstaude.roguelike.combat.DamageSystem;
import de.felixstaude.roguelike.entity.Bullet;
import de.felixstaude.roguelike.entity.Enemy;
import de.felixstaude.roguelike.entity.Particle;
import de.felixstaude.roguelike.entity.Player;
import de.felixstaude.roguelike.input.Input;
import de.felixstaude.roguelike.math.Vec2;
import de.felixstaude.roguelike.shop.Shop;
import de.felixstaude.roguelike.ui.HUD;
import de.felixstaude.roguelike.util.Colors;
import de.felixstaude.roguelike.util.Draw;
import de.felixstaude.roguelike.world.Arena;
import de.felixstaude.roguelike.world.EnemySpawner;
import de.felixstaude.roguelike.world.WaveManager;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Coordinates the high level game states and delegates tick/render work to the underlying systems.
 */
public class Engine implements GameLoop.Handler {
    public static final int TARGET_UPS = 60;

    private final GameCanvas canvas;
    private final GameLoop loop;
    private final Input input = new Input();
    private final EngineArena arenaViewport = new EngineArena();
    private final Runnable toggleFullscreen;

    private final Player player = new Player();
    private final Arena worldBounds = new Arena(0, 0, EngineArena.ARENA_W, EngineArena.ARENA_H);

    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();
    private final EnemySpawner spawner = new EnemySpawner(worldBounds);
    private final DamageSystem damageSystem = new DamageSystem(player, bullets, enemies, particles);
    private final WaveManager waves = new WaveManager(30.0);
    private final Shop shop = new Shop();

    private GameState state = GameState.RUNNING;
    private boolean showDebug = true;

    private int lastCanvasW = -1;
    private int lastCanvasH = -1;
    private Rectangle restartButton = new Rectangle();

    public Engine(GameCanvas canvas, Runnable toggleFullscreen) {
        this.canvas = canvas;
        this.toggleFullscreen = toggleFullscreen;
        this.loop = new GameLoop(canvas, TARGET_UPS, this);

        canvas.addKeyListener(input);
        canvas.addMouseListener(input);
        canvas.addMouseMotionListener(input);
        canvas.setFocusable(true);
        canvas.requestFocus();

        input.onKeyPressed = code -> {
            if (code == KeyEvent.VK_F3) {
                showDebug = !showDebug;
            }
            if (code == KeyEvent.VK_F11 && toggleFullscreen != null) {
                toggleFullscreen.run();
            }
        };

        player.pos.set(worldBounds.w / 2.0, worldBounds.h / 2.0);
        spawner.onWaveStart(waves.getWave());
    }

    public void start() {
        loop.start();
    }

    public void stop() {
        loop.stop();
    }

    @Override
    public void onUpdate(double dt) {
        ensureCanvasSize();
        input.poll();

        Point2D.Double mouseWorld = arenaViewport.toWorld(input.mouseX, input.mouseY);
        input.setMouseWorld(mouseWorld.x, mouseWorld.y);

        if (state == GameState.GAME_OVER) {
            if (input.wasPressed(KeyEvent.VK_R) ||
                    (input.mousePressedL && restartButton.contains(input.mouseX, input.mouseY))) {
                restartGame();
            }
            return;
        }

        if (state == GameState.SHOP) {
            shop.updatePointer(input.mouseX, input.mouseY);
            boolean startNext = shop.handleInput(input, player);
            if (startNext) {
                waves.nextWave();
                spawner.onWaveStart(waves.getWave());
                state = GameState.RUNNING;
            }
            return;
        }

        updateRunning(dt);
    }

    private void updateRunning(double dt) {
        double ax = 0;
        double ay = 0;
        if (input.isDown(KeyEvent.VK_W)) ay -= 1;
        if (input.isDown(KeyEvent.VK_S)) ay += 1;
        if (input.isDown(KeyEvent.VK_A)) ax -= 1;
        if (input.isDown(KeyEvent.VK_D)) ax += 1;
        Vec2 move = new Vec2(ax, ay).normalized();

        player.update(dt, move, input.mouseWorldX, input.mouseWorldY, bullets, particles);
        arenaViewport.clampWorld(player.pos, player.radius);

        waves.update(dt);
        spawner.update(dt, enemies, player);

        for (Enemy enemy : enemies) {
            enemy.update(dt, player, bullets);
            arenaViewport.clampWorld(enemy.pos, enemy.radius);
        }

        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet b = bullets.get(i);
            b.update(dt, enemies);
            if (b.dead) bullets.remove(i);
        }
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.update(dt);
            if (p.dead) particles.remove(i);
        }

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
        }

        if (input.wasPressed(KeyEvent.VK_K)) player.damage(8);
        if (input.wasPressed(KeyEvent.VK_L)) player.heal(8);
    }

    @Override
    public void onRender(Graphics2D g) {
        ensureCanvasSize();

        g.setColor(Colors.BACKDROP);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        Draw.applyQualityHints(g);

        arenaViewport.renderBackground(g);
        AffineTransform old = g.getTransform();
        arenaViewport.applyWorldTransform(g);
        drawWorld(g);
        g.setTransform(old);

        Rectangle view = arenaViewport.getViewportRect();
        if (state == GameState.RUNNING) {
            String title = String.format("Wave %d — %02d:%02d", waves.getWave(),
                    (int) (waves.getTimeLeft() / 60), (int) (waves.getTimeLeft() % 60));
            HUD.drawTopBanner(g, view, title);
        } else if (state == GameState.SHOP) {
            HUD.drawTopBanner(g, view, "Shop – Wave " + waves.getWave() + " beendet");
        }

        HUD.drawBars(g, player, view);
        if (showDebug) {
            HUD.drawDebug(g, view, loop.getFps(), loop.getUps(), bullets.size(), particles.size(), player);
        }

        if (state == GameState.GAME_OVER) {
            HUD.drawGameOverOverlay(g, canvas.getWidth(), canvas.getHeight(), restartButton);
        } else if (state == GameState.SHOP) {
            shop.render(g, player, waves.getWave());
        } else {
            HUD.drawCrosshair(g, input.mouseCanvasX, input.mouseCanvasY);
        }
    }

    private void drawWorld(Graphics2D g) {
        float invScale = (float) (1.0 / Math.max(0.0001, arenaViewport.getScale()));
        g.setStroke(new BasicStroke(invScale));
        g.setColor(Colors.FLOOR_GRID);
        for (int x = 0; x <= EngineArena.ARENA_W; x += 32) {
            g.drawLine(x, 0, x, EngineArena.ARENA_H);
        }
        for (int y = 0; y <= EngineArena.ARENA_H; y += 32) {
            g.drawLine(0, y, EngineArena.ARENA_W, y);
        }

        for (Enemy enemy : enemies) enemy.render(g);
        for (Particle particle : particles) particle.render(g);
        for (Bullet bullet : bullets) bullet.render(g);
        player.render(g);
    }

    private void ensureCanvasSize() {
        int w = Math.max(1, canvas.getWidth());
        int h = Math.max(1, canvas.getHeight());
        if (w == lastCanvasW && h == lastCanvasH) {
            return;
        }
        lastCanvasW = w;
        lastCanvasH = h;
        arenaViewport.resizeToCanvas(w, h);
        shop.setCanvasSize(w, h);
        Rectangle viewport = arenaViewport.getViewportRect();
        int bw = 240;
        int bh = 52;
        int bx = viewport.x + (viewport.width - bw) / 2;
        int by = viewport.y + viewport.height / 2 + 60;
        restartButton = new Rectangle(bx, by, bw, bh);
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
        player.pos.set(worldBounds.w / 2.0, worldBounds.h / 2.0);

        spawner.onWaveStart(waves.getWave());
        state = GameState.RUNNING;
    }
}
