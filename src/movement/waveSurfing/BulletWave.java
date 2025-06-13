package movement.waveSurfing;

/**
 * Classe para o armazenamento dos dados dos tiros disparados
 */
public class BulletWave {

    private double originX;
    private double originY;

    private long fireTime; // turno que o tiro foi disparado

    // atributos relacionados a bala
    private double bulletPower;
    private double bulletSpeed;
    private double directAngle;
    private int direction;

    /**
     * Construtor da classe
     * 
     * @param originX     posicao horizontal do inimigo
     * @param originY     posicao vertical do inimigo
     * @param fireTime    turno em que ocorreu o tiro
     * @param bulletPower forca do tiro
     * @param directAngle angulo do tiro disparado
     * @param direction   direcao
     */
    public BulletWave(double originX, double originY, long fireTime, double bulletPower, double directAngle,
            int direction) {

        this.originX = originX;
        this.originY = originY;
        this.fireTime = fireTime;
        this.bulletPower = bulletPower;
        this.bulletSpeed = 20 - (3 * bulletPower); // forma de calcular a velocidade de acordo com o robocode
        this.directAngle = directAngle;
        this.direction = direction;

    }

    public double getMaxEscapeAngle() {
        return Math.asin(8.0 / getBulletSpeed());
    }

    // gets:
    public double getOriginX() {
        return originX;
    }

    public double getOriginY() {
        return originY;
    }

    public long getFireTime() {
        return fireTime;
    }

    public double getBulletPower() {
        return bulletPower;
    }

    public double getBulletSpeed() {
        return bulletSpeed;
    }

    public double getDirectAngle() {
        return directAngle;
    }

    public int getDirection() {
        return direction;
    }

}
