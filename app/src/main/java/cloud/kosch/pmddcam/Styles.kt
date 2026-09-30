package cloud.kosch.pmddcam

enum class Technique { PHOTO, COMIC, WATERCOLOR, INK, PENCIL, OIL, PIXEL, HALFTONE, NEON, DUOTONE, HATCH, SOLAR }
data class PhotoStyle(val id:String,val name:String,val group:String,val technique:Technique,
    val warmth:Float=0f,val saturation:Float=1f,val contrast:Float=1f,val levels:Int=256,
    val tint:Int=0xffb8d4ff.toInt(),val grain:Float=0f,
    val ink:Float=.68f,val lift:Float=0f,val toning:Float=0f,
    val shadow:Int=0xff142c48.toInt(),val highlight:Int=0xfff2ead8.toInt())

object Styles {
    // Each preset changes an actual rendering recipe; PMDD is applied after every style.
    val all=listOf(
        PhotoStyle("vivid","PMDD Vivid","Foto",Technique.PHOTO,.045f,1.16f,1.02f,
            shadow=0xff23364a.toInt(),highlight=0xffffefd0.toInt(),toning=.28f),
        PhotoStyle("natural","PMDD Natural","Foto",Technique.PHOTO),
        PhotoStyle("cinema","Cinematic","Foto",Technique.PHOTO,-.08f,.85f,1.18f),
        PhotoStyle("portrait","Soft Portrait","Foto",Technique.PHOTO,.07f,.82f,.9f),
        PhotoStyle("landscape","Alpine Clarity","Foto",Technique.PHOTO,-.05f,1.2f,1.12f),
        PhotoStyle("gold","Golden Hour","Foto",Technique.PHOTO,.24f,1.12f,.96f),
        PhotoStyle("blue","Blue Hour","Foto",Technique.PHOTO,-.24f,.95f,1.1f),
        PhotoStyle("noir","Film Noir","Foto",Technique.PHOTO,0f,0f,1.45f,grain=.08f),
        PhotoStyle("silver","Silver Gelatin","Foto",Technique.PHOTO,-.04f,0f,1.12f,grain=.13f),
        PhotoStyle("matte","Matte Editorial","Foto",Technique.PHOTO,.04f,.65f,.78f),
        PhotoStyle("chrome","Chrome Color","Foto",Technique.PHOTO,-.03f,1.45f,1.2f),
        PhotoStyle("comic","Comic Classic","Illustration",Technique.COMIC,0f,1.25f,1.1f,7),
        PhotoStyle("manga","Manga Ink","Illustration",Technique.COMIC,0f,0f,1.25f,5),
        PhotoStyle("anime","Anime Cel","Illustration",Technique.COMIC,.04f,1.25f,.92f,10),
        PhotoStyle("graphic","Graphic Novel","Illustration",Technique.COMIC,-.08f,.6f,1.45f,5),
        PhotoStyle("pop","Pop Art","Illustration",Technique.HALFTONE,.06f,1.7f,1.3f,5),
        PhotoStyle("ligne","Ligne Claire","Illustration",Technique.COMIC,.02f,.85f,.85f,12),
        PhotoStyle("pastelcel","Pastel Cel","Illustration",Technique.COMIC,.08f,.6f,.75f,9),
        PhotoStyle("superhero","Superhero","Illustration",Technique.COMIC,-.08f,1.65f,1.35f,6),
        PhotoStyle("storybook","Storybook","Illustration",Technique.WATERCOLOR,.15f,.85f,.8f,14),
        PhotoStyle("risocomic","Riso Comic","Illustration",Technique.HALFTONE,.18f,.8f,1.12f,6,0xffef7488.toInt(),.13f),
        PhotoStyle("watercolor","Wasserfarben","Atelier",Technique.WATERCOLOR,0f,.82f,.78f,18,grain=.08f),
        PhotoStyle("aquarelle","Aquarell Warm","Atelier",Technique.WATERCOLOR,.2f,.95f,.82f,22,grain=.12f),
        PhotoStyle("gouache","Gouache","Atelier",Technique.OIL,.05f,1.1f,.92f,12,grain=.08f),
        PhotoStyle("oil","Ölgemälde","Atelier",Technique.OIL,.08f,1.15f,1.08f,20,grain=.16f),
        PhotoStyle("impression","Impression","Atelier",Technique.OIL,.13f,1.3f,.86f,15,grain=.2f),
        PhotoStyle("ink","Tusche","Atelier",Technique.INK,0f,0f,1.2f),
        PhotoStyle("pencil","Bleistift","Atelier",Technique.PENCIL,0f,0f,1f,grain=.15f),
        PhotoStyle("coloredpencil","Buntstift","Atelier",Technique.PENCIL,.06f,1.3f,.92f,grain=.12f),
        PhotoStyle("charcoal","Kohle","Atelier",Technique.HATCH,0f,0f,1.4f,grain=.23f),
        PhotoStyle("etching","Kupferstich","Atelier",Technique.HATCH,.14f,.15f,1.12f,grain=.1f),
        PhotoStyle("retro70","Retro 70s","Retro",Technique.PHOTO,.22f,.68f,.92f,grain=.2f),
        PhotoStyle("retro80","Retro 80s","Retro",Technique.PHOTO,-.03f,1.25f,1.1f,grain=.16f),
        PhotoStyle("instant","Instant Film","Retro",Technique.PHOTO,.12f,.72f,.76f,grain=.14f),
        PhotoStyle("sepia","Sepia","Retro",Technique.DUOTONE,.2f,.2f,.95f,256,0xffbb894d.toInt(),.13f),
        PhotoStyle("cyanotype","Cyanotypie","Retro",Technique.DUOTONE,-.1f,.8f,1.15f,256,0xff327cb5.toInt(),.1f),
        PhotoStyle("vhs","VHS Print","Retro",Technique.HALFTONE,.08f,.7f,.86f,22,grain=.22f),
        PhotoStyle("arcade","8-Bit Arcade","Retro",Technique.PIXEL,0f,1.5f,1.15f,4),
        PhotoStyle("pixel16","16-Bit Adventure","Retro",Technique.PIXEL,.02f,1.3f,1.05f,8),
        PhotoStyle("gameboy","Pocket Green","Retro",Technique.PIXEL,0f,0f,1.2f,4,0xff97af49.toInt()),
        PhotoStyle("newspaper","Newspaper","Retro",Technique.HALFTONE,.07f,0f,1.2f,8,grain=.12f),
        PhotoStyle("futuretech","Futuretech","Zukunft",Technique.NEON,-.16f,1.25f,1.2f,256,0xff66eedd.toInt()),
        PhotoStyle("cyberpunk","Cyberpunk","Zukunft",Technique.NEON,-.06f,1.65f,1.3f,256,0xffff55c4.toInt()),
        PhotoStyle("neontokyo","Neon Tokyo","Zukunft",Technique.NEON,.07f,1.45f,1.12f,256,0xffed559e.toInt()),
        PhotoStyle("hologram","Holographic","Zukunft",Technique.NEON,-.12f,.75f,.84f,32,0xff7eeeff.toInt()),
        PhotoStyle("synthwave","Synthwave","Zukunft",Technique.DUOTONE,.08f,1.2f,1.2f,32,0xffbf65f6.toInt()),
        PhotoStyle("blueprint","Blueprint","Zukunft",Technique.INK,-.1f,0f,1.1f,256,0xff3789d0.toInt()),
        PhotoStyle("infrared","Infrared Dream","Zukunft",Technique.SOLAR,.18f,1.2f,1.12f,32),
        PhotoStyle("matrix","Matrix Green","Zukunft",Technique.NEON,-.04f,0f,1.45f,32,0xff75ff91.toInt()),
        PhotoStyle("space","Deep Space","Zukunft",Technique.NEON,-.22f,.75f,1.4f,256,0xffb296ff.toInt()),
        PhotoStyle("liquidmetal","Liquid Metal","Zukunft",Technique.SOLAR,-.1f,.12f,1.18f,48),
        PhotoStyle("dream","Dream Pastel","Atmosphäre",Technique.WATERCOLOR,.1f,.62f,.7f,28),
        PhotoStyle("nordic","Nordic Mist","Atmosphäre",Technique.PHOTO,-.12f,.48f,.82f),
        PhotoStyle("desert","Desert Sand","Atmosphäre",Technique.DUOTONE,.2f,.4f,.94f,256,0xffddb781.toInt()),
        PhotoStyle("emerald","Emerald Forest","Atmosphäre",Technique.PHOTO,-.04f,1.1f,1.16f,256,0xff68ae91.toInt()),
        PhotoStyle("sakura","Sakura Bloom","Atmosphäre",Technique.WATERCOLOR,.15f,.72f,.8f,20,0xffffabc9.toInt()),
        PhotoStyle("underwater","Underwater","Atmosphäre",Technique.DUOTONE,-.2f,.75f,.91f,256,0xff4abec1.toInt()),
        PhotoStyle("lava","Lava Light","Atmosphäre",Technique.NEON,.25f,1.4f,1.3f,256,0xffff8353.toInt()),
        PhotoStyle("arctic","Arctic Ice","Atmosphäre",Technique.PHOTO,-.26f,.5f,1.02f),
        PhotoStyle("moon","Moonlight","Atmosphäre",Technique.DUOTONE,-.18f,.2f,1.25f,256,0xff7c97c4.toInt()),
        PhotoStyle("velvet","Velvet Dusk","Atmosphäre",Technique.DUOTONE,.05f,.6f,.87f,256,0xffc58bbf.toInt())
    ).map { s ->
        when(s.id){
            "cinema"->s.copy(shadow=0xff174750.toInt(),highlight=0xffffbd7d.toInt(),toning=.7f)
            "portrait"->s.copy(lift=.025f,shadow=0xff71526b.toInt(),highlight=0xffffdcc4.toInt(),toning=.25f)
            "gold"->s.copy(shadow=0xff733b32.toInt(),highlight=0xffffce7a.toInt(),toning=.7f)
            "blue"->s.copy(shadow=0xff203264.toInt(),highlight=0xff9acffa.toInt(),toning=.65f)
            "matte"->s.copy(lift=.075f,toning=.35f)
            "retro70"->s.copy(lift=.045f,shadow=0xff3b6756.toInt(),highlight=0xffffc581.toInt(),toning=.6f)
            "retro80"->s.copy(shadow=0xff493662.toInt(),highlight=0xff93dbeb.toInt(),toning=.5f)
            "instant"->s.copy(lift=.09f,shadow=0xff355a68.toInt(),highlight=0xffeec59e.toInt(),toning=.35f)
            "manga"->s.copy(ink=.92f)
            "anime"->s.copy(ink=.4f)
            "ligne"->s.copy(ink=.52f,lift=.025f)
            "graphic"->s.copy(ink=.86f)
            "pastelcel"->s.copy(ink=.3f,lift=.08f)
            "storybook"->s.copy(toning=.3f,shadow=0xff775275.toInt(),highlight=0xffffdfb0.toInt())
            "sepia"->s.copy(shadow=0xff271709.toInt(),highlight=0xfff9e2b1.toInt(),saturation=.85f)
            "cyanotype"->s.copy(shadow=0xff061f54.toInt(),highlight=0xffd8eef3.toInt())
            "synthwave"->s.copy(shadow=0xff361349.toInt(),highlight=0xff78e8f2.toInt(),saturation=1.25f)
            "desert"->s.copy(shadow=0xff55352c.toInt(),highlight=0xffffe8b1.toInt(),saturation=.8f)
            "underwater"->s.copy(shadow=0xff072a45.toInt(),highlight=0xffa1eee1.toInt())
            "moon"->s.copy(shadow=0xff0d142c.toInt(),highlight=0xffc9ddfa.toInt(),saturation=.7f)
            "velvet"->s.copy(shadow=0xff391c43.toInt(),highlight=0xfff7ccd6.toInt())
            "emerald"->s.copy(shadow=0xff145142.toInt(),highlight=0xffd4eea7.toInt(),toning=.65f)
            "sakura"->s.copy(shadow=0xff8b5789.toInt(),highlight=0xffffd2df.toInt(),toning=.65f)
            "arctic"->s.copy(shadow=0xff276e94.toInt(),highlight=0xffc6f7fa.toInt(),toning=.5f)
            "vhs"->s.copy(technique=Technique.PHOTO,toning=.4f,shadow=0xff5b346b.toInt(),highlight=0xff87e2df.toInt())
            else->s
        }
    }
    fun get(id:String)=all.firstOrNull{it.id==id}?:all.first()
}
