package killBot;

import robocode.*;
import killBot.radar.RadarControl;
import killBot.gun.Aimer;
import killBot.gun.HitRateTracker;
import killBot.gun.WaveManager;
import killBot.utils.math.AuxiliarFunctions;
import killBot.utils.math.FasterCalcs;
import killBot.data.GameData;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import robocode.util.Utils;
import killBot.movement.MovementControl;
import killBot.movement.waveSurfing.BulletWave;
import killBot.movement.waveSurfing.Enemy;
import killBot.movement.waveSurfing.WaveSurfer;

public class KillBot extends AdvancedRobot{

    private Enemy targetEnemy; // alvo atual
    private ArrayList<BulletWave> activeWaves = new ArrayList<>(); // lista que armazena as ondas de tiro do inimigo
    private WaveSurfer waveSurfer;
    private MovementControl movementControl;
    private boolean inContactEvasion = false; // flag de controle de colisao
    private GameData gameData;
    private RadarControl radar;
    private HitRateTracker hitTracker;
    private WaveManager waveManager;
    private Aimer aimer;
    //private Movement move;

    public void run()
    {
        gameData = new GameData(this);
        hitTracker = new HitRateTracker();
        radar = new RadarControl(this);
        waveManager = new WaveManager(this, gameData);
        aimer = new Aimer(this, waveManager, gameData, hitTracker);
        movementControl = new MovementControl(this);
        waveSurfer = new WaveSurfer(this, activeWaves, movementControl);

        //sets iniciais padroes
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);

        //radar faz uma varredura
        radar.initialScan();

        while(true)
        {
            //continuamente roda a logica de busca
            radar.radarLogic();
            waveManager.updateWaves();
            aimer.shoot();
            aimer.decayBINS();
            // Se estamos em modo de fuga, mas já nos afastamos o suficiente, desativa a flag
            if (inContactEvasion && targetEnemy != null &&
                    AuxiliarFunctions.getDistance(getX(), getY(), targetEnemy.getX(),
                            targetEnemy.getY()) > MovementControl.MIN_DANGER_DISTANCE) {

                inContactEvasion = false;
                out.println("KillBot: Distância segura atingida. Saindo do modo de FUGA DE CONTATO.");
            }
            if (getRadarTurnRemaining() == 0) { // Se o radar não está girando por alguma ação
                setTurnRadarRight(360); // Gira 360 graus para escanear a área
            }

            // out.println("Ondas Ativas (Antes da remoção): " + activeWaves.size());
            for (int i = activeWaves.size() - 1; i >= 0; i--) {
                BulletWave wave = activeWaves.get(i);

                // a distancia da bala = (o tempo atual - o tempo que a bala foi disparada) *
                // velocidade da bala
                double bulletDistanceTraveled = ((getTime() - wave.getFireTime())) * wave.getBulletSpeed();

                if (AuxiliarFunctions.getDistance(wave.getOriginX(), wave.getOriginY(), getX(),
                        getY()) < bulletDistanceTraveled - 18) { // -18 para dar uma margem, já que o robô tem

                    // out.println(" Removendo Onda: Origem=(" + wave.getOriginX() + "," +
                    // wave.getOriginY() + ") Power="
                    // + wave.getBulletPower() + " FireTime=" + wave.getFireTime() + " (Passou do
                    // robo)");
                    // se a bala ja passou pelo robo ou o atingiu remove a wave
                    activeWaves.remove(i);
                }
            }
            // out.println("Ondas Ativas (Após a remoção): " + activeWaves.size());
            if (activeWaves.isEmpty() || targetEnemy == null) {
                movementControl.doStandardMovement();
            } else {
                waveSurfer.doWaveSurfing();
            }

            execute(); 
        }
    }

    public void onScannedRobot(ScannedRobotEvent e)
    {
        //chama a funcao de travar a mira assim que scaneia um robo
        radar.onScannedRobot(e);
        gameData.update(e);
          double oldEnergy = 0;
        if (targetEnemy != null && e.getName().equals(targetEnemy.getEnemyName())) { // caso ja tenha inimigo rastreado
            oldEnergy = targetEnemy.getLastEnemyEnergy();
        } else {
            targetEnemy = new Enemy(e.getName()); // Cria o inimigo antes de usar
            movementControl.setTargetEnemy(targetEnemy);
            oldEnergy = e.getEnergy() + 0.1;
            System.out.println("ONSCANNED: Novo inimigo: " + e.getName() + ". Energia inicial: " + e.getEnergy());

        }

        // Calcular a posicao do inimigo

        double absoluteBearingRadians = getHeadingRadians() + e.getBearingRadians();
        double enemyX = getX() + e.getDistance() * FasterCalcs.sin(absoluteBearingRadians);
        double enemyY = getY() + e.getDistance() * FasterCalcs.cos(absoluteBearingRadians);

        targetEnemy.updateEnemyData(enemyX, enemyY, e.getHeadingRadians(), e.getVelocity(), e.getEnergy(),
                getTime());


        double energyDrop = oldEnergy - targetEnemy.getLastEnemyEnergy();

        if (energyDrop > 0.2 && energyDrop <= 3.0 && targetEnemy.getLastEnemyEnergy() > 0) {
            double bulletDirectAngle = AuxiliarFunctions.absoluteBearing(targetEnemy.getX(), targetEnemy.getY(), getX(),
                    getY());

            // Calcula a direção do seu robô em relação ao inimigo.
            double angleOffset = Utils.normalRelativeAngle(absoluteBearingRadians - getHeadingRadians());
            int direction = (angleOffset > 0) ? 1 : -1;

            activeWaves.add(new BulletWave(targetEnemy.getX(), targetEnemy.getY(), getTime(), energyDrop,
                    bulletDirectAngle, direction));
            out.println("NOVA ONDA DETECTADA! Power: " + energyDrop + " - Turno: " + getTime());
        } else {
            out.println("ONSCANNED: Queda de energia não gerou onda. energyDrop: " + energyDrop);
        }
        out.println("Inimigo escaneado: " + e.getName() + ", Dist: " + e.getDistance() + ", Energia: " + e.getEnergy()
                + ", Queda Energia: " + energyDrop);

    }

    public void onBulletHit(BulletHitEvent e)
    {
        Point2D.Double impactLocation = new Point2D.Double(
            e.getBullet().getX(), e.getBullet().getY()
        );
        aimer.onBulletHit(impactLocation, e.getBullet().getPower());
    }

        public void onHitByBullet(HitByBulletEvent e) {
        out.println(
                "ROBÔ ATINGIDO! Bala Power: " + e.getPower() + ", Angulo: " + Math.toDegrees(e.getBearingRadians()));

        // Se não temos ondas ativas para analisar, não há o que aprender.
        if (activeWaves.isEmpty()) {
            return;
        }

        BulletWave hittingWave = null;
        double minDistance = Double.POSITIVE_INFINITY;

        // Encontra a onda que mais provavelmente atingiu, baseando-se no tempo
        // de voo da bala.
        for (int i = activeWaves.size() - 1; i >= 0; i--) {
            BulletWave wave = activeWaves.get(i);
            double bulletDistanceTraveled = (getTime() - wave.getFireTime()) * wave.getBulletSpeed();
            double distanceToWaveOrigin = AuxiliarFunctions.getDistance(wave.getOriginX(), wave.getOriginY(), getX(),
                    getY());
            double distanceDiff = Math.abs(bulletDistanceTraveled - distanceToWaveOrigin);

            // A onda "culpada" é aquela cuja distância percorrida pela bala bate
            // com a nossa distância da origem da onda.
            if (distanceDiff < minDistance) {
                minDistance = distanceDiff;
                hittingWave = wave;
            }
        }

        //  APRENDE COM A ONDA CULPADA 
        if (hittingWave != null) {
            out.println("  Analisando a onda que nos atingiu. FireTime=" + hittingWave.getFireTime());

            // --- INÍCIO DA LÓGICA DO GUESSFACTOR ---

            // Calcula o ângulo real do ponto de impacto em relação à origem da onda.
            double hitAngle = AuxiliarFunctions.absoluteBearing(hittingWave.getOriginX(), hittingWave.getOriginY(),
                    getX(), getY());

            // Calcula o "offset", ou seja, o quanto o inimigo ajustou a mira.
            double angleOffset = Utils.normalRelativeAngle(hitAngle - hittingWave.getDirectAngle());

            // Calcula o GuessFactor. É a razão entre o ângulo de offset e o ângulo máximo
            // de escape.
            // O `hittingWave.getDirection()` corrige a "imagem espelhada" do desvio.
            double guessFactor = Math.max(-1, Math.min(1, angleOffset / hittingWave.getMaxEscapeAngle()))
                    * hittingWave.getDirection();

            // Mapeia o GuessFactor (de -1 a 1) para um índice do nosso array (de 0 a 30).
            int binIndex = (int) Math.round(((guessFactor + 1) / 2) * (WaveSurfer.STATS_BINS.length - 1));

            // ATUALIZA A ESTATÍSTICA
            WaveSurfer.STATS_BINS[binIndex]++;

            out.println("  APRENDIZADO: Inimigo usou GuessFactor " + String.format("%.2f", guessFactor) +
                    ", atualizando bin #" + binIndex);

            // --- FIM DA LÓGICA DO GUESSFACTOR ---

            // Remove a onda que nos atingiu da lista de ondas ativas.
            activeWaves.remove(hittingWave);
        }
    }


    public void onHitWall(HitWallEvent event) {

    }

        public boolean isInContactEvasion() {
        return this.inContactEvasion;
    }

    public void onHitRobot(HitRobotEvent e) {
        out.println("ROBÔ COLIDIU COM " + e.getName() + "! Bearing: "
                + String.format("%.2f", Math.toDegrees(e.getBearing())) + "deg. ATIVANDO FUGA DE CONTATO.");
        inContactEvasion = true;
        setTurnRadarRight(360);
    }
}
