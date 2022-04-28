package ca.uwaterloo.flix.tools

import ca.uwaterloo.flix.tools.Packager.PathComparator
import org.scalatest.FunSuite

import java.nio.file.{Path, Paths}

class TestPathComparator extends FunSuite {
  test("order of sorted Path instances") {
    val comparator = new PathComparator()
    val list = Array(
      Paths.get("a.txt"),
      Paths.get("b.txt"),
      Paths.get("a/child.txt")
    )
    assert(list.sorted(comparator).equals(Array(
      Paths.get("a/child.txt"),
      Paths.get("a.txt"),
      Paths.get("b.txt")
    )))
  }
}
