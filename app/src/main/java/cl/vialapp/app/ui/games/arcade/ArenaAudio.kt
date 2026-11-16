package cl.vialapp.app.ui.games.arcade

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.vialapp.app.R
import java.util.concurrent.ConcurrentHashMap

/** One app-wide SoundPool for short, original synthesized effects. */
internal class ArenaAudio(context:Context){
    private val prefs=context.applicationContext.getSharedPreferences("vial_audio",Context.MODE_PRIVATE)
    var enabled by mutableStateOf(prefs.getBoolean("effects_enabled",true))
        private set
    private val pool=SoundPool.Builder().setMaxStreams(6)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
    private val loaded=ConcurrentHashMap.newKeySet<Int>()
    private val ids:Map<String,Int>
    private val streams=mutableMapOf<String,Int>()
    private val lastPlayed=mutableMapOf<String,Long>()
    private var engineStream=0
    private var engineOwner:Any?=null
    private var foreground=true
    private var closed=false
    init{
        pool.setOnLoadCompleteListener{_,sample,status->if(status==0)loaded.add(sample)}
        ids=mapOf("hit" to pool.load(context,R.raw.arena_hit,1),
            "heavy" to pool.load(context,R.raw.arena_heavy,1),
            "block" to pool.load(context,R.raw.arena_block,1),
            "shield" to pool.load(context,R.raw.arena_shield,1),
            "power" to pool.load(context,R.raw.arena_power,1),
            "coin" to pool.load(context,R.raw.sfx_coin,1),
            "reward" to pool.load(context,R.raw.sfx_reward,1),
            "xp" to pool.load(context,R.raw.sfx_xp,1),
            "correct" to pool.load(context,R.raw.sfx_correct,1),
            "wrong" to pool.load(context,R.raw.sfx_wrong,1),
            "victory" to pool.load(context,R.raw.sfx_victory,1),
            "start" to pool.load(context,R.raw.sfx_start,1),
            "horn" to pool.load(context,R.raw.sfx_horn,1),
            "crash" to pool.load(context,R.raw.sfx_crash,1),
            "turbo" to pool.load(context,R.raw.sfx_turbo,1),
            "jump" to pool.load(context,R.raw.sfx_jump,1),
            "land" to pool.load(context,R.raw.sfx_land,1),
            "tool" to pool.load(context,R.raw.sfx_tool,1),
            "signal" to pool.load(context,R.raw.sfx_signal,1),
            "drift" to pool.load(context,R.raw.sfx_drift,1),
            "engine" to pool.load(context,R.raw.sfx_engine,1),
            "doom_shot" to pool.load(context,R.raw.doom_shot,1),
            "doom_blast" to pool.load(context,R.raw.doom_blast,1),
            "doom_plasma" to pool.load(context,R.raw.doom_plasma,1),
            "doom_door" to pool.load(context,R.raw.doom_door,1),
            "doom_hit" to pool.load(context,R.raw.doom_hit,1),
            "doom_hurt" to pool.load(context,R.raw.doom_hurt,1),
            "doom_pickup" to pool.load(context,R.raw.doom_pickup,1),
            "doom_switch" to pool.load(context,R.raw.doom_switch,1))
    }
    fun setEffectsEnabled(value:Boolean){
        enabled=value
        prefs.edit().putBoolean("effects_enabled",value).apply()
        if(!value)stopAll()
    }
    fun setForeground(value:Boolean){
        foreground=value
        if(!value)stopAll()
    }
    fun play(name:String){
        val id=ids[name]?:return
        if(closed||!enabled||!foreground||!loaded.contains(id)||name=="engine")return
        val now=SystemClock.elapsedRealtime()
        val interval=when(name){"coin"->65L;"hit","heavy","doom_shot","doom_blast","doom_plasma"->75L;"correct","signal","tool"->180L;else->220L}
        if(now-(lastPlayed[name]?:-10000L)<interval)return
        lastPlayed[name]=now
        streams.remove(name)?.let{pool.stop(it)}
        val volume=when(name){"doom_shot"->.48f;"doom_blast"->.62f;"doom_plasma"->.40f;"doom_door"->.43f;"doom_hit"->.24f;"doom_hurt"->.44f;"doom_pickup","doom_switch"->.40f;"horn"->.58f;"crash"->.55f;"coin","reward"->.68f;"signal"->.38f;else->.61f}
        val stream=pool.play(id,volume,volume,1,0,1f)
        if(stream!=0)streams[name]=stream
    }
    fun drive(owner:Any,active:Boolean,rate:Float=1f,gain:Float=.13f){
        if(!active||!enabled||!foreground||closed){stopDrive(owner);return}
        val id=ids["engine"]?:return
        if(!loaded.contains(id))return
        if(engineOwner!==owner){stopEngine();engineOwner=owner}
        val pitch=rate.coerceIn(.5f,2f)
        val volume=gain.coerceIn(0f,.25f)
        if(engineStream==0)engineStream=pool.play(id,volume,volume,2,-1,pitch)
        else{pool.setRate(engineStream,pitch);pool.setVolume(engineStream,volume,volume)}
    }
    fun stopDrive(owner:Any){if(engineOwner===owner)stopEngine()}
    private fun stopEngine(){
        if(engineStream!=0)pool.stop(engineStream)
        engineStream=0;engineOwner=null
    }
    private fun stopAll(){
        stopEngine()
        streams.values.forEach{pool.stop(it)}
        streams.clear();lastPlayed.clear()
    }
    fun close(){if(!closed){stopAll();closed=true;pool.release();loaded.clear()}}
}
internal val LocalGameAudio=staticCompositionLocalOf<ArenaAudio?>{null}

@Composable internal fun GameAudioSetting(){
    val audio=LocalGameAudio.current?:return
    Card(modifier=Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){
        Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){
            Column(Modifier.weight(1f)){
                Text("Efectos de sonido",fontWeight=FontWeight.Bold)
                Text("Monedas, recompensas y minijuegos",style=MaterialTheme.typography.bodySmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked=audio.enabled,onCheckedChange={enabled->
                audio.setEffectsEnabled(enabled)
                if(enabled)audio.play("coin")
            })
        }
    }
}
