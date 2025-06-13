package killBot.gun;

import killBot.data.*;
import robocode.AdvancedRobot;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

public class WaveManager {
    
    private List<Wave> waves = new ArrayList<>();
    private AdvancedRobot robot;
    private GameData gameData;

    public WaveManager(AdvancedRobot robot, GameData gameData)
    {
        this.robot = robot;
        this.gameData = gameData;
    }

    public void addWave(double power)
    {
        Point2D.Double myLocation = gameData.myState.location;
        Point2D.Double enemyLocation = gameData.enemyState.location;

        double absoluteBearing = Math.atan2(
            enemyLocation.x - myLocation.x,
            enemyLocation.y - myLocation.y
        );

        //double enemyHeading = gameData.enemyState.heading;
        double enemyDirection = Math.signum(gameData.enemyState.velocity);
        
        Wave wave = new Wave(myLocation, power, robot.getTime(), (int) enemyDirection, absoluteBearing);
        waves.add(wave);
    }

    public void updateWaves()
    {
        Iterator<Wave> iterator = waves.iterator();
        while(iterator.hasNext())
        {
            Wave wave = iterator.next();

            if(wave.getRadius(robot.getTime()) > wave.origin.distance(gameData.enemyState.location) + 50)
            {
                iterator.remove();
                continue;
            }

            if(wave.getRadius(robot.getTime()) > 1000)
            {
                iterator.remove();
            }
        }
    }

    public Wave findCorrectWave(Point2D.Double impactLocation, double bulletPower, long currentTime)
    {
        Wave bestMatch = null;
        double minDeviation = Double.POSITIVE_INFINITY;

        for(Wave wave : waves)
        {
            if(Math.abs(wave.bulletPower - bulletPower) < 0.001)
            {
                double travelTime = (currentTime - wave.bulletTime);
                double waveRadius = travelTime * wave.bulletSpeed;
                double deviation = Math.abs(waveRadius - wave.origin.distance(impactLocation));

                if(deviation < minDeviation)
                {
                    minDeviation = deviation;
                    bestMatch = wave;
                }
            }
        }

        return bestMatch;
    }
}
