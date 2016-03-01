package scenery

import cleargl.GLVector
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

interface HasGeometry {
    val vertexSize: Int
    val texcoordSize: Int
    val geometryType: GeometryType

    var vertices: FloatArray
    var normals: FloatArray
    var texcoords: FloatArray
    var indices: IntArray

    fun readFromOBJ(filename: String) {
        var name: String = ""
        var vbuffer = ArrayList<Float>()
        var nbuffer = ArrayList<Float>()
        var tbuffer = ArrayList<Float>()

        var tmpV = ArrayList<Float>()
        var tmpN = ArrayList<Float>()
        var tmpUV = ArrayList<Float>()

        var f = File(filename)
        if(!f.exists()) {
            System.out.println("Could not read from ${filename}, file does not exist.")

            vertices = FloatArray(0)
            normals = FloatArray(0)
            texcoords = FloatArray(0)
            indices = IntArray(0)
            return
        }

        val start = System.nanoTime()
        val inputStream = FileInputStream(filename)
        val lines = BufferedReader(InputStreamReader(inputStream)).readLines()

        inputStream.close()
        var count = 0

        System.out.println("Reading from OBJ file")
        lines.forEach {
            line ->
            val tokens = line.trim().trimEnd().split(" ").filter { it.length > 0 }
            if(tokens.size > 0) {
                when (tokens[0]) {
                    "" -> {
                    }
                    "#" -> {
                    }
                    "mtllib" -> {
                    }
                    "o" -> {
                    }
                // vertices are specified as v x y z
                    "v" -> tokens.drop(1).forEach { tmpV.add(it.toFloat()) }

                // normal coords are specified as vn x y z
                    "vn" -> tokens.drop(1).forEach { tmpN.add(it.toFloat()) }

                // UV coords maybe vt t1 t2 0.0 or vt t1 t2
                    "vt" -> {
                        if (tokens.drop(1).size == 3) {
                            tokens.drop(1).dropLast(1)
                        } else {
                            tokens.drop(1)
                        }.forEach { tmpUV.add(it.toFloat()) }
                    }

                // faces can reference to three or more vertices in these notations:
                // f v1 v2 ... vn
                // f v1//vn1 v2//vn2 ... vn//vnn
                // f v1/vt1/vn1 v2/vt2/vn2 ... vn/vtn/vnn
                    "f" -> {
                        count++
                        val elements = tokens.drop(1).map { it.split("/") }

                        val vertices = elements.map { it[0].toInt() }
                        val uvs = elements.filter { it.size > 1 }. map { it[1].toInt() }
                        val normals = elements.filter { it.size > 2 }.map { it[2].toInt() }

                        var range = ArrayList<Int>()
                        if(vertices.size == 3) {
                            range.addAll(listOf(0, 1, 2))
                        } else if(vertices.size == 4) {
                            range.addAll(listOf(0, 1, 2, 0, 2, 3))
                        }
                        else {
                            System.err.println("Polygonal triangulation is not yet supported")
                            range.addAll((0..vertices.size-1).toList())
                            // TODO: Implement polygons!
                        }

                        fun toBufferIndex(obj: List<Number>, num: Int, vectorSize: Int, offset: Int): Int {
                            val index: Int
                            if (num >= 0) {
                                index = (num - 1) * vectorSize + offset
                            } else {
                                index = (obj.size / vectorSize + num) * vectorSize + offset
                            }

                            return index
                        }

                        fun defaultHandler(x: Int): Float {
                            System.err.println("Could not find v/n/uv for index $x. File broken?")
                            return 0.0f
                        }

                        for(i in range) {
                            vbuffer.add(tmpV.getOrElse(toBufferIndex(tmpV, vertices[i], 3, 0), ::defaultHandler))
                            vbuffer.add(tmpV.getOrElse(toBufferIndex(tmpV, vertices[i], 3, 1), ::defaultHandler))
                            vbuffer.add(tmpV.getOrElse(toBufferIndex(tmpV, vertices[i], 3, 2), ::defaultHandler))

                            if(normals.size == vertices.size) {
                                nbuffer.add(tmpN.getOrElse(toBufferIndex(tmpN, normals[i], 3, 0), ::defaultHandler))
                                nbuffer.add(tmpN.getOrElse(toBufferIndex(tmpN, normals[i], 3, 1), ::defaultHandler))
                                nbuffer.add(tmpN.getOrElse(toBufferIndex(tmpN, normals[i], 3, 2), ::defaultHandler))
                            }

                            if(uvs.size == vertices.size) {
                                tbuffer.add(tmpUV.getOrElse(toBufferIndex(tmpUV, uvs[i], 2, 0), ::defaultHandler))
                                tbuffer.add(tmpUV.getOrElse(toBufferIndex(tmpUV, uvs[i], 2, 1), ::defaultHandler))
                            }
                        }
                    }
                    "s" -> {
                        // TODO: Implement smooth shading across faces
                    }
                    "g" -> {
                        // TODO: Implement groups
                    }
                    "usemtl" -> {
                        // TODO: Implement materials
                    }
                    else -> System.err.println("Unknown element: ${tokens.joinToString(" ")}")
                }
            }
        }

        val end = System.nanoTime()
        System.out.println("Read ${vbuffer.size}/${nbuffer.size}/${tbuffer.size} v/t/uv of model ${name} in ${(end-start)/1e6} ms")

        vertices = vbuffer.toFloatArray()
        normals = nbuffer.toFloatArray()
        texcoords = tbuffer.toFloatArray()
    }

    fun readFromSTL(filename: String) {
        var name: String = ""
        var vbuffer = ArrayList<Float>()
        var nbuffer = ArrayList<Float>()

        var f = File(filename)
        if(!f.exists()) {
            System.out.println("Could not read from ${filename}, file does not exist.")

            vertices = FloatArray(0)
            normals = FloatArray(0)
            texcoords = FloatArray(0)
            indices = IntArray(0)
            return
        }

        val start = System.nanoTime()
        val inputStream = FileInputStream(filename)
        val lines = BufferedReader(InputStreamReader(inputStream)).readLines()

        inputStream.close()

        val readFromAscii = {
            System.out.println("Reading from ASCII STL file")
            lines.forEach {
                line ->
                val tokens = line.trim().trimEnd().split(" ")
                when(tokens[0]) {
                    "solid" -> name = tokens.drop(1).joinToString(" ")
                    "vertex" -> tokens.drop(1).forEach { vbuffer.add(it.toFloat()) }
                    "facet" -> tokens.drop(2).forEach { nbuffer.add(it.toFloat()) }
                    "outer" -> {}
                    "end" -> {}
                    "endloop" -> {}
                    "endfacet" -> {}
                    "endsolid" -> {}
                    else -> System.err.println("Unknown element: ${tokens.joinToString(" ")}")
                }
            }}

        val readFromBinary = {
            System.out.println("Reading from binary STL file")
            val f = FileInputStream(filename)
            val b = BufferedInputStream(f)
            var headerB: ByteArray = ByteArray(80)
            var sizeB: ByteArray = ByteArray(4)
            var buffer: ByteArray = ByteArray(12)
            var size: Int

            b.read(headerB, 0, 80)
            b.read(sizeB, 0, 4)

            size = ((sizeB[0].toInt() and 0xFF)
                    or ((sizeB[1].toInt() and 0xFF) shl 8)
                    or ((sizeB[2].toInt() and 0xFF) shl 16)
                    or ((sizeB[3].toInt() and 0xFF) shl 24))

            fun readFloatFromInputStream(fis: BufferedInputStream): Float {
                var floatBuf = ByteArray(4)
                var bBuf: ByteBuffer

                fis.read(floatBuf, 0, 4)
                bBuf = ByteBuffer.wrap(floatBuf)
                bBuf.order(ByteOrder.LITTLE_ENDIAN)

                return bBuf.float
            }

            val name = String(headerB.copyOfRange(0, headerB.indexOfFirst { it == 0.toByte()  }))

            for (i in 1..size) {
                // surface normal
//                                nbuffer.add(readFloatFromInputStream(b))
//                                nbuffer.add(readFloatFromInputStream(b))
//                                nbuffer.add(readFloatFromInputStream(b))
                readFloatFromInputStream(b)
                readFloatFromInputStream(b)
                readFloatFromInputStream(b)

                // vertices
                for (vertex in 1..3) {
                    vbuffer.add(readFloatFromInputStream(b))
                    vbuffer.add(readFloatFromInputStream(b))
                    vbuffer.add(readFloatFromInputStream(b))
                }

                b.read(buffer, 0, 2)
            }

            f.close()
        }

        if(lines[0].startsWith("solid ")) {
            readFromAscii
        } else {
            readFromBinary
        }.invoke()

        var i = 0
        while(i < vbuffer.size) {
            val v1 = GLVector(vbuffer[i], vbuffer[i + 1], vbuffer[i + 2])
            i += 3

            val v2 = GLVector(vbuffer[i], vbuffer[i+1], vbuffer[i+2])
            i += 3

            val v3 = GLVector(vbuffer[i], vbuffer[i+1], vbuffer[i+2])
            i += 3

            val a = v2 - v1
            val b = v3 - v1

            val n = a.cross(b).normalized

            nbuffer.add(n.x())
            nbuffer.add(n.y())
            nbuffer.add(n.z())
        }


        val end = System.nanoTime()
        System.out.println("Read ${vbuffer.size} vertices/${nbuffer.size} normals of model ${name} in ${(end-start)/1e6} ms")

        vertices = vbuffer.toFloatArray()
        normals = nbuffer.toFloatArray()
    }
}