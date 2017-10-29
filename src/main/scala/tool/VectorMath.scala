package tool

object VectorMath {

  def distVec(a: Array[Double], b: Array[Double]): Double = {
    var sum = 0d
    for (elem <- a.indices) {
      sum = sum + ((a(elem) - b(elem)) * (a(elem) - b(elem)))
    }
    Math.sqrt(sum)
  }

  def addVec(a: Array[Double], b: Array[Double]): Array[Double] = {
    var result: Array[Double] = Array()

    for (elem <- a.indices) {
      result = result :+ (a(elem) + b(elem))
    }
    result
  }

  def subVec(a: Array[Double], b: Array[Double]): Array[Double] = {
    var result: Array[Double] = Array()

    for (elem <- a.indices) {
      result = result :+ (a(elem) - b(elem))
    }
    result
  }
}
