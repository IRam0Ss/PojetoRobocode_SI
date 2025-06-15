package killBot.gun;

/**
 * Uma classe utilitária simples para rastrear o número de tiros disparados e acertos.
 * É usada para calcular a taxa de acerto (hit rate), uma informação valiosa
 * para outras partes do robô, como o {@link BulletPowerSelector}, tomarem decisões.
 */
public class HitRateTracker {
    
    /** Contador para o número total de tiros que nosso robô disparou. */
    private int shotsFired = 0;
    /** Contador para o número total de tiros que atingiram o inimigo. */
    private int hits = 0;

    /**
     * Construtor padrão da classe.
     */
    public HitRateTracker() {
        // O construtor está vazio, pois os campos já são inicializados com 0.
    }

    /**
     * Incrementa o contador de tiros disparados.
     * Este método deve ser chamado toda vez que o robô efetua um disparo.
     */
    public void logShotFired()
    {
        shotsFired++;
    }

    /**
     * Incrementa o contador de acertos.
     * Este método deve ser chamado quando um evento {@code onBulletHit} ocorre.
     */
    public void logShotHit()
    {
        hits++;
    }

    /**
     * Calcula e retorna a taxa de acertos atual.
     * @return A taxa de acertos, representada como um double entre 0.0 e 1.0.
     * Retorna 0.0 se nenhum tiro foi disparado para evitar divisão por zero.
     */
    public double getHitRate()
    {
        if(shotsFired == 0)
        {
            return 0.0;
        }
        else
        {
            return (double) hits / shotsFired;
        }
    }

    /**
     * Retorna o número total de tiros disparados.
     * @return O total de tiros disparados.
     */
    public int getShots()
    {
        return shotsFired;
    }

    /**
     * Retorna o número total de acertos.
     * @return O total de tiros que atingiram o alvo.
     */
    public int getHits()
    {
        return hits;
    }

    /**
     * Reseta os contadores de tiros e acertos para zero.
     * Pode ser útil para reiniciar as estatísticas entre as rodadas de uma batalha.
     */
    public void reset()
    {
        shotsFired = 0;
        hits = 0;
    }
}