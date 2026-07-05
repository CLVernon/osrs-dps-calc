package com.osrs.dps.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeaponStylesTest {

    @Test
    void whipOnlyHasSlashStyles() {
        List<CombatStyle> styles = WeaponStyles.forCategory("Whip");
        assertEquals(4, styles.size()); // Flick, Lash, Deflect + manual cast
        assertTrue(styles.stream()
                .filter(s -> s.stance() != Stance.MANUAL_CAST)
                .allMatch(s -> s.type() == AttackType.SLASH));
        assertEquals("Flick", styles.get(0).name());
        assertEquals(Stance.CONTROLLED, styles.get(1).stance());
    }

    @Test
    void stabSwordHasSlashOption() {
        List<CombatStyle> styles = WeaponStyles.forCategory("Stab Sword");
        assertTrue(styles.stream().anyMatch(
                s -> s.type() == AttackType.SLASH && "Slash".equals(s.name())));
        assertEquals(AttackType.STAB, styles.get(0).type());
    }

    @Test
    void unarmedFallsBackToPunchKickBlock() {
        List<CombatStyle> styles = WeaponStyles.forWeapon(null);
        assertEquals("Punch", styles.get(0).name());
        assertEquals(AttackType.CRUSH, styles.get(0).type());
    }

    @Test
    void staffHasAutocastStyles() {
        List<CombatStyle> styles = WeaponStyles.forCategory("Staff");
        assertTrue(styles.stream().anyMatch(s -> s.stance() == Stance.AUTOCAST));
        assertTrue(styles.stream().anyMatch(s -> s.stance() == Stance.DEFENSIVE_AUTOCAST));
        assertTrue(styles.stream().anyMatch(s -> s.stance() == Stance.MANUAL_CAST));
    }

    @Test
    void styleSelectionSetsTypeAndStance() {
        PlayerSetup setup = new PlayerSetup("test");
        CombatStyle lash = WeaponStyles.forCategory("Whip").get(1);
        setup.setCombatStyle(lash);
        assertEquals(AttackType.SLASH, setup.getAttackType());
        assertEquals(Stance.CONTROLLED, setup.getStance());
        assertEquals("Lash", setup.getStyleName());
    }
}
