package killBot.utils.math;

import static robocode.util.Utils.*; // Importe este também

public class AuxiliarFunctions {
    /**
     * Metodo de calculo da distancia entre dois pontos
     * 
     * @param x1 x do ponto 1
     * @param y1 y do ponto 1
     * @param x2 x do ponto 2
     * @param y2 y do ponto 2
     * @return a distancia entre os pontos
     */
    public static double getDistance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    /**
     * Metodo de calculo do angulo absoluto entre dois pontos
     * 
     * @param x1 x do ponto 1
     * @param y1 y do ponto 1
     * @param x2 x do ponto 2
     * @param y2 y do ponto 2
     * @return o angulo absoluto entre os pontos.
     */
    public static double absoluteBearing(double x1, double y1, double x2, double y2) {
        return Math.atan2(x2 - x1, y2 - y1);
    }

    /**
     * Calcula a distância de um ponto à parede mais próxima.
     * Retorna um valor baixo se estiver muito perto da parede.
     * 
     * @param x                 coordenada x
     * @param y                 coordenada y
     * @param battlefieldWidth  largura parede
     * @param battlefieldHeight altura parede
     * @return um valor baixo se proximo a parede
     */
    public static double getWallProximity(double x, double y, double battlefieldWidth, double battlefieldHeight) {
        double distToLeft = x;
        double distToRight = battlefieldWidth - x;
        double distToBottom = y;
        double distToTop = battlefieldHeight - y;
        return Math.min(distToLeft, Math.min(distToRight, Math.min(distToBottom, distToTop)));
    }

    /**
     * Calcula o ângulo para a parede mais próxima, para se mover paralelo a ela.
     * Retorna o ângulo absoluto para a parede.
     * 
     * @param x                 coordenada x
     * @param y                 coordenada y
     * @param battlefieldWidth  largura da parede
     * @param battlefieldHeight altura da parede
     * @return angulo ate a parede
     */
    public static double getWallBearing(double x, double y, double battlefieldWidth, double battlefieldHeight) {
        double wallBearing = 0;
        // Definir uma margem para considerar "perto da parede"
        double wallStickMargin = 50; // Ajuste este valor conforme necessário

        if (x < wallStickMargin)
            wallBearing = normalAbsoluteAngle(Math.toRadians(90)); // Parede esquerda
        else if (x > battlefieldWidth - wallStickMargin)
            wallBearing = normalAbsoluteAngle(Math.toRadians(270)); // Parede direita
        else if (y < wallStickMargin)
            wallBearing = normalAbsoluteAngle(Math.toRadians(0)); // Parede de baixo
        else if (y > battlefieldHeight - wallStickMargin)
            wallBearing = normalAbsoluteAngle(Math.toRadians(180)); // Parede de cima

        // Se não estiver perto de nenhuma parede, retorna um valor neutro ou dependendo
        // da lógica.
        // Para a órbita, geralmente a gente só chama isso se já sabe que está perto de
        // uma parede.
        return wallBearing;
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Classe auxiliar para constantes de física do Robocode
     */
    public static class RoboPhysics {
        public static final double MAX_ROBOT_VELOCITY = 8.0;
        public static final double MAX_ROBOT_TURN_RATE_RADIANS = Math.toRadians(10.0); // 10 graus em radianos

        public static double getMaxTurnRateRadians(double velocity) {
            return Math.toRadians(Math.max(10, 20 - 0.75 * Math.abs(velocity)));
        }
    }

}
