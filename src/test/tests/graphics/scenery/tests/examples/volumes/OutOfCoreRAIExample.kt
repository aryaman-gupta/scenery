package graphics.scenery.tests.examples.volumes

import bdv.util.AxisOrder
import bdv.util.volatiles.VolatileViews
import org.joml.Vector3f
import graphics.scenery.Camera
import graphics.scenery.DetachedHeadCamera
import graphics.scenery.PointLight
import graphics.scenery.SceneryBase
import graphics.scenery.backends.Renderer
import graphics.scenery.volumes.TransferFunction
import graphics.scenery.volumes.Volume
import ij.IJ
import ij.ImagePlus
import net.imglib2.RandomAccessibleInterval
import net.imglib2.img.Img
import net.imglib2.img.display.imagej.ImageJFunctions
import net.imglib2.type.numeric.integer.UnsignedShortType
import org.janelia.saalfeldlab.n5.GzipCompression
import org.janelia.saalfeldlab.n5.N5FSReader
import org.janelia.saalfeldlab.n5.N5FSWriter
import org.janelia.saalfeldlab.n5.imglib2.N5Utils
import org.junit.Test
import tpietzsch.example2.VolumeViewerOptions
import java.nio.file.Files


/**
 * BDV Rendering Example loading an out-of-core RAI
 *
 * @author Ulrik Günther <hello@ulrik.is>
 * @author Tobias Pietzsch <pietzsch@mpi-cbg.de>
 */
class OutOfCoreRAIExample: SceneryBase("Out-of-core RAI Rendering example", 1280, 720) {
    lateinit var volume: Volume

    override fun init() {
        renderer = hub.add(Renderer.createRenderer(hub, applicationName, scene, windowWidth, windowHeight))

        val cam: Camera = DetachedHeadCamera()
        with(cam) {
            perspectiveCamera(50.0f, windowWidth, windowHeight)

            position = Vector3f(0.0f, 0.0f, 10.0f)
            scene.addChild(this)
        }

        val imp: ImagePlus = IJ.openImage("https://imagej.nih.gov/ij/images/t1-head.zip")
        val img: Img<UnsignedShortType> = ImageJFunctions.wrapShort(imp)

        val datasetName = "testDataset"
        val n5path = Files.createTempDirectory("scenery-t1-head-n5")
        val n5 = N5FSWriter(n5path.toString())
        N5Utils.save(img, n5, datasetName, intArrayOf(img.dimension(0).toInt(), img.dimension(1).toInt(), img.dimension(2).toInt()), GzipCompression())
        logger.info("Image written to n5 file at $n5path")

        val ooc: RandomAccessibleInterval<UnsignedShortType> = N5Utils.openVolatile(N5FSReader(n5path.toString()), datasetName)
        logger.info("Image read from n5 file at $n5path")

        val wrapped = VolatileViews.wrapAsVolatile(ooc)

        volume = Volume.fromRAI(wrapped as RandomAccessibleInterval<UnsignedShortType>, UnsignedShortType(), AxisOrder.DEFAULT, "T1 head OOC", hub, VolumeViewerOptions())
        volume.transferFunction = TransferFunction.ramp(0.001f, 0.8f)
        volume.transferFunction.addControlPoint(0.01f, 0.5f)
        volume.scale = Vector3f(5.0f, 5.0f, 15.0f)
        scene.addChild(volume)

        val lights = (0 until 3).map {
            PointLight(radius = 15.0f)
        }

        lights.mapIndexed { i, light ->
            light.position = Vector3f(2.0f * i - 4.0f,  i - 1.0f, 0.0f)
            light.emissionColor = Vector3f(1.0f, 1.0f, 1.0f)
            light.intensity = 50.0f
            scene.addChild(light)
        }
    }

    @Test override fun main() {
        super.main()
    }
}
