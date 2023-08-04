package `in`.shabinder.soundbound.parcelize

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.TYPE)
actual annotation class WriteWith<P : Parceler<*>> actual constructor()
