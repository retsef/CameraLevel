package it.cameralevel.roberto.cameralevel.sensors.orientations

import java.util.Arrays
import java.util.List

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.support.v4.content.ContextCompat
import android.view.Surface


class OrientationProvider : SensorEventListener {
    private val context: Context
    private val MIN_VALUES = 20

    /** Calibration  */
    private val SAVED_PITCH = "pitch."
    private val SAVED_ROLL = "roll."
    private val SAVED_BALANCE = "balance."

    private var sensor: Sensor? = null
    private var sensorManager: SensorManager? = null
    private var listener: OrientationListener? = null

    /** indicates whether or not Accelerometer Sensor is supported  */
    private var supported: Boolean? = null

    /** indicates whether or not Accelerometer Sensor is running  */
    private var running = false

    /** Calibration  */
    private val calibratedPitch = FloatArray(5)
    private val calibratedRoll = FloatArray(5)
    private val calibratedBalance = FloatArray(5)
    private var calibrating = false

    /** Orientation  */
    private var pitch: Float = 0.toFloat()
    private var roll: Float = 0.toFloat()
    private var balance: Float = 0.toFloat()
    private var tmp: Float = 0.toFloat()
    private var oldPitch: Float = 0.toFloat()
    private var oldRoll: Float = 0.toFloat()
    private var oldBalance: Float = 0.toFloat()
    private var minStep = 360f
    private var refValues = 0f
    private var orientation: Orientation? = null
    private var locked: Boolean = false
    private val displayOrientation: Int = 0

    /** Rotation Matrix  */
    private val MAG = floatArrayOf(1f, 1f, 1f)
    private val I = FloatArray(16)
    private val R = FloatArray(16)
    private val outR = FloatArray(16)
    private val LOC = FloatArray(3)

    private var provider: OrientationProvider? = null

    constructor(context: Context) {
        this.context = context
        //this.displayOrientation = Level.getContext().getWindowManager().getDefaultDisplay().getRotation()
    }

    fun getInstance(): OrientationProvider {
        if (provider == null) {
            provider = OrientationProvider()
        }
        return provider as OrientationProvider
    }

    /**
     * Returns true if the manager is listening to orientation changes
     */
    fun isListening(): Boolean {
        return running
    }

    /**
     * Unregisters listeners
     */
    fun stopListening() {
        running = false
        try {
            if (sensorManager != null) {
                sensorManager!!.unregisterListener(this)
            }
        } catch (e: Exception) {
        }

    }

    private fun getRequiredSensors(): List<Int> {
        return Arrays.asList(
                Integer.valueOf(Sensor.TYPE_ACCELEROMETER)!!
        )
    }

    /**
     * Returns true if at least one Accelerometer sensor is available
     */
    fun isSupported(): Boolean {
        if (supported == null) {
            if (context != null) {
                sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
                var supported = true
                for (sensorType in getRequiredSensors()) {
                    val sensors = sensorManager!!.getSensorList(sensorType)
                    supported = sensors.size > 0 && supported
                }
                this.supported = java.lang.Boolean.valueOf(supported)
                return supported
            }
        }
        return supported!!
    }


    /**
     * Registers a listener and start listening
     * @param accelerometerListener
     * callback for accelerometer events
     */
    fun startListening(orientationListener: OrientationListener) {
        // load calibration
        calibrating = false
        Arrays.fill(calibratedPitch, 0f)
        Arrays.fill(calibratedRoll, 0f)
        Arrays.fill(calibratedBalance, 0f)
        val prefs = context.getPreferences(Context.MODE_PRIVATE)
        for (orientation in Orientation.values()) {
            calibratedPitch[orientation.ordinal()] = prefs.getFloat(SAVED_PITCH + orientation.toString(), 0f)
            calibratedRoll[orientation.ordinal()] = prefs.getFloat(SAVED_ROLL + orientation.toString(), 0f)
            calibratedBalance[orientation.ordinal()] = prefs.getFloat(SAVED_BALANCE + orientation.toString(), 0f)
        }
        // register listener and start listening
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE)
        running = true
        for (sensorType in getRequiredSensors()) {
            val sensors = sensorManager!!.getSensorList(sensorType)
            if (sensors.size > 0) {
                sensor = sensors[0]
                running = sensorManager!!.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL) && running
            }
        }
        if (running) {
            listener = orientationListener
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {

        oldPitch = pitch
        oldRoll = roll
        oldBalance = balance

        SensorManager.getRotationMatrix(R, I, event.values, MAG)

        // compute pitch, roll & balance
        when (displayOrientation) {
            Surface.ROTATION_270 -> SensorManager.remapCoordinateSystem(
                    R,
                    SensorManager.AXIS_MINUS_Y,
                    SensorManager.AXIS_X,
                    outR)
            Surface.ROTATION_180 -> SensorManager.remapCoordinateSystem(
                    R,
                    SensorManager.AXIS_MINUS_X,
                    SensorManager.AXIS_MINUS_Y,
                    outR)
            Surface.ROTATION_90 -> SensorManager.remapCoordinateSystem(
                    R,
                    SensorManager.AXIS_Y,
                    SensorManager.AXIS_MINUS_X,
                    outR)
            Surface.ROTATION_0 -> SensorManager.remapCoordinateSystem(
                    R,
                    SensorManager.AXIS_X,
                    SensorManager.AXIS_Y,
                    outR)
            else -> SensorManager.remapCoordinateSystem(R, SensorManager.AXIS_X, SensorManager.AXIS_Y, outR)
        }

        SensorManager.getOrientation(outR, LOC)

        // normalize z on ux, uy
        tmp = Math.sqrt((outR[8] * outR[8] + outR[9] * outR[9]).toDouble()).toFloat()
        tmp = if (tmp == 0f) 0 else outR[8] / tmp

        // LOC[0] compass
        pitch = Math.toDegrees(LOC[1].toDouble()).toFloat()
        roll = -Math.toDegrees(LOC[2].toDouble()).toFloat()
        balance = Math.toDegrees(Math.asin(tmp.toDouble())).toFloat()

        // calculating minimal sensor step
        if (oldRoll != roll || oldPitch != pitch || oldBalance != balance) {
            if (oldPitch != pitch) {
                minStep = Math.min(minStep, Math.abs(pitch - oldPitch))
            }
            if (oldRoll != roll) {
                minStep = Math.min(minStep, Math.abs(roll - oldRoll))
            }
            if (oldBalance != balance) {
                minStep = Math.min(minStep, Math.abs(balance - oldBalance))
            }
            if (refValues < MIN_VALUES) {
                refValues++
            }
        }

        if (!locked || orientation == null) {
            if (pitch < -45 && pitch > -135) {
                // top side up
                orientation = Orientation.TOP
            } else if (pitch > 45 && pitch < 135) {
                // bottom side up
                orientation = Orientation.BOTTOM
            } else if (roll > 45) {
                // right side up
                orientation = Orientation.RIGHT
            } else if (roll < -45) {
                // left side up
                orientation = Orientation.LEFT
            } else {
                // landing
                orientation = Orientation.LANDING
            }
        }

        if (calibrating) {
            calibrating = false
            val editor = context.getPreferences(Context.MODE_PRIVATE).edit()
            editor.putFloat(SAVED_PITCH + orientation!!.toString(), pitch)
            editor.putFloat(SAVED_ROLL + orientation!!.toString(), roll)
            editor.putFloat(SAVED_BALANCE + orientation!!.toString(), balance)
            val success = editor.commit()
            if (success) {
                calibratedPitch[orientation!!.ordinal()] = pitch
                calibratedRoll[orientation!!.ordinal()] = roll
                calibratedBalance[orientation!!.ordinal()] = balance
            }
            listener!!.onCalibrationSaved(success)
            pitch = 0f
            roll = 0f
            balance = 0f
        } else {
            pitch -= calibratedPitch[orientation!!.ordinal()]
            roll -= calibratedRoll[orientation!!.ordinal()]
            balance -= calibratedBalance[orientation!!.ordinal()]
        }

        // propagation of the orientation
        listener!!.onOrientationChanged(orientation, pitch, roll, balance)
    }

    /**
     * Tell the provider to restore the calibration
     * to the default factory values
     */
    fun resetCalibration() {
        var success = false
        try {
            success = context.getPreferences(Context.MODE_PRIVATE).edit().clear().commit()
        } catch (e: Exception) {
        }

        if (success) {
            Arrays.fill(calibratedPitch, 0f)
            Arrays.fill(calibratedRoll, 0f)
            Arrays.fill(calibratedBalance, 0f)
        }
        if (listener != null) {
            listener!!.onCalibrationReset(success)
        }
    }


    /**
     * Tell the provider to save the calibration
     * The calibration is actually saved on the next
     * sensor change event
     */
    fun saveCalibration() {
        calibrating = true
    }


    fun setLocked(locked: Boolean) {
        this.locked = locked
    }


    /**
     * Return the minimal sensor step
     * @return
     * the minimal sensor step
     * 0 if not yet known
     */
    fun getSensibility(): Float {
        return if (refValues >= MIN_VALUES) {
            minStep
        } else {
            0f
        }
    }

}