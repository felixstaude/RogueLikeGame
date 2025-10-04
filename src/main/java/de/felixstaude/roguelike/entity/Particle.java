package de.felixstaude.roguelike.entity;

import de.felixstaude.roguelike.math.Vec2;

import java.awt.*;

public class Particle {
    public final Vec2 pos = new Vec2();
    public Vec2 vel = new Vec2();
    public double life = 0.2;
    public double maxLife = 0.2;
    public boolean dead=false;
    public float size=4f;
    public Color color = new Color(120,200,255);

    public static Particle muzzle(double x,double y, Vec2 aim){
        Particle p=new Particle();
        p.pos.set(x,y);
        double px=-aim.y, py=aim.x;
        double spread=(Math.random()-0.5)*80.0;
        p.vel = aim.mul(80).add(new Vec2(px*spread, py*spread).mul(0.01));
        p.life=0.16+Math.random()*0.08;
        p.maxLife=p.life;
        p.size= (float)(3+Math.random()*2);
        p.color = new Color(120,200,255);
        return p;
    }

    public static Particle hit(double x,double y){
        Particle p=new Particle();
        p.pos.set(x,y);
        double ang = Math.random()*Math.PI*2;
        double spd = 100 + Math.random()*180;
        p.vel = new Vec2(Math.cos(ang)*spd, Math.sin(ang)*spd);
        p.life = 0.18 + Math.random()*0.10;
        p.maxLife = p.life;
        p.size = (float)(2 + Math.random()*2);
        p.color = new Color(255, 240, 120);
        return p;
    }

    public static Particle burst(double x,double y, Color c){
        Particle p=new Particle();
        p.pos.set(x,y);
        double ang = Math.random()*Math.PI*2;
        double spd = 120 + Math.random()*240;
        p.vel = new Vec2(Math.cos(ang)*spd, Math.sin(ang)*spd);
        p.life = 0.35 + Math.random()*0.25;
        p.maxLife = p.life;
        p.size = (float)(3 + Math.random()*3);
        p.color = c;
        return p;
    }

    public void update(double dt){
        pos.x+=vel.x*dt; pos.y+=vel.y*dt;
        life-=dt;
        if(life<=0) dead=true;
    }
    public void render(Graphics2D g){
        double t = Math.max(0, life / Math.max(0.0001, maxLife)); // 1..0
        int alpha=(int)(t * 255);
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
        int s=(int)size;
        g.fillRect((int)(pos.x-s/2),(int)(pos.y-s/2), s, s);
    }
}
