package dev.slne.surf.gecko.server.config

import org.spongepowered.configurate.kotlin.dataClassFieldDiscoverer
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.objectmapping.ObjectMapper
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolute

class ConfigLoader(private val path: Path) {
    private val loader = YamlConfigurationLoader.builder()
        .path(path)
        .indent(2)
        .nodeStyle(NodeStyle.BLOCK)
        .defaultOptions { options ->
            options.serializers { serializers ->
                serializers.registerAnnotatedObjects(
                    ObjectMapper.factoryBuilder()
                        .addDiscoverer(dataClassFieldDiscoverer())
                        .build()
                )
            }
        }
        .build()

    fun load(): Config {
        path.parent?.let(Files::createDirectories)

        val fileExisted = Files.exists(path)
        val root = loader.load()

        val config = if (fileExisted) {
            root.get<Config>()
                ?: error("Could not deserialize configuration at ${path.absolute()}")
        } else {
            Config()
        }

        root.set(config)
        loader.save(root)

        return config
    }
}