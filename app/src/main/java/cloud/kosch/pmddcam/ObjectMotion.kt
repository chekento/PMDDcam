package cloud.kosch.pmddcam

/** Motion-capable classes, not a claim that a parked vehicle was moving during exposure. */
object ObjectMotion {
    data class Profile(val role:Role,val motion:Motion=Motion.DRIFT,val angle:Float=0f,val speed:Float=0f,val intensity:Float=0f)
    fun profile(category:Int):Profile=when(category){
        1->Profile(Role.DYNAMIC,Motion.APPROACH,15f,.45f,.72f)
        2,3,4,6,7,8->Profile(Role.DYNAMIC,Motion.APPROACH,8f,.68f,.85f)
        5,16->Profile(Role.DYNAMIC,Motion.DRIFT,342f,.8f,.95f)
        9->Profile(Role.DYNAMIC,Motion.FLOW,5f,.48f,.75f)
        in 17..25->Profile(Role.DYNAMIC,Motion.APPROACH,20f,.6f,.82f)
        34,37,38,43->Profile(Role.DYNAMIC,Motion.ROTATE,12f,.65f,.75f)
        else->Profile(Role.ANCHOR)
    }
}
