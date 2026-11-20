package cl.vialapp.app.ui.games.doom

// GUÍA DEL ARCHIVO:
// Historia de los cinco niveles y las notas. No inventes textos en la pantalla: van acá.

/** Narrative is deliberately independent of Compose so it can be checked by the
 * headless Doom smoke test. Nothing is downloaded while playing. */
internal data class DoomStoryCard(val heading:String,val text:String)
internal data class DoomMissionBrief(val subtitle:String,val teaser:String,val objective:String)


internal object DoomStory {
    fun brief(level:Int):DoomMissionBrief=when(level){
        1->DoomMissionBrief("ROBOTS FUERA DE CONTROL",
            "Una actualización desconocida convirtió los robots de práctica y drones en enemigos. La señal que los controla viene de fuera del estacionamiento.",
            "Recupera la credencial y averigua quién está controlando las máquinas.")
        2->DoomMissionBrief("EL CONVOY DE LAS MÁQUINAS",
            "El túnel de pruebas está tomado. Los saboteadores lo están utilizando para trasladar unidades de combate hacia otras instalaciones.",
            "Atravesar el túnel, recuperar los controles y seguir el rastro de las máquinas.")
        3->DoomMissionBrief("EL ORIGEN DE LA CONSPIRACIÓN",
            "El centro de control vial guarda órdenes y equipos que no pertenecen a VialApp. Alguien está financiando una operación mucho mayor.",
            "Romper las defensas de la fortaleza y llegar a la central de mando.")
        4->DoomMissionBrief("EL EXPERIMENTO DEL CAPITÁN",
            "La ciudad está colapsada. Una fórmula experimental del Capitán Minoxidil convirtió varios reclutas en criaturas peligrosas.",
            "Cruza Zona Cero y detén al Capitán Minoxidil.")
        5->DoomMissionBrief("EL ÚLTIMO RESPALDO",
            "Las autoescuelas rivales han ocupado el centro de datos. El sistema de licencias y exámenes está a punto de desaparecer.",
            "Recupera las credenciales, restaura el respaldo y derrota al Administrador Rival.")
        else->error("Nivel desconocido: $level")
    }

    fun title(level:Int):String=when(level){
        1->"ESTACIONAMIENTO INFERNAL"
        2->"TUNEL BAJO CONTROL"
        3->"LA FORTALEZA VIAL"
        4->"ZONA CERO CAPILAR"
        5->"ASALTO AL SERVIDOR"
        else->error("Nivel desconocido: $level")
    }

    fun intro(level:Int):List<DoomStoryCard> = when(level){
        1->listOf(
            DoomStoryCard("00:17 · ALARMA EN LAS PISTAS",
                "Una alarma interrumpe las prácticas de VialApp. Los accesos del estacionamiento se cierran y los vehículos quedan atrapados. Carvier pierde contacto con el personal de seguridad."),
            DoomStoryCard("LOS GUARDIAS YA NO OBEDECEN",
                "Los robots de entrenamiento y los drones de vigilancia han sido reprogramados. Ahora atacan a cualquiera que intente recuperar las instalaciones. Esto no parece una simple avería."),
            DoomStoryCard("ORDEN PARA DON PELAO",
                "Entra por mantenimiento, consigue una credencial y busca el origen de la señal. Si alguien tomó el control de los robots, también puede tomar el de toda la ciudad.")
        )
        2->listOf(
            DoomStoryCard("01:42 · EL TUNEL ESTA CERRADO",
                "La señal que controlaba a los robots conduce al túnel de pruebas. Las barreras están bloqueadas, las cámaras giran solas y varios conductores quedaron aislados al otro lado."),
            DoomStoryCard("UN EJERCITO DE PRACTICA",
                "Entre los drones aparecen unidades de choque y robots más veloces. Alguien convirtió el sistema de entrenamiento vial en una fuerza de ocupación y está trasladando equipos por el túnel."),
            DoomStoryCard("BUSCA LA CREDENCIAL",
                "Don Pelao debe recuperar los controles, atravesar los puestos de seguridad y llevarse la credencial del sector. Al final del túnel hay una ruta hacia el centro de operaciones.")
        )
        3->listOf(
            DoomStoryCard("03:08 · LA FORTALEZA VIAL",
                "El antiguo centro de control del tránsito es ahora una fortaleza. Detrás de sus puertas reforzadas hay máquinas de combate, depósitos de armas y registros de órdenes transmitidas desde el exterior."),
            DoomStoryCard("LA CONSPIRACION TOMA FORMA",
                "Las órdenes no vienen de VialApp. Varias autoescuelas rivales han financiado el sabotaje para recuperar a sus alumnos. Pero alguien dentro del grupo también habla de un extraño proyecto capilar."),
            DoomStoryCard("ABRE PASO HACIA ZONA CERO",
                "Rompe las defensas, encuentra los suministros y llega al mando central. Allí está la última pista sobre los experimentos que los rivales están ocultando.")
        )
        4->listOf(
            DoomStoryCard("04:26 · ZONA CERO CAPILAR",
                "Las calles están llenas de autos chocados y señales destruidas. En medio del desastre aparece una publicidad absurda: un tratamiento milagroso del Capitán Minoxidil."),
            DoomStoryCard("ALGO SALIO MUY MAL",
                "Los rivales probaron una sustancia experimental sobre sus propios reclutas. Algunos se convirtieron en criaturas violentas, casi demoníacas. Minoxidil tomó el control de la zona y se niega a entregar sus registros."),
            DoomStoryCard("DERROTA AL CAPITAN",
                "Don Pelao debe cruzar las avenidas, enfrentar a las criaturas y detener a Minoxidil. Los documentos recuperados apuntan al verdadero objetivo del ataque: los servidores de VialApp.")
        )
        5->listOf(
            DoomStoryCard("05:51 · ASALTO AL SERVIDOR",
                "Las autoescuelas rivales invadieron el centro de datos de VialApp. Sus instructores y técnicos armados están destruyendo terminales y tratando de borrar los registros de exámenes."),
            DoomStoryCard("LA CIUDAD SIN LICENCIAS",
                "Sin el servicio, miles de alumnos no pueden practicar ni rendir. Afuera hay choques, bocinazos y caos. El Administrador Rival pretende culpar a VialApp por el desastre que él mismo provocó."),
            DoomStoryCard("EL ULTIMO RESPALDO",
                "Entra, recupera las credenciales y restaura el backup. Los informes escondidos revelarán quién organizó todo. Derrota al Administrador antes de que el sistema desaparezca.")
        )
        else->error("Nivel desconocido: $level")
    }

    /** Physical story documents; E1M5 already has its own nine map-specific notes. */
    fun fieldNotes(level:Int):List<DoomNote> = when(level){
        1->listOf(
            DoomNote(4.5f,2.5f,"01 · PARTE DE SEGURIDAD",
                "00:11. Los robots solo debían vigilar los conos y registrar infracciones de práctica. Esta noche recibieron instrucciones que no figuran en el servidor local. No obedecen ni el protocolo de apagado."),
            DoomNote(11.5f,4.5f,"02 · REGISTRO DE DRONES",
                "La consola registra una actualización enviada desde el túnel de pruebas. La firma digital fue falsificada. Quien la envió conocía las contraseñas de los simuladores de conducción."),
            DoomNote(18.5f,14.5f,"03 · MENSAJE DE CARVIER",
                "Don Pelao: recuperamos una parte de la señal. Los drones no se rebelaron solos. Sigue el túnel y busca una credencial de seguridad. No confíes en ningún robot de instrucción.")
        )
        2->listOf(
            DoomNote(2.5f,2.5f,"01 · OPERADOR DEL TUNEL",
                "Las barreras se cerraron durante un simulacro de conducción nocturna. Después llegaron los robots armados. Los conductores atrapados no tenían idea de que el sistema de práctica había sido intervenido."),
            DoomNote(10.5f,4.5f,"02 · ORDEN DE TRASLADO",
                "Enviar las unidades de choque al centro de control. Los drones mantienen despejada la ruta. La dirección de destino fue borrada, pero el sello dice FORTALEZA VIAL."),
            DoomNote(18.5f,12.5f,"03 · AUDIO TRANSCRITO",
                "Una voz en la radio dijo: «Cuando caiga VialApp, los alumnos tendrán que volver a nuestras escuelas». No era un técnico. Parecía el dueño de una autoescuela.")
        )
        3->listOf(
            DoomNote(2.5f,3.5f,"01 · ALMACEN DE DEFENSA",
                "Las armas y las máquinas de la fortaleza se pagaron con fondos de varias escuelas de conductores. Nadie quiere firmar la compra, pero todos exigen que VialApp deje de funcionar."),
            DoomNote(14.5f,12.5f,"02 · PROYECTO M-0",
                "No todos los reclutas aceptaron entrenar con robots. El Capitán Minoxidil ofreció una fórmula capilar que prometía fuerza y resistencia. Los primeros voluntarios presentaron cambios físicos imposibles de explicar."),
            DoomNote(23.5f,19.5f,"03 · ORDEN DE ATAQUE",
                "El plan tiene dos partes: usar la fortaleza para distraer a Don Pelao y enviar instructores humanos al centro de datos. Si cae el servidor de exámenes, caerá VialApp.")
        )
        4->listOf(
            DoomNote(4.5f,3.5f,"01 · CONSULTA MEDICA",
                "Los pacientes expuestos a la fórmula M-0 desarrollaron fiebre, crecimiento anormal de tejido y ataques violentos. El Capitán Minoxidil insiste en llamarlos resultados prometedores."),
            DoomNote(14.5f,15.5f,"02 · INFORME DE TRANSITO",
                "Las calles de Zona Cero están colapsadas. Los semáforos de práctica fueron manipulados y hay vehículos chocados en varios cruces. No permitan que los alumnos se acerquen a la zona."),
            DoomNote(27.5f,22.5f,"03 · MENSAJE DE MINOXIDIL",
                "Si Don Pelao ha llegado hasta aquí, todo terminó para nosotros. Los instructores rivales ya entraron al centro de datos. Los monstruos solo tenían que hacerle perder tiempo.")
        )
        5->emptyList()
        else->emptyList()
    }
}
