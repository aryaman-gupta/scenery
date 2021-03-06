package graphics.scenery.tests.examples.stresstests

import graphics.scenery.SceneryBase
import graphics.scenery.SceneryElement
import graphics.scenery.backends.Renderer
import graphics.scenery.utils.ExtractsNatives
import graphics.scenery.utils.LazyLogger
import graphics.scenery.utils.SystemHelpers
import org.junit.Test
import org.reflections.Reflections
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Experimental test runner that saves screenshots of all discovered tests.
 *
 * @author Ulrik Guenther <hello@ulrik.is>
 */
class ExampleRunner {
    val logger by LazyLogger()

    @Test fun runAllExamples() {
        val reflections = Reflections("graphics.scenery.tests")

        // blacklist contains examples that require user interaction or additional devices
        val blacklist = mutableListOf("LocalisationExample",
            "SwingTexturedCubeExample",
            "TexturedCubeJavaApplication",
            "XwingLiverExample",
            "VRControllerExample",
            "EyeTrackingExample",
            "ARExample",
            "ReaderExample",
            "BDVExample",
            "BigAndSmallVolumeExample",
            "FlybrainOutOfCoreExample",
            "RAIExample",
            "OutOfCoreRAIExample",
            "VolumeSamplingExample",
            "SwingTexturedCubeExample",
            "VideoRecordingExample"
        )

        blacklist.addAll(System.getProperty("scenery.ExampleRunner.Blacklist", "").split(","))

        // find all basic and advanced examples, exclude blacklist
        val examples = reflections
            .getSubTypesOf(SceneryBase::class.java)
            .filter { !it.canonicalName.contains("stresstests") && !it.canonicalName.contains("cluster") }
            .filter { !blacklist.contains(it.simpleName) }

        val rendererProperty = System.getProperty("scenery.Renderer")
        val renderers = rendererProperty?.split(",") ?: when(ExtractsNatives.getPlatform()) {
            ExtractsNatives.Platform.WINDOWS,
            ExtractsNatives.Platform.LINUX -> listOf("VulkanRenderer", "OpenGLRenderer")
            ExtractsNatives.Platform.MACOS -> listOf("OpenGLRenderer")
            ExtractsNatives.Platform.UNKNOWN -> { logger.error("Don't know what to do on this platform, sorry."); return }
        }

        val configurations = listOf("DeferredShading.yml", "DeferredShadingStereo.yml")

        val directoryName = System.getProperty("scenery.ExampleRunner.OutputDir") ?: "ExampleRunner-${SystemHelpers.formatDateTime(delimiter = "_")}"
        Files.createDirectory(Paths.get(directoryName))

        logger.info("ExampleRunner: Running ${examples.size} examples with ${configurations.size} configurations. Memory: ${Runtime.getRuntime().freeMemory().toFloat()/1024.0f/1024.0f}M/${Runtime.getRuntime().totalMemory().toFloat()/1024.0f/1024.0f}/${Runtime.getRuntime().maxMemory().toFloat()/1024.0f/1024.0f}M (free/total/max) available.")

        renderers.shuffled().forEach { renderer ->
            System.setProperty("scenery.Renderer", renderer)

            configurations.shuffled().forEach { config ->
                System.setProperty("scenery.Renderer.Config", config)
                val executor = Executors.newSingleThreadExecutor()
                val rendererDirectory = "$directoryName/$renderer-${config.substringBefore(".")}"
                Files.createDirectory(Paths.get(rendererDirectory))

                examples.shuffled().forEachIndexed { i, example ->
                    logger.info("Running ${example.simpleName} with $renderer ($i/${examples.size}) ...")
                    logger.info("Memory: ${Runtime.getRuntime().freeMemory().toFloat()/1024.0f/1024.0f}M/${Runtime.getRuntime().totalMemory().toFloat()/1024.0f/1024.0f}/${Runtime.getRuntime().maxMemory().toFloat()/1024.0f/1024.0f}M (free/total/max) available.")

                    if (!example.simpleName.contains("JavaFX")) {
                        System.setProperty("scenery.Headless", "true")
                    }

                    val instance = example.getConstructor().newInstance()
                    val future: Future<*>

                    try {
                        val exampleRunnable = Runnable {
                            instance.assertions[SceneryBase.AssertionCheckPoint.BeforeStart]?.forEach {
                                it.invoke()
                            }
                            instance.main()
                        }

                        future = executor.submit(exampleRunnable)

                        while (!instance.running || !instance.sceneInitialized() || instance.hub.get(SceneryElement.Renderer) == null) {
                            Thread.sleep(200)
                        }
                        val r = (instance.hub.get(SceneryElement.Renderer) as Renderer)

                        while(!r.firstImageReady) {
                            Thread.sleep(200)
                        }

                        r.screenshot("$rendererDirectory/${example.simpleName}.png")
                        Thread.sleep(2000)

                        logger.info("Sending close to ${example.simpleName}")
                        instance.close()
                        instance.assertions[SceneryBase.AssertionCheckPoint.AfterClose]?.forEach {
                            it.invoke()
                        }

                        while (instance.running) {
                            Thread.sleep(200)
                        }

                        future.get()
                    } catch (e: ThreadDeath) {
                        logger.info("JOGL threw ThreadDeath")
                    }

                    logger.info("${example.simpleName} closed ($renderer ran ${i + 1}/${examples.size} so far).")
                }
            }
        }
    }
}
