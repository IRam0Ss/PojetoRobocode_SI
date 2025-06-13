package movement;

import static robocode.util.Utils.normalRelativeAngle;

import movement.waveSurfing.Enemy;
import myBot.KillBot;
import robocode.AdvancedRobot;
import utils.math.AuxiliarFunctions;
import utils.math.AuxiliarFunctions.RoboPhysics;

public class MovementControl {

    private KillBot bot;

    private Enemy targetEnemy;

    public static final double IDEAL_ORBITAL_DISTANCE = 300;
    public static final double MIN_DANGER_DISTANCE = 300; // Distância mínima que queremos manter do inimigo.

    private double lastDirection = 1; // usada para suavizar a evasao
    private int moveDirection = 1;

    /**
     * Construtor da classe
     * 
     * @param bot         o killbot
     * @param targetEnemy o inimigo a ser orbitado
     */
    public MovementControl(KillBot bot) {
        this.bot = bot;
    }

    /**
     * Metodo que executa a logica de movimentacao
     */
    public void doStandardMovement() {

        if (targetEnemy == null) {
            double angle = wallSmoothing(bot.getX(), bot.getY(), bot.getHeadingRadians(), moveDirection);
            bot.setTurnRightRadians(normalRelativeAngle(angle - bot.getHeadingRadians()));

            if (Math.abs(bot.getVelocity()) < 0.1 && Math.abs(bot.getDistanceRemaining()) < 1) {
                moveDirection *= -1; // Inverte direção se ficar travado
            }

            bot.setAhead(100 * moveDirection);
            return;
        }

        double angleToEnemy = AuxiliarFunctions.absoluteBearing(bot.getX(), bot.getY(), targetEnemy.getX(),
                targetEnemy.getY());
        double currentDistance = AuxiliarFunctions.getDistance(bot.getX(), bot.getY(), targetEnemy.getX(),
                targetEnemy.getY());

        double orbitDirection = this.lastDirection;

        // Simular um passo para cada lado da órbita para avaliar perigo de parede
        double antiClockwiseAngle = angleToEnemy + (Math.PI / 2);
        double clockwiseAngle = angleToEnemy - (Math.PI / 2);

        // Projeta posições futuras para avaliação de parede
        double antiClockwiseX = bot.getX()
                + AuxiliarFunctions.RoboPhysics.MAX_ROBOT_VELOCITY * Math.sin(antiClockwiseAngle);
        double antiClockwiseY = bot.getY()
                + AuxiliarFunctions.RoboPhysics.MAX_ROBOT_VELOCITY * Math.cos(antiClockwiseAngle);
        double clockwiseX = bot.getX() + AuxiliarFunctions.RoboPhysics.MAX_ROBOT_VELOCITY * Math.sin(clockwiseAngle);
        double clockwiseY = bot.getY() + AuxiliarFunctions.RoboPhysics.MAX_ROBOT_VELOCITY * Math.cos(clockwiseAngle);

        double antiClockwiseWallDanger = getWallDanger(antiClockwiseX, antiClockwiseY);
        double clockwiseWallDanger = getWallDanger(clockwiseX, clockwiseY);

        if (clockwiseWallDanger > antiClockwiseWallDanger + 100) {
            orbitDirection = -1;
        } else if (antiClockwiseWallDanger > clockwiseWallDanger + 100) {
            orbitDirection = 1;
        }

        double targetHeading = angleToEnemy + (Math.PI / 2 * orbitDirection); // Ângulo para orbitar

        double headingAdjustment = 0;
        double closeEnoughThreshold = 200; // Margem para "distância ideal"

        if (currentDistance < IDEAL_ORBITAL_DISTANCE - closeEnoughThreshold) {
            headingAdjustment = Math.toRadians(20);
        } else if (currentDistance > IDEAL_ORBITAL_DISTANCE + closeEnoughThreshold) {
            headingAdjustment = Math.toRadians(-20);
        }

        // Aplica o ajuste de heading à direção orbital
        targetHeading += (orbitDirection * headingAdjustment);

        // Gira o robô para o heading calculado
        bot.setTurnRightRadians(normalRelativeAngle(targetHeading - bot.getHeadingRadians()));

        double moveDistance = 400;

        // Inverte a direção de movimento (para frente/trás) se o robô estiver parado.
        if (Math.abs(bot.getVelocity()) < 0.1 && Math.abs(bot.getDistanceRemaining()) < 1) {
            this.lastDirection *= -1;
        }
        bot.setAhead(moveDistance * this.lastDirection);
    }

    /**
     * Metodo responsavel por fazer movimento de evasao com os dados calculados pelo
     * waveSurfer
     * 
     * @param bestHeading       melhor angulacao calculada
     * @param bestMoveDirection melhor direcao calculada
     */
    public void doEvasiveMovement(double bestHeading, double bestMoveDirection) {

        System.out.println("\n--- MovementControl.doEvasiveMovement() ---");
        System.out.println("  Recebido: BestHeading=" + String.format("%.2f", Math.toDegrees(bestHeading))
                + "deg, BestMoveDirection=" + bestMoveDirection);

        /*
         * if (handleWallProximity()) { // A lógica de parede ainda tem prioridade, pois
         * é uma emergência.
         * System.out.println("  handleWallProximity ativado. doEvasiveMovement PAROU."
         * );
         * return;
         * }
         */

        double currentHeading = bot.getHeadingRadians();
        double currentSpeed = bot.getVelocity();

        double turnRate = normalRelativeAngle(bestHeading - currentHeading);
        double maxAllowedTurnRate = RoboPhysics.getMaxTurnRateRadians(currentSpeed);
        turnRate = Math.max(-maxAllowedTurnRate, Math.min(maxAllowedTurnRate, turnRate));

        System.out.println("  Robô Real Pos: (" + String.format("%.2f", bot.getX()) + ", "
                + String.format("%.2f", bot.getY()) + ") Heading: "
                + String.format("%.2f", Math.toDegrees(currentHeading)) + " Vel: " + currentSpeed);
        System.out.println("  TurnRate CALCULADO (e LIMITADO) para o Robô Real: "
                + String.format("%.2f", Math.toDegrees(turnRate)) + "deg");

        bot.setTurnRightRadians(turnRate);
        bot.setAhead(Double.POSITIVE_INFINITY * bestMoveDirection);
        System.out.println(
                "  Comandos WaveSurfer enviados: setTurnRightRadians(" + String.format("%.2f", Math.toDegrees(turnRate))
                        + "deg), setAhead(INFINITO * " + bestMoveDirection + ")");
    }

    /**
     * Método para lidar com a evasão imediata após uma colisão com outro robô.
     * Ele tenta descolar o robô agressivamente.
     */
    public void doContactEvasion() {
        System.out.println("MovementControl: ***ATIVANDO FUGA DE CONTATO!***");

        double currentX = bot.getX();
        double currentY = bot.getY();
        double battleFieldWidth = bot.getBattleFieldWidth();
        double battleFieldHeight = bot.getBattleFieldHeight();
        double currentHeading = bot.getHeadingRadians();

        double stuckWallMargin = 70;
        double botMargin = 18;

        double distToLeftEdge = currentX - botMargin;
        double distToRightEdge = battleFieldWidth - currentX - botMargin;
        double distToBottomEdge = currentY - botMargin;
        double distToTopEdge = battleFieldHeight - currentY - botMargin;

        // Se o robô está colado em UMA PAREDE (enquanto em fuga de contato)
        if (distToLeftEdge < stuckWallMargin || distToRightEdge < stuckWallMargin ||
                distToBottomEdge < stuckWallMargin || distToTopEdge < stuckWallMargin) {

            double wallBearingToFlee = 0;
            if (distToLeftEdge < stuckWallMargin)
                wallBearingToFlee = Math.toRadians(90);
            else if (distToRightEdge < stuckWallMargin)
                wallBearingToFlee = Math.toRadians(270);
            else if (distToBottomEdge < stuckWallMargin)
                wallBearingToFlee = Math.toRadians(0);
            else if (distToTopEdge < stuckWallMargin)
                wallBearingToFlee = Math.toRadians(180);

            bot.setTurnRightRadians(normalRelativeAngle(wallBearingToFlee - currentHeading));
            bot.setAhead(Double.POSITIVE_INFINITY); // Empurra para fora da parede
            System.out.println("MovementControl: Fuga de contato: Colado na parede. Virando para "
                    + String.format("%.2f", Math.toDegrees(wallBearingToFlee)) + "deg e setAhead(INFINITO).");
        } else {
            // Se não está colado na parede, faz a fuga normal de contato (do inimigo)
            bot.setBack(Double.POSITIVE_INFINITY);

            // Acha o ângulo para o inimigo atual
            if (targetEnemy != null) {
                double angleToEnemy = AuxiliarFunctions.absoluteBearing(bot.getX(), bot.getY(), targetEnemy.getX(),
                        targetEnemy.getY());
                bot.setTurnRightRadians(normalRelativeAngle(angleToEnemy + Math.PI / 2 - currentHeading)); // Gira
                                                                                                           // perpendicular
                System.out.println(
                        "MovementControl: Comandos de FUGA DE CONTATO: Back(INFINITO), TurnPerpendicular(90deg).");
            } else {
                bot.setTurnRightRadians(normalRelativeAngle(Math.PI / 2));
                System.out.println(
                        "MovementControl: Comandos de FUGA DE CONTATO: Back(INFINITO), TurnRight(90deg). (Sem alvo)");
            }
        }
    }

    public void setTargetEnemy(Enemy targetEnemy) {
        this.targetEnemy = targetEnemy;
    }

    /**
     * Método para lidar com a prevenção de parede
     * tenta guiar o robô para longe das paredes, com prioridade máxima.
     * 
     * @return true se acao foi necessaria e false se nao
     */
    public boolean handleWallProximity() {
        double currentX = bot.getX();
        double currentY = bot.getY();
        double battleFieldWidth = bot.getBattleFieldWidth();
        double battleFieldHeight = bot.getBattleFieldHeight();
        double currentHeading = bot.getHeadingRadians();

        double wallMargin = 120;

        double botMargin = 18;
        double stuckInWallMargin = 20;

        // Calcular a distância para cada parede
        double distToLeft = currentX - botMargin;
        double distToRight = battleFieldWidth - currentX - botMargin;
        double distToBottom = currentY - botMargin;
        double distToTop = battleFieldHeight - currentY - botMargin;

        if (distToLeft < wallMargin || distToRight < wallMargin ||
                distToBottom < wallMargin || distToTop < wallMargin) {

            System.out.println("MovementControl.handleWallProximity(): ***PERTO DA PAREDE!*** Distâncias: L:"
                    + String.format("%.0f", distToLeft) + " R:" + String.format("%.0f", distToRight) + " B:"
                    + String.format("%.0f", distToBottom) + " T:" + String.format("%.0f", distToTop));

            // Prioridade se o robo ficou travado
            if (Math.abs(bot.getVelocity()) < 1.0
                    && (distToLeft < stuckInWallMargin || distToRight < stuckInWallMargin ||
                            distToBottom < stuckInWallMargin || distToTop < stuckInWallMargin)) {

                double wallBearing = 0; // Ângulo para a parede
                if (distToLeft < stuckInWallMargin)
                    wallBearing = Math.toRadians(90); // Parede esquerda, aponta para a direita
                else if (distToRight < stuckInWallMargin)
                    wallBearing = Math.toRadians(270); // Parede direita, aponta para a esquerda
                else if (distToBottom < stuckInWallMargin)
                    wallBearing = Math.toRadians(0); // Parede de baixo, aponta para cima
                else if (distToTop < stuckInWallMargin)
                    wallBearing = Math.toRadians(180); // Parede de cima, aponta para baixo

                bot.setTurnRightRadians(normalRelativeAngle(wallBearing - currentHeading)); // gira pro oposto da parede
                bot.setAhead(Double.POSITIVE_INFINITY); // Tenta ir para frente para descolar

                System.out
                        .println("MovementControl.handleWallProximity(): Robô travado, forçando RECUA e GIRO.");
                return true;
            }

            // Se não estiver travado, mas ainda perto da parede, usa a lógica de desvio
            // padrão da parede. vira pro centro
            double targetX = battleFieldWidth / 2;
            double targetY = battleFieldHeight / 2;
            double angleToCenter = AuxiliarFunctions.absoluteBearing(currentX, currentY, targetX, targetY);

            bot.setTurnRightRadians(normalRelativeAngle(angleToCenter - currentHeading));
            bot.setAhead(Double.POSITIVE_INFINITY);

            System.out.println("MovementControl.handleWallProximity(): Ação de parede. Virando para centro ("
                    + String.format("%.2f", Math.toDegrees(angleToCenter)) + "deg).");
            return true; // Indicou que uma ação de parede foi tomada
        }
        return false; // Nenhuma ação de parede necessária
    }

    /**
     * Calcula um "fator de perigo" de parede para uma dada posição (X, Y).
     * Quanto maior o valor retornado, mais longe da parede a posição está.
     * 
     * @param x Posição X a ser avaliada.
     * @param y Posição Y a ser avaliada.
     * @return Um valor que indica o quão perto da parede a posição está.
     */
    private double getWallDanger(double x, double y) {
        double battleFieldWidth = bot.getBattleFieldWidth();
        double battleFieldHeight = bot.getBattleFieldHeight();

        // Distância das 4 paredes
        double distToLeft = x;
        double distToRight = battleFieldWidth - x;
        double distToBottom = y;
        double distToTop = battleFieldHeight - y;

        // Retorna o mínimo entre as distâncias para as paredes
        return Math.min(distToLeft, Math.min(distToRight, Math.min(distToBottom, distToTop)));
    }

    public Enemy getTargetEnemy() {
        return targetEnemy;
    }

    private double wallSmoothing(double x, double y, double heading, int direction) {
        double angle = heading;
        double wallMargin = 40; // Margem de segurança

        for (int i = 0; i < 100; i++) { // Tenta suavizar por até 100 iterações
            double testX = x + Math.sin(angle) * 120;
            double testY = y + Math.cos(angle) * 120;

            if (isSafe(testX, testY, wallMargin)) {
                break;
            }

            angle += direction * 0.05; // Suaviza o ângulo girando aos poucos
        }

        return angle;
    }

    private boolean isSafe(double x, double y, double margin) {
        return x > margin && x < bot.getBattleFieldWidth() - margin
                && y > margin && y < bot.getBattleFieldHeight() - margin;
    }

    public static boolean isBotStuckNearWall(AdvancedRobot bot, double currentX, double currentY) {
        double stuckWallMargin = 20; // Use a mesma margem da sua lógica de "travado" em handleWallProximity
        double botMargin = 18; // Raio do robô

        double distToLeftEdge = currentX - botMargin;
        double distToRightEdge = bot.getBattleFieldWidth() - currentX - botMargin;
        double distToBottomEdge = currentY - botMargin;
        double distToTopEdge = bot.getBattleFieldHeight() - currentY - botMargin;

        // Está colado em alguma parede?
        boolean isActuallyStuck = (distToLeftEdge < stuckWallMargin || distToRightEdge < stuckWallMargin ||
                distToBottomEdge < stuckWallMargin || distToTopEdge < stuckWallMargin);

        // E a velocidade é quase zero?
        return isActuallyStuck && (Math.abs(bot.getVelocity()) < 1.0); // Use 1.0 ou 0.5 para "quase parado"
    }

    public boolean isInContactEvasion() {
        return bot.isInContactEvasion();
    }

    /**
     * Método de emergência para quando o WaveSurfer não encontra nenhuma rota
     * válida.
     * Força o robô a se mover em direção ao centro do mapa para descolar de cantos.
     */
    public void doEmergencyWallSmoothing() {
        double currentX = bot.getX();
        double currentY = bot.getY();
        double currentHeading = bot.getHeadingRadians();

        // Calcula o ângulo para o centro do campo de batalha
        double targetX = bot.getBattleFieldWidth() / 2;
        double targetY = bot.getBattleFieldHeight() / 2;
        double angleToCenter = AuxiliarFunctions.absoluteBearing(currentX, currentY, targetX, targetY);

        // Vira o robô para o centro
        bot.setTurnRightRadians(normalRelativeAngle(angleToCenter - currentHeading));

        // Anda para frente para sair do canto
        bot.setAhead(100);
    }

}
