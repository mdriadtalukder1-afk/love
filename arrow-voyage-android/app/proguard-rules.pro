# The game is plain HTML/JS in assets. Keep the methods the page calls on window.ArrowAds.
-keepclassmembers class com.arrowvoyage.game.Ads {
    @android.webkit.JavascriptInterface <methods>;
}
