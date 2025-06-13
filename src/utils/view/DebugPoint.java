// DebugPoint.java
package utils.view; 
;

public class DebugPoint {
    private double heading;
    private double dangerRating;
    private double botX; // Adicionado
    private double botY; // Adicionado
    private double moveDirection; // 1 para frente, -1 para trás

    public DebugPoint(double heading, double dangerRating, double botX, double botY, double moveDirection) { // Construtor atualizado
        this.heading = heading;
        this.dangerRating = dangerRating;
        this.botX = botX;   // Atribui
        this.botY = botY;   // Atribui
        this.moveDirection = moveDirection;
    }

    public double getHeading() {
        return heading;
    }

    public double getDangerRating() {
        return dangerRating;
    }

    public double getBotX() { // Novo getter
        return botX;
    }

    public double getBotY() { // Novo getter
        return botY;
    }

    public double getMoveDirection() {
        return moveDirection;
    }
}