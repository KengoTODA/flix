package ca.uwaterloo.flix.tools

import ca.uwaterloo.flix.util.Options
import org.scalatest.FunSuite

import java.nio.file.Files
import java.util.zip.ZipFile
import scala.util.Using

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

    val packageName = p.toAbsolutePath.getParent.getFileName.toString
    val jarPath = p.resolve(packageName + ".jar")
    assert(Files.exists(jarPath))
    assert(jarPath.getFileName.toString.startsWith(ProjectPrefix))
  }

  test("build-pkg") {
    val p = Files.createTempDirectory(ProjectPrefix)
    Packager.init(p, DefaultOptions)
    Packager.buildPkg(p, DefaultOptions)

    val packageName = p.toAbsolutePath.getParent.getFileName.toString
    val packagePath = p.resolve(packageName + ".fpkg")
    assert(Files.exists(packagePath))
    assert(packagePath.getFileName.toString.startsWith(ProjectPrefix))
  }

  test("build-pkg-with-notice") {
    val p = Files.createTempDirectory(ProjectPrefix)
    Packager.init(p, DefaultOptions)
    Files.createFile(p.resolve("NOTICE"))
    Packager.buildPkg(p, DefaultOptions)

    val packageName = p.toAbsolutePath.getParent.getFileName.toString
    val packagePath = p.resolve(packageName + ".fpkg")
    val zipEntry = Using(new ZipFile(packagePath.toFile)) { zipFile =>
      zipFile.getEntry("NOTICE")
    }
    assert(zipEntry.isSuccess && zipEntry.get != null, "NOTICE file not found in %s".format(packagePath.toString))
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
