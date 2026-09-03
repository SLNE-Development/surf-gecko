package dev.slne.surf.gecko.server.instrumentation;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import dev.slne.surf.gecko.server.instrumentation.mixin.InstrumentationMixinService;
import java.lang.instrument.Instrumentation;
import me.lucko.luckperms.minestom.dependencies.LuckPermsAgent;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

@NullMarked
public final class GeckoRunner {

  private GeckoRunner() {
  }

  public static void runnerMain(String agentArgs, Instrumentation instrumentation) {
    try {
      DependencyInstaller.install(instrumentation);

      InstrumentationMixinService.setInstrumentation(instrumentation);
      MixinBootstrap.init();
      MixinExtrasBootstrap.init();
      Mixins.addConfiguration("mixins.surf-gecko.json");
      advanceMixinPhases();

      LuckPermsAgent.agentmain(agentArgs, instrumentation);
    } catch (Throwable e) {
      e.printStackTrace();
      throw new RuntimeException("Failed to bootstrap surf gecko instrumentation", e);
    }
  }

  private static void advanceMixinPhases() {
    try {
      var gotoPhase = MixinEnvironment.class.getDeclaredMethod(
          "gotoPhase",
          MixinEnvironment.Phase.class
      );

      gotoPhase.setAccessible(true);

      gotoPhase.invoke(null, MixinEnvironment.Phase.INIT);
      gotoPhase.invoke(null, MixinEnvironment.Phase.DEFAULT);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException("Failed to advance Mixin phases", e);
    }
  }
}
