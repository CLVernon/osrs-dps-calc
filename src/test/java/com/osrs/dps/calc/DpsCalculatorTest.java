package com.osrs.dps.calc;

import com.osrs.dps.data.DataRepository;
import com.osrs.dps.model.AttackType;
import com.osrs.dps.model.EquipmentSlot;
import com.osrs.dps.model.Monster;
import com.osrs.dps.model.PlayerSetup;
import com.osrs.dps.model.Potion;
import com.osrs.dps.model.Prayer;
import com.osrs.dps.model.Stance;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DpsCalculatorTest {

    private static final DataRepository DATA = DataRepository.get();

    private static Monster dummyMonster(int def, int stab, int slash, int crush,
                                        int magicLevel, int magicDef,
                                        int heavy, int standard, int light) {
        Monster m = new Monster();
        m.id = -1;
        m.name = "Test dummy";
        m.skills.def = def;
        m.skills.magic = magicLevel;
        m.skills.hp = 100;
        m.defensive.stab = stab;
        m.defensive.slash = slash;
        m.defensive.crush = crush;
        m.defensive.magic = magicDef;
        m.defensive.heavy = heavy;
        m.defensive.standard = standard;
        m.defensive.light = light;
        return m;
    }

    private static PlayerSetup whipSetup() {
        PlayerSetup p = new PlayerSetup("Whip test");
        p.setEquipped(EquipmentSlot.WEAPON, DATA.findEquipment("Abyssal whip"));
        p.setAttackType(AttackType.SLASH);
        p.setStance(Stance.AGGRESSIVE);
        p.setPrayer(Prayer.PIETY);
        p.setPotion(Potion.SUPER_COMBAT);
        return p;
    }

    @Test
    void whipMaxHitWithPietyAndSuperCombat() {
        // visible str = 99 + 5 + floor(99*0.15) = 118; floor(118*1.23) = 145
        // effective = 145 + 3 (aggressive) + 8 = 156
        // max hit = floor((156*(82+64)+320)/640) = 36
        Monster m = dummyMonster(100, 50, 50, 50, 1, 0, 0, 0, 0);
        DpsResult r = DpsCalculator.calculate(whipSetup(), m);
        assertEquals(36, r.maxHit());
    }

    @Test
    void whipAccuracyAndDpsAgainstKnownDefence() {
        // eff attack = floor(118*1.20) + 0 + 8 = 149; roll = 149*(82+64) = 21754
        // def roll = (100+9)*(50+64) = 12426
        // accuracy = 1 - (12426+2)/(2*(21754+1)) = 0.7143644...
        Monster m = dummyMonster(100, 50, 50, 50, 1, 0, 0, 0, 0);
        DpsResult r = DpsCalculator.calculate(whipSetup(), m);
        assertEquals(0.7143644, r.accuracy(), 1e-6);
        // accurate zeros become 1s: E[hit] = (1 + 1 + 2 + ... + 36)/37 = 667/37
        double expectedAvg = 0.7143644 * (667.0 / 37.0);
        assertEquals(expectedAvg, r.avgDamagePerAttack(), 1e-4);
        assertEquals(4, r.attackSpeedTicks());
        assertEquals(expectedAvg / 2.4, r.dps(), 1e-4);
        // overkill-aware TTK must exceed the naive hp/dps estimate
        assertTrue(r.ttkSeconds() >= 100 / r.dps() - 1e-9);
    }

    @Test
    void slayerHelmBoostsMeleeOnTask() {
        Monster m = dummyMonster(100, 50, 50, 50, 1, 0, 0, 0, 0);
        PlayerSetup p = whipSetup();
        p.setEquipped(EquipmentSlot.HEAD, DATA.findEquipment("Slayer helmet (i)"));
        p.setOnSlayerTask(true);
        DpsResult r = DpsCalculator.calculate(p, m);
        // max hit 36 -> floor(36 * 7/6) = 42
        assertEquals(42, r.maxHit());
    }

    @Test
    void tridentOfTheSwampMaxHit() {
        // floor(99/3) - 2 = 31 base; occult +5% -> floor(31*1.05) = 32
        Monster m = dummyMonster(100, 0, 0, 0, 100, 30, 0, 0, 0);
        PlayerSetup p = new PlayerSetup("Trident test");
        p.setEquipped(EquipmentSlot.WEAPON, DATA.findEquipment("Trident of the swamp (Charged)"));
        p.setEquipped(EquipmentSlot.NECK, DATA.findEquipment("Occult necklace"));
        p.setAttackType(AttackType.MAGIC);
        p.setStance(Stance.ACCURATE);
        DpsResult r = DpsCalculator.calculate(p, m);
        assertEquals(32, r.maxHit());
        assertEquals(4, r.attackSpeedTicks());
    }

    @Test
    void tumekensShadowTriplesMagicBonuses() {
        // base = floor(99/3)+1 = 34; occult 5% tripled to 15% -> floor(34*1.15) = 39
        Monster m = dummyMonster(100, 0, 0, 0, 100, 30, 0, 0, 0);
        PlayerSetup p = new PlayerSetup("Shadow test");
        p.setEquipped(EquipmentSlot.WEAPON, DATA.findEquipment("Tumeken's shadow (Charged)"));
        p.setEquipped(EquipmentSlot.NECK, DATA.findEquipment("Occult necklace"));
        p.setAttackType(AttackType.MAGIC);
        p.setStance(Stance.ACCURATE);
        DpsResult r = DpsCalculator.calculate(p, m);
        assertEquals(39, r.maxHit());
    }

    @Test
    void fangRerollsAccuracy() {
        Monster m = dummyMonster(200, 100, 100, 100, 1, 0, 0, 0, 0);
        PlayerSetup fang = new PlayerSetup("Fang");
        fang.setEquipped(EquipmentSlot.WEAPON, DATA.findEquipment("Osmumten's fang"));
        fang.setAttackType(AttackType.STAB);
        fang.setStance(Stance.AGGRESSIVE);

        PlayerSetup plain = fang.copy();
        plain.setEquipped(EquipmentSlot.WEAPON, DATA.findEquipment("Abyssal whip"));
        plain.setAttackType(AttackType.SLASH);

        DpsResult fangResult = DpsCalculator.calculate(fang, m);
        DpsResult plainResult = DpsCalculator.calculate(plain, m);
        assertTrue(fangResult.accuracy() > plainResult.accuracy(),
                "fang accuracy should exceed a single-roll weapon on a high-defence target");
        assertTrue(fangResult.accuracy() <= 1.0);
    }

    @Test
    void scytheHitsThreeTimesOnLargeTargets() {
        Monster small = dummyMonster(100, 50, 50, 50, 1, 0, 0, 0, 0);
        small.size = 1;
        Monster large = dummyMonster(100, 50, 50, 50, 1, 0, 0, 0, 0);
        large.size = 5;

        PlayerSetup p = new PlayerSetup("Scythe");
        p.setEquipped(EquipmentSlot.WEAPON, DATA.findEquipment("Scythe of vitur (Charged)"));
        p.setAttackType(AttackType.SLASH);
        p.setStance(Stance.AGGRESSIVE);

        DpsResult vsSmall = DpsCalculator.calculate(p, small);
        DpsResult vsLarge = DpsCalculator.calculate(p, large);
        int maxHit = vsSmall.maxHit();
        assertEquals(maxHit + maxHit / 2 + maxHit / 4, vsLarge.maxHit());
        assertTrue(vsLarge.dps() > vsSmall.dps() * 1.6);
    }

    @Test
    void twistedBowScalesWithMonsterMagic() {
        Monster lowMagic = dummyMonster(100, 0, 0, 0, 1, 0, 50, 50, 50);
        Monster highMagic = dummyMonster(100, 0, 0, 0, 250, 0, 50, 50, 50);

        PlayerSetup p = new PlayerSetup("Tbow");
        p.setEquipped(EquipmentSlot.WEAPON, DATA.findEquipment("Twisted bow"));
        p.setEquipped(EquipmentSlot.AMMO, DATA.findEquipment("Dragon arrow (Unpoisoned)"));
        p.setAttackType(AttackType.RANGED);
        p.setStance(Stance.RAPID);
        p.setPrayer(Prayer.RIGOUR);
        p.setPotion(Potion.RANGING_POTION);

        DpsResult vsLow = DpsCalculator.calculate(p, lowMagic);
        DpsResult vsHigh = DpsCalculator.calculate(p, highMagic);
        assertTrue(vsHigh.maxHit() > vsLow.maxHit() * 2,
                "tbow vs 250 magic should more than double the max hit vs 1 magic");
        assertEquals(5, vsHigh.attackSpeedTicks(), "tbow on rapid is 5 ticks");
    }

    @Test
    void voidMeleeBoostsBothRolls() {
        Monster m = dummyMonster(100, 50, 50, 50, 1, 0, 0, 0, 0);
        PlayerSetup p = whipSetup();
        p.setEquipped(EquipmentSlot.HEAD, DATA.findEquipment("Void melee helm (Normal)"));
        p.setEquipped(EquipmentSlot.BODY, DATA.findEquipment("Void knight top (Normal)"));
        p.setEquipped(EquipmentSlot.LEGS, DATA.findEquipment("Void knight robe (Normal)"));
        p.setEquipped(EquipmentSlot.HANDS, DATA.findEquipment("Void knight gloves (Normal)"));
        DpsResult withVoid = DpsCalculator.calculate(p, m);
        DpsResult without = DpsCalculator.calculate(whipSetup(), m);
        // effective str 156 -> floor(156*1.1) = 171 -> max floor((171*146+320)/640) = 39
        assertEquals(39, withVoid.maxHit());
        assertTrue(withVoid.accuracy() > without.accuracy());
    }

    @Test
    void flatArmourReducesAverageDamage() {
        Monster armoured = dummyMonster(100, 50, 50, 50, 1, 0, 0, 0, 0);
        armoured.defensive.flatArmour = 5;
        Monster plain = dummyMonster(100, 50, 50, 50, 1, 0, 0, 0, 0);

        DpsResult vsArmoured = DpsCalculator.calculate(whipSetup(), armoured);
        DpsResult vsPlain = DpsCalculator.calculate(whipSetup(), plain);
        assertTrue(vsArmoured.avgDamagePerAttack() < vsPlain.avgDamagePerAttack());
    }

    @Test
    void kerisTripleHitVsKalphite() {
        Monster kalphite = dummyMonster(50, 30, 30, 30, 1, 0, 0, 0, 0);
        kalphite.attributes = List.of("kalphite");
        Monster plain = dummyMonster(50, 30, 30, 30, 1, 0, 0, 0, 0);

        PlayerSetup p = new PlayerSetup("Keris");
        p.setEquipped(EquipmentSlot.WEAPON, DATA.findEquipment("Keris partisan"));
        p.setAttackType(AttackType.STAB);
        p.setStance(Stance.AGGRESSIVE);

        DpsResult vsKalphite = DpsCalculator.calculate(p, kalphite);
        DpsResult vsPlain = DpsCalculator.calculate(p, plain);
        // 4/3 damage and 1/51 triple: max hit is 3x the boosted max
        assertTrue(vsKalphite.maxHit() >= vsPlain.maxHit() * 3);
        assertTrue(vsKalphite.avgDamagePerAttack() > vsPlain.avgDamagePerAttack() * 1.3);
    }

    @Test
    void vampyreTier3ImmuneWithoutBlisterwood() {
        Monster vampyre = dummyMonster(100, 50, 50, 50, 1, 0, 0, 0, 0);
        vampyre.attributes = List.of("vampyre3");
        DpsResult r = DpsCalculator.calculate(whipSetup(), vampyre);
        assertEquals(0, r.dps());
    }

    @Test
    void leafyImmuneWithoutLeafBladed() {
        Monster leafy = dummyMonster(100, 50, 50, 50, 1, 0, 0, 0, 0);
        leafy.attributes = List.of("leafy");
        assertEquals(0, DpsCalculator.calculate(whipSetup(), leafy).dps());

        PlayerSetup lbb = whipSetup();
        lbb.setEquipped(EquipmentSlot.WEAPON, DATA.findEquipment("Leaf-bladed battleaxe"));
        assertTrue(DpsCalculator.calculate(lbb, leafy).dps() > 0);
    }

    @Test
    void rubyBoltsBoostAverageDamageVsHighHp(){
        Monster bigHp = dummyMonster(100, 0, 0, 0, 1, 0, 50, 50, 50);
        bigHp.skills.hp = 500;

        PlayerSetup p = new PlayerSetup("RCB");
        p.setEquipped(EquipmentSlot.WEAPON, DATA.findEquipment("Rune crossbow"));
        p.setAttackType(AttackType.RANGED);
        p.setStance(Stance.RAPID);
        PlayerSetup withRuby = p.copy();
        withRuby.setEquipped(EquipmentSlot.AMMO, DATA.findEquipment("Ruby bolts (e)"));

        DpsResult without = DpsCalculator.calculate(p, bigHp);
        DpsResult with = DpsCalculator.calculate(withRuby, bigHp);
        assertTrue(with.avgDamagePerAttack() > without.avgDamagePerAttack());
        assertTrue(with.maxHit() >= 100, "ruby proc caps at 100 vs 500hp");
    }

    @Test
    void spellMaxHitsScaleWithLevel() {
        assertNotNull(DATA.findSpell("Fire Surge"));
        // Wind Strike at level 99 resolves to the Fire Strike tier (8)
        assertEquals(8, DATA.findSpell("Wind Strike").maxHitAtLevel(99, DATA.allSpells()));
    }

    @Test
    void manualCastDoesNotTripleShadowBonuses() {
        // Fire Surge base 24; occult +5%. Autocast on the shadow triples to +15%,
        // manual cast does not: floor(24*1.05)=25 vs floor(24*1.15)=27.
        Monster m = dummyMonster(100, 0, 0, 0, 100, 30, 0, 0, 0);
        PlayerSetup p = new PlayerSetup("Manual cast");
        p.setEquipped(EquipmentSlot.WEAPON, DATA.findEquipment("Tumeken's shadow (Charged)"));
        p.setEquipped(EquipmentSlot.NECK, DATA.findEquipment("Occult necklace"));
        p.setAttackType(AttackType.MAGIC);
        p.setSpell(DATA.findSpell("Fire Surge"));

        p.setStance(Stance.MANUAL_CAST);
        assertEquals(25, DpsCalculator.calculate(p, m).maxHit());
        assertEquals(5, DpsCalculator.calculate(p, m).attackSpeedTicks());

        p.setStance(Stance.AUTOCAST);
        assertEquals(27, DpsCalculator.calculate(p, m).maxHit());
    }

    @Test
    void salamanderBlazeMaxHit() {
        // Black salamander on the magic (Blaze) style: floor((99*(92+64)+320)/640) = 24
        Monster m = dummyMonster(100, 0, 0, 0, 100, 0, 0, 0, 0);
        PlayerSetup p = new PlayerSetup("Salamander");
        p.setEquipped(EquipmentSlot.WEAPON, DATA.findEquipment("Black salamander"));
        p.setAttackType(AttackType.MAGIC);
        p.setStance(Stance.DEFENSIVE);
        DpsResult r = DpsCalculator.calculate(p, m);
        assertEquals(24, r.maxHit());
    }

    @Test
    void potionListsMatchAttackType() {
        List<Potion> ranged = List.of(Potion.forAttackType(AttackType.RANGED));
        assertTrue(ranged.contains(Potion.RANGING_POTION));
        assertTrue(ranged.contains(Potion.BASTION_POTION));
        List<Potion> magic = List.of(Potion.forAttackType(AttackType.MAGIC));
        assertTrue(magic.contains(Potion.SATURATED_HEART));
        assertTrue(magic.contains(Potion.IMBUED_HEART));
        List<Potion> melee = List.of(Potion.forAttackType(AttackType.SLASH));
        assertTrue(melee.contains(Potion.SUPER_COMBAT));
    }

    @Test
    void magicWithoutSpellOrPoweredStaffIsZero() {
        Monster m = dummyMonster(100, 0, 0, 0, 100, 0, 0, 0, 0);
        PlayerSetup p = new PlayerSetup("No spell");
        p.setEquipped(EquipmentSlot.WEAPON, DATA.findEquipment("Abyssal whip"));
        p.setAttackType(AttackType.MAGIC);
        p.setStance(Stance.AUTOCAST);
        DpsResult r = DpsCalculator.calculate(p, m);
        assertEquals(0, r.dps());
    }

    @Test
    void bundledDataContainsExpectedEntries() {
        assertNotNull(DATA.findEquipment("Abyssal whip"));
        assertNotNull(DATA.findEquipment("Twisted bow"));
        assertNotNull(DATA.findMonster("Zulrah (Magma)"));
        assertTrue(DATA.allEquipment().size() > 5000);
        assertTrue(DATA.allMonsters().size() > 2500);
        assertTrue(DATA.allSpells().size() > 30);
    }
}
