package it.cameralevel.roberto.cameralevel.sensors.orientations

interface OrientationListener {

    fun onOrientationChanged(orientation: Orientation, pitch: Float, roll: Float, balance: Float)

    fun onCalibrationSaved(success: Boolean)

    fun onCalibrationReset(success: Boolean)

}