package it.cameralevel.roberto.cameralevel.sensors.orientations

class Orientation {
    companion object {
        val LANDING: Orientation = Orientation(1, 0)
        val TOP: Orientation = Orientation(1, 0)
        val RIGHT: Orientation = Orientation(1, 90)
        val BOTTOM: Orientation = Orientation(-1, 180)
        val LEFT: Orientation = Orientation(-1, -90)
    }

    private val reverse: Int
    private val rotation: Int

    constructor(reverse: Int, rotation: Int) {
        this.reverse = reverse
        this.rotation = rotation
    }

    fun values(): Int[2] {
        return [reverse, rotation]
    }

    fun isLevel(pitch: Float, roll: Float, balance: Float, sensibility: Float): Boolean {
        when (this) {
            BOTTOM, TOP -> return balance <= sensibility && balance >= -sensibility
            LANDING -> return (roll <= sensibility
                    && roll >= -sensibility
                    && (Math.abs(pitch) <= sensibility || Math.abs(pitch) >= 180 - sensibility))
            LEFT, RIGHT -> return Math.abs(pitch) <= sensibility || Math.abs(pitch) >= 180 - sensibility
        }
        return false
    }

}