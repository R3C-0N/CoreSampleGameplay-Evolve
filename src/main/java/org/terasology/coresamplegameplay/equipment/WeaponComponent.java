// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.equipment;

import org.terasology.engine.network.Replicate;
import org.terasology.gestalt.entitysystem.component.Component;

/**
 * What kind of weapon an item is. Classes pick weapons by type, never by item; nothing reads this yet.
 * <p>
 * Types and the classes they serve:
 * <ul>
 *     <li>{@code oneHandedSword} — Champion, Paladin, Soldier, Monster Hunter</li>
 *     <li>{@code shield} — Champion, Paladin, Soldier</li>
 *     <li>{@code warHammer} — Warrior Priest, Paladin</li>
 *     <li>{@code club} — Berserker, Soldier</li>
 *     <li>{@code windBlade} — Wind Blade</li>
 *     <li>{@code bow} — Archer</li>
 *     <li>{@code handCrossbow} — Thief, Ranger (one per hand), Monster Hunter</li>
 *     <li>{@code engineerKit} — Engineer</li>
 *     <li>{@code healingScepter} — Chaplain, Warrior Priest</li>
 *     <li>{@code elementalStaff} — Elemental Sorcerer</li>
 *     <li>{@code druidStaff} — Druid</li>
 *     <li>from flint on: {@code necromancerFocus} — Necromancer, {@code battleAxe} — Berserker and
 *     Soldier, {@code dagger} and {@code spear} — Soldier</li>
 * </ul>
 * A weapon strengthens a class, it never unlocks it: every class casts its powers bare-handed.
 */
public class WeaponComponent implements Component<WeaponComponent> {
    @Replicate
    public String type;

    @Replicate
    public int hands = 1;

    @Override
    public void copyFrom(WeaponComponent other) {
        this.type = other.type;
        this.hands = other.hands;
    }
}
