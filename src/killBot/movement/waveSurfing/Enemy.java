package killBot.movement.waveSurfing;

public class Enemy {

    // dados do inimigo
    private String enemyName;
    private double x, y;
    private double lastEnemyHeading;
    private double lastEnemyVelocity;
    private double lastEnemyEnergy;

    private long cTime; // tempo que foi escaneado o inimigo

    public Enemy(String enemyName) {
        this.enemyName = enemyName;
    }

    /**
     * Metodo de atualizacao dos dados do Inimigo detectado
     * 
     * @param x                 posicao horizontal
     * @param y                 posicao vertical
     * @param lastEnemyHeading  angulo que ta virado
     * @param lastEnemyVelocity velocidade
     * @param lastEnemyEnergy   energia
     * @param cTime             tempo que aconteceu o escaneamento
     */
    public void updateEnemyData(double x, double y, double lastEnemyHeading, double lastEnemyVelocity,
            double lastEnemyEnergy, long cTime) {
        this.x = x;
        this.y = y;
        this.lastEnemyHeading = lastEnemyHeading;
        this.lastEnemyVelocity = lastEnemyVelocity;
        this.lastEnemyEnergy = lastEnemyEnergy;
        this.cTime = cTime;
    }

    // gets:
    public String getEnemyName() {
        return enemyName;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getLastEnemyHeading() {
        return lastEnemyHeading;
    }

    public double getLastEnemyVelocity() {
        return lastEnemyVelocity;
    }

    public double getLastEnemyEnergy() {
        return lastEnemyEnergy;
    }

    public long getcTime() {
        return cTime;
    }

    /**
     * Metodo de resetar os dados do robo, em caso de morte pode ser util
     */
    public void reset() {
        this.x = 0;
        this.y = 0;
        this.lastEnemyHeading = 0;
        this.lastEnemyVelocity = 0;
        this.lastEnemyEnergy = 0;
        this.cTime = 0;
    }
}
