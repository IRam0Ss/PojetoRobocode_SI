package killBot.gun;

public class HitRateTracker {
    
    private int shotsFired = 0;
    private int hits = 0;

    public void logShotFired()
    {
        shotsFired++;
    }

    public void logShotHit()
    {
        hits++;
    }

    public double getHitRate()
    {
        if(shotsFired == 0)
        {
            return 0.0;
        }
        else
        {
            return (double) hits / shotsFired;
        }
    }

    public int getShots()
    {
        return shotsFired;
    }

    public int getHits()
    {
        return hits;
    }

    public void reset()
    {
        shotsFired = 0;
        hits = 0;
    }
}
