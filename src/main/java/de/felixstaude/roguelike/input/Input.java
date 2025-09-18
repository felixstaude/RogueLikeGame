package de.felixstaude.roguelike.input;

import java.awt.event.*;

public class Input implements KeyListener, MouseListener, MouseMotionListener {
    private final boolean[] keys = new boolean[256];
    private final boolean[] pressed = new boolean[256];
    private final boolean[] released = new boolean[256];

    public int mouseX=0, mouseY=0;
    public boolean mousePressedL=false, mousePressedR=false;

    public java.util.function.IntConsumer onKeyPressed = null;

    public void poll(){ for (int i=0;i<pressed.length;i++){ pressed[i]=false; released[i]=false; } }
    public boolean isDown(int code){ return code>=0 && code<keys.length && keys[code]; }
    public boolean wasPressed(int code){ return code>=0 && code<pressed.length && pressed[code]; }
    public boolean wasReleased(int code){ return code>=0 && code<released.length && released[code]; }

    @Override public void keyTyped(KeyEvent e) { }
    @Override public void keyPressed(KeyEvent e){
        int c=e.getKeyCode();
        if(c>=0&&c<keys.length){ if(!keys[c]) pressed[c]=true; keys[c]=true; }
        if(onKeyPressed!=null) onKeyPressed.accept(c);
    }
    @Override public void keyReleased(KeyEvent e){
        int c=e.getKeyCode();
        if(c>=0&&c<keys.length){ keys[c]=false; released[c]=true; }
    }

    @Override public void mouseClicked(MouseEvent e) { }
    @Override public void mousePressed(MouseEvent e){
        if(e.getButton()==MouseEvent.BUTTON1) mousePressedL=true;
        if(e.getButton()==MouseEvent.BUTTON3) mousePressedR=true;
    }
    @Override public void mouseReleased(MouseEvent e){
        if(e.getButton()==MouseEvent.BUTTON1) mousePressedL=false;
        if(e.getButton()==MouseEvent.BUTTON3) mousePressedR=false;
    }
    @Override public void mouseEntered(MouseEvent e) { }
    @Override public void mouseExited(MouseEvent e) { }
    @Override public void mouseDragged(MouseEvent e){ mouseMoved(e); }
    @Override public void mouseMoved(MouseEvent e){ mouseX=e.getX(); mouseY=e.getY(); }
}
