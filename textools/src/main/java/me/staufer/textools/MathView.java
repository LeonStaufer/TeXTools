package me.staufer.textools;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.text.Html;
import android.util.AttributeSet;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * MathView class that can render TeX using KaTeX in WebView
 */
public class MathView extends WebView {
    private JSONObject options;
    private String tex;
    private String buffer;
    private boolean loaded = false;

    /**
     * constructor for creating a MathView from XML
     *
     * @param context Context of view
     * @param attrs   AttributeSet
     */
    public MathView(Context context, AttributeSet attrs) {
        super(context, attrs);

        //set the options using the AttributeSet
        loadAttributes(context, attrs);

        //intialize the WebView
        initialize(context);

        //set up a WebViewClient to listen for when the page finishes loading
        this.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                loaded = true;
                //render tex if it was set in the meantime
                if (buffer != null) render(buffer);
            }
        });

        //load the correct page
        if (tex != null) {
            loadWithTeX();
        } else load();
    }

    /**
     * constructor for creating MathView programmatically
     *
     * @param context Context of view
     * @param options Map of options
     */
    public MathView(Context context, Map options) {
        super(context);

        loadOptions(options);

        initialize(context);

        //load the correct page
        if (tex != null) {
            loadWithTeX();
        } else load();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initialize(Context context) {
        //enable JavaScript
        WebSettings webSettings = this.getSettings();
        webSettings.setJavaScriptEnabled(true);

        //connect MathViewInterface to MathView
        this.addJavascriptInterface(new MathViewInterface(context), "MathViewInterface");
    }

    private void load() {
        //default URI
        URI uri = URI.create("file:///android_asset/index.html");
        try {
            //URI with options
            uri = new URI("file", null, "///android_asset/index.html", "options=" + options.toString(), null);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        //load HTML page with KaTeX
        this.loadUrl(uri.toString());
    }

    private void loadWithTeX() {
        //default URI
        URI uri = URI.create("file:///android_asset/index.html");
        try {
            //URI with options and TeX
            uri = new URI("file", null, "///android_asset/index.html", "options=" + options.toString() + "&tex=" + tex, null);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        //load HTML page with KaTeX
        this.loadUrl(uri.toString());
    }

    private void loadAttributes(Context context, AttributeSet attrs) {
        options = new JSONObject();

        TypedArray styledAttributes = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.MathView,
                0, 0);

        try {
            options.put("color", String.format("#%06X", (0xFFFFFF & styledAttributes.getColor(R.styleable.MathView_color, Color.BLACK))));
            options.put("background", String.format("#%06X", (0xFFFFFF & styledAttributes.getColor(R.styleable.MathView_colorBackground, Color.WHITE))));
            options.put("errorColor", String.format("#%06X", (0xFFFFFF & styledAttributes.getColor(R.styleable.MathView_colorError, Color.rgb(204, 0, 0)))));
            options.put("displayMode", styledAttributes.getBoolean(R.styleable.MathView_displayMode, false));
            options.put("fleqn", styledAttributes.getBoolean(R.styleable.MathView_fleqn, false));
            options.put("leqno", styledAttributes.getBoolean(R.styleable.MathView_leqno, false));
            options.put("throwOnError", styledAttributes.getBoolean(R.styleable.MathView_throwOnError, false));

            tex = styledAttributes.getString(R.styleable.MathView_tex);
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            styledAttributes.recycle();
        }
    }

    private void loadOptions(Map options) {
        this.options = new JSONObject();

        try {
            this.options.put("color", String.format("#%06X", (0xFFFFFF & getOrDefault(options, "color", Color.BLACK))));
            this.options.put("background", String.format("#%06X", (0xFFFFFF & getOrDefault(options, "backgroundColor", Color.WHITE))));
            this.options.put("errorColor", String.format("#%06X", (0xFFFFFF & getOrDefault(options, "colorError", Color.rgb(204, 0, 0)))));
            this.options.put("displayMode", getOrDefault(options, "displayMode", false));
            this.options.put("fleqn", getOrDefault(options, "fleqn", false));
            this.options.put("leqno", getOrDefault(options, "leqno", false));
            this.options.put("throwOnError", getOrDefault(options, "throwOnError", false));

            tex = getOrDefault(options, "tex", null);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * render a TeX string in the MathView
     *
     * @param tex string using the TeX format
     */
    public void render(String tex) {
        //add TeX to buffer if the MathView has not loaded yet
        if (!loaded) {
            this.buffer = tex;
            return;
        }

        //sanitize input
        tex = Html.escapeHtml(tex);
        tex = StringEscapeUtils.escapeEcmaScript(tex);
        //call JS update function with tex
        this.loadUrl("javascript:update('" + tex + "')");
    }

    private static <T> T getOrDefault(Map map, String key, T def) {
        return map.containsKey(key) ? (T) map.get(key) : def;
    }
}
