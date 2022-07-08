package YOUR_PACKAGE_GOES_HERE

object Secrets {

    //Method calls will be added by gradle task hideSecret
    //Example : external fun getWellHiddenSecret(packageName: String): String
    init {
        System.loadLibrary("secrets")
    }
}
