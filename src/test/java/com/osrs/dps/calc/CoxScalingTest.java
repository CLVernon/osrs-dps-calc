package com.osrs.dps.calc;

import com.osrs.dps.model.Monster;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CoxScalingTest {

    private static Monster xerician(int id) {
        Monster m = new Monster();
        m.id = id;
        m.name = "Test CoX monster";
        m.attributes = List.of("xerician");
        m.skills.atk = 250;
        m.skills.str = 250;
        m.skills.ranged = 250;
        m.skills.magic = 250;
        m.skills.def = 150;
        m.skills.hp = 300;
        return m;
    }

    @Test
    void nonXericianMonstersAreUntouched() {
        Monster m = new Monster();
        m.name = "Zulrah";
        m.partySize = 5;
        assertSame(m, CoxScaling.scale(m));
    }

    @Test
    void soloAtMaxStatsKeepsBaseValues() {
        Monster m = xerician(999_001);
        m.partySize = 1;
        Monster scaled = CoxScaling.scale(m);
        // highestHp=99 -> stats x1; combat 126 -> hp x1; party 1 -> no size bonus
        assertEquals(250, scaled.skills.atk);
        assertEquals(150, scaled.skills.def);
        // hp += hp * trunc(1*50/100) = +0
        assertEquals(300, scaled.skills.hp);
    }

    @Test
    void fivePlayerScalingMatchesFormulas() {
        Monster m = xerician(999_001);
        m.partySize = 5;
        Monster scaled = CoxScaling.scale(m);
        // offensive: 250 * (100 + isqrt(4)*7 + 4)/100 = 250*118/100 = 295
        assertEquals(295, scaled.skills.atk);
        assertEquals(295, scaled.skills.magic);
        // defensive: 150 * (100 + isqrt(4) + trunc(4*7/10))/100 = 150*104/100 = 156
        assertEquals(156, scaled.skills.def);
        // hp: 300 + 300*trunc(5*50/100) = 900
        assertEquals(900, scaled.skills.hp);
    }

    @Test
    void challengeModeAddsFiftyPercent() {
        Monster m = xerician(999_001);
        m.partySize = 5;
        m.coxChallengeMode = true;
        Monster scaled = CoxScaling.scale(m);
        assertEquals(295 + 295 * 50 / 100, scaled.skills.atk); // 442
        assertEquals(156 + 156 * 50 / 100, scaled.skills.def); // 234
        assertEquals(900 + 900 * 50 / 100, scaled.skills.hp); // 1350
    }

    @Test
    void tektonCmDefenceIsSpecialCased() {
        Monster tekton = xerician(7540);
        tekton.skills.magic = 205;
        tekton.skills.def = 205;
        tekton.partySize = 5;
        tekton.coxChallengeMode = true;
        Monster scaled = CoxScaling.scale(tekton);
        // magic is defensive for Tekton; base defensive = 205
        // def: 205*104/100 = 213; CM party>=4: +35% -> 213+74 = 287
        assertEquals(287, scaled.skills.def);
        assertEquals(287, scaled.skills.magic);
    }

    @Test
    void olmHeadHpUsesPhaseFormula() {
        Monster olmHead = xerician(7551);
        olmHead.partySize = 5;
        Monster scaled = CoxScaling.scale(olmHead);
        // factor = min(4,50) - 3*trunc(min(5,50)/8) = 4; hp = 800 + 400*4 = 2400
        assertEquals(2400, scaled.skills.hp);
    }

    @Test
    void olmMageHandHalvesMagicAndUsesHandHp() {
        Monster mageHand = xerician(7550);
        mageHand.partySize = 3;
        Monster scaled = CoxScaling.scale(mageHand);
        // factor = 2 - 0 = 2; hp = 600 + 300*2 = 1200
        assertEquals(1200, scaled.skills.hp);
        // magic is defensive for hands: 250*102/100=255, then halved = 127
        assertEquals(127, scaled.skills.magic);
    }

    @Test
    void guardiansHpScalesWithMining() {
        Monster guardian = xerician(7569);
        guardian.partySize = 2;
        guardian.partyAvgMiningLevel = 99;
        Monster scaled = CoxScaling.scale(guardian);
        // baseHp = 151 + trunc(198/2) = 250; hp = 250*126/126 = 250; +250*trunc(2*50/100)=+250 -> 500
        assertEquals(500, scaled.skills.hp);
    }
}
