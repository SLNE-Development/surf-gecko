package dev.slne.surf.gecko.server.duck;

import java.util.Set;
import net.minestom.server.entity.Player;

public interface ConnectionManagerDuck {

  Set<Player> surf$configurationPlayers();

  Set<Player> surf$keepAlivePlayers();
}