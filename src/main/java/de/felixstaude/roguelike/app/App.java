package de.felixstaude.roguelike.app;

import de.felixstaude.roguelike.core.Engine;
import de.felixstaude.roguelike.core.GameCanvas;

import javax.swing.*;
import java.awt.*;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Roguelike Engine – Maven");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setIgnoreRepaint(true);
            frame.setResizable(false);

            GameCanvas canvas = new GameCanvas();
            canvas.setPreferredSize(new Dimension(1280, 720));
            frame.add(canvas);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            Engine engine = new Engine(canvas);
            engine.start();
        });
    }
}
