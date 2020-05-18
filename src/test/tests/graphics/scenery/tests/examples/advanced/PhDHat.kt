package graphics.scenery.tests.examples.advanced

import graphics.scenery.*
import graphics.scenery.backends.Renderer
import graphics.scenery.numerics.Random
import graphics.scenery.textures.Texture
import graphics.scenery.utils.Image
import graphics.scenery.utils.extensions.minus
import org.joml.Vector3f
import org.joml.Vector4f
import org.junit.Test
import kotlin.concurrent.thread
import kotlin.math.cos
import kotlin.math.sin

/**
 * <Description>
 *
 * @author MOSAIC Group <mosaic@mpi-cbg.de>
 */
class PhDHat:SceneryBase("CongratsUlrik!", windowWidth = 800, windowHeight = 800) {
    val lightCount = 20
    val leucocyteCount = 200
    val positionRange = 250.0f

    override fun init() {
        renderer = hub.add(Renderer.createRenderer(hub, applicationName, scene, windowWidth, windowHeight))

        val cam: Camera = DetachedHeadCamera()
        with(cam) {
            position = Vector3f(0.0f, 0.0f, 5.0f)
            perspectiveCamera(50.0f, 512, 512)

            scene.addChild(this)
        }

        val hull = Box(Vector3f(2*positionRange, 2*positionRange, 2*positionRange), insideNormals = true)
        hull.material.ambient = Vector3f(0.9f, 0.9f, 0.9f)
        hull.material.diffuse = Vector3f(1.0f, 1.0f, 1.0f)
        hull.material.specular = Vector3f(0.9f, 0.9f, 0.9f)
        hull.material.cullingMode = Material.CullingMode.Front
        hull.name = "hull"

        scene.addChild(hull)

        val hat = Mesh()
        hat.readFromOBJ(Mesh::class.java.getResource("models/PhDHat/graduation.obj").file)

        hat.name = "the phd hat!"

        hat.material.ambient = Vector3f(0.0f, 0.0f, 0.0f)
        hat.material.diffuse = Vector3f(0.0f, 0.0f, 0.0f)
        hat.material.specular = Vector3f(0.0f, 0f, 0f)
        hat.position = Vector3f(0.0f, 0.0f, -15.0f)
//        hat.material.textures["diffuse"] = Texture.fromImage(Image.fromResource("textures/helix.png", this::class.java))
        hat.scale = Vector3f(0.05f, 0.05f, 0.05f)
        hat.rotation.rotateY(20.0f)

//        hat.material.metallic = 0.0f
//        hat.material.roughness = 0.5f

        val lights = (0 until lightCount).map { PointLight(radius = positionRange) }

        lights.map {
            it.position = Random.random3DVectorFromRange(-positionRange/2, positionRange/2)
            it.emissionColor = Vector3f(1.0f, 1.0f, 1.0f)
            it.intensity = 0.7f

            scene.addChild(it)
        }


        val vrGlass = Mesh()
        vrGlass.readFromOBJ(Mesh::class.java.getResource("models/PhDHat/VR_simple.obj").file)

        vrGlass.name = "VR Glasses"
        vrGlass.position = Vector3f(0.0f, -40.0f, 85.0f)
//        vrGlass.material.ambient = Vector3f(0.0f, 0.0f, 0.0f)
//        vrGlass.material.textures["diffuse"] = Texture.fromImage(Image.fromResource("textures/VR_Base_Color.png", this::class.java))
//        vrGlass.material.specular = Vector3f(0.0f, 0f, 0f)
        vrGlass.scale = Vector3f(50.0f, 50.0f, 50.0f)

        hat.addChild(vrGlass)

        scene.addChild(hat)

//        val desc = TextBoard()
//        desc.text = "sponza"
//        desc.position = Vector3f(-2.0f, -0.1f, -4.0f)
//        desc.fontColor = Vector4f(1.0f, 1.0f, 1.0f, 1.0f)
//        desc.backgroundColor = Vector4f(0.1f, 0.1f, 0.1f, 1.0f)
//        desc.transparent = 0
//        scene.addChild(desc)

        val erythrocyte = Mesh()
        erythrocyte.readFromOBJ(Mesh::class.java.getResource("models/erythrocyte.obj").file)
        erythrocyte.material = ShaderMaterial.fromFiles("DefaultDeferredInstanced.vert", "DefaultDeferred.frag")
        erythrocyte.material.ambient = Vector3f(0.1f, 0.0f, 0.0f)
        erythrocyte.material.diffuse = Vector3f(0.9f, 0.0f, 0.02f)
        erythrocyte.material.specular = Vector3f(0.05f, 0f, 0f)
        erythrocyte.material.metallic = 0.01f
        erythrocyte.material.roughness = 0.9f
        erythrocyte.name = "Erythrocyte_Master"
        erythrocyte.instancedProperties["ModelMatrix"] = { erythrocyte.model }
        scene.addChild(erythrocyte)

        val leucocyte = Mesh()
        leucocyte.readFromOBJ(Mesh::class.java.getResource("models/leukocyte.obj").file)
        leucocyte.name = "leucocyte_Master"
        leucocyte.material = ShaderMaterial.fromFiles("DefaultDeferredInstanced.vert", "DefaultDeferred.frag")
        leucocyte.material.ambient = Vector3f(0.1f, 0.0f, 0.0f)
        leucocyte.material.diffuse = Vector3f(0.8f, 0.7f, 0.7f)
        leucocyte.material.specular = Vector3f(0.05f, 0f, 0f)
        leucocyte.material.metallic = 0.01f
        leucocyte.material.roughness = 0.5f
        leucocyte.instancedProperties["ModelMatrix"] = { leucocyte.model }
        scene.addChild(leucocyte)

        val container = Node("Cell container")

        val leucocytes = (0 until leucocyteCount).map {
            val v = Mesh()
            v.name = "leucocyte_$it"
            v.instancedProperties["ModelMatrix"] = { v.world }
            v.metadata["axis"] = Vector3f(sin(0.1 * it).toFloat(), -cos(0.1 * it).toFloat(), sin(1.0f*it) * cos(1.0f*it)).normalize()
            v.parent = container

            val scale = Random.randomFromRange(3.0f, 4.0f)
            v.scale = Vector3f(scale, scale, scale)
            v.position = Random.random3DVectorFromRange(-positionRange, positionRange)
            v.rotation.rotateXYZ(
                Random.randomFromRange(0.01f, 0.9f),
                Random.randomFromRange(0.01f, 0.9f),
                Random.randomFromRange(0.01f, 0.9f)
            )

            v
        }
        leucocyte.instances.addAll(leucocytes)

        val erythrocytes = (0 until leucocyteCount*40).map {
            val v = Mesh()
            v.name = "erythrocyte_$it"
            v.instancedProperties["ModelMatrix"] = { v.world }
            v.metadata["axis"] = Vector3f(sin(0.1 * it).toFloat(), -cos(0.1 * it).toFloat(), sin(1.0f*it)*cos(1.0f*it)).normalize()
            v.parent = container

            val scale = Random.randomFromRange(0.5f, 1.2f)
            v.scale = Vector3f(scale, scale, scale)
            v.position = Random.random3DVectorFromRange(-positionRange, positionRange)
            v.rotation.rotateXYZ(
                Random.randomFromRange(0.01f, 0.9f),
                Random.randomFromRange(0.01f, 0.9f),
                Random.randomFromRange(0.01f, 0.9f)
            )

            v
        }
        erythrocyte.instances.addAll(erythrocytes)

        scene.addChild(container)

        fun Node.hoverAndTumble(magnitude: Float) {
            val axis = this.metadata["axis"] as? Vector3f ?: return
            this.rotation.rotateAxis(magnitude, axis.x(), axis.y(), axis.z())
            this.rotation.rotateY(-1.0f * magnitude)
            this.needsUpdate = true
        }

        thread {
            while(!sceneInitialized()) {
                Thread.sleep(200)
            }

            while(true) {
                erythrocytes.parallelStream().forEach { erythrocyte -> erythrocyte.hoverAndTumble(Random.randomFromRange(0.001f, 0.01f)) }
                leucocytes.parallelStream().forEach { leucocyte -> leucocyte.hoverAndTumble(0.001f) }

                container.position = container.position - Vector3f(0.01f, 0.01f, 0.01f)
                container.updateWorld(false)

                Thread.sleep(5)
                ticks++
            }
        }
    }

    @Test
    override fun main() {
        super.main()
    }
}
