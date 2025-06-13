package killBot.movement.waveSurfing;

import static robocode.util.Utils.normalRelativeAngle;

import java.awt.geom.Point2D;
import java.util.ArrayList;

import killBot.movement.MovementControl;
import robocode.AdvancedRobot;
import killBot.utils.math.AuxiliarFunctions;

public class WaveSurfer {

    private AdvancedRobot bot;
    private ArrayList<BulletWave> activeWaves;
    private MovementControl movementControl;


    // NOVO: Armazena a trajetória simulada da rota escolhida
    private ArrayList<Point2D.Double> chosenPathPoints = new ArrayList<>();
    // NOVO: Armazena as posições de impacto simuladas para a rota escolhida
    private Point2D.Double chosenImpactPoint = null;

    public static int[] STATS_BINS = new int[40];

    /**
     * Construtor da classe
     * 
     * @param bot             referencia ao KillBot
     * @param activeWaves     o array de tiros disparados
     * @param movementControl o objeto que controla a movimentacao
     */
    public WaveSurfer(AdvancedRobot bot, ArrayList<BulletWave> activeWaves, MovementControl movementControl) {
        this.bot = bot;
        this.activeWaves = activeWaves;
        this.movementControl = movementControl;
    }

    /**
     * Preve a posicao da bala no proximo turno
     * 
     * @param wave       onda da bala
     * @param futureTime tempo futuro
     * @return retorna uma cordenada de onde a bala vai estar
     */
    public Point2D.Double getBulletFuturePosition(BulletWave wave, long futureTime) {
        double bulletDistanceTraveled = ((futureTime - wave.getFireTime())) * wave.getBulletSpeed();

        double bulletX = wave.getOriginX() + bulletDistanceTraveled * Math.sin(wave.getDirectAngle());
        double bulletY = wave.getOriginY() + bulletDistanceTraveled * Math.cos(wave.getDirectAngle());

        return new Point2D.Double(bulletX, bulletY);
    }

    /**
     * NOVA VERSÃO - Calcula o perigo ESTATÍSTICO de um único ponto no espaço
     * para uma única onda.
     * * @param wave A onda de tiro inimiga que estamos analisando.
     * 
     * @param targetX A posição X futura do nosso robô que queremos avaliar.
     * @param targetY A posição Y futura do nosso robô que queremos avaliar.
     * @return O número de vezes que o inimigo já acertou um tiro nesse "fator de
     *         mira".
     */
    public double getDangerRating(BulletWave wave, double targetX, double targetY) {
        // Calcula o GuessFactor necessário para o inimigo nos atingir na posição
        // (targetX, targetY).
        // Esta é a mesma lógica que usamos em onHitByBullet, mas agora para um ponto
        // hipotético.

        double hitAngle = AuxiliarFunctions.absoluteBearing(wave.getOriginX(), wave.getOriginY(), targetX, targetY);
        double angleOffset = normalRelativeAngle(hitAngle - wave.getDirectAngle());

        // Precisamos da direção do robô em relação à onda no momento do disparo.
        // Usamos o valor que salvamos na própria onda.
        int waveDirection = wave.getDirection();

        double guessFactor = Math.max(-1, Math.min(1, angleOffset / wave.getMaxEscapeAngle())) * waveDirection;

        // Mapeia o GuessFactor para o índice do nosso array de estatísticas.
        int binIndex = (int) Math.round(((guessFactor + 1) / 2) * (STATS_BINS.length - 1));

        // O "perigo" deste ponto é simplesmente o valor que está guardado no bin
        // correspondente.
        // Quanto mais vezes o inimigo acertou com este GF, maior o perigo.
        return STATS_BINS[binIndex];
    }

    public void doWaveSurfing() {
        System.out.println("\n--- WaveSurfer.doWaveSurfing() (MODO PREDITIVO v4.2 - Orbital) ---");

        if (activeWaves.isEmpty()) {
            movementControl.doStandardMovement();
            return;
        }

        double bestDanger = Double.POSITIVE_INFINITY;
        Point2D.Double bestDestination = new Point2D.Double(bot.getX(), bot.getY());

        // PASSO 1: Itera sobre todos os possíveis PONTOS DE DESTINO orbitando o
        // inimigo.
        for (BulletWave waveForReference : activeWaves) { // Pega uma onda como referência para a posição inimiga
            double angleToEnemy = AuxiliarFunctions.absoluteBearing(bot.getX(), bot.getY(),
                    waveForReference.getOriginX(), waveForReference.getOriginY());

            // Testa para os dois lados da órbita (horário e anti-horário)
            for (int direction = -1; direction <= 1; direction += 2) {

                // Simula "passos" se afastando na órbita
                for (int i = 0; i < 20; i++) {

                    double orbitAngle = angleToEnemy + (Math.PI / 2 * direction) + (direction * Math.toRadians(i * 2));
                    Point2D.Double testPoint = new Point2D.Double(
                            bot.getX() + Math.sin(orbitAngle) * (i * 6),
                            bot.getY() + Math.cos(orbitAngle) * (i * 6));

                    // PASSO 2: CHECAGEM DE PAREDE
                    if (testPoint.x < 18 || testPoint.y < 18 ||
                            testPoint.x > bot.getBattleFieldWidth() - 18 ||
                            testPoint.y > bot.getBattleFieldHeight() - 18) {
                        continue;
                    }

                    // PASSO 3: Calcula o perigo SOMADO de TODAS as ondas para este ponto.
                    double totalDangerForThisPoint = 0;
                    for (BulletWave wave : activeWaves) {
                        double statisticalDanger = getDangerRating(wave, testPoint.x, testPoint.y);
                        double distanceToEnemy = testPoint.distance(wave.getOriginX(), wave.getOriginY());
                        double idealDistance = 350.0;
                        double distancePenalty = Math.abs(distanceToEnemy - idealDistance) * 0.02;
                        totalDangerForThisPoint += statisticalDanger + distancePenalty;
                    }

                    // PASSO 4: Compara com o melhor que encontramos.
                    if (totalDangerForThisPoint < bestDanger) {
                        bestDanger = totalDangerForThisPoint;
                        bestDestination = testPoint;
                    }
                }
            }
            break; // A gente só precisa da posição do inimigo, então só usamos a primeira onda
                   // como referência.
        }

        // O resto do método é igual...
        if (bestDanger == Double.POSITIVE_INFINITY) {
            movementControl.doEmergencyWallSmoothing();
            System.out.println("WaveSurfer: Nenhuma rota válida. Ativando Wall Smoothing de emergência.");
            return;
        }

        goTo(bestDestination);
        System.out.println("WaveSurfer: Rota Escolhida -> Destino: (" +
                String.format("%.0f", bestDestination.x) + ", " +
                String.format("%.0f", bestDestination.y) + "), Perigo Mínimo Total: " + bestDanger);
    }


    // Novos getters para os pontos de debug
    public ArrayList<Point2D.Double> getChosenPathPoints() {
        return chosenPathPoints;
    }

    public Point2D.Double getChosenImpactPoint() {
        return chosenImpactPoint;
    }

    private void goTo(Point2D.Double destination) {
        double angleToDest = AuxiliarFunctions.absoluteBearing(bot.getX(), bot.getY(), destination.x, destination.y);
        double turnAngle = normalRelativeAngle(angleToDest - bot.getHeadingRadians());
        double distance = AuxiliarFunctions.getDistance(bot.getX(), bot.getY(), destination.x, destination.y);

        // Vira na direção mais curta para o destino
        if (Math.abs(turnAngle) > Math.PI / 2) {
            turnAngle = normalRelativeAngle(turnAngle + Math.PI);
            bot.setTurnRightRadians(turnAngle);
            bot.setBack(distance);
        } else {
            bot.setTurnRightRadians(turnAngle);
            bot.setAhead(distance);
        }
    }

}
