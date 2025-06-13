package killBot.data;

import java.awt.geom.Point2D;

public class BotState {
    public final Point2D.Double location;
    public final double heading;
    public final double velocity;
    public final double energy;
    public final long time;

    public BotState(Point2D.Double location, double heading, double velocity, double energy, long time)
    {
        this.location = location;
        this.heading = heading;
        this.velocity = velocity;
        this.energy = energy;
        this.time = time;
    }
}
