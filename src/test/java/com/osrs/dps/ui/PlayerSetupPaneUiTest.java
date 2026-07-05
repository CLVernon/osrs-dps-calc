package com.osrs.dps.ui;

import com.osrs.dps.data.DataRepository;
import com.osrs.dps.model.CombatStyle;
import com.osrs.dps.model.EquipmentSlot;
import com.osrs.dps.model.PlayerCharacter;
import com.osrs.dps.model.PlayerSetup;
import com.osrs.dps.model.Potion;
import com.osrs.dps.model.Prayer;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UI smoke test that drives the real combo listeners on the JavaFX thread.
 * Regression test for the listener recursion that crashed the UI thread with
 * a StackOverflowError whenever gear or combat style changed.
 */
class PlayerSetupPaneUiTest {

    @BeforeAll
    static void initToolkit() {
        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException alreadyStarted) {
            // toolkit already running
        }
    }

    /** Runs on the FX thread, capturing any thrown error (incl. StackOverflowError). */
    private static Throwable onFxThread(Runnable action) throws InterruptedException {
        AtomicReference<Throwable> thrown = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                thrown.set(t);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(30, TimeUnit.SECONDS), "FX action timed out");
        return thrown.get();
    }

    private static void collectComboBoxes(Node node, List<ComboBox<?>> out) {
        if (node instanceof ComboBox<?> combo) {
            out.add(combo);
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collectComboBoxes(child, out);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> ComboBox<T> comboWithItemType(List<ComboBox<?>> combos, Class<T> type) {
        for (ComboBox<?> combo : combos) {
            if (!combo.getItems().isEmpty() && type.isInstance(combo.getItems().get(0))) {
                return (ComboBox<T>) combo;
            }
        }
        throw new AssertionError("No ComboBox with items of type " + type.getSimpleName());
    }

    @Test
    void gearAndStyleChangesDoNotRecurse() throws Exception {
        DataRepository data = DataRepository.get();

        Throwable error = onFxThread(() -> {
            PlayerSetupPane pane = new PlayerSetupPane(() -> {
            });
            PlayerSetup setup = new PlayerSetup("ui test");
            setup.setCharacter(new PlayerCharacter());
            pane.setSetup(setup);

            List<ComboBox<?>> combos = new ArrayList<>();
            collectComboBoxes(pane, combos);
            ComboBox<CombatStyle> style = comboWithItemType(combos, CombatStyle.class);
            ComboBox<Prayer> prayer = comboWithItemType(combos, Prayer.class);
            ComboBox<Potion> potion = comboWithItemType(combos, Potion.class);

            // Equip a weapon, then drive the listeners the way the UI does
            setup.setEquipped(EquipmentSlot.WEAPON, data.findEquipment("Abyssal whip"));
            pane.setSetup(setup);
            style.setValue(style.getItems().get(1)); // Lash
            prayer.setValue(Prayer.PIETY);
            potion.setValue(Potion.SUPER_COMBAT);

            // Swap to a ranged weapon: style list changes, prayers/potions re-filter
            setup.setEquipped(EquipmentSlot.WEAPON, data.findEquipment("Twisted bow"));
            pane.setSetup(setup);
            style.setValue(style.getItems().get(1)); // Rapid
            prayer.setValue(Prayer.RIGOUR);
            potion.setValue(Potion.RANGING_POTION);

            // Swap to a powered staff and pick a magic style
            setup.setEquipped(EquipmentSlot.WEAPON,
                    data.findEquipment("Tumeken's shadow (Charged)"));
            pane.setSetup(setup);
            style.setValue(style.getItems().get(0)); // Accurate (magic)
            prayer.setValue(Prayer.AUGURY);
        });

        assertNull(error, () -> "UI interaction threw: " + error);
    }
}
