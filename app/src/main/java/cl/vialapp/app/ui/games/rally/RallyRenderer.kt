package cl.vialapp.app.ui.games.rally

import android.content.Context
import android.opengl.GLES20 as GL
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import cl.vialapp.app.ui.career.GarageKit
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

internal data class RallyLoadout(val style:String,val paint:Color,val kit:GarageKit)
private val rivalLoadouts=listOf(
    RallyLoadout("sport",Color(0xFFE46D61),GarageKit()),
    RallyLoadout("hatch",Color(0xFFECC45E),GarageKit()),
    RallyLoadout("sedan",Color(0xFF79CD9B),GarageKit()),
    RallyLoadout("suv",Color(0xFFAF9CE8),GarageKit()),
    RallyLoadout("compact",Color(0xFFF0F2F5),GarageKit()))

/** Native OpenGL ES 2.0, one static world batch and six shared garage meshes.
 * The renderer reads immutable snapshots; all gameplay remains on the simulation thread. */
@Composable internal fun Rally3D(track:RallyTrack,frame:RallyFrame,loadout:RallyLoadout,modifier:Modifier){
    val context=LocalContext.current
    val renderer=remember(track,loadout){RallyRenderer(track,loadout,frame)}
    val view=remember(context,renderer){
        GLSurfaceView(context).apply{
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8,8,8,8,16,0)
            preserveEGLContextOnPause=true
            setRenderer(renderer)
            renderMode=GLSurfaceView.RENDERMODE_WHEN_DIRTY
        }
    }
    DisposableEffect(view){
        var ownerContext=context
        while(ownerContext is android.content.ContextWrapper&&ownerContext !is LifecycleOwner)ownerContext=ownerContext.baseContext
        val lifecycle=(ownerContext as? LifecycleOwner)?.lifecycle
        val observer=LifecycleEventObserver{_,event->when(event){
            Lifecycle.Event.ON_PAUSE->view.onPause()
            Lifecycle.Event.ON_RESUME->view.onResume()
            else->Unit
        }}
        lifecycle?.addObserver(observer)
        onDispose{lifecycle?.removeObserver(observer);view.onPause()}
    }
    AndroidView(factory={view},modifier=modifier,update={
        renderer.frame=frame
        it.requestRender()
    })
}

private class RallyRenderer(private val track:RallyTrack,private val loadout:RallyLoadout,initial:RallyFrame):GLSurfaceView.Renderer{
    @Volatile var frame=initial
    private val palette=rallyPalette(track)
    private data class Batch(val id:Int,val count:Int)
    private var program=0
    private var position=0;private var color=0;private var mvp=0;private var fog=0
    private var world=Batch(0,0)
    private var cars=emptyList<Batch>()
    private var box=Batch(0,0);private var coin=Batch(0,0);private var shadow=Batch(0,0);private var oil=Batch(0,0)
    private var shield=Batch(0,0);private var flame=Batch(0,0);private var pulse=Batch(0,0)
    private var neon:Batch?=null
    private val projection=FloatArray(16);private val camera=FloatArray(16)
    private val vp=FloatArray(16);private val model=FloatArray(16);private val matrix=FloatArray(16)
    private var aspect=1f
    private var cameraAngle=initial.angle
    private var cameraX=initial.pos.x
    private var cameraZ=initial.pos.y
    private var lastElapsed=initial.elapsed
    override fun onSurfaceCreated(gl:GL10?,config:EGLConfig?){
        GL.glClearColor(palette.sky.red,palette.sky.green,palette.sky.blue,1f)
        GL.glEnable(GL.GL_DEPTH_TEST)
        GL.glDepthFunc(GL.GL_LEQUAL)
        GL.glDisable(GL.GL_CULL_FACE)
        program=link(
            "attribute vec3 aPos; attribute vec3 aColor; uniform mat4 uMvp; varying mediump vec3 vColor; varying mediump float vDepth; void main(){ gl_Position=uMvp*vec4(aPos,1.0); vColor=aColor; vDepth=gl_Position.w; }",
            "precision mediump float; varying mediump vec3 vColor; varying mediump float vDepth; uniform vec3 uFog; void main(){ float fog=smoothstep(280.0,1050.0,vDepth); gl_FragColor=vec4(mix(vColor,uFog,fog),1.0); }")
        position=GL.glGetAttribLocation(program,"aPos");color=GL.glGetAttribLocation(program,"aColor")
        mvp=GL.glGetUniformLocation(program,"uMvp");fog=GL.glGetUniformLocation(program,"uFog")
        world=upload(makeRallyWorld(track,palette))
        cars=(listOf(loadout)+rivalLoadouts).map{upload(makeRallyCar(it.style,it.paint,it.kit))}
        box=upload(makePickup(true));coin=upload(makePickup(false))
        shadow=upload(RallyMeshBuilder().apply{disc(0f,.20f,0f,1f,shade(palette.road,.66f))}.finish())
        oil=upload(RallyMeshBuilder().apply{disc(0f,.21f,0f,1f,Color(0xFF212732))}.finish())
        neon=loadout.kit.neonColor()?.let{c->upload(RallyMeshBuilder().apply{disc(0f,.24f,0f,1f,c)}.finish())}
        // Hollow outlines leave the car visible. Meshes are built once per EGL context.
        shield=upload(ringMesh(Color(0xFF6DEAFF),.10f))
        pulse=upload(ringMesh(Color(0xFFD5A4FF),.15f))
        flame=upload(RallyMeshBuilder().apply{cone(0f,0f,0f,1.8f,9f,Color(0xFF56D9FF),5)}.finish())
    }
    override fun onSurfaceChanged(gl:GL10?,width:Int,height:Int){
        GL.glViewport(0,0,width,height)
        aspect=width.toFloat()/height.coerceAtLeast(1)
        Matrix.perspectiveM(projection,0,56f,aspect,.9f,1700f)
    }
    override fun onDrawFrame(gl:GL10?){
        val f=frame
        GL.glClear(GL.GL_COLOR_BUFFER_BIT or GL.GL_DEPTH_BUFFER_BIT)
        if(program==0)return
        val elapsedDelta=(f.elapsed-lastElapsed).coerceIn(0f,.1f)
        lastElapsed=f.elapsed
        val dx=f.pos.x-cameraX;val dz=f.pos.y-cameraZ
        if(dx*dx+dz*dz>6400f){cameraX=f.pos.x;cameraZ=f.pos.y;cameraAngle=f.angle}
        val mix=(elapsedDelta*15f).coerceIn(.12f,.85f)
        cameraX+= (f.pos.x-cameraX)*mix;cameraZ+=(f.pos.y-cameraZ)*mix
        cameraAngle+=wrappedDelta(f.angle-cameraAngle,2f*PI.toFloat())*(mix*.80f)
        val forwardX=cos(cameraAngle);val forwardZ=sin(cameraAngle)
        val back=if(f.nitro>0f)72f else 61f
        Matrix.setLookAtM(camera,0,cameraX-forwardX*back,33f,cameraZ-forwardZ*back,
            cameraX+forwardX*43f,9f,cameraZ+forwardZ*43f,0f,1f,0f)
        Matrix.multiplyMM(vp,0,projection,0,camera,0)
        GL.glUseProgram(program)
        GL.glUniform3f(fog,palette.fog.red,palette.fog.green,palette.fog.blue)
        GL.glEnableVertexAttribArray(position);GL.glEnableVertexAttribArray(color)
        draw(world,0f,0f,0f)
        for(o in f.oils)draw(oil,o.pos.x,0f,o.pos.y,sx=13f,sy=1f,sz=10f)
        for(b in f.boxes){
            val p=track.at(b).first
            if(near(p,f.pos,740f))draw(box,p.x,8f+sin(f.elapsed*4f+b)*1.7f,p.y,f.elapsed*85f)
        }
        for(b in f.coins){
            val i=((b/track.length)*18f).toInt()
            val p=track.at(b,if(i%2==0)-21f else 21f).first
            if(near(p,f.pos,570f))draw(coin,p.x,5f+sin(f.elapsed*5f+b),p.y,f.elapsed*155f)
        }
        for((i,r) in f.rivals.withIndex()){
            val at=track.at(r.progress,r.lane)
            if(near(at.first,f.pos,780f)){
                drawCar(cars[(i+1)%cars.size],at.first,at.second)
                if(r.slowed>0f)draw(shield,at.first.x,9f,at.first.y,sx=15f,sy=1f,sz=15f)
            }
        }
        neon?.let{draw(it,f.pos.x,.10f,f.pos.y,sx=9f,sy=1f,sz=15f)}
        drawCar(cars[0],f.pos,f.angle)
        if(f.nitro>0f){
            val bx=-cos(f.angle);val bz=-sin(f.angle)
            for(side in listOf(-1f,1f)){
                val x=f.pos.x+bx*15f-bz*side*4f
                val z=f.pos.y+bz*15f+bx*side*4f
                draw(flame,x,2.7f,z,90f-f.angle*180f/PI.toFloat(),1f,1f+sin(f.elapsed*50f)*.20f,1f,90f)
            }
        }
        if(f.shield>0f)draw(shield,f.pos.x,8f,f.pos.y,f.elapsed*45f,17f,1f,17f)
        if(f.pulse>0f){
            val radius=20f+(.65f-f.pulse)*170f
            draw(pulse,f.pos.x,3f,f.pos.y,sx=radius,sy=1f,sz=radius)
        }
        GL.glDisableVertexAttribArray(position);GL.glDisableVertexAttribArray(color)
        GL.glBindBuffer(GL.GL_ARRAY_BUFFER,0)
    }
    private fun near(a:V2,b:V2,r:Float):Boolean{val d=a-b;return d.dot(d)<r*r}
    private fun drawCar(batch:Batch,p:V2,angle:Float){
        val yaw=90f-angle*180f/PI.toFloat()
        draw(shadow,p.x,0f,p.y,yaw,8f,1f,14f)
        draw(batch,p.x,.38f,p.y,yaw,6.3f,6.3f,6.3f)
    }
    private fun draw(batch:Batch,x:Float,y:Float,z:Float,yaw:Float=0f,sx:Float=1f,sy:Float=1f,sz:Float=1f,pitch:Float=0f){
        Matrix.setIdentityM(model,0)
        Matrix.translateM(model,0,x,y,z)
        Matrix.rotateM(model,0,yaw,0f,1f,0f)
        if(pitch!=0f)Matrix.rotateM(model,0,pitch,1f,0f,0f)
        Matrix.scaleM(model,0,sx,sy,sz)
        Matrix.multiplyMM(matrix,0,vp,0,model,0)
        GL.glUniformMatrix4fv(mvp,1,false,matrix,0)
        GL.glBindBuffer(GL.GL_ARRAY_BUFFER,batch.id)
        GL.glVertexAttribPointer(position,3,GL.GL_FLOAT,false,24,0)
        GL.glVertexAttribPointer(color,3,GL.GL_FLOAT,false,24,12)
        GL.glDrawArrays(GL.GL_TRIANGLES,0,batch.count)
    }
    private fun upload(vertices:FloatArray):Batch{
        val ids=IntArray(1);GL.glGenBuffers(1,ids,0)
        GL.glBindBuffer(GL.GL_ARRAY_BUFFER,ids[0])
        val buffer=ByteBuffer.allocateDirect(vertices.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        buffer.put(vertices);buffer.position(0)
        GL.glBufferData(GL.GL_ARRAY_BUFFER,vertices.size*4,buffer,GL.GL_STATIC_DRAW)
        return Batch(ids[0],vertices.size/6)
    }
    private fun ringMesh(c:Color,w:Float):FloatArray{
        val b=RallyMeshBuilder()
        for(i in 0 until 36){
            val a=i*2f*PI.toFloat()/36f;val t=(i+1)*2f*PI.toFloat()/36f
            fun v(angle:Float,rad:Float)=cl.vialapp.app.ui.career.V3(cos(angle)*rad,0f,sin(angle)*rad)
            b.quad(v(a,1f),v(t,1f),v(t,1f-w),v(a,1f-w),c)
        }
        return b.finish()
    }
    private fun link(vertex:String,fragment:String):Int{
        fun shader(type:Int,source:String):Int{
            val id=GL.glCreateShader(type);GL.glShaderSource(id,source);GL.glCompileShader(id)
            val status=IntArray(1);GL.glGetShaderiv(id,GL.GL_COMPILE_STATUS,status,0)
            check(status[0]!=0){GL.glGetShaderInfoLog(id)}
            return id
        }
        val v=shader(GL.GL_VERTEX_SHADER,vertex);val f=shader(GL.GL_FRAGMENT_SHADER,fragment)
        val p=GL.glCreateProgram();GL.glAttachShader(p,v);GL.glAttachShader(p,f);GL.glLinkProgram(p)
        val status=IntArray(1);GL.glGetProgramiv(p,GL.GL_LINK_STATUS,status,0)
        check(status[0]!=0){GL.glGetProgramInfoLog(p)}
        GL.glDeleteShader(v);GL.glDeleteShader(f)
        return p
    }
}
