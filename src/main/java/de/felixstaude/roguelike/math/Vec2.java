package de.felixstaude.roguelike.math;

public class Vec2 {
    public double x,y;
    public Vec2(){this(0,0);}
    public Vec2(double x,double y){this.x=x;this.y=y;}
    public void set(double x,double y){this.x=x;this.y=y;}
    public Vec2 add(Vec2 o){ return new Vec2(x+o.x, y+o.y); }
    public Vec2 sub(Vec2 o){ return new Vec2(x-o.x, y-o.y); }
    public Vec2 mul(double s){ return new Vec2(x*s, y*s); }
    public double len(){ return Math.sqrt(x*x + y*y); }
    public double dot(Vec2 o){ return x*o.x + y*o.y; }
    public double angle(){ return Math.atan2(y, x); }
    public Vec2 normalized(){ double l=len(); return l>1e-9? new Vec2(x/l, y/l): new Vec2(0,0); }

    public static Vec2 rotate(Vec2 v, double rad){
        double c=Math.cos(rad), s=Math.sin(rad);
        return new Vec2(v.x*c - v.y*s, v.x*s + v.y*c);
    }
    public static Vec2 fromAngle(double ang){ return new Vec2(Math.cos(ang), Math.sin(ang)); }
    public static Vec2 lerp(Vec2 a, Vec2 b, double t){ return new Vec2(a.x+(b.x-a.x)*t, a.y+(b.y-a.y)*t); }
}
