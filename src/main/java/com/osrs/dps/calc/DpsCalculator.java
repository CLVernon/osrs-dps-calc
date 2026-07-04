package com.osrs.dps.calc;

import com.osrs.dps.model.Monster;
import com.osrs.dps.model.PlayerSetup;

/** Entry point for DPS calculations. */
public final class DpsCalculator {

    private DpsCalculator() {
    }

    public static DpsResult calculate(PlayerSetup player, Monster monster) {
        return PlayerVsNpcCalc.calculate(player, monster);
    }
}
