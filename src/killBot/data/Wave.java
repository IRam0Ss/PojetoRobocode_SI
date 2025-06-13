package killBot.data;

import java.awt.geom.Point2D;

public class Wave {
    
    public final Point2D.Double origin;
    public final double bulletSpeed;
    public final double bulletPower;
    public final long bulletTime;
    public final int direciton;
    public final double absoluteBearing;

    public Wave(Point2D.Double origin, double bulletPower, long bulletTime, int direciton, double absoluteBearing)
    {
        this.origin = origin;
        this.bulletSpeed = 20 - (3 * bulletPower);
        this.bulletPower = bulletPower;
        this.bulletTime = bulletTime;
        this.direciton = direciton;
        this.absoluteBearing = absoluteBearing;
    }

    public double getRadius(long currentTime)
    {
        return bulletSpeed * (currentTime - bulletTime);
    }
}
