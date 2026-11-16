package cl.vialapp.app.ui.games.rally

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import kotlin.math.*
import kotlin.random.Random

private data class Landscape(val land:Color,val shoulder:Color,val road:Color,val curb:Color,val decor:Color)
private fun landscape(track:RallyTrack)=when(track.terrain){
    0->Landscape(Color(0xFFCE9D67),Color(0xFFB68150),Color(0xFF796E62),Color(0xFFF2D4A6),Color(0xFF9B6C49))
    1->Landscape(Color(0xFF9A9D8D),Color(0xFFCBC5B7),Color(0xFF414A55),Color(0xFFEBD8BC),Color(0xFFBB705B))
    2->Landscape(Color(0xFF547749),Color(0xFF96805D),Color(0xFF80715E),Color(0xFFC5B88C),Color(0xFF245A41))
    3->Landscape(Color(0xFF78ABAB),Color(0xFFDBC297),Color(0xFF4A5861),Color(0xFFF1E1B9),Color(0xFF418D80))
    4->Landscape(Color(0xFF202C42),Color(0xFF34445D),Color(0xFF343F50),Color(0xFF8DE6E9),Color(0xFF6778AB))
    5->Landscape(Color(0xFFDCE7EF),Color(0xFFAAC1D6),Color(0xFF748390),Color(0xFFF5FAFF),Color(0xFF83A2BD))
    6->Landscape(Color(0xFF514750),Color(0xFF805C51),Color(0xFF494548),Color(0xFFFFB57D),Color(0xFF94614E))
    else->Landscape(Color(0xFF739885),Color(0xFFA3B39B),Color(0xFF55635D),Color(0xFFD9E2D0),Color(0xFF437765))
}
private fun V2.o()=Offset(x,y)
private fun pathFor(track:RallyTrack)=Path().apply{
    moveTo(track.nodes[0].x,track.nodes[0].y)
    for(p in track.nodes.drop(1))lineTo(p.x,p.y)
    close()
}
private data class Scenery(val pos:V2,val radius:Float,val style:Int)
private fun scenery(track:RallyTrack):List<Scenery>{
    val random=Random(track.id.hashCode())
    val out=mutableListOf<Scenery>()
    for(i in 0 until 100){
        val d=track.length*i/100f+random.nextFloat()*8f
        val side=if(i%2==0)1f else -1f
        val position=track.at(d,side*(track.halfWidth+28f+random.nextFloat()*95f)).first
        val minDistance=track.nodes.minOf{(it-position).length()}
        if(minDistance>track.halfWidth+22f)out+=Scenery(position,12f+random.nextFloat()*13f,i%4)
    }
    return out
}
@Composable internal fun RallyPreview(track:RallyTrack,modifier:Modifier){
    val path=remember(track){pathFor(track)}
    val colors=remember(track){landscape(track)}
    Canvas(modifier){
        drawRect(colors.land)
        val minX=track.nodes.minOf{it.x};val maxX=track.nodes.maxOf{it.x}
        val minY=track.nodes.minOf{it.y};val maxY=track.nodes.maxOf{it.y}
        val u=minOf(size.width/(maxX-minX+140f),size.height/(maxY-minY+140f))
        translate(size.width/2,size.height/2){
            scale(u,u,pivot=Offset.Zero){
                translate(-(minX+maxX)/2,-(minY+maxY)/2){
                    drawPath(path,colors.shoulder,style=Stroke(104f,cap=StrokeCap.Round,join=StrokeJoin.Round))
                    drawPath(path,colors.road,style=Stroke(82f,cap=StrokeCap.Round,join=StrokeJoin.Round))
                    drawPath(path,Color.White.copy(alpha=.6f),style=Stroke(3f,pathEffect=PathEffect.dashPathEffect(floatArrayOf(13f,12f))))
                    val start=track.at(0f)
                    drawCircle(Color.White,12f,start.first.o())
                }
            }
        }
    }
}
@Composable internal fun RallyScene(track:RallyTrack,f:RallyFrame,player:ImageBitmap,rivals:List<ImageBitmap>,neon:Color?,modifier:Modifier){
    val trackPath=remember(track){pathFor(track)}
    val objects=remember(track){scenery(track)}
    val colors=remember(track){landscape(track)}
    Canvas(modifier){
        val u=size.width/245f
        val focus=Offset(size.width*.5f,size.height*.61f)
        drawRect(colors.land)
        // One world transform for track, roadside geometry, pickups and vehicles.
        translate(focus.x,focus.y){
            scale(u,u,pivot=Offset.Zero){
                rotate(-f.angle*180f/PI.toFloat()-90f,pivot=Offset.Zero){
                    translate(-f.pos.x,-f.pos.y){
                        drawPath(trackPath,colors.shoulder,style=Stroke(track.halfWidth*2+12f,cap=StrokeCap.Round,join=StrokeJoin.Round))
                        drawPath(trackPath,colors.road,style=Stroke(track.halfWidth*2,cap=StrokeCap.Round,join=StrokeJoin.Round))
                        for(i in 0..((track.length/33f).toInt())){
                            val a=track.at(i*33f);val b=track.at(i*33f+14f)
                            if((a.first-f.pos).length()>420f)continue
                            drawLine(Color.White.copy(alpha=.48f),a.first.o(),b.first.o(),1.5f,StrokeCap.Round)
                            for(side in listOf(-1f,1f)){
                                val left=track.at(i*33f,side*(track.halfWidth-2f)).first
                                val right=track.at(i*33f+14f,side*(track.halfWidth-2f)).first
                                drawLine(colors.curb,left.o(),right.o(),3.2f,StrokeCap.Round)
                            }
                        }
                        val start=track.at(0f)
                        translate(start.first.x,start.first.y){rotate(start.second*180f/PI.toFloat(),pivot=Offset.Zero){
                            for(i in 0 until 12)for(j in 0..1)drawRect(if((i+j)%2==0)Color.White else Color(0xFF23303B),Offset(-6f+j*5f,-track.halfWidth+i*track.halfWidth/6f),Size(5f,track.halfWidth/6f))
                        }}
                        for(obj in objects){
                            if((obj.pos-f.pos).length()>420f)continue
                            val p=obj.pos.o();val r=obj.radius
                            drawOval(Color.Black.copy(alpha=.19f),p+Offset(-r*.75f,-r*.15f),Size(r*1.7f,r*.8f))
                            when(track.terrain){
                                1,4->{
                                    drawRect(colors.decor,p-Offset(r,r*.8f),Size(r*1.8f,r*1.6f))
                                    drawRect(Color(0xFFEBE0CB).copy(alpha=.22f),p-Offset(r*.8f,r*.65f),Size(r*1.4f,r*.24f))
                                    for(j in 0..2)drawRect(Color(0xFF27374A),p+Offset(-r*.65f+j*r*.45f,-r*.20f),Size(r*.24f,r*.50f))
                                }
                                2,7->{
                                    drawCircle(colors.decor,r,p)
                                    drawCircle(colors.decor.copy(alpha=.7f),r*.70f,p-Offset(r*.35f,r*.35f))
                                    drawCircle(Color.White.copy(alpha=.10f),r*.54f,p-Offset(r*.2f,r*.25f))
                                }
                                else->{
                                    val rock=Path().apply{moveTo(p.x-r,p.y+r*.25f);lineTo(p.x-r*.48f,p.y-r*.7f);lineTo(p.x+r*.29f,p.y-r*.85f);lineTo(p.x+r,p.y+r*.2f);lineTo(p.x+r*.45f,p.y+r*.7f);close()}
                                    drawPath(rock,colors.decor)
                                    drawLine(Color.White.copy(alpha=.23f),p-Offset(r*.45f,r*.55f),p+Offset(r*.28f,-r*.37f),2f)
                                }
                            }
                        }
                        for(o in f.oils)drawOval(Color(0xFF171D24).copy(alpha=.70f),o.pos.o()-Offset(10f,7f),Size(20f,14f))
                        for(b in f.boxes){
                            val p=track.at(b.progress,b.lane).first.o()
                            drawCircle(Color(0xFF45DBF2).copy(alpha=.25f),17f,p)
                            drawRoundRect(Color(0xFF137F9D),p-Offset(8f,8f),Size(16f,16f),CornerRadius(3f))
                            drawRoundRect(Color(0xFFB9F8FF),p-Offset(8f,8f),Size(16f,16f),CornerRadius(3f),style=Stroke(1.6f))
                            drawLine(Color.White,p-Offset(4f,0f),p+Offset(4f,0f),2f)
                            drawLine(Color.White,p-Offset(0f,4f),p+Offset(0f,4f),2f)
                        }
                        // Same alternating lateral placements as the collection logic.
                        for(b in f.coins){
                            val index=((b/track.length)*RALLY_COIN_COUNT).toInt()
                            val p=track.at(b,if(index%2==0)-21f else 21f).first.o()
                            drawCircle(Color(0xFFFFC64E),5f,p);drawCircle(Color(0xFFFFEBA7),3.7f,p,style=Stroke(1f))
                        }
                        for((i,r) in f.rivals.withIndex()){
                            val at=track.at(r.progress,r.lane)
                            if((at.first-f.pos).length()>420f)continue
                            car(rivals[i%rivals.size],at.first.o(),at.second,null,false)
                            if(r.slowed>0f)drawCircle(Color(0xFFB99BFF),15f,at.first.o(),style=Stroke(1.5f))
                        }
                        val at=f.pos.o()
                        if(f.offroad||abs(f.drift)>.05f){
                            val back=Offset(-cos(f.angle),-sin(f.angle))
                            for(i in 1..5)drawCircle(colors.shoulder.copy(alpha=.65f-i*.08f),3f+i*1.2f,at+back*(13f+i*5f)+Offset(sin(f.elapsed*13f+i)*4f,0f))
                        }
                        if(f.nitro>0f){
                            val back=Offset(-cos(f.angle),-sin(f.angle))
                            drawLine(Color(0xFF5ADFFF).copy(alpha=.55f),at+back*12f,at+back*40f,7f,StrokeCap.Round)
                        }
                        car(player,at,f.angle,neon,f.hit>0f)
                        if(f.shield>0f)drawCircle(Color(0xFF7CEAFF).copy(alpha=.8f),22f,at,style=Stroke(2f))
                        if(f.pulse>0f)drawCircle(Color(0xFFD4A6FF).copy(alpha=f.pulse/.65f),20f+(.65f-f.pulse)*180f,at,style=Stroke(3f))
                    }
                }
            }
        }
        // Fixed north-up minimap, independent of camera.
        val minX=track.nodes.minOf{it.x};val maxX=track.nodes.maxOf{it.x};val minY=track.nodes.minOf{it.y};val maxY=track.nodes.maxOf{it.y}
        val mapSize=size.width*.22f;val mapScale=mapSize/maxOf(maxX-minX+80f,maxY-minY+80f)
        val origin=Offset(size.width-mapSize-14f,14f)
        drawRoundRect(Color(0xFF141D2C).copy(alpha=.85f),origin,Size(mapSize,mapSize),CornerRadius(10f))
        translate(origin.x+mapSize/2,origin.y+mapSize/2){scale(mapScale,mapScale,pivot=Offset.Zero){
            translate(-(minX+maxX)/2,-(minY+maxY)/2){
                drawPath(trackPath,Color(0xFFC0CED8),style=Stroke(15f,cap=StrokeCap.Round,join=StrokeJoin.Round))
                for(r in f.rivals)drawCircle(Color(0xFFFF9970),9f,track.at(r.progress,r.lane).first.o())
                drawCircle(Color(0xFF59EAAC),13f,f.pos.o())
            }
        }}
    }
}
private fun DrawScope.car(sprite:ImageBitmap,at:Offset,heading:Float,neon:Color?,hit:Boolean){
    translate(at.x,at.y){rotate(heading*180f/PI.toFloat()+90f,pivot=Offset.Zero){
        drawOval(Color.Black.copy(alpha=.36f),Offset(-12f,-14f),Size(27f,35f))
        if(neon!=null)drawOval(neon.copy(alpha=.36f),Offset(-14f,-19f),Size(28f,38f))
        drawImage(sprite,dstOffset=androidx.compose.ui.unit.IntOffset(-12,-18),dstSize=androidx.compose.ui.unit.IntSize(24,36),alpha=if(hit).65f else 1f)
    }}
}
