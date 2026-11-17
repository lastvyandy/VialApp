package cl.vialapp.app.ui.games.doom

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign

/** Uses Android's native Xbox-style HID events; Bluetooth pairing stays with Android. */
internal enum class DoomPadAction { USE, RELOAD, NEXT_WEAPON, PREVIOUS_WEAPON, PAUSE, CANCEL, UP, DOWN }

internal class DoomGamepad {
    private var deviceId = -1
    private var lx=0f
    private var ly=0f
    private var rx=0f
    private var ry=0f
    private var hatX=0f
    private var hatY=0f
    private var rt=0f
    private var lt=0f
    private val down=mutableSetOf<Int>()
    private val actions=ArrayDeque<DoomPadAction>()
    val connected get()=deviceId>=0

    fun reset(){
        deviceId=-1;lx=0f;ly=0f;rx=0f;ry=0f;hatX=0f;hatY=0f;rt=0f;lt=0f
        down.clear();actions.clear()
    }
    private fun attach(id:Int){
        if(deviceId!=-1&&deviceId!=id)reset()
        deviceId=id
    }
    private fun axis(event:MotionEvent,axis:Int):Float {
        val range=event.device?.getMotionRange(axis,InputDevice.SOURCE_JOYSTICK)
        if(range==null)return 0f
        val raw=event.getAxisValue(axis).coerceIn(-1f,1f)
        val dead=max(.16f,range.flat).coerceAtMost(.9f)
        return if(abs(raw)<=dead)0f else sign(raw)*((abs(raw)-dead)/(1f-dead)).coerceIn(0f,1f)
    }
    fun onMotion(event:MotionEvent):Boolean{
        if((event.source and InputDevice.SOURCE_JOYSTICK)!=InputDevice.SOURCE_JOYSTICK||
            event.actionMasked!=MotionEvent.ACTION_MOVE)return false
        attach(event.deviceId)
        lx=axis(event,MotionEvent.AXIS_X)
        ly=axis(event,MotionEvent.AXIS_Y)
        val dev=event.device
        rx=axis(event,if(dev?.getMotionRange(MotionEvent.AXIS_Z,InputDevice.SOURCE_JOYSTICK)!=null)MotionEvent.AXIS_Z else MotionEvent.AXIS_RX)
        ry=axis(event,if(dev?.getMotionRange(MotionEvent.AXIS_RZ,InputDevice.SOURCE_JOYSTICK)!=null)MotionEvent.AXIS_RZ else MotionEvent.AXIS_RY)
        hatX=axis(event,MotionEvent.AXIS_HAT_X)
        hatY=axis(event,MotionEvent.AXIS_HAT_Y)
        rt=max(axis(event,MotionEvent.AXIS_RTRIGGER),axis(event,MotionEvent.AXIS_GAS)).coerceAtLeast(0f)
        lt=max(axis(event,MotionEvent.AXIS_LTRIGGER),axis(event,MotionEvent.AXIS_BRAKE)).coerceAtLeast(0f)
        return true
    }
    fun onKey(event:KeyEvent):Boolean {
        val gamepad=(event.source and InputDevice.SOURCE_GAMEPAD)==InputDevice.SOURCE_GAMEPAD ||
            (event.source and InputDevice.SOURCE_JOYSTICK)==InputDevice.SOURCE_JOYSTICK ||
            (event.source and InputDevice.SOURCE_DPAD)==InputDevice.SOURCE_DPAD
        if(!gamepad)return false
        val action=when(event.keyCode){
            KeyEvent.KEYCODE_BUTTON_A,KeyEvent.KEYCODE_DPAD_CENTER->DoomPadAction.USE
            KeyEvent.KEYCODE_BUTTON_X->DoomPadAction.RELOAD
            KeyEvent.KEYCODE_BUTTON_Y,KeyEvent.KEYCODE_BUTTON_R1->DoomPadAction.NEXT_WEAPON
            KeyEvent.KEYCODE_BUTTON_L1->DoomPadAction.PREVIOUS_WEAPON
            KeyEvent.KEYCODE_BUTTON_START->DoomPadAction.PAUSE
            KeyEvent.KEYCODE_BUTTON_B,KeyEvent.KEYCODE_BACK->DoomPadAction.CANCEL
            KeyEvent.KEYCODE_DPAD_UP->DoomPadAction.UP
            KeyEvent.KEYCODE_DPAD_DOWN->DoomPadAction.DOWN
            KeyEvent.KEYCODE_DPAD_LEFT,KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_BUTTON_R2,KeyEvent.KEYCODE_BUTTON_L2,
            KeyEvent.KEYCODE_BUTTON_THUMBR->null
            else->return false
        }
        if(event.action!=KeyEvent.ACTION_DOWN&&event.action!=KeyEvent.ACTION_UP)return false
        attach(event.deviceId)
        if(event.action==KeyEvent.ACTION_UP)down.remove(event.keyCode)
        else if(down.add(event.keyCode)&&event.repeatCount==0&&action!=null)actions.addLast(action)
        return true
    }
    fun consumeActions():List<DoomPadAction>{
        val queued=actions.toList()
        actions.clear()
        return queued
    }
    fun snapshot():DoomInput{
        if(deviceId>=0&&InputDevice.getDevice(deviceId)==null)reset()
        val dpadX=if(KeyEvent.KEYCODE_DPAD_RIGHT in down)1f else if(KeyEvent.KEYCODE_DPAD_LEFT in down)-1f else hatX
        val dpadY=if(KeyEvent.KEYCODE_DPAD_DOWN in down)1f else if(KeyEvent.KEYCODE_DPAD_UP in down)-1f else hatY
        return DoomInput(
            forward=(-ly-dpadY).coerceIn(-1f,1f),
            strafe=(lx+dpadX).coerceIn(-1f,1f),
            turn=rx*1.35f,
            fire=rt>.30f||KeyEvent.KEYCODE_BUTTON_R2 in down,
            punch=lt>.36f||KeyEvent.KEYCODE_BUTTON_L2 in down||KeyEvent.KEYCODE_BUTTON_THUMBR in down,
            lookVertical=ry
        )
    }
}
