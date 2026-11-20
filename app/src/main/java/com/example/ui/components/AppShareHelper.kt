package com.example.ui.components

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface ApkSharePreparation {
    data class Ready(
        val uris: List<Uri>,
        val totalSizeLabel: String,
        val isSplitBundle: Boolean,
    ) : ApkSharePreparation

    data class Error(val message: String) : ApkSharePreparation
}

object AppShareHelper {

    private const val SHARED_SUBDIR = "shared"

    fun playStoreWebUrl(context: Context): String =
        "https://play.google.com/store/apps/details?id=${context.packageName}"

    fun fileProviderAuthority(context: Context): String =
        "${context.packageName}.fileprovider"

    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap? {
        return runCatching {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 2,
                EncodeHintType.CHARACTER_SET to "UTF-8",
            )
            val bitMatrix = QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                sizePx,
                sizePx,
                hints,
            )
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            val darkColor = Color.rgb(14, 23, 19)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix[x, y]) darkColor else Color.WHITE
                }
            }
            Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        }.getOrNull()
    }

    suspend fun createShareableQrCardUri(context: Context): Uri? = withContext(Dispatchers.IO) {
        runCatching {
            val qrBitmap = generateQrBitmap(playStoreWebUrl(context), sizePx = 560)
                ?: return@withContext null

            val cardWidth = 720
            val cardHeight = 860
            val card = Bitmap.createBitmap(cardWidth, cardHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(card)
            canvas.drawColor(Color.WHITE)

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 92, 59)
                textSize = 48f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(66, 82, 74)
                textSize = 26f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
            }
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 92, 59)
                textSize = 24f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            canvas.drawText("ALEVINI", cardWidth / 2f, 76f, titlePaint)
            canvas.drawText("Scannez pour ouvrir sur Google Play", cardWidth / 2f, 118f, subtitlePaint)

            val qrLeft = (cardWidth - qrBitmap.width) / 2f
            val qrTop = 148f
            canvas.drawBitmap(qrBitmap, qrLeft, qrTop, null)

            canvas.drawText(
                "Disponible sur Google Play • v${BuildConfig.VERSION_NAME}",
                cardWidth / 2f,
                776f,
                footerPaint,
            )
            canvas.drawText(
                "Partage communautaire officiel",
                cardWidth / 2f,
                816f,
                subtitlePaint,
            )

            val sharedDir = ensureSharedDir(context)
            val outFile = File(sharedDir, "alevini_qr_communaute.png")
            outFile.outputStream().use { stream ->
                card.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            card.recycle()
            qrBitmap.recycle()

            FileProvider.getUriForFile(context, fileProviderAuthority(context), outFile)
        }.getOrNull()
    }

    suspend fun prepareAppApkForSharing(context: Context): ApkSharePreparation =
        withContext(Dispatchers.IO) {
            runCatching {
                val appInfo = context.applicationInfo
                val baseSource = appInfo.sourceDir
                    ?: return@withContext ApkSharePreparation.Error(
                        "Fichier d'installation introuvable sur cet appareil."
                    )
                val baseFile = File(baseSource)
                if (!baseFile.exists() || !baseFile.canRead()) {
                    return@withContext ApkSharePreparation.Error(
                        "Impossible de lire l'application installée sur cet appareil."
                    )
                }

                val sharedDir = ensureSharedDir(context)
                sharedDir.listFiles { file -> file.extension.equals("apk", ignoreCase = true) }
                    ?.forEach { it.delete() }

                val authority = fileProviderAuthority(context)
                val safeVersion = BuildConfig.VERSION_NAME.replace(Regex("[^A-Za-z0-9._-]"), "_")
                val splits = appInfo.splitSourceDirs?.filter { it.isNotBlank() }.orEmpty()

                val uris = mutableListOf<Uri>()
                var totalBytes = 0L

                val targetBaseName = if (splits.isEmpty()) {
                    "Alevini-v$safeVersion.apk"
                } else {
                    "Alevini-v$safeVersion-base.apk"
                }
                val destBase = File(sharedDir, targetBaseName)
                copyFileBuffered(baseFile, destBase)
                totalBytes += destBase.length()
                uris += FileProvider.getUriForFile(context, authority, destBase)

                splits.forEachIndexed { index, splitPath ->
                    val splitSrc = File(splitPath)
                    if (splitSrc.exists() && splitSrc.canRead()) {
                        val destSplit = File(sharedDir, "Alevini-v$safeVersion-part${index + 1}.apk")
                        copyFileBuffered(splitSrc, destSplit)
                        totalBytes += destSplit.length()
                        uris += FileProvider.getUriForFile(context, authority, destSplit)
                    }
                }

                val sizeMb = String.format(Locale.US, "%.1f Mo", totalBytes / (1024.0 * 1024.0))
                ApkSharePreparation.Ready(
                    uris = uris,
                    totalSizeLabel = sizeMb,
                    isSplitBundle = splits.isNotEmpty(),
                )
            }.getOrElse { err ->
                ApkSharePreparation.Error(
                    err.localizedMessage ?: "Erreur lors de la préparation de l'application."
                )
            }
        }

    fun openOnGooglePlay(context: Context) {
        val pkg = context.packageName
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(marketIntent) }
            .onFailure {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(playStoreWebUrl(context))).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                runCatching { context.startActivity(webIntent) }
            }
    }

    fun shareQrImage(context: Context, qrUri: Uri) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, qrUri)
            putExtra(Intent.EXTRA_SUBJECT, "Alevini — QR Code Google Play")
            putExtra(
                Intent.EXTRA_TEXT,
                "Rejoignez la communauté Alevini ! Scannez ce QR Code pour ouvrir et installer l'application directement sur Google Play.",
            )
            clipData = ClipData.newRawUri("Alevini QR Code", qrUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(sendIntent, "Partager le QR Code Alevini")
        )
    }

    fun shareApkDirect(context: Context, preparation: ApkSharePreparation.Ready) {
        val intent = if (preparation.uris.size <= 1) {
            val uri = preparation.uris.first()
            Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Application Alevini v${BuildConfig.VERSION_NAME}")
                clipData = ClipData.newRawUri("Alevini APK", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            val arrayList = ArrayList(preparation.uris)
            val clip = ClipData.newRawUri("Alevini APK", arrayList.first()).apply {
                arrayList.drop(1).forEach { extraUri ->
                    addItem(ClipData.Item(extraUri))
                }
            }
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "application/vnd.android.package-archive"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayList)
                putExtra(Intent.EXTRA_SUBJECT, "Application Alevini v${BuildConfig.VERSION_NAME}")
                clipData = clip
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        context.startActivity(
            Intent.createChooser(intent, "Envoyer Alevini vers un autre appareil")
        )
    }

    private fun ensureSharedDir(context: Context): File {
        val dir = File(context.cacheDir, SHARED_SUBDIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun copyFileBuffered(source: File, dest: File) {
        source.inputStream().buffered(64 * 1024).use { input ->
            dest.outputStream().buffered(64 * 1024).use { output ->
                input.copyTo(output, bufferSize = 64 * 1024)
            }
        }
    }
}
