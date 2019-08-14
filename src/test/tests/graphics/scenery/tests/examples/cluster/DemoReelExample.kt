package graphics.scenery.tests.examples.cluster

import cleargl.GLVector
import coremem.enums.NativeTypeEnum
import graphics.scenery.*
import graphics.scenery.backends.Renderer
import graphics.scenery.controls.InputHandler
import graphics.scenery.controls.TrackedStereoGlasses
import graphics.scenery.net.NodePublisher
import graphics.scenery.net.NodeSubscriber
import graphics.scenery.volumes.TransferFunction
import graphics.scenery.volumes.Volume
import org.junit.Test
import org.scijava.ui.behaviour.ClickBehaviour
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Demo reel example to be run on a CAVE system.
 *
 * @author Ulrik Günther <hello@ulrik.is>
 */
class DemoReelExample: SceneryBase("Demo Reel") {
    var hmd: TrackedStereoGlasses? = null
    var publishedNodes = ArrayList<Node>()

    var cam = DetachedHeadCamera()
    var bileScene = Mesh(name = "bile")
    var histoneScene = Mesh(name = "histone")
    var drosophilaScene = Mesh(name = "drosophila")
    var retinaScene = Mesh(name = "retina")

    lateinit var goto_scene_bile: ClickBehaviour
    lateinit var goto_scene_drosophila: ClickBehaviour
    lateinit var goto_scene_histone: ClickBehaviour
    lateinit var goto_scene_retina: ClickBehaviour

    override fun init() {
        logger.warn("*** WARNING - EXPERIMENTAL ***")
        logger.warn("This is an experimental example, which might need additional configuration on your computer")
        logger.warn("or might not work at all. You have been warned!")

        hmd = TrackedStereoGlasses("DTrack@10.1.2.201", screenConfig = "CAVEExample.yml")
        hub.add(SceneryElement.HMDInput, hmd!!)

        renderer = Renderer.createRenderer(hub, applicationName, scene, 2560, 1600)
        hub.add(SceneryElement.Renderer, renderer!!)
        settings.set("Renderer.HDR.Exposure", 5.0f)

        cam = DetachedHeadCamera(hmd)
        with(cam) {
//            position = GLVector(0.0f, -1.3190879f, 0.8841703f)
            position = GLVector(0.0f, 0.0f, 55.0f)
            perspectiveCamera(50.0f, 1.0f*windowWidth, 1.0f*windowHeight, 0.02f, 500.0f)
            active = true
            disableCulling = true

            scene.addChild(this)
        }

        // box setup

        val shell = Box(GLVector(120.0f, 120.0f, 120.0f), insideNormals = true)
        shell.material.cullingMode = Material.CullingMode.Front
        shell.material.diffuse = GLVector(0.0f, 0.0f, 0.0f)
        shell.material.specular = GLVector.getNullVector(3)
        shell.material.ambient = GLVector.getNullVector(3)
        //scene.addChild(shell)

        // lighting setup

        val lights = (0..4).map {
            PointLight(150.0f)
        }

        val tetrahedron = listOf(
            GLVector(1.0f, 0f, -1.0f/Math.sqrt(2.0).toFloat()),
            GLVector(-1.0f,0f,-1.0f/Math.sqrt(2.0).toFloat()),
            GLVector(0.0f,1.0f,1.0f/Math.sqrt(2.0).toFloat()),
            GLVector(0.0f,-1.0f,1.0f/Math.sqrt(2.0).toFloat()))

        tetrahedron.mapIndexed { i, position ->
            lights[i].position = position * 50.0f
            lights[i].emissionColor = GLVector(1.0f, 0.5f,0.3f) //Random.randomVectorFromRange(3, 0.2f, 0.8f)
            lights[i].intensity = 200.2f
            scene.addChild(lights[i])
        }

        // scene setup
        val driveLetter = System.getProperty("scenery.DriveLetter", "E")
        val volumes = HashMap<String, List<String>>()

        volumes.put(histoneScene.name, getVolumes("$driveLetter:/ssd-backup-inauguration/CAVE_DATA/histones-isonet/stacks/default/"))
        volumes.put(retinaScene.name, getVolumes("$driveLetter:/ssd-backup-inauguration/CAVE_DATA/retina_test2/"))
        volumes.put(drosophilaScene.name, getVolumes("$driveLetter:/ssd-backup-inauguration/CAVE_DATA/droso-royer-autopilot-transposed/"))
        volumes.put(retinaScene.name, getVolumes("$driveLetter:/ssd-backup-inauguration/CAVE_DATA/retina_test2/"))

        val histoneVolume = Volume()
        histoneVolume.transferFunction = TransferFunction.ramp(0.3f, 0.8f)
        histoneVolume.trangemax = 3000.0f
        histoneVolume.renderingMethod = 2
        histoneVolume.deallocationThreshold = 50000
        histoneVolume.colormap = "viridis"
        histoneScene.addChild(histoneVolume)
        histoneScene.visible = false
        scene.addChild(histoneScene)

        val drosophilaVolume = Volume()
        drosophilaVolume.rotation.rotateByAngleX(1.57f)
        drosophilaVolume.renderingMethod = 2
        drosophilaVolume.transferFunction = TransferFunction.ramp(0.15f, 1.0f)
        drosophilaVolume.trangemin = 5.0f
        drosophilaVolume.trangemax = 500.0f
        drosophilaVolume.stepSize = 0.005f
        drosophilaVolume.deallocationThreshold = 50000
        drosophilaVolume.colormap = "hot"
        drosophilaScene.addChild(drosophilaVolume)
        drosophilaScene.visible = false
        scene.addChild(drosophilaScene)

        val retinaVolume = Volume()
        retinaScene.addChild(retinaVolume)
        retinaScene.visible = false
        scene.addChild(retinaScene)

        val bile = Mesh()
        bile.position = GLVector(0.0f, 0.0f, -10.0f)
        val canaliculi = Mesh()
        canaliculi.readFrom("$driveLetter:/ssd-backup-inauguration/meshes/bile-canaliculi.obj")
        canaliculi.scale = GLVector(0.1f, 0.1f, 0.1f)
        canaliculi.position = GLVector(-80.0f, -60.0f, 10.0f)
        canaliculi.material.diffuse = GLVector(0.5f, 0.7f, 0.1f)
        bile.addChild(canaliculi)

        val nuclei = Mesh()
        nuclei.readFrom("$driveLetter:/ssd-backup-inauguration/meshes/bile-nuclei.obj")
        nuclei.scale = GLVector(0.1f, 0.1f, 0.1f)
        nuclei.position = GLVector(-80.0f, -60.0f, 10.0f)
        nuclei.material.diffuse = GLVector(0.8f, 0.8f, 0.8f)
        bile.addChild(nuclei)

        val sinusoidal = Mesh()
        sinusoidal.readFrom("$driveLetter:/ssd-backup-inauguration/meshes/bile-sinus.obj")
        sinusoidal.scale = GLVector(0.1f, 0.1f, 0.1f)
        sinusoidal.position = GLVector(-80.0f, -60.0f, 10.0f)
        sinusoidal.material.ambient = GLVector(0.1f, 0.0f, 0.0f)
        sinusoidal.material.diffuse = GLVector(0.4f, 0.0f, 0.02f)
        sinusoidal.material.specular = GLVector(0.05f, 0f, 0f)
        bile.addChild(sinusoidal)
        bileScene.addChild(bile)
        scene.addChild(bileScene)

        publishedNodes.add(cam)
        publishedNodes.add(drosophilaVolume)
        publishedNodes.add(drosophilaScene)

        publishedNodes.add(histoneVolume)
        publishedNodes.add(histoneScene)

        publishedNodes.add(bile)
        publishedNodes.add(canaliculi)
        publishedNodes.add(nuclei)
        publishedNodes.add(sinusoidal)
        publishedNodes.add(bileScene)

        val publisher = hub.get<NodePublisher>(SceneryElement.NodePublisher)
        val subscriber = hub.get<NodeSubscriber>(SceneryElement.NodeSubscriber)

        publishedNodes.forEachIndexed { index, node ->
            publisher?.nodes?.put(13337 + index, node)

            subscriber?.nodes?.put(13337 + index, node)
        }

        val min_delay = 200

        scene.metadata["DrosophilaSpeed"] = 40
        scene.metadata["HistoneSpeed"] = 30

        logger.info("Publisher is: $publisher")
        if(publisher != null) {
            thread {
                while (!scene.initialized) {
                    logger.info("Wainting for scene init")
                    Thread.sleep(1000)
                }

                while (true) {
                    var sleepDuration = 50L


                    arrayOf(drosophilaScene, histoneScene).forEach {
                        if(it.visible) {
                            logger.info("Reading next volume for ${it.name} ...")
                            val start = System.currentTimeMillis()

                            val v = it.children[0]
                            if(v is Volume && volumes.containsKey(it.name)) {
                                v.nextVolume(volumes[it.name]!!)

                                val time_to_read  = System.currentTimeMillis()-start

                                if(it.name == "drosophila") {
                                    sleepDuration = Math.max((scene.metadata["DrosophilaSpeed"] as? Int ?: 40).toLong(), min_delay-time_to_read)

                                    with(v) {
                                        //trangemin = 0.00f
                                        //trangemax = 1024.0f
                                        //alphaBlending = 0.5f
                                        scale = GLVector(1.0f, 1.0f, 1.0f)
                                        voxelSizeX = 1.0f
                                        voxelSizeY = 5.0f
                                        voxelSizeZ = 1.0f
                                    }
                                }

                                if(it.name == "histone") {
                                    sleepDuration = Math.max((scene.metadata["HistoneSpeed"] as? Int ?: 30).toLong(),min_delay-time_to_read)

                                    with(v) {
                                        //trangemin = 0.0f
                                        //trangemax = 255.0f
                                        //alphaBlending = 0.2f
                                        scale = GLVector(1.0f, 1.0f, 1.0f)
                                        voxelSizeX = 1.0f
                                        voxelSizeY = 1.0f
                                        voxelSizeZ = 1.0f
                                    }
                                }
                            }



                        }
                    }

//                    logger.info("Sleeping for $sleepDuration")
                    // sleep if no volume is active
                    Thread.sleep(sleepDuration)
                }
            }

        }

        thread {
            logger.info("Preloading volumes")
            volumes["histone"]?.map { histoneVolume.preloadRawFromPath(Paths.get(it), dataType = NativeTypeEnum.UnsignedShort) }
            volumes["drosophila"]?.map { drosophilaVolume.preloadRawFromPath(Paths.get(it), dataType = NativeTypeEnum.UnsignedShort) }
        }
    }

    /**
     * Returns all the RAW volumes stored in [path] as a list of strings.
     */
    fun getVolumes(path: String): List<String> {
        val folder = File(path)
        val files = folder.listFiles()
        val volumes = files.filter { it.isFile && it.name.endsWith("raw") }.map { it.absolutePath }.sorted()

        volumes.forEach { logger.info("Volume: $it")}

        return volumes
    }

    /**
     * Switches to the next volume, and returns its name.
     */
    fun Volume.nextVolume(volumes: List<String>): String {
        var curr = if (volumes.indexOf(this.currentVolume) == -1) {
            0
        } else {
            volumes.indexOf(this.currentVolume)
        }

        if(curr+1 == volumes.size) {
            curr = 0
        }

        val v = volumes[curr+1 % volumes.size]

//        if(this.lock.tryLock(2, TimeUnit.MILLISECONDS)) {
            this.currentVolume = v
//        } else {
//            logger.warn("Failed to advance to $v")
//        }

        return v
    }

    /**
     * Shows this [Node] and all children.
     */
    fun Node.showAll() {
        this.children.map { visible = true }
        this.visible = true
    }

    /**
     * Hides this [Node] and all children.
     */
    fun Node.hideAll() {
        this.children.map { visible = false }
        this.visible = false
    }

    /**
     * Standard input setup, plus additional key bindings to
     * switch scenes.
     */
    override fun inputSetup() {
        setupCameraModeSwitching(keybinding = "C")
        val inputHandler = (hub.get(SceneryElement.Input) as? InputHandler) ?: return
        var activeScene = bileScene

        goto_scene_bile = ClickBehaviour { _, _ ->
            bileScene.showAll()
            histoneScene.hideAll()
            drosophilaScene.hideAll()
            activeScene = bileScene

            scene.findObserver()?.position = GLVector(0.0f, 0.0f, 3.0f)
        }

        goto_scene_histone = ClickBehaviour { _, _ ->
            bileScene.hideAll()
            histoneScene.showAll()
            drosophilaScene.hideAll()
            activeScene = histoneScene

            with(histoneScene.children[0] as Volume) {
                trangemin = 0.5f
                trangemax = 2500.0f
                alphaBlending = 0.2f
                stepSize = 0.005f
                scale = GLVector(1.0f, 1.0f, 1.0f)
                voxelSizeX = 1.0f
                voxelSizeY = 1.0f
                voxelSizeZ = 1.0f
            }


            scene.findObserver()?.position = GLVector(0.0f, 0.0f, 3.0f)
        }

        goto_scene_drosophila = ClickBehaviour { _, _ ->
            bileScene.hideAll()
            histoneScene.hideAll()
            drosophilaScene.showAll()
            activeScene = drosophilaScene

            with(drosophilaScene.children[0] as Volume) {
                trangemin = 5.0f
                trangemax = 500.0f
                alphaBlending = 0.005f
                scale = GLVector(1.0f, 1.0f, 1.0f)
                voxelSizeX = 1.0f
                voxelSizeY = 5.0f
                voxelSizeZ = 1.0f
            }

            scene.findObserver()?.position = GLVector(0.0f, 0.0f, 4.0f)

        }

        goto_scene_retina = ClickBehaviour { _, _ ->
            bileScene.hideAll()
            histoneScene.hideAll()
            drosophilaScene.hideAll()
            retinaScene.showAll()
            activeScene = retinaScene

            with(retinaScene.children[0] as Volume) {
                trangemin = 0.00f
                trangemax = 255.0f
                alphaBlending = 0.01f
                scale = GLVector(1.0f, 1.0f, 1.0f)
                voxelSizeX = 1.0f
                voxelSizeY = 1.0f
                voxelSizeZ = 5.0f
            }

            //scene.findObserver().position = GLVector(-0.16273244f, -0.85279214f, 1.0995241f)
            scene.findObserver()?.position = GLVector(0.0f,-1.1f, 2.0f)
        }

        val rotateLeft = ClickBehaviour { _, _ ->
            activeScene.rotation = activeScene.rotation.rotateByAngleY(0.05f)
        }

        val rotateRight = ClickBehaviour { _, _ ->
            activeScene.rotation = activeScene.rotation.rotateByAngleY(-0.05f)
        }

        val rotateUp = ClickBehaviour { _, _ ->
            activeScene.rotation = activeScene.rotation.rotateByAngleX(-0.05f)
        }

        val rotateDown = ClickBehaviour { _, _ ->
            activeScene.rotation = activeScene.rotation.rotateByAngleX(0.05f)
        }

        val faster = ClickBehaviour { _, _ ->
            if(activeScene == drosophilaScene) {
                scene.metadata["DrosophilaSpeed"] = Math.max(scene.metadata["DrosophilaSpeed"] as Int/2, 10)
            }

            if(activeScene == histoneScene) {
                scene.metadata["HistoneSpeed"] = Math.max(scene.metadata["HistoneSpeed"] as Int/2, 10)
            }
        }

        val slower = ClickBehaviour { _, _ ->
            if(activeScene == drosophilaScene) {
                scene.metadata["DrosophilaSpeed"] = Math.min(scene.metadata["DrosophilaSpeed"] as Int*2, 500000)
            }

            if(activeScene == histoneScene) {
                scene.metadata["HistoneSpeed"] = Math.min(scene.metadata["HistoneSpeed"] as Int*2, 500000)
            }
        }

        inputHandler.addBehaviour("goto_scene_bile", goto_scene_bile)
        inputHandler.addBehaviour("goto_scene_histone", goto_scene_histone)
        inputHandler.addBehaviour("goto_scene_drosophila", goto_scene_drosophila)
        inputHandler.addBehaviour("goto_scene_retina", goto_scene_retina)
        inputHandler.addBehaviour("rotate_left", rotateLeft)
        inputHandler.addBehaviour("rotate_right", rotateRight)
        inputHandler.addBehaviour("rotate_up", rotateUp)
        inputHandler.addBehaviour("rotate_down", rotateDown)
        inputHandler.addBehaviour("faster", faster)
        inputHandler.addBehaviour("slower", slower)


        inputHandler.addKeyBinding("goto_scene_bile", "shift 1")
        inputHandler.addKeyBinding("goto_scene_histone", "shift 2")
        inputHandler.addKeyBinding("goto_scene_drosophila", "shift 3")
        inputHandler.addKeyBinding("goto_scene_retina", "shift 4")
        inputHandler.addKeyBinding("rotate_left", "J")
        inputHandler.addKeyBinding("rotate_right", "L")
        inputHandler.addKeyBinding("rotate_up", "I")
        inputHandler.addKeyBinding("rotate_down", "K")
        inputHandler.addKeyBinding("faster", "O")
        inputHandler.addKeyBinding("slower", "U")
    }

    @Test override fun main() {
        super.main()
    }
}
