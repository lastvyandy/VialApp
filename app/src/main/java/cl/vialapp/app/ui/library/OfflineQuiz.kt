package cl.vialapp.app.ui.library

internal data class OfflineQuestion(val articleId:String,val prompt:String,val options:List<String>,val correct:Int,val explanation:String)

internal object OfflineQuiz {
    val questions=listOf(
        OfflineQuestion("stopping-distance","¿Qué compone la distancia total de detención?",listOf("Solo la distancia de frenado","La distancia de reacción más la de frenado","La distancia de seguimiento"),1,"Se recorren metros antes de accionar el freno y durante la frenada. Ambas distancias se suman."),
        OfflineQuestion("stopping-distance","En condiciones comparables, si duplicas la velocidad, ¿cómo cambia la distancia de frenado?",listOf("Se multiplica por cuatro","No cambia","Se multiplica por dos"),0,"La distancia de frenado crece con el cuadrado de la velocidad."),
        OfflineQuestion("brakes","¿Qué aporta el sistema ABS?",listOf("Impide cualquier siniestro","Permite frenar siempre en menos metros","Ayuda a evitar que las ruedas se bloqueen"),2,"El ABS ayuda a conservar el control direccional; no garantiza una distancia menor."),
        OfflineQuestion("tires","¿Cuándo conviene comprobar la presión de los neumáticos?",listOf("Con los neumáticos fríos","Solo después de un viaje largo","Solo cuando se enciende una alerta"),0,"Comprueba la presión en frío y sigue las indicaciones del fabricante."),
        OfflineQuestion("lights","¿Los espejos correctamente regulados eliminan todos los puntos ciegos?",listOf("Sí, siempre","No, siguen existiendo sectores sin visibilidad","Solo al estacionar"),1,"Antes de maniobrar verifica también los ángulos muertos."),
        OfflineQuestion("seatbelt","¿Dónde debe quedar la banda inferior del cinturón?",listOf("Sobre el abdomen","Por encima del pecho","Sobre los huesos de la cadera, bajo el abdomen"),2,"La banda inferior debe apoyar sobre la cadera y estar ajustada."),
        OfflineQuestion("airbag","¿El airbag sustituye al cinturón de seguridad?",listOf("No, ambos se complementan","Sí, adelante","Sí, a baja velocidad"),0,"El airbag protege mejor junto al cinturón."),
        OfflineQuestion("following","Si llegas al punto de referencia antes de contar tres segundos, ¿qué indica?",listOf("Que circulas demasiado despacio","Que la separación es insuficiente según la regla","Que debes adelantar"),1,"Aumenta la distancia y agrega margen extra si la vía o el clima son adversos."),
        OfflineQuestion("priority","Al acercarte a un paso de cebra donde una persona quiere cruzar, ¿qué haces?",listOf("Acelerar antes de que cruce","Tocar la bocina y continuar","Detenerte y cederle el paso"),2,"Quien se dispone a cruzar tiene prioridad en el paso de cebra."),
        OfflineQuestion("speed","Según el manual Clase B 2024, ¿qué velocidad máxima corresponde al exterior de un colegio durante la entrada y salida de clases?",listOf("30 km/h","50 km/h","70 km/h"),0,"El manual, página 97, señala un máximo de 30 km/h en esa circunstancia. Respeta la señalización vigente."),
        OfflineQuestion("weather","Con lluvia y menor adherencia, ¿cómo debes adaptar la conducción?",listOf("Acercarte al vehículo delantero","Reducir velocidad y aumentar la separación","Mantener la velocidad y frenar bruscamente"),1,"La lluvia exige más margen y maniobras suaves."),
        OfflineQuestion("temporary","¿Qué color caracteriza las señales transitorias de trabajos en la vía, salvo la señal inicial indicada en el manual?",listOf("Azul","Blanco","Naranja"),2,"El naranja identifica la señalización transitoria; la señal inicial de trabajos es amarilla."),
        OfflineQuestion("helmet","¿Qué debes hacer con un casco que sufrió un impacto?",listOf("Reemplazarlo aunque no se vean daños","Usarlo mientras no tenga rayas","Pintarlo y reutilizarlo"),0,"Puede haber daños internos invisibles."),
        OfflineQuestion("posture","¿Hacia dónde conviene dirigir la mirada al conducir una moto?",listOf("Justo delante de la rueda","Con anticipación hacia la trayectoria que quieres seguir","Solo hacia el tablero"),1,"Mirar con anticipación ayuda a detectar riesgos y preparar maniobras."),
        OfflineQuestion("moto-braking","¿Cuál es una recomendación general de frenado en motocicleta?",listOf("Usar solo el freno trasero","Accionar los frenos de golpe","Coordinar ambos frenos de forma progresiva"),2,"El uso progresivo y coordinado ayuda a controlar la transferencia de peso."),
        OfflineQuestion("moto-curve","¿Cuándo conviene reducir la velocidad al aproximarse a una curva?",listOf("Antes de entrar en ella","Solo al salir","Nunca si la vía está seca"),0,"Anticipa la velocidad para mantener un margen de seguridad en tu pista."),
        OfflineQuestion("passenger","¿Qué cambia al llevar un acompañante en moto?",listOf("No cambia nada","El reparto de peso y el comportamiento del vehículo","Solo el consumo"),1,"El peso adicional modifica equilibrio, aceleración y frenado."),
        OfflineQuestion("cargo","¿Cómo conviene distribuir el equipaje en una moto?",listOf("Alto y a un costado","Suelto sobre el asiento","Bajo, equilibrado y firmemente sujeto"),2,"La distribución y sujeción adecuadas ayudan a mantener el control.")
    )
    private val articleMap=VialLibrary.articles.associateBy{it.id}
    fun forClass(license:String):List<OfflineQuestion> = questions.filter{
        license=="C" || articleMap[it.articleId]?.manual==Manual.B
    }
    fun source(q:OfflineQuestion):VialArticle=articleMap.getValue(q.articleId)
}
