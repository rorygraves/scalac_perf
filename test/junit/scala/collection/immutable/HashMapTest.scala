package scala.collection.immutable

import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(classOf[JUnit4])
class HashMapTest {

  private val computeHashF = {
    HashMap.empty.computeHash _
  }

  @Test
  def canMergeIdenticalHashMap1sWithNullKvs() {
    def m = new HashMap.HashMap1(1, computeHashF(1), 1, null)

    val merged = m.merged(m)(null)
    assertEquals(m, merged)
  }

  @Test
  def canMergeIdenticalHashMap1sWithNullKvsCustomMerge() {
    def m = new HashMap.HashMap1(1, computeHashF(1), 1, null)

    val merged = m.merged(m) {
      case ((k1, v1), (k2, v2)) =>
        (k1, v1 + v2)
    }
    assertEquals(new HashMap.HashMap1(1, computeHashF(1), 2, null), merged)
  }

  @Test
  def canMergeHashMap1sWithNullKvsHashCollision() {
    val key1 = 1000L * 1000 * 1000 * 10
    val key2 = key1.##.toLong
    assert(key1.## == key2.##)

    val m1 = new HashMap.HashMap1(key1, computeHashF(key1.##), 1, null)
    val m2 = new HashMap.HashMap1(key2, computeHashF(key2.##), 1, null)
    val expected = HashMap(key1 -> 1, key2 -> 1)
    val merged = m1.merged(m2)(null)
    assertEquals(expected, merged)
    val mergedWithMergeFunction = m1.merged(m2) { (kv1, kv2) =>
      throw new RuntimeException("Should not be reached.")
    }
    assertEquals(expected, mergedWithMergeFunction)
  }

  def checkBuild[K, V](range: Seq[K], expected: Map[K, V], underTest: HashMap.HashMapBuilder[K, V]): Unit = {

    val m2 = underTest.result()
    assertEquals(expected.size, m2.size)

    expected foreach {
      case (k, v) =>
        assertTrue(s"failed for $k $v", m2.contains(k))
        assertEquals(s"failed for $k $v", expected(k), m2(k))
        assertEquals(s"failed for $k $v", expected.get(k), m2.get(k))
    }
    assertEquals(expected.toString, m2.toString)
    assertEquals(expected, m2)

    range.filterNot(expected.keySet) foreach {
      absent =>
        assertFalse(s"contains failed at (expected missed) key $absent", m2.contains(absent))
        assertEquals(s"get failed at (expected missed) key $absent", None, m2.get(absent))
    }

  }

  def `both+=`[K, V](expected: Map[K, V], underTest: HashMap.HashMapBuilder[K, V], key: K, value: V): Map[K, V] = {
    var exp = expected
    exp = exp + (key -> value)
    underTest += (key -> value)
    exp
  }

  def `both++=`[K, V](expected: Map[K, V], underTest: HashMap.HashMapBuilder[K, V], values: Traversable[(K, V)]): Map[K, V] = {
    var exp = expected
    //we dont use ++ on expected as that uses a builder internally
    values foreach { kv => exp = exp + kv }
    underTest ++= values
    exp
  }

  def `both-=`[K, V](expected: Map[K, V], underTest: HashMap.HashMapBuilder[K, V], key: K): Map[K, V] = {
    var exp = expected
    exp = exp - key
    underTest -= key
    exp
  }

  //  def `both--=` [K,V](expected:Map[K,V], underTest:HashMap.HashMapBuilder[K,V], values: Traversable[K]) : Map[K,V] = {
  //    var exp = expected
  //    //we dont use ++ on expected as that uses a builder internally
  //    values foreach{ k => exp = exp - k}
  //    underTest --= values
  //    exp
  //  }
  @Test
  def checkBuildAddBasic(): Unit = checkBuildAdd(true)

  @Test
  def checkBuildAddReuse(): Unit = checkBuildAdd(false)

  def checkBuildAdd(intermediate: Boolean): Unit = {
    var expected = Map.empty[Int, String]
    val underTest = HashMap.newBuilder[Int, String]
    val range = 1 to 2000

    (1 to 1500) foreach { value =>
      expected = `both+=`(expected, underTest, value, s"xx $value")
    }
    if (intermediate) checkBuild(range, expected, underTest)

    (1000 to 2000) foreach { value =>
      expected = `both+=`(expected, underTest, value, s"xx $value")
    }
    checkBuild(range, expected, underTest)

  }

  @Test
  def checkBuildAddCombinations(): Unit = {
    for (intermediate <- List(true, false);
         bulk <- List(noop _, toMap _, toHashMap _)) {
      checkBuildAddCombinations(intermediate, bulk)
    }
  }

  def noop(in: Seq[(Int, String)]): Traversable[(Int, String)] = in

  // too a map without using a builder
  def toMap(in: Seq[(Int, String)]): Traversable[(Int, String)] = {
    var acc = Map.empty[Int, String]
    for (x <- in) acc += x
    acc
  }

  // too a hashmap without using a builder
  def toHashMap(in: Seq[(Int, String)]): Traversable[(Int, String)] = {
    var acc = HashMap.empty[Int, String]
    for (x <- in) acc += x
    acc
  }
  def checkBuildAddCombinations(intermediate: Boolean, bulkAdd:Seq[(Int, String)] => Traversable[(Int, String)]): Unit = {
    var expected = Map.empty[Int,String]
    val underTest = HashMap.newBuilder[Int,String]
    val range = 1 to 2000

    (1 to 1500) foreach{ value =>
      expected = `both+=`(expected, underTest, value, s"xx $value")
    }
    if (intermediate) checkBuild(range, expected,underTest)

    val bulk = bulkAdd((1000 to 2000) map { value => (value, s"yy $value")})

    expected = `both++=`(expected,underTest, bulk)

    if (intermediate) checkBuild(range, expected,underTest)

    (500 to 1500) foreach{ value =>
      expected = `both-=`(expected, underTest, value)
    }
    checkBuild(range, expected,underTest)
  }


}