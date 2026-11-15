package cl.vialapp.app.ui.games.arcade

/** The signal is derived solely from the engine; no UI-side traffic rules. */
internal enum class DriveCue { NORMAL, STOP, WAIT, GO, STOP_DONE, SCHOOL_AHEAD, SCHOOL }
internal fun driveCue(frame:DriveFrame):DriveCue = when {
    frame.stopChallenge && frame.stopCompleted && frame.stopped -> DriveCue.GO
    frame.stopChallenge && frame.stopCompleted -> DriveCue.STOP_DONE
    frame.stopChallenge && frame.stopped -> DriveCue.WAIT
    frame.stopChallenge -> DriveCue.STOP
    frame.schoolZone -> DriveCue.SCHOOL
    frame.elapsed in 15f..18f || frame.elapsed in 40f..43f -> DriveCue.SCHOOL_AHEAD
    else -> DriveCue.NORMAL
}
