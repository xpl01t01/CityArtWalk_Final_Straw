package com.example.cityartwalk

import android.os.Bundle
import android.view.*
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment

class HelpFragment : Fragment() {
    //this fragment displays help content using a webview
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_help, container, false)
        val webView: WebView = view.findViewById(R.id.webview)

        //make sure links open within the webview itself, not in a browser
        webView.webViewClient = WebViewClient()
        //enable javascript if the content requires it
        webView.settings.javaScriptEnabled = true

        //load the help page url
        webView.loadUrl("https://www.youtube.com")
        return view
    }

    //handle back navigation within the webview so users can go back to the previous page
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val webView: WebView = view.findViewById(R.id.webview)

        //make the view focusable to handle back key events
        view.isFocusableInTouchMode = true
        view.requestFocus()
        view.setOnKeyListener { _, keyCode, event ->
            //if the back key is pressed and the webview can go back, navigate back within the webview
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP && webView.canGoBack()) {
                webView.goBack()
                true
            } else {
                false
            }
        }
    }
}
