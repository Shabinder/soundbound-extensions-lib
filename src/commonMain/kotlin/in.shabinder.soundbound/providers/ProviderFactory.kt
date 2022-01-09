package `in`.shabinder.soundbound.providers

abstract class ProviderFactory(open val dependencies: Dependencies) {

    /*
    * This function will create all Providers
    * */
    abstract suspend fun createProviders(): List<Provider>
}