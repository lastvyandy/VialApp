package cl.vialapp.app.ui.games.arcade

// GUÍA DEL ARCHIVO:
// Preguntas usadas en la arena. La parte educativa y los golpes deben poder cambiarse por separado.

import kotlin.random.Random

internal enum class ArenaIllustration { STOP,YIELD,ROUNDABOUT,RED_LIGHT,PHONE,BELT,SCHOOL,DISTANCE,RAIN,SPEED }
internal data class ArenaQuestion(val illustration:ArenaIllustration,val prompt:String,val answers:List<String>,val correct:Int,val explanation:String)

/** Original practice questions. Reference: CONASET, Libro para la conducción en Chile,
 * clase B (febrero 2026), normas de circulación; preguntas-velocidad; dudas frecuentes clase B.
 * These are not claimed to be official exam questions. */
internal object ArenaQuestions{
    private fun q(i:ArenaIllustration,p:String,right:String,w1:String,w2:String,why:String)=
        ArenaQuestion(i,p,listOf(right,w1,w2),0,why)
    private val banks=listOf(
        listOf(
            q(ArenaIllustration.STOP,"Llegas a una señal PARE. ¿Qué haces?","Me detengo por completo y cedo el paso","Solo reduzco la velocidad","Sigo si no veo autos","PARE requiere detenerse completamente y continuar cuando sea seguro."),
            q(ArenaIllustration.YIELD,"Hay un vehículo próximo por la otra vía y tienes CEDA EL PASO.","Reduzco y me detengo si es necesario","Acelero para pasar primero","Tengo prioridad siempre","Cede el paso a quien circula por la otra vía si existe riesgo."),
            q(ArenaIllustration.ROUNDABOUT,"Un auto ya circula por la rotonda. ¿Quién tiene preferencia?","El que circula dentro de la rotonda","El que quiere entrar","El que toca la bocina","Al incorporarte, cede el paso a los vehículos que ya circulan por la rotonda."),
            q(ArenaIllustration.RED_LIGHT,"El semáforo de tu pista está rojo.","Me detengo antes de la línea","Avanzo si está despejado","Acelero antes de que cambie","La luz roja obliga a detenerse.")
        ),
        listOf(
            q(ArenaIllustration.PHONE,"Recibes una notificación mientras conduces.","Espero a estar estacionado en un lugar permitido","Miro el teléfono unos segundos","Escribo mientras avanzo lentamente","Manipular el teléfono distrae de la conducción."),
            q(ArenaIllustration.BELT,"Un pasajero aún no lleva el cinturón.","Espero a que todos lo abrochen","Parto porque el mío está abrochado","Solo importa en carretera","Todas las personas deben usar correctamente el cinturón antes de iniciar la marcha."),
            q(ArenaIllustration.SCHOOL,"Ves niños cerca de la calzada.","Reduzco la velocidad y estoy preparado para detenerme","Mantengo velocidad y toco la bocina","Acelero para salir de la zona","Anticipa movimientos inesperados y protege a quienes caminan."),
            q(ArenaIllustration.RED_LIGHT,"El semáforo está rojo aunque la vía está vacía.","Me detengo y espero la autorización para avanzar","Paso con precaución","Cruzo si nadie me ve","La ausencia de tráfico no anula la señal roja.")
        ),
        listOf(
            q(ArenaIllustration.DISTANCE,"En carretera seca, ¿qué referencia ayuda a conservar distancia?","Al menos tres segundos","Medio segundo","La longitud de un auto a cualquier velocidad","La referencia de tres segundos ayuda a conservar espacio para reaccionar."),
            q(ArenaIllustration.RAIN,"Llueve y disminuye la adherencia.","Reduzco velocidad y aumento separación","Mantengo la distancia habitual","Me acerco al auto delantero","En condiciones adversas necesitas más espacio y una conducción prudente."),
            q(ArenaIllustration.SPEED,"El límite señalizado es 50 km/h, pero hay poca visibilidad.","Adapto la velocidad por debajo del límite si hace falta","Debo circular exactamente a 50","Puedo superar el límite si no hay autos","El máximo permitido no es una velocidad obligatoria: adáptala al riesgo."),
            q(ArenaIllustration.DISTANCE,"Vas demasiado cerca del auto que te precede.","Aumento la separación progresivamente","Me acerco para que acelere","Adelanto sin comprobar si es seguro","Conserva distancia suficiente para detenerte sin colisionar.")
        )
    )
    fun forRival(rival:Int,random:Random):List<ArenaQuestion> = banks[(rival-1).coerceIn(0,2)].shuffled(random).map{question->
        val shuffled=question.answers.indices.shuffled(random)
        question.copy(answers=shuffled.map{question.answers[it]},correct=shuffled.indexOf(question.correct))
    }
}
