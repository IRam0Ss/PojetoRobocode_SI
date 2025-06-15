package killBot.gun;

import killBot.data.BotState;
import killBot.data.GameData;
import robocode.AdvancedRobot;
import robocode.util.Utils;
import java.awt.geom.Point2D;
import killBot.data.Wave;
import killBot.utils.math.FasterCalcs;

/**
 * Gerencia toda a lógica de mira (targeting) do robô.
 * Esta classe utiliza uma mira híbrida:
 * 1. Mira Preditiva Linear: Para alvos que se movem de forma previsível em linha reta.
 * 2. Mira Estatística (GuessFactor): Para todos os outros cenários, adaptando-se
 * ao padrão de desvio do inimigo.
 */
public class Aimer {
    /** Referência ao robô principal para executar ações. */
    private AdvancedRobot robot;
    /** Gerenciador de ondas de tiro para identificar qual tiro acertou. */
    private WaveManager waveManager;
    /** Objeto que contém todos os dados de estado do jogo (nosso e do inimigo). */
    private GameData gameData;
    /** Rastreador de taxa de acertos para a seleção de poder do tiro. */
    private HitRateTracker hitTracker;

    // Constantes para as dimensões do array de estatísticas (BINS)
    public static final int DISTANCE_BINS = 7;
    public static final int VELOCITY_BINS = 5;
    public static final int ACCEL_BINS = 3;
    /** Número total de "fatias" para o GuessFactor. Deve ser um número ímpar. */
    public static final int GF_BINS = 47;
    /** O índice do meio do array GF_BINS, correspondendo a um GuessFactor de 0. */
    public static final int zeroIndex = 23;

    /** * O array quadridimensional que armazena as estatísticas de acerto.
     * Estrutura: [distância][velocidade][aceleração][guessFactor]
     */
    public static double[][][][] BINS;

    /**
     * Bloco inicializador estático para alocar memória para o array de estatísticas.
     */
    static
    {
        BINS = new double[DISTANCE_BINS][VELOCITY_BINS][ACCEL_BINS][GF_BINS];
    }

    /**
     * Construtor da classe Aimer.
     * @param robot A instância do robô principal.
     * @param waveManager O gerenciador de ondas de tiro.
     * @param gameData O objeto de dados do jogo.
     * @param hitTracker O rastreador de taxa de acertos.
     */
    public Aimer(AdvancedRobot robot, WaveManager waveManager, GameData gameData, HitRateTracker hitTracker) 
        {
            this.robot = robot;
            this.waveManager = waveManager;
            this.gameData = gameData;
            this.hitTracker = hitTracker;
        }
    
    /**
     * Calcula o índice de 'distância' para o array BINS com base na distância atual do inimigo.
     * @return O índice de distância (0-6).
     */
    private int getDistanceIndex() {
        double distance = gameData.myState.location.distance(gameData.enemyState.location);
        // Fatias: 0-120, 120-240, 240-360, 360-480, 480-600, 600-720, >720
        int index = (int)(distance / 120);
        if (index > 6) {
            index = 6;
        }
        return index;
    }

    /**
     * Calcula o índice de 'velocidade' para o array BINS com base na velocidade atual do inimigo.
     * @return O índice de velocidade (0-4).
     */
    private int getVelocityIndex() {
        double velocity = Math.abs(gameData.enemyState.velocity);
        // Fatias: 0, 1-2, 3-4, 5-6, 7-8
        if (velocity < 1) return 0;
        if (velocity < 3) return 1;
        if (velocity < 5) return 2;
        if (velocity < 7) return 3;
        return 4;
    }

    /**
     * Calcula o índice de 'aceleração' para o array BINS.
     * Aceleração é classificada como desacelerando (0), constante (1) ou acelerando (2).
     * @return O índice de aceleração (0, 1 ou 2).
     */
    private int getAccelerationIndex() {
        if (gameData.enemyHistory.size() > 1) {
            BotState currentState = gameData.enemyState;
            BotState previousState = gameData.enemyHistory.get(1); // O estado de 1 tick atrás

            double currentVelocity = Math.abs(currentState.velocity);
            double previousVelocity = Math.abs(previousState.velocity);
            
            // Compara a mudança na velocidade com uma pequena tolerância.
            if (currentVelocity > previousVelocity + 0.1) {
                return 2; // Acelerando
            } else if (currentVelocity < previousVelocity - 0.1) {
                return 0; // Desacelerando
            } else {
                return 1; // Velocidade Constante
            }
        }
        return 1; // Padrão se não houver histórico suficiente.
    }

    /**
     * O método principal de mira, chamado a cada turno.
     * Decide entre a mira preditiva linear e a mira estatística (GuessFactor).
     */
    public void shoot()
    {
        if(gameData.enemyState == null) return;

        double firePower = BulletPowerSelector.getBestPower(
            gameData.myState.energy,
            gameData.enemyState.energy,
            gameData.myState.location.distance(gameData.enemyState.location),
            hitTracker);

        // Heurística para ativar a mira linear: inimigo rápido e com aceleração constante.
        boolean isMovingLinearly = Math.abs(gameData.enemyState.velocity) > 4 && getAccelerationIndex() == 1;
        if (isMovingLinearly) {
            // --- INÍCIO DA MIRA PREDITIVA LINEAR ---
            double bulletSpeed = 20 - (3 * firePower);

            Point2D.Double myPos = gameData.myState.location;
            Point2D.Double enemyPos = gameData.enemyState.location;
            double enemyHeading = gameData.enemyState.heading;
            double enemyVelocity = gameData.enemyState.velocity;
            double distance = myPos.distance(enemyPos);
            
            double futureTime = 0;
            Point2D.Double predictedPos = (Point2D.Double) enemyPos.clone();

            // Itera no tempo para encontrar o ponto de interceptação futuro.
            while ((++futureTime) * bulletSpeed < myPos.distance(predictedPos)) {
                predictedPos.x = enemyPos.x + enemyVelocity * FasterCalcs.sin(enemyHeading) * futureTime;
                predictedPos.y = enemyPos.y + enemyVelocity * FasterCalcs.cos(enemyHeading) * futureTime;

                // Se a previsão sair do campo de batalha, aborta e usa a mira padrão.
                if (predictedPos.x < 18 || predictedPos.y < 18 ||
                    predictedPos.x > robot.getBattleFieldWidth() - 18 ||
                    predictedPos.y > robot.getBattleFieldHeight() - 18) {
                    findBestGFAndShoot();
                    return;
                }
            }
        
            // Aponta o canhão para o ponto previsto.
            double theta = Utils.normalRelativeAngle(
                FasterCalcs.atan2(predictedPos.x - myPos.x, predictedPos.y - myPos.y) - robot.getGunHeadingRadians()
            );
            robot.setTurnGunRightRadians(theta);

            // Atira se a arma não estiver quente e a mira estiver estável.
            if (robot.getGunHeat() == 0 && Math.abs(robot.getGunTurnRemainingRadians()) < Math.atan(36.0 / distance)) {
                if (robot.setFireBullet(firePower) != null) {
                    waveManager.addWave(firePower);
                    hitTracker.logShotFired();
                }
            }
            return; // Finaliza para não usar a mira GF.
        }

        // Se o alvo não for linear, usa a mira GuessFactor original.
        findBestGFAndShoot();
    }

    /**
     * Implementa a lógica da mira estatística (GuessFactor).
     * Encontra o GF mais provável e comanda o robô para atirar.
     * Este método foi refatorado de shoot() para evitar duplicação de código.
     */
    private void findBestGFAndShoot() {
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

        if(robot.getGunHeat() == 0 && Math.abs(pointGun) < 0.02) {
            if(robot.setFireBullet(firePower) != null) {
                waveManager.addWave(firePower);
                hitTracker.logShotFired();
            }
        }
    }
    
    /**
     * Procura no array BINS pelo GuessFactor com a maior pontuação (mais acertos)
     * para a situação atual.
     * @return O melhor GuessFactor (um valor entre -1.0 e 1.0).
     */
    private double findBestGF() {
        int distanceIndex = getDistanceIndex();
        int velocityIndex = getVelocityIndex();
        int accelIndex = getAccelerationIndex();
        int bestIndex = zeroIndex;

        for(int i = 0; i < GF_BINS; i++) {
            if(BINS[distanceIndex][velocityIndex][accelIndex][i] > BINS[distanceIndex][velocityIndex][accelIndex][bestIndex]) {
                bestIndex = i;
            }
        }

        double bestGF = (double)(bestIndex - zeroIndex) / (double) zeroIndex;

        System.out.println(
            "MIRA INFO [T:" + robot.getTime() + "]" +
            " | D.Bin: " + distanceIndex +
            " | V.Bin: " + velocityIndex +
            " | A.Bin: " + accelIndex +
            " | Best GF: " + String.format("%.2f", bestGF)
        );

        return bestGF;
    }

    /**
     * Registra um acerto no array de estatísticas, aumentando a pontuação
     * do GuessFactor que resultou no acerto.
     * @param GF O GuessFactor do tiro bem-sucedido.
     */
    public void logSuccess(double GF) {
        int distanceIndex = getDistanceIndex();
        int velocityIndex = getVelocityIndex();
        int accelIndex = getAccelerationIndex(); 
        
        int index = (int) Math.round((GF * zeroIndex) + zeroIndex); 
        
        BINS[distanceIndex][velocityIndex][accelIndex][index] += 1.0;
    }

    /**
     * Chamado quando um de nossos tiros atinge o inimigo.
     * Este método calcula qual foi o GuessFactor do tiro e o registra.
     * @param impactLocation O ponto exato (x, y) do impacto.
     * @param bulletPower A potência do tiro que acertou.
     */
    public void onBulletHit(Point2D.Double impactLocation, double bulletPower) {
        Wave hitWave = waveManager.findCorrectWave(impactLocation, bulletPower, robot.getTime());
        if(hitWave == null) {
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

    /**
     * Reduz (decaimento) levemente todos os valores no array BINS.
     * Isso permite que a mira "esqueça" dados antigos e se adapte a novas
     * estratégias do oponente.
     */
    public void decayBINS() {
        for (int i = 0; i < DISTANCE_BINS; i++) {
            for (int j = 0; j < VELOCITY_BINS; j++) {
                for (int k = 0; k < ACCEL_BINS; k++) {
                    for (int l = 0; l < GF_BINS; l++) {
                        BINS[i][j][k][l] *= 0.999;
                    }
                }
            }
        }
    }
}