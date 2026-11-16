package cl.vialapp.app.ui.games.arcade

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import cl.vialapp.app.R
import java.util.concurrent.ConcurrentHashMap

/** Short original synthesized effects only. No music or network access. */
internal class ArenaAudio(context:Context){
    private val pool=SoundPool.Builder().setMaxStreams(3)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
    private val loaded=ConcurrentHashMap.newKeySet<Int>()
    private val ids:Map<String,Int>
    private var closed=false
    init{
        pool.setOnLoadCompleteListener{_,sample,status->if(status==0)loaded.add(sample)}
        ids=mapOf("hit" to pool.load(context,R.raw.arena_hit,1),
            "heavy" to pool.load(context,R.raw.arena_heavy,1),
            "block" to pool.load(context,R.raw.arena_block,1),
            "shield" to pool.load(context,R.raw.arena_shield,1),
            "power" to pool.load(context,R.raw.arena_power,1))
    }
    fun play(name:String){
        val id=ids[name]?:return
        if(!closed&&loaded.contains(id))pool.play(id,.62f,.62f,1,0,1f)
    }
    fun close(){if(!closed){closed=true;pool.release();loaded.clear()}}
}
