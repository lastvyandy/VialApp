package cl.vialapp.app.ui.games.doom

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path

/**
 * Original low-resolution first-person fist artwork, cached once for the whole game.
 *
 * The two mirrored sprites use a single 128x144 drawing with a hand-painted palette:
 * heavy inked contours, four distinct knuckles, thumb, skin planes and worn wrist wrap.
 * No DOOM WAD assets or runtime image allocations are involved.
 */
internal object DoomFistSprite {
    val bitmap:Bitmap by lazy {
        val bitmap=Bitmap.createBitmap(128,144,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(bitmap)
        val ink=Paint().apply{isAntiAlias=false;isFilterBitmap=false}

        fun color(rgb:Int)=Color.rgb(rgb shr 16 and 255,rgb shr 8 and 255,rgb and 255)
        fun polygon(rgb:Int,vararg pts:Int){
            val path=Path()
            path.moveTo(pts[0].toFloat(),pts[1].toFloat())
            for(i in 2 until pts.size step 2)path.lineTo(pts[i].toFloat(),pts[i+1].toFloat())
            path.close()
            ink.color=color(rgb)
            canvas.drawPath(path,ink)
        }
        fun rect(left:Int,top:Int,right:Int,bottom:Int,rgb:Int){
            ink.color=color(rgb)
            canvas.drawRect(left.toFloat(),top.toFloat(),(right+1).toFloat(),(bottom+1).toFloat(),ink)
        }
        fun oval(left:Int,top:Int,right:Int,bottom:Int,rgb:Int){
            ink.color=color(rgb)
            canvas.drawOval(left.toFloat(),top.toFloat(),right.toFloat(),bottom.toFloat(),ink)
        }
        fun stroke(rgb:Int,weight:Float,vararg pts:Int){
            ink.color=color(rgb);ink.strokeWidth=weight;ink.style=Paint.Style.STROKE
            val path=Path()
            path.moveTo(pts[0].toFloat(),pts[1].toFloat())
            for(i in 2 until pts.size step 2)path.lineTo(pts[i].toFloat(),pts[i+1].toFloat())
            canvas.drawPath(path,ink)
            ink.style=Paint.Style.FILL
        }

        // Wrist and forearm extend underneath the HUD, not floating above it.
        polygon(0x190f12,24,144,14,118,17,90,25,75,35,64,71,65,91,77,103,102,117,144)
        polygon(0x5b322c,28,144,20,119,22,93,32,72,44,69,70,72,86,84,98,106,109,144)
        polygon(0x9b6148,29,144,26,111,29,89,41,76,65,77,83,89,91,112,102,144)
        polygon(0xc88a66,39,142,33,120,34,94,44,83,64,87,77,100,82,116,87,144)
        polygon(0xdda079,49,141,41,113,45,97,60,95,75,104,77,117,81,144)

        // Used leather wrapping: deep shadows separate the fist from its forearm.
        polygon(0x271d1d,18,105,29,104,48,116,77,117,102,111,109,128,99,141,26,135)
        polygon(0x4b3430,20,110,36,111,56,122,91,121,104,116,104,127,88,134,40,126,21,121)
        polygon(0x8d6248,25,110,34,109,53,119,93,120,93,124,48,122,27,117)
        stroke(0xb78563,2f,31,115,58,127,89,127)

        // Clenched silhouette is irregular and anatomically readable at 400px render width.
        polygon(0x1c1214,17,88,12,69,15,51,27,43,35,33,48,28,59,33,70,29,82,34,92,38,104,47,110,62,104,81,93,96,76,107,44,106,24,98)
        polygon(0x6d4036,21,87,17,67,19,54,30,45,36,38,49,34,61,39,70,35,81,39,92,43,102,49,105,63,99,79,89,92,74,102,45,101,28,94)
        polygon(0xb27a5e,26,87,23,61,30,49,44,39,58,43,69,39,82,45,97,52,98,68,92,82,79,95,46,96,31,90)
        polygon(0xd09a78,35,91,32,64,38,50,56,47,73,49,88,55,92,67,88,82,77,92,52,93)

        // Individual knuckles have separate shadows and reflected light, not four rectangles.
        polygon(0x754538,27,54,28,43,35,38,46,40,50,49,48,62,36,65)
        polygon(0x754538,45,50,45,39,52,35,63,39,66,50,62,64,51,64)
        polygon(0x754538,62,49,62,38,70,36,81,42,81,54,75,66,64,64)
        polygon(0x754538,79,55,78,45,88,42,99,49,101,62,94,72,82,66)
        oval(31,46,45,60,0xd99f7a)
        oval(49,42,64,59,0xd99f7a)
        oval(66,43,80,60,0xd99f7a)
        oval(83,49,98,64,0xbd8063)
        polygon(0xf0bf97,31,49,35,43,41,43,45,48,43,53,34,54)
        polygon(0xf0bf97,51,45,54,39,60,41,63,46,61,49,53,50)
        polygon(0xe8b18c,68,45,72,40,77,44,78,50,73,51,67,49)
        polygon(0xd99a78,85,53,88,47,95,52,96,57,89,61)
        polygon(0x864b3b,29,63,36,67,48,63,60,66,74,63,89,70,85,77,71,73,61,75,48,72,35,75)
        stroke(0x54302a,2f,35,68,42,71)
        stroke(0x54302a,2f,53,69,59,73)
        stroke(0x54302a,2f,70,70,74,75)

        // Thumb wraps across the fingers and conveys the depth of a closed fist.
        polygon(0x563228,18,78,22,65,33,61,44,64,58,73,67,80,65,90,53,97,35,92,22,89)
        polygon(0xad7055,23,78,27,68,35,66,44,69,56,78,62,83,60,88,50,92,36,88,25,86)
        polygon(0xdfa47f,31,74,36,70,45,75,55,81,52,85,37,83)
        stroke(0x744636,2f,45,87,55,91)
        polygon(0x724334,32,93,44,98,70,99,90,87,84,96,70,104,44,103)

        // Pixel-perfect scuffs, subtle veins and tiny flesh highlights.
        for((x,y) in listOf(33 to 56,40 to 59,61 to 57,77 to 60,87 to 65,
            46 to 89,55 to 94,68 to 88,39 to 83,80 to 91,47 to 51,73 to 52)){
            rect(x,y,x+2,y+1,if(x%2==0)0x774739 else 0xe1b18b)
        }
        bitmap
    }
}
