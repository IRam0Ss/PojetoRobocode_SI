package killBot.gun;

import killBot.data.GameData;
import robocode.AdvancedRobot;
import robocode.util.Utils;
import java.awt.geom.Point2D;
import killBot.data.Wave;
import killBot.utils.math.FasterCalcs;

public class Aimer {
    private AdvancedRobot robot;
    private WaveManager waveManager;
    private GameData gameData;
    private HitRateTracker hitTracker;

    public static final int DISTANCE_BINS = 4;
    public static final int VELOCITY_BINS = 3;
    public static final int GF_BINS = 47;
    public static final int zeroIndex = 23;
    public static double[][][] BINS;

    static
    {
        BINS = new double[DISTANCE_BINS][VELOCITY_BINS][GF_BINS];
    }

    public Aimer(AdvancedRobot robot, WaveManager waveManager, GameData gameData, HitRateTracker hitTracker) 
        {
            this.robot = robot;
            this.waveManager = waveManager;
            this.gameData = gameData;
            this.hitTracker = hitTracker;
        }
    
    private int getDistanceIndex()
    {
        double distance = gameData.myState.location.distance(gameData.enemyState.location);
        if(distance < 200) return 0;
        if(distance < 400) return 1;
        if(distance < 600) return 2;
        return 3;
    }

    private int getVelocityIndex()
    {
        double velocity = Math.abs(gameData.enemyState.velocity);
        if(velocity < 2) return 0;
        if(velocity < 6) return 1;
        return 2;
    }

    public void shoot()
    {
        if(gameData.enemyState == null) return;

        double bestGF = findBestGF();

        double basePower = 1.5;
        double baseSpeed = 20 - (3 * basePower);
        double escapeAngle = FasterCalcs.asin(8.0 / baseSpeed);

        double absoluteBearing = FasterCalcs.atan2(
            gameData.enemyState.location.x - gameData.myState.location.x,
            gameData.enemyState.location.y - gameData.myState.location.y
        );

        int enemyDirection = (int) Math.signum(gameData.enemyState.velocity);
        if(enemyDirection == 0) enemyDirection = 1;

        double finalAngle = absoluteBearing + (bestGF * escapeAngle * enemyDirection);
        double pointGun = Utils.normalRelativeAngle(finalAngle - robot.getGunHeadingRadians());

        robot.setTurnGunRightRadians(pointGun);

        if(robot.getGunHeat() == 0 && Math.abs(pointGun) < 0.02)
        {
            double firePower = BulletPowerSelector.getBestPower(
                gameData.myState.energy,
                gameData.enemyState.energy,
                gameData.myState.location.distance(gameData.enemyState.location),
                hitTracker);

            if(robot.setFireBullet(firePower) != null)
            {
                waveManager.addWave(firePower);
                hitTracker.logShotFired();
            }
        }
    }

    private double findBestGF()
    {
        int distanceIndex = getDistanceIndex();
        int velocityIndex = getVelocityIndex();
        int bestIndex = zeroIndex;

        for(int i = 0; i < GF_BINS; i++)
        {
            if(BINS[distanceIndex][velocityIndex][i] > BINS[distanceIndex][velocityIndex][bestIndex])
            {
                bestIndex = i;
            }
        }

        double bestGF = (double)(bestIndex - zeroIndex) / (double) zeroIndex;

        //Debug
        System.out.println(
            "MIRA INFO [T:" + robot.getTime() + "]" +
            " | Dist.Bin: " + distanceIndex + "| Dist.Enemy" + gameData.enemyState.location.distance(gameData.myState.location) +
            " | Vel.Bin: " + velocityIndex + "| Vel.Enemy" + gameData.enemyState.velocity +
            " | Best GF: " + String.format("%.2f", bestGF) +
            " (Score: " + String.format("%.3f", BINS[distanceIndex][velocityIndex][bestIndex]) + ")"
        );

        return bestGF;
    }

    public void logSuccess(double GF)
    {
        int distanceIndex = getDistanceIndex();
        int velocityIndex = getVelocityIndex();
        int index = (int) Math.round((GF + 1.0) * zeroIndex);
        BINS[distanceIndex][velocityIndex][index] += 1.0;
    }

    public void onBulletHit(Point2D.Double impactLocation, double bulletPower)
    {
        Wave hitWave = waveManager.findCorrectWave(impactLocation, bulletPower, robot.getTime());
        if(hitWave == null)
        {
            return;
        }

        double offsetAngle = Utils.normalRelativeAngle(
            FasterCalcs.atan2(impactLocation.x - hitWave.origin.x, impactLocation.y - hitWave.origin.y) - hitWave.absoluteBearing
        );

        double escapeAngle = FasterCalcs.asin(8.0 / hitWave.bulletSpeed);
        double guessFactor = (offsetAngle / escapeAngle * hitWave.direciton);

        guessFactor = Math.max(-1.0, Math.min(1.0, guessFactor));

        logSuccess(guessFactor);
        hitTracker.logShotHit();

        System.out.println("ACERTO! GF = " + String.format("%.2f", guessFactor));
    }

    public void decayBINS()
    {
        for(int i = 0; i < BINS.length; i++)
        {
            for(int j = 0; j < BINS[i].length; j++)
            {
                for(int k = 0; k < BINS[i][j].length; k++)
                {
                    BINS[i][j][k] *= 0.999;
                }
            }
        }
    }
}
