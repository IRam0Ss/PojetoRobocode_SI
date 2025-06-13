package utils.view;

import java.awt.Color;
import robocode.AdvancedRobot;

public class DefView {

    
    public static void setColors(AdvancedRobot bot){
        bot.setBodyColor(Color.black);
        bot.setGunColor(Color.blue);
        bot.setRadarColor(Color.yellow);
        bot.setBulletColor(Color.red);
        bot.setScanColor(Color.green);
    }

    public static void confirmDetect(AdvancedRobot bot){
        bot.setBodyColor(Color.yellow);
    }
    
}
