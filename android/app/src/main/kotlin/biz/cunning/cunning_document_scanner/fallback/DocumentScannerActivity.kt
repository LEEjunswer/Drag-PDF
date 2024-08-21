package biz.cunning.cunning_document_scanner.fallback

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import biz.cunning.cunning_document_scanner.R
import biz.cunning.cunning_document_scanner.fallback.constants.DefaultSetting
import biz.cunning.cunning_document_scanner.fallback.constants.DocumentScannerExtra
import biz.cunning.cunning_document_scanner.fallback.extensions.move
import biz.cunning.cunning_document_scanner.fallback.extensions.onClick
import biz.cunning.cunning_document_scanner.fallback.extensions.saveToFile
import biz.cunning.cunning_document_scanner.fallback.extensions.screenHeight
import biz.cunning.cunning_document_scanner.fallback.extensions.screenWidth
import biz.cunning.cunning_document_scanner.fallback.models.Document
import biz.cunning.cunning_document_scanner.fallback.models.Point
import biz.cunning.cunning_document_scanner.fallback.models.Quad
import biz.cunning.cunning_document_scanner.fallback.ui.ImageCropView
import biz.cunning.cunning_document_scanner.fallback.utils.CameraUtil
import biz.cunning.cunning_document_scanner.fallback.utils.FileUtil
import biz.cunning.cunning_document_scanner.fallback.utils.ImageUtil
import java.io.File
/**
 * This class contains the main document scanner code. It opens the camera, lets the user
 * take a photo of a document (homework paper, business card, etc.), detects document corners,
 * allows user to make adjustments to the detected corners, depending on options, and saves
 * the cropped document. It allows the user to do this for 1 or more documents.
 *
 * @constructor creates document scanner activity
 */

class DocumentScannerActivity : AppCompatActivity() {
    private var croppedImageQuality = DefaultSetting.CROPPED_IMAGE_QUALITY
    private val cropperOffsetWhenCornersNotFound = 100.0
    private val documents = mutableListOf<Document>()

    private val cameraUtil = CameraUtil(
        this,
        onPhotoCaptureSuccess = { originalPhotoPath ->
            val photo: Bitmap? = try {
                ImageUtil().getImageFromFilePath(originalPhotoPath)
            } catch (exception: Exception) {
                finishIntentWithError("비트맵을 가져올 수 없습니다: ${exception.localizedMessage}")
                return@CameraUtil
            }

            if (photo == null) {
                finishIntentWithError("문서 비트맵이 null입니다.")
                return@CameraUtil
            }

            val corners = try {
                val (topLeft, topRight, bottomLeft, bottomRight) = getDocumentCorners(photo)
                Quad(topLeft, topRight, bottomRight, bottomLeft)
            } catch (exception: Exception) {
                finishIntentWithError("문서 모서리를 가져올 수 없습니다: ${exception.message}")
                return@CameraUtil
            }

            val document = Document(originalPhotoPath, photo.width, photo.height, corners)
            documents.add(document)

        },
        onCancelPhoto = {
            if (documents.isEmpty()) {
                onClickCancel()
            }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_crop)

        try {
            validateExtras()
            openCamera()
        } catch (exception: Exception) {
            finishIntentWithError("카메라를 여는 중 오류가 발생했습니다: ${exception.message}")
        }

        val completeDocumentScanButton: ImageButton = findViewById(R.id.complete_document_scan_button)
        completeDocumentScanButton.setOnClickListener { onClickDone() }
    }

    private fun validateExtras() {
        intent.extras?.get(DocumentScannerExtra.EXTRA_CROPPED_IMAGE_QUALITY)?.let {
            if (it !is Int || it < 0 || it > 100) {
                throw Exception("${DocumentScannerExtra.EXTRA_CROPPED_IMAGE_QUALITY}는 0에서 100 사이의 숫자이어야 합니다.")
            }
            croppedImageQuality = it
        }
    }

    private fun getDocumentCorners(photo: Bitmap): List<Point> {
        return listOf(
            Point(0.0, 0.0).move(cropperOffsetWhenCornersNotFound, cropperOffsetWhenCornersNotFound),
            Point(photo.width.toDouble(), 0.0).move(-cropperOffsetWhenCornersNotFound, cropperOffsetWhenCornersNotFound),
            Point(0.0, photo.height.toDouble()).move(cropperOffsetWhenCornersNotFound, -cropperOffsetWhenCornersNotFound),
            Point(photo.width.toDouble(), photo.height.toDouble()).move(-cropperOffsetWhenCornersNotFound, -cropperOffsetWhenCornersNotFound)
        )
    }

    private fun openCamera() {
        cameraUtil.openCamera(documents.size)
    }

    private fun onClickDone() {
        cropDocumentsAndFinishIntent()
    }

    private fun cropDocumentsAndFinishIntent() {
        val croppedImageResults = arrayListOf<String>()
        for ((pageNumber, document) in documents.withIndex()) {
            val croppedImage: Bitmap? = try {
                ImageUtil().crop(document.originalPhotoFilePath, document.corners)
            } catch (exception: Exception) {
                finishIntentWithError("이미지를 자를 수 없습니다: ${exception.message}")
                return
            }

            if (croppedImage == null) {
                finishIntentWithError("자르기 결과가 null입니다.")
                return
            }

            File(document.originalPhotoFilePath).delete()

            try {
                val croppedImageFile = FileUtil().createImageFile(this, pageNumber)
                croppedImage.saveToFile(croppedImageFile, croppedImageQuality)
                croppedImageResults.add(Uri.fromFile(croppedImageFile).toString())
            } catch (exception: Exception) {
                finishIntentWithError("잘린 이미지를 저장할 수 없습니다: ${exception.message}")
            }
        }

        setResult(Activity.RESULT_OK, Intent().putExtra("croppedImageResults", croppedImageResults))
        finish()
    }

    private fun onClickCancel() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    private fun finishIntentWithError(errorMessage: String) {
        setResult(Activity.RESULT_OK, Intent().putExtra("error", errorMessage))
        finish()
    }
}