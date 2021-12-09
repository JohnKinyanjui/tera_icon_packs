package com.tera.iconpacks

/**
 * Created by Michele on 12/03/2017.
 */
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException
import java.util.*

class IconPackManager {
    private var mContext: Context

    constructor(mContext: Context) {
        this.mContext = mContext
    }


    inner class IconPack {
        var packageName: String? = null
        var name: String? = null
        private var mLoaded = false
        private val mPackagesDrawables = HashMap<String?, String?>()
        private val mBackImages: MutableList<Bitmap> = ArrayList()
        private var mMaskImage: Bitmap? = null
        private var mFrontImage: Bitmap? = null
        private var mFactor = 1.0f
        var iconPackres: Resources? = null
        fun load() {
            // load appfilter.xml from the icon pack package
            val pm = mContext.packageManager
            try {
                var xpp: XmlPullParser? = null
                iconPackres = pm.getResourcesForApplication(packageName)
                val appfilterid = iconPackres?.getIdentifier("appfilter", "xml", packageName)
                if (appfilterid!! > 0) {
                    xpp = iconPackres!!.getXml(appfilterid)
                } else {
                    // no resource found, try to open it from assests folder
                    try {
                        val appfilterstream = iconPackres!!.getAssets().open("appfilter.xml")
                        val factory = XmlPullParserFactory.newInstance()
                        factory.isNamespaceAware = true
                        xpp = factory.newPullParser()
                        xpp.setInput(appfilterstream, "utf-8")
                    } catch (e1: IOException) {
                        Log.d(TAG, "No appfilter.xml file")
                    }
                }
                if (xpp != null) {
                    var eventType = xpp.eventType
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG) {
                            if (xpp.name == "iconback") {
                                for (i in 0 until xpp.attributeCount) {
                                    if (xpp.getAttributeName(i).startsWith("img")) {
                                        val drawableName = xpp.getAttributeValue(i)
                                        val iconback = loadBitmap(drawableName)
                                        if (iconback != null) mBackImages.add(iconback)
                                    }
                                }
                            } else if (xpp.name == "iconmask") {
                                if (xpp.attributeCount > 0 && xpp.getAttributeName(0) == "img1") {
                                    val drawableName = xpp.getAttributeValue(0)
                                    mMaskImage = loadBitmap(drawableName)
                                }
                            } else if (xpp.name == "iconupon") {
                                if (xpp.attributeCount > 0 && xpp.getAttributeName(0) == "img1") {
                                    val drawableName = xpp.getAttributeValue(0)
                                    mFrontImage = loadBitmap(drawableName)
                                }
                            } else if (xpp.name == "scale") {
                                // mFactor
                                if (xpp.attributeCount > 0 && xpp.getAttributeName(0) == "factor") {
                                    mFactor = java.lang.Float.valueOf(xpp.getAttributeValue(0))
                                }
                            } else if (xpp.name == "item") {
                                var componentName: String? = null
                                var drawableName: String? = null
                                for (i in 0 until xpp.attributeCount) {
                                    if (xpp.getAttributeName(i) == "component") {
                                        componentName = xpp.getAttributeValue(i)
                                    } else if (xpp.getAttributeName(i) == "drawable") {
                                        drawableName = xpp.getAttributeValue(i)
                                    }
                                }
                                if (!mPackagesDrawables.containsKey(componentName)) mPackagesDrawables[componentName] = drawableName
                            }
                        }
                        eventType = xpp.next()
                    }
                }
                mLoaded = true
            } catch (e: PackageManager.NameNotFoundException) {
                Log.d(TAG, "Cannot load icon pack")
            } catch (e: XmlPullParserException) {
                Log.d(TAG, "Cannot parse icon pack appfilter.xml")
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        private fun loadBitmap(drawableName: String): Bitmap? {
            val id = iconPackres!!.getIdentifier(drawableName, "drawable", packageName)
            if (id > 0) {
                val bitmap = iconPackres!!.getDrawable(id)
                if (bitmap is BitmapDrawable) return bitmap.bitmap
            }
            return null
        }

        private fun loadDrawable(drawableName: String): Drawable? {
            val id = iconPackres!!.getIdentifier(drawableName, "drawable", packageName)
            return if (id > 0) {
                iconPackres!!.getDrawable(id)
            } else null
        }

        fun getDrawableIconForPackage(appPackageName: String?, defaultDrawable: Drawable?): Drawable? {
            if (!mLoaded) load()
            val pm = mContext.packageManager
            val launchIntent = pm.getLaunchIntentForPackage(appPackageName)
            var componentName: String? = null
            if (launchIntent != null) componentName = pm.getLaunchIntentForPackage(appPackageName).component.toString()
            var drawable = mPackagesDrawables[componentName]
            if (drawable != null) {
                return loadDrawable(drawable)
            } else {
                // try to get a resource with the component filename
                if (componentName != null) {
                    val start = componentName.indexOf("{") + 1
                    val end = componentName.indexOf("}", start)
                    if (end > start) {
                        drawable = componentName.substring(start, end).toLowerCase(Locale.getDefault()).replace(".", "_").replace("/", "_")
                        if (iconPackres!!.getIdentifier(drawable, "drawable", packageName) > 0) return loadDrawable(drawable)
                    }
                }
            }
            return defaultDrawable
        }

        fun getIconForPackage(appPackageName: String, defaultBitmap: Bitmap?): Bitmap? {
            if (!mLoaded) load()
            val pm = mContext.packageManager
            val launchIntent = pm.getLaunchIntentForPackage(appPackageName)
            var componentName: String? = null
            if (launchIntent != null) componentName = pm.getLaunchIntentForPackage(appPackageName).component.toString()
            var drawable = mPackagesDrawables[componentName]
            if (drawable != null) {
                return loadBitmap(drawable)
            } else {
                // try to get a resource with the component filename
                if (componentName != null) {
                    val start = componentName.indexOf("{") + 1
                    val end = componentName.indexOf("}", start)
                    if (end > start) {
                        drawable = componentName.substring(start, end).toLowerCase(Locale.getDefault()).replace(".", "_").replace("/", "_")
                        if (iconPackres!!.getIdentifier(drawable, "drawable", packageName) > 0) return loadBitmap(drawable)
                    }
                }
            }
            return defaultBitmap
        }

        private fun generateBitmap(appPackageName: String, defaultBitmap: Bitmap?): Bitmap? {
            // the key for the cache is the icon pack package name and the app package name
            val key = "$packageName:$appPackageName"

            // if generated bitmaps cache already contains the package name return it
//            Bitmap cachedBitmap = BitmapCache.getInstance(mContext).getBitmap(key);
//            if (cachedBitmap != null)
//                return cachedBitmap;

            // if no support images in the icon pack return the bitmap itself
            if (mBackImages.size == 0) return defaultBitmap
            val r = Random()
            val backImageInd = r.nextInt(mBackImages.size)
            val backImage = mBackImages[backImageInd]
            val w = backImage.width
            val h = backImage.height

            // create a bitmap for the result
            val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val mCanvas = Canvas(result)

            // draw the background first
            mCanvas.drawBitmap(backImage, 0f, 0f, null)

            // create a mutable mask bitmap with the same mask
            if (defaultBitmap != null && (defaultBitmap.width > w || defaultBitmap.height > h)) Bitmap.createScaledBitmap(defaultBitmap, (w * mFactor).toInt(), (h * mFactor).toInt(), false)
            if (mMaskImage != null) {
                // draw the scaled bitmap with mask
                val mutableMask = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val maskCanvas = Canvas(mutableMask)
                maskCanvas.drawBitmap(mMaskImage, 0f, 0f, Paint())

                // paint the bitmap with mask into the result
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                mCanvas.drawBitmap(defaultBitmap, (w - defaultBitmap!!.width) / 2.toFloat(), (h - defaultBitmap.height) / 2.toFloat(), null)
                mCanvas.drawBitmap(mutableMask, 0f, 0f, paint)
                paint.xfermode = null
            } else  // draw the scaled bitmap without mask
            {
                mCanvas.drawBitmap(defaultBitmap, (w - defaultBitmap!!.width) / 2.toFloat(), (h - defaultBitmap.height) / 2.toFloat(), null)
            }

            // paint the front
            if (mFrontImage != null) {
                mCanvas.drawBitmap(mFrontImage, 0f, 0f, null)
            }

            // store the bitmap in cache
//            BitmapCache.getInstance(mContext).putBitmap(key, result);

            // return it
            return result
        }
    }

    private var iconPacks: HashMap<String, IconPack> = hashMapOf()
    fun getAvailableIconPacks(forceReload: Boolean): Map<String, IconPack> {
        if (iconPacks.size == 0 || forceReload) {
            iconPacks = HashMap()

            // find apps with intent-filter "com.gau.go.launcherex.theme" and return build the HashMap
            val pm = mContext.packageManager
            val adwlauncherthemes = pm.queryIntentActivities(Intent("org.adw.launcher.THEMES"), PackageManager.GET_META_DATA)
            val golauncherthemes = pm.queryIntentActivities(Intent("com.gau.go.launcherex.theme"), PackageManager.GET_META_DATA)

            // merge those lists
            val rinfo: MutableList<ResolveInfo> = ArrayList(adwlauncherthemes)
            rinfo.addAll(golauncherthemes)
            for (ri in rinfo) {
                val ip = IconPack()
                ip.packageName = ri.activityInfo.packageName
                var ai: ApplicationInfo? = null
                try {
                    ai = pm.getApplicationInfo(ip.packageName, PackageManager.GET_META_DATA)
                    ip.name = mContext.packageManager.getApplicationLabel(ai).toString()
                    iconPacks!![ip.packageName!!] = ip
                } catch (e: PackageManager.NameNotFoundException) {
                    // shouldn't happen
                    e.printStackTrace()
                }
            }
        }
        return iconPacks
    }

    companion object {
        private const val TAG = "IconPackManager"
    }
}