package killBot.gun;

import killBot.data.BotState;
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

    public static final int DISTANCE_BINS = 7;
    public static final int VELOCITY_BINS = 5;
    public static final int ACCEL_BINS = 3;
    public static final int GF_BINS = 47;
    public static final int zeroIndex = 23;
    public static double[][][][] BINS;

    static
    {
        BINS = new double[DISTANCE_BINS][VELOCITY_BINS][ACCEL_BINS][GF_BINS];
    }

    public Aimer(AdvancedRobot robot, WaveManager waveManager, GameData gameData, HitRateTracker hitTracker) 
        {
            this.robot = robot;
            this.waveManager = waveManager;
            this.gameData = gameData;
            this.hitTracker = hitTracker;
        }
    
    private int getDistanceIndex() {
        double distance = gameData.myState.location.distance(gameData.enemyState.location);
        // 0-120, 120-240, 240-360, 360-480, 480-600, 600-720, >720
        int index = (int)(distance / 120);
        if (index > 6) {
            index = 6;
        }
        return index;
    }

    private int getVelocityIndex() {
        double velocity = Math.abs(gameData.enemyState.velocity); //
        // 0, 1-2, 3-4, 5-6, 7-8
        if (velocity < 1) return 0;
        if (velocity < 3) return 1;
        if (velocity < 5) return 2;
        if (velocity < 7) return 3;
        return 4;
    }

    private int getAccelerationIndex() {
        // Precisamos de pelo menos dois registros no histórico para calcular a aceleração
        if (gameData.enemyHistory.size() > 1) { //
            // Pega o estado atual e o anterior
            BotState currentState = gameData.enemyState; //
            BotState previousState = gameData.enemyHistory.get(1); // O estado de 1 tick atrás

            double currentVelocity = Math.abs(currentState.velocity);
            double previousVelocity = Math.abs(previousState.velocity);
            
            // Compara a mudança na velocidade
            // Usamos uma pequena tolerância (0.1) para considerar a velocidade "constante"
            if (currentVelocity > previousVelocity + 0.1) {
                return 2; // Acelerando
            } else if (currentVelocity < previousVelocity - 0.1) {
                return 0; // Desacelerando
            } else {
                return 1; // Velocidade Constante
            }
        }
        
        // Caso não tenhamos histórico suficiente, retornamos "Constante" como padrão
        return 1;
}

    public void shoot()
    {
        if(gameData.enemyState == null) return;

        double bestGF = findBestGF();
        double firePower = BulletPowerSelector.getBestPower(
            gameData.myState.energy,
            gameData.enemyState.energy,
            gameData.myState.location.distance(gameData.enemyState.location),
            hitTracker);
        double bulletSpeed = 20 - (3 * firePower);
        double escapeAngle = FasterCalcs.asin(8.0 / bulletSpeed);

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
        int accelIndex = getAccelerationIndex();
        int bestIndex = zeroIndex;

        for(int i = 0; i < GF_BINS; i++)
        {
            if(BINS[distanceIndex][velocityIndex][accelIndex][i] > BINS[distanceIndex][velocityIndex][accelIndex][bestIndex])
            {
                bestIndex = i;
            }
        }

        double bestGF = (double)(bestIndex - zeroIndex) / (double) zeroIndex;

        //Debug
        // ... (O System.out.println de debug pode ser atualizado para incluir o accelIndex)
        System.out.println(
            "MIRA INFO [T:" + robot.getTime() + "]" +
            " | D.Bin: " + distanceIndex +
            " | V.Bin: " + velocityIndex +
            " | A.Bin: " + accelIndex + // Novo debug
            " | Best GF: " + String.format("%.2f", bestGF)
        );

        return bestGF;
    }

public void logSuccess(double GF) {
    int distanceIndex = getDistanceIndex();
    int velocityIndex = getVelocityIndex();
    int accelIndex = getAccelerationIndex(); 
    
    int index = (int) Math.round((GF * zeroIndex) + zeroIndex); 
    
    BINS[distanceIndex][velocityIndex][accelIndex][index] += 1.0;
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

    public void decayBINS() {
    for (int i = 0; i < DISTANCE_BINS; i++) {
        for (int j = 0; j < VELOCITY_BINS; j++) {
            for (int k = 0; k < ACCEL_BINS; k++) { // Novo laço para aceleração
                for (int l = 0; l < GF_BINS; l++) {
                    BINS[i][j][k][l] *= 0.999;
                }
            }
        }
    }
}
}
