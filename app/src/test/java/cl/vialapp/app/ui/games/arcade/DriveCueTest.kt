package cl.vialapp.app.ui.games.arcade

import org.junit.Assert.assertEquals
import org.junit.Test

class DriveCueTest {
    @Test fun distinguishesStopWaitGoAndCompletion(){
        val start=DriveFrame(elapsed=10f,stopChallenge=true)
        assertEquals(DriveCue.STOP,driveCue(start))
        assertEquals(DriveCue.WAIT,driveCue(start.copy(stopped=true)))
        assertEquals(DriveCue.GO,driveCue(start.copy(stopped=true,stopCompleted=true)))
        assertEquals(DriveCue.STOP_DONE,driveCue(start.copy(stopCompleted=true)))
        assertEquals(DriveCue.NORMAL,driveCue(start.copy(elapsed=29f,stopChallenge=false,stopCompleted=true)))
    }
    @Test fun schoolWarningsFollowEngineWindows(){
        assertEquals(DriveCue.SCHOOL_AHEAD,driveCue(DriveFrame(elapsed=16f)))
        assertEquals(DriveCue.SCHOOL_AHEAD,driveCue(DriveFrame(elapsed=41f)))
        assertEquals(DriveCue.SCHOOL,driveCue(DriveFrame(elapsed=18f,schoolZone=true)))
        assertEquals(DriveCue.STOP,driveCue(DriveFrame(elapsed=15f,stopChallenge=true)))
        assertEquals(DriveCue.NORMAL,driveCue(DriveFrame(elapsed=54f)))
    }
}
