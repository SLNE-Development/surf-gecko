package dev.slne.surf.gecko.server.duck;

import net.minestom.server.coordinate.Point;
import org.jetbrains.annotations.Nullable;

public interface EntityTrackerEntryDuck {

  @Nullable
  Point surf$lastPosition();
}