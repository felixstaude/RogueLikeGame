package de.felixstaude.roguelike.app;

import de.felixstaude.roguelike.core.Engine;
import de.felixstaude.roguelike.core.EngineArena;
import de.felixstaude.roguelike.core.GameCanvas;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(App::launch);
    }

    private static void launch() {
        JFrame frame = new JFrame("Roguelike");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setIgnoreRepaint(true);

        GameCanvas canvas = new GameCanvas();
        canvas.setBackground(Color.BLACK);
        canvas.setPreferredSize(new Dimension(EngineArena.ARENA_W, EngineArena.ARENA_H));
        frame.add(canvas);
        frame.pack();
        frame.setLocationRelativeTo(null);

        FullscreenController fullscreen = new FullscreenController(frame, canvas);
        Engine engine = new Engine(canvas, fullscreen::toggle);

        frame.setVisible(true);
        fullscreen.enterFullscreen();
        canvas.requestFocus();

        engine.start();
    }

    private static final class FullscreenController {
        private final JFrame frame;
        private final GameCanvas canvas;
        private final GraphicsDevice device;
        private boolean fullscreen = false;

        private FullscreenController(JFrame frame, GameCanvas canvas) {
            this.frame = frame;
            this.canvas = canvas;
            this.device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        }

        private void enterFullscreen() {
            if (fullscreen) {
                return;
            }
            frame.dispose();
            frame.setUndecorated(true);
            frame.setResizable(false);
            frame.setVisible(true);
            device.setFullScreenWindow(frame);
            fullscreen = true;
            canvas.requestFocus();
        }

        private void exitFullscreen() {
            if (!fullscreen) {
                return;
            }
            device.setFullScreenWindow(null);
            frame.dispose();
            frame.setUndecorated(false);
            frame.setResizable(true);
            frame.setVisible(true);
            frame.pack();
            frame.setLocationRelativeTo(null);
            fullscreen = false;
            canvas.requestFocus();
        }

        private void toggle() {
            if (fullscreen) {
                exitFullscreen();
            } else {
                enterFullscreen();
            }
        }
    }
}
