package killBot.data;

import robocode.*;
import java.util.List;
import java.util.ArrayList;
import java.awt.geom.Point2D;

public class GameData {
    private AdvancedRobot robot;
    public BotState myState;
    public BotState enemyState;
    
    public List<BotState> myHistory;
    public List<BotState> enemyHistory;

    public static final int HISTORY_SIZE = 100;

    public GameData(AdvancedRobot robot)
    {
        this.robot = robot;
        myHistory = new ArrayList<>();
        enemyHistory = new ArrayList<>();
    }

    public void update(ScannedRobotEvent e)
    {
        Point2D.Double myLocation = new Point2D.Double(robot.getX(), robot.getY());
        myState = new BotState(
            myLocation,
            robot.getHeadingRadians(),
            robot.getVelocity(),
            robot.getEnergy(),
            robot.getTime()
        );

        addHistory(myHistory, myState);

        double absoluteBearing = robot.getHeadingRadians() + Math.toRadians(e.getBearing());
        double distance = e.getDistance();
        Point2D.Double enemyLocation = project(myLocation, absoluteBearing, distance);

        enemyState = new BotState(
            enemyLocation,
            e.getHeadingRadians(),
            e.getVelocity(),
            e.getEnergy(),
            e.getTime()
        );

        addHistory(enemyHistory, enemyState);
    }

    private void addHistory(List<BotState> history, BotState state)
    {
        history.add(0, state);
        if(history.size() > HISTORY_SIZE)
        {
            history.remove(history.size() - 1);
        }
    }

    public static Point2D.Double project(Point2D.Double sourceLocation, double angle, double length)
    {
        return new Point2D.Double(
            sourceLocation.x + Math.sin(angle) * length,
            sourceLocation.y + Math.cos(angle) * length
        );
    }

}
