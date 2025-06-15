package killBot.movement.waveSurfing;

import static robocode.util.Utils.normalRelativeAngle;

import java.awt.geom.Point2D;
import java.util.ArrayList;

import killBot.movement.MovementControl;
import robocode.AdvancedRobot;
import killBot.utils.math.AuxiliarFunctions;
import killBot.utils.math.FasterCalcs;

public class WaveSurfer {

    private AdvancedRobot bot;
    private ArrayList<BulletWave> activeWaves;
    private MovementControl movementControl;
    private int lastOrbitDirection = 1;
    private ArrayList<Point2D.Double> chosenPathPoints = new ArrayList<>();
    private Point2D.Double chosenImpactPoint = null; //coordenada de impacto 

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

        double bulletX = wave.getOriginX() + bulletDistanceTraveled * FasterCalcs.sin(wave.getDirectAngle());
        double bulletY = wave.getOriginY() + bulletDistanceTraveled * FasterCalcs.cos(wave.getDirectAngle());

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

    /**
     * Metodo que realiza a tecnica de movimentacao WaveSurfing
    */
    public void doWaveSurfing() {
    System.out.println("\n--- WaveSurfer.doWaveSurfing() (MODO PREDITIVO v4.3 - Inércia) ---");

    if (activeWaves.isEmpty()) {
        movementControl.doStandardMovement();
        return;
    }

    // MUDANÇA: Variáveis para guardar o melhor ponto e perigo para cada direção
    Point2D.Double bestClockwisePoint = null;       // Melhor ponto na órbita HORÁRIA (-1)
    Point2D.Double bestAntiClockwisePoint = null;   // Melhor ponto na órbita ANTI-HORÁRIA (1)
    double bestClockwiseDanger = Double.POSITIVE_INFINITY;
    double bestAntiClockwiseDanger = Double.POSITIVE_INFINITY;

    BulletWave waveForReference = activeWaves.get(0); // Pega uma onda como referência para a posição inimiga
    double angleToEnemy = AuxiliarFunctions.absoluteBearing(bot.getX(), bot.getY(),
            waveForReference.getOriginX(), waveForReference.getOriginY());

    // MUDANÇA: Avalia todos os pontos possíveis, mas guarda os melhores de cada lado separadamente
    for (int direction = -1; direction <= 1; direction += 2) { // direction -1 = horário, 1 = anti-horário
        for (int i = 0; i < 20; i++) { // Simula "passos" se afastando na órbita
            double orbitAngle = angleToEnemy + (FasterCalcs.HALF_PI * direction) + (direction * Math.toRadians(i * 2));
            Point2D.Double testPoint = new Point2D.Double(
                    bot.getX() + FasterCalcs.sin(orbitAngle) * (i * 6),
                    bot.getY() + FasterCalcs.cos(orbitAngle) * (i * 6));

            if (testPoint.x < 18 || testPoint.y < 18 ||
                    testPoint.x > bot.getBattleFieldWidth() - 18 ||
                    testPoint.y > bot.getBattleFieldHeight() - 18) {
                continue;
            }

            double totalDangerForThisPoint = 0;
            for (BulletWave wave : activeWaves) {
                double statisticalDanger = getDangerRating(wave, testPoint.x, testPoint.y);
                double distanceToEnemy = testPoint.distance(wave.getOriginX(), wave.getOriginY());
                double idealDistance = 300.0;
                double distancePenalty = Math.abs(distanceToEnemy - idealDistance) * 0.02;
                double wallPenalty = calculateWallPenalty(testPoint.x, testPoint.y);
                totalDangerForThisPoint += statisticalDanger + distancePenalty + wallPenalty;
            }

            // Guarda o melhor ponto PARA AQUELA DIREÇÃO ESPECÍFICA
            if (direction == -1) { // Sentido Horário
                if (totalDangerForThisPoint < bestClockwiseDanger) {
                    bestClockwiseDanger = totalDangerForThisPoint;
                    bestClockwisePoint = testPoint;
                }
            } else { // Sentido Anti-Horário
                if (totalDangerForThisPoint < bestAntiClockwiseDanger) {
                    bestAntiClockwiseDanger = totalDangerForThisPoint;
                    bestAntiClockwisePoint = testPoint;
                }
            }
        }
    }

    // MUDANÇA: LÓGICA DE DECISÃO COM INÉRCIA
    // Antes de comparar, adicionamos uma penalidade se a direção for MUDAR
    double directionPenalty = 40; // Valor da penalidade. Ajuste se necessário.
    if (this.lastOrbitDirection == -1) { // Se estávamos indo no sentido horário...
        bestAntiClockwiseDanger += directionPenalty; // ...penalize a troca para anti-horário.
    } else { // Se estávamos indo no sentido anti-horário...
        bestClockwiseDanger += directionPenalty; // ...penalize a troca para horário.
    }

    Point2D.Double bestDestination;

    // Compara os perigos TOTAIS (incluindo a penalidade) e escolhe o melhor caminho
    if (bestClockwiseDanger < bestAntiClockwiseDanger) {
        bestDestination = bestClockwisePoint;
        this.lastOrbitDirection = -1; // Atualiza a direção para o próximo turno
    } else {
        bestDestination = bestAntiClockwisePoint;
        this.lastOrbitDirection = 1; // Atualiza a direção para o próximo turno
    }
    
    // Se, mesmo assim, nenhum ponto for válido (ex: preso num canto)
    if (bestDestination == null) {
        movementControl.doEmergencyWallSmoothing();
        System.out.println("WaveSurfer: Nenhuma rota válida. Ativando Wall Smoothing de emergência.");
        return;
    }

    goTo(bestDestination);
    System.out.println("WaveSurfer: Rota Escolhida -> Destino: (" +
            String.format("%.0f", bestDestination.x) + ", " +
            String.format("%.0f", bestDestination.y) + "), Perigo Mínimo Total: " + Math.min(bestClockwiseDanger, bestAntiClockwiseDanger));
}

    // Novos getters para os pontos de debug
    public ArrayList<Point2D.Double> getChosenPathPoints() {
        return chosenPathPoints;
    }

    public Point2D.Double getChosenImpactPoint() {
        return chosenImpactPoint;
    }

    /**
     * metodo para fazer o robo ir para uma direcao
     * 
     * @param destination coordenadas do destino
     */
    private void goTo(Point2D.Double destination) {

        double distance = bot.getX() - destination.x; // Usado apenas para obter a direção para trás/frente
        double angleToDest = AuxiliarFunctions.absoluteBearing(bot.getX(), bot.getY(), destination.x, destination.y);
        
        // Determina se devemos nos mover para frente ou para trás.
        // Se o ângulo para o destino estiver mais "atrás" do que "na frente", nos movemos de ré.
        double turnAngle = normalRelativeAngle(angleToDest - bot.getHeadingRadians());
        int direction = (Math.abs(turnAngle) > FasterCalcs.HALF_PI) ? -1 : 1;
        
        // Se a direção for para trás, ajustamos o ângulo em 180 graus.
        if (direction == -1) {
            angleToDest = normalRelativeAngle(angleToDest + FasterCalcs.PI);
        }
        // ======================= INÍCIO DA MUDANÇA PRINCIPAL =======================
        double smoothedAngle = movementControl.wallSmoothing(bot.getX(), bot.getY(), angleToDest, lastOrbitDirection);
        // ======================== FIM DA MUDANÇA PRINCIPAL =========================

        // Agora, viramos para o ângulo SUAVIZADO, não para o ângulo original.
        bot.setTurnRightRadians(normalRelativeAngle(smoothedAngle - bot.getHeadingRadians()));

        // E nos movemos na direção calculada (frente ou trás).
        bot.setAhead(AuxiliarFunctions.getDistance(bot.getX(), bot.getY(), destination.x, destination.y) * direction);
    }       

    /**
     * Calcula uma penalidade baseada na proximidade de um ponto com as paredes.
     * Quanto mais perto, maior a penalidade.
     * 
     * @param x A coordenada X do ponto.
     * @param y A coordenada Y do ponto.
     * @return Um valor de penalidade.
     */
private double calculateWallPenalty(double x, double y) {
    // Define uma margem de segurança. Abaixo desta distância, a penalidade começa a
    // ser aplicada.
    // AUMENTAMOS A MARGEM DE 60 PARA 80
    double wallMargin = 80.0;
    double penalty = 0.0;

    // Calcula o quão "fundo" o ponto está dentro da margem de perigo de cada
    // parede.
    penalty += Math.max(0, wallMargin - x); // Parede esquerda
    penalty += Math.max(0, wallMargin - y); // Parede de baixo
    
    // Correção na fórmula original para paredes direita e de cima
    penalty += Math.max(0, x - (bot.getBattleFieldWidth() - wallMargin)); // Parede direita
    penalty += Math.max(0, y - (bot.getBattleFieldHeight() - wallMargin)); // Parede de cima

    // Multiplica por um fator de peso para ajustar a "fobia" de parede do robô.
    // AUMENTAMOS O PESO DE 0.08 PARA 0.15
    return penalty * 0.15;
}

}
