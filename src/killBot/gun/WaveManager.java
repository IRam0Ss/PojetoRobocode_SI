package killBot.gun;

import killBot.data.*;
import killBot.utils.math.FasterCalcs;
import robocode.AdvancedRobot;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

/**
 * Gerencia as "ondas de tiro" (bullet waves) que se originam do nosso próprio robô.
 * Esta classe é fundamental para o funcionamento da mira GuessFactor. Quando um
 * tiro nosso atinge o inimigo, este gerenciador nos ajuda a identificar qual foi
 * o tiro exato, permitindo-nos calcular o GuessFactor daquele acerto e aprender com ele.
 */
public class WaveManager {
    
    /** Uma lista que armazena todas as ondas de tiro ativas que nosso robô disparou. */
    private List<Wave> waves = new ArrayList<>();
    /** Referência ao robô principal para obter dados como o tempo atual. */
    private AdvancedRobot robot;
    /** Objeto que contém os dados de estado do jogo. */
    private GameData gameData;

    /**
     * Construtor da classe WaveManager.
     * @param robot A instância do robô principal.
     * @param gameData O objeto de dados do jogo.
     */
    public WaveManager(AdvancedRobot robot, GameData gameData)
    {
        this.robot = robot;
        this.gameData = gameData;
    }

    /**
     * Cria e adiciona uma nova onda à lista cada vez que nosso robô atira.
     * A onda contém um "snapshot" da situação da batalha no momento do tiro.
     * @param power A potência do tiro disparado, usada para calcular a velocidade da onda.
     */
    public void addWave(double power)
    {
        Point2D.Double myLocation = gameData.myState.location;
        Point2D.Double enemyLocation = gameData.enemyState.location;

        double absoluteBearing = FasterCalcs.atan2(
            enemyLocation.x - myLocation.x,
            enemyLocation.y - myLocation.y
        );

        double enemyDirection = Math.signum(gameData.enemyState.velocity);
        
        Wave wave = new Wave(myLocation, power, robot.getTime(), (int) enemyDirection, absoluteBearing);
        waves.add(wave);
    }

    /**
     * Método de manutenção chamado a cada turno para limpar ondas antigas da lista.
     * Remove ondas que já passaram do inimigo ou saíram do campo de batalha para
     * economizar memória e processamento.
     */
    public void updateWaves()
    {
        Iterator<Wave> iterator = waves.iterator();
        while(iterator.hasNext())
        {
            Wave wave = iterator.next();

            // Remove a onda se seu raio já ultrapassou a localização do inimigo com uma margem.
            if(wave.getRadius(robot.getTime()) > wave.origin.distance(gameData.enemyState.location) + 50)
            {
                iterator.remove();
                continue;
            }

            // Remove a onda se ela viajou uma distância excessiva (provavelmente saiu do campo).
            if(wave.getRadius(robot.getTime()) > 1000)
            {
                iterator.remove();
            }
        }
    }

    /**
     * Encontra a onda de tiro específica que corresponde a um acerto de bala no inimigo.
     * A correspondência é feita comparando a distância percorrida pela onda com a
     * distância do ponto de impacto, para balas de mesma potência.
     *
     * @param impactLocation O local (x, y) onde a bala atingiu o inimigo.
     * @param bulletPower A potência da bala que acertou.
     * @param currentTime O tempo de jogo no momento do impacto.
     * @return O objeto Wave correspondente ao acerto, ou null se nenhuma correspondência for encontrada.
     */
    public Wave findCorrectWave(Point2D.Double impactLocation, double bulletPower, long currentTime)
    {
        Wave bestMatch = null;
        double minDeviation = Double.POSITIVE_INFINITY;

        for(Wave wave : waves)
        {
            // Compara apenas ondas com a mesma potência de tiro (e, portanto, mesma velocidade).
            if(Math.abs(wave.bulletPower - bulletPower) < 0.001)
            {
                double travelTime = (currentTime - wave.bulletTime);
                double waveRadius = travelTime * wave.bulletSpeed;
                double deviation = Math.abs(waveRadius - wave.origin.distance(impactLocation));

                // A onda correta é aquela com a menor diferença entre o raio e a distância do impacto.
                if(deviation < minDeviation)
                {
                    minDeviation = deviation;
                    bestMatch = wave;
                }
            }
        }

        return bestMatch;
    }
}