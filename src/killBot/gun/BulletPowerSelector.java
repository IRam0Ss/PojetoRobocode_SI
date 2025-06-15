package killBot.gun;

import robocode.Rules;

/**
 * Uma classe utilitária estática para selecionar dinamicamente a potência do tiro.
 * Utiliza um conjunto de heurísticas baseadas na situação atual da batalha
 * (energia, distância, taxa de acerto) para equilibrar a produção de dano
 * com a conservação de energia.
 */
public class BulletPowerSelector {

    /**
     * Calcula a melhor potência de tiro a ser usada com base em vários fatores do jogo.
     *
     * @param myEnergy A energia atual do nosso robô.
     * @param enemyEnergy A energia atual do robô inimigo.
     * @param distance A distância atual até o inimigo.
     * @param hitTracker O objeto que rastreia nossa taxa de acertos histórica.
     * @return A potência de tiro calculada, um valor geralmente entre 0.1 e 3.0.
     */
    public static double getBestPower(double myEnergy, double enemyEnergy, double distance, HitRateTracker hitTracker)
    {
        double finalPower;
        double currentHitRate = hitTracker.getHitRate();
        int hits = hitTracker.getHits();
        
        // --- Lógica 1: Tiro de Misericórdia (Kill Shot) ---
        // Se pudermos destruir o inimigo com um tiro, essa é a maior prioridade.
        double killShot = enemyEnergy / 4.0;
        if(myEnergy > killShot + 1 && killShot > 0.1 && killShot <= Rules.MAX_BULLET_POWER) {
            return killShot;
        }

        // --- Lógica 2: Conservação Extrema de Energia ---
        // Se nossa energia está muito baixa, atiramos o mínimo possível apenas para coletar dados.
        if(myEnergy < 15) {
            return 0.1;
        }

        // --- Lógica 3: Vantagem Energética Massiva ---
        // Se temos muito mais energia, podemos ser mais agressivos.
        if(myEnergy > enemyEnergy + 40)
        {
            // Se a mira está ruim, não adianta gastar muita energia.
            if(currentHitRate < 0.20 && hits > 7)
            {
                finalPower = currentHitRate * 10.0;
            }
            else
            {
                finalPower = Rules.MAX_BULLET_POWER; // Atira com força total.
            }
        }

        // --- Lógica 4: Desvantagem Energética ---
        // Se temos menos energia, jogamos de forma mais conservadora.
        else if(myEnergy < enemyEnergy)
        {
            // Se a mira está boa, usamos um tiro de potência média e confiável.
            if(currentHitRate > 0.35 && hits > 7)
            {
                finalPower = 1.99;
            }
            else
            {
                // Senão, calcula a potência com base na distância e na nossa "confiança" (ratio de energia).
                double confidenceFactor = myEnergy / enemyEnergy;
                double powerByDistance = 700 / distance;
                finalPower = powerByDistance * confidenceFactor;

                // Garante um tiro um pouco mais forte se estivermos muito perto.
                if(distance < 150)
                {
                    finalPower = Math.max(finalPower, 1.5);
                }
            }
        }

        // --- Lógica 5: Situação de Energia Equilibrada ---
        // A potência é baseada principalmente na distância.
        else
        {
            finalPower = 700 / distance;
        }

        // --- Finalização: Garante que a potência é válida ---
        // Garante que a potência não é menor que 0.1 e que não gastaremos energia a ponto de morrer.
        return Math.max(0.1, Math.min(finalPower, myEnergy - 0.1));
    }
}