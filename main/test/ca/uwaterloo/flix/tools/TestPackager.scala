package ca.uwaterloo.flix.tools

import ca.uwaterloo.flix.util.Options
import org.scalatest.FunSuite

import java.nio.file.Files
import java.util.Date
import java.util.zip.{ZipEntry, ZipFile}
import scala.jdk.CollectionConverters.EnumerationHasAsScala

class TestPackager extends FunSuite {

  private val ProjectPrefix: String = "flix-project-"

  private val DefaultOptions: Options = Options.Default

  test("init") {
    val p = Files.createTempDirectory(ProjectPrefix)
    Packager.init(p, DefaultOptions)
  }

  test("check") {
    val p = Files.createTempDirectory(ProjectPrefix)
    Packager.init(p, DefaultOptions)
    Packager.check(p, DefaultOptions)
  }

  test("build") {
    val p = Files.createTempDirectory(ProjectPrefix)
    Packager.init(p, DefaultOptions)
    Packager.build(p, DefaultOptions)
  }

  test("build-jar") {
    val p = Files.createTempDirectory(ProjectPrefix)
    Packager.init(p, DefaultOptions)
    Packager.build(p, DefaultOptions)
    Packager.buildJar(p, DefaultOptions)

    val packageName = p.getFileName.toString
    val jarPath = p.resolve(packageName + ".jar")
    assert(Files.exists(jarPath))
    assert(jarPath.getFileName.toString.startsWith(ProjectPrefix))
  }

  test("build-pkg") {
    val p = Files.createTempDirectory(ProjectPrefix)
    Packager.init(p, DefaultOptions)
    Packager.buildPkg(p, DefaultOptions)

    val packageName = p.getFileName.toString
    val packagePath = p.resolve(packageName + ".fpkg")
    assert(Files.exists(packagePath))
    assert(packagePath.getFileName.toString.startsWith(ProjectPrefix))
  }

  test("build-pkg generates ZIP entries with fixed time") {
    val p = Files.createTempDirectory(ProjectPrefix)
    Packager.init(p, DefaultOptions)
    Packager.buildPkg(p, DefaultOptions)

    val packageName = p.getFileName.toString
    val packagePath = p.resolve(packageName + ".fpkg")
    for (e <- new ZipFile(packagePath.toFile).entries().asScala) {
      val time = new Date(e.getTime)
      assert(time.formatted("yyyy-MM-dd").equals("1990-02-01"))
    }
  }

  test("benchmark") {
    val p = Files.createTempDirectory(ProjectPrefix)
    Packager.init(p, DefaultOptions)
    Packager.benchmark(p, DefaultOptions)
  }

  test("run") {
    val p = Files.createTempDirectory(ProjectPrefix)
    Packager.init(p, DefaultOptions)
    Packager.run(p, DefaultOptions)
  }

  test("test") {
    val p = Files.createTempDirectory(ProjectPrefix)
    Packager.init(p, DefaultOptions)
    Packager.test(p, DefaultOptions)
  }

}
