package killBot.gun;

import robocode.Rules;

public class BulletPowerSelector {

    public static double getBestPower(double myEnergy, double enemyEnergy, double distance, HitRateTracker hitTracker)
    {
        double finalPower;
        double currentHitRate = hitTracker.getHitRate();
        int hits = hitTracker.getHits();
        
        double killShot = enemyEnergy / 4.0;
        if(myEnergy > killShot + 1 && killShot > 0.1 && killShot <= Rules.MAX_BULLET_POWER) return killShot;

        if(myEnergy < 15) return 0.1;

        if(myEnergy > enemyEnergy + 40)
        {
            if(currentHitRate < 0.20 && hits > 7)
            {
                finalPower = currentHitRate * 10.0;
            }
            else
            {
                finalPower = Rules.MAX_BULLET_POWER;
            }
        }

        else if(myEnergy < enemyEnergy)
        {
            if(currentHitRate > 0.35 && hits > 7)
            {
                finalPower = 1.99;
            }
            else
            {
                double confidenceFactor = myEnergy / enemyEnergy;
                double powerByDistance = 700 / distance;
                finalPower = powerByDistance * confidenceFactor;

                if(distance < 150)
                {
                    finalPower = Math.max(finalPower, 1.5);
                }
            }
        }

        else
        {
            finalPower = 700 / distance;
        }

        return Math.max(0.1, Math.min(finalPower, myEnergy - 0.1));
    }
}
