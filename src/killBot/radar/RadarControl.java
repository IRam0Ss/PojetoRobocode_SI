package killBot.radar;

import robocode.*;
import robocode.util.Utils;
import java.awt.geom.Point2D;

public class RadarControl {

    private AdvancedRobot robot; //robo
    public ScannedRobotEvent lastScan = null; //guarda o ultimo robo que viu

    private static final int shortSweeps = 3; //quantas varreduras curtas antes de entrar no modo busca novamente
    private double radarDirection = 1; //1 para direita, -1 para esquerda
    private int radarSweep = 0; //Varredura completa

    
    public RadarControl(AdvancedRobot robot)
    {
        this.robot = robot;
    }

    //calcula angulos absolutos
    private double absoluteBearing(Point2D.Double p1, Point2D.Double p2)
    {
        return Math.toDegrees(Math.atan2(p2.x - p1.x, p2.y - p1.y));
    }

    //calcula o angulo absuluto para o ultimo scan inimigo
    private double absoluteBearingLastScan()
    {
        return robot.getHeading() - lastScan.getBearing();
    }

    //Scaneia diretamente o centro da arena para achar o robo mais facilmente
    public void initialScan()
    {
        Point2D.Double center = new Point2D.Double(robot.getBattleFieldWidth() /2, robot.getBattleFieldHeight() / 2);
        double angleCenter = absoluteBearing(new Point2D.Double(robot.getX(), robot.getY()), center);
        double aim = Utils.normalRelativeAngleDegrees(angleCenter - robot.getRadarHeading());
        robot.setTurnRadarRight(aim);

    }

    //define a logica principal do radar
    public void radarLogic()
    {
        if(lastScan == null || robot.getTime() - lastScan.getTime() > 2)
        {
            search();
        }
        else
        {
            radarLock();
        }
    }

    //trava o radar assim que avista um inimigo
    public void radarLock()
    {

        double absoluteBearing = robot.getHeading() + lastScan.getBearing();
        double aim = Utils.normalRelativeAngleDegrees(absoluteBearing - robot.getRadarHeading());
        robot.setTurnRadarRight(aim * 2.0);
    }

    //metodo de busca do radar
    public void search()
    {
        //se ja fizemos buscas curtas demais, entao faca uma busca longa
        if(lastScan == null || radarSweep >= shortSweeps)
        {
            robot.setTurnRadarRight(radarDirection * Double.POSITIVE_INFINITY);
            return;
        }

        //ultima posicao conhecida pelo inimigo
        double lastPos = Utils.normalRelativeAngleDegrees(absoluteBearingLastScan() - robot.getRadarHeading());

        //se a direcao do movimento inimigo mudou, entao voltamos a trackear ele
        if(Math.signum(lastPos) != radarDirection && lastPos != 0)
        {  
            radarDirection = Math.signum(lastPos);
            radarSweep++;
        }

        //tenta voltar a varredura para o inimigo
        robot.setTurnRadarRight(radarDirection * Double.POSITIVE_INFINITY);
    }

    public void onScannedRobot(ScannedRobotEvent e)
    {
        //guarda o scan e zera a contagem de varreduras completas pois ja achamos o inimigo
        this.lastScan = e;
        radarSweep = 0;
    }
}