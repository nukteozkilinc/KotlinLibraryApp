package com.nukte.kotlinlibraryapp

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.nukte.kotlinlibraryapp.databinding.ActivityBookDetailsBinding
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.util.jar.Manifest

class BookDetails : AppCompatActivity() {

    private lateinit var binding : ActivityBookDetailsBinding
    var selectedBitmap : Bitmap?= null
    private lateinit var activityResultLauncher : ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher : ActivityResultLauncher<String>
    private lateinit var database: SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookDetailsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        database = this.openOrCreateDatabase("Books", MODE_PRIVATE,null)

        registerLauncher()

        val intent = intent
        val info = intent.getStringExtra("info")

        if(info.equals("new")){
            binding.bookNameText.setText("")
            binding.authorNameText.setText("")
            binding.issueText.setText("")
            binding.pageText.setText("")
            binding.deleteBtn.isEnabled = false
            binding.saveBtn.isEnabled = true
        }else {
            binding.deleteBtn.isEnabled = true
            val selectedId = intent.getIntExtra("id",1)


            var cursor = database.rawQuery("SELECT * FROM  books WHERE id = ?", arrayOf(selectedId.toString()))


            val nameIx = cursor.getColumnIndex("bookname")
            val authorIx = cursor.getColumnIndex("authorname")
            val pageIx = cursor.getColumnIndex("numberofpage")
            val dateIx = cursor.getColumnIndex("dateofissue")
            val imageIx = cursor.getColumnIndex("image")

            while (cursor.moveToNext()){
                binding.bookNameText.setText(cursor.getString(nameIx))
                binding.authorNameText.setText(cursor.getString(authorIx))
                binding.pageText.setText(cursor.getString(pageIx))
                binding.issueText.setText(cursor.getString(dateIx))

                val byteArray = cursor.getBlob(imageIx)
                val bitmap = BitmapFactory.decodeByteArray(byteArray,0,byteArray.size)
                binding.imageView.setImageBitmap(bitmap)
            }

            cursor.close()
        }
    }

    fun saveButtonClick(view : View) {

        val bookName = binding.bookNameText.text.toString()
        val authorName = binding.authorNameText.text.toString()
        val numberOfPage = binding.pageText.text.toString()
        val dateOfIssue = binding.issueText.text.toString()

            if (selectedBitmap != null){

                val smallBitmap = makeSmallerBitmap(selectedBitmap!!,300)

                val outputStream = ByteArrayOutputStream()
                smallBitmap.compress(Bitmap.CompressFormat.PNG,50,outputStream)
                val byteArray = outputStream.toByteArray()

            try {
                database.execSQL("CREATE TABLE IF NOT EXISTS books (id INTEGER PRIMARY KEY,bookname VARCHAR,authorname VARCHAR,numberofpage VARCHAR,dateofissue VARCHAR, image BLOB)")

                val sqlString =
                    "INSERT INTO books (bookname,authorname,numberofpage,dateofissue,image) VALUES (?,?,?,?,?)"

                val statement = database.compileStatement(sqlString)
                statement.bindString(1, bookName)
                statement.bindString(2, authorName)
                statement.bindString(3, numberOfPage)
                statement.bindString(4, dateOfIssue)
                statement.bindBlob(5, byteArray)
                statement.execute()

            } catch (e: Exception) {
                e.printStackTrace()
            }

        val intent = Intent(this@BookDetails, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)

    }
    }


    fun selectImage(view:View){

        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            //rationale
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)){
                Snackbar.make(view,"Permission nedded for gallery",Snackbar.LENGTH_INDEFINITE).setAction("Give permission",View.OnClickListener {
                    //request permission
                    permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                }).show()

            }else{
                //request permission
                permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }

        }else{
            val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            //intent
            activityResultLauncher.launch(intentToGallery)
        }

    }

    fun deleteButtonClick(view : View){
        val intent = intent
        val info = intent.getStringExtra("info")

        if(info.equals("old")) {
            val selectedId = intent.getIntExtra("id", 1)
            database.execSQL("DELETE FROM books WHERE id = ?", arrayOf(selectedId.toString()))
        }

        val intent2 = Intent(this@BookDetails, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent2)

    }

    private fun registerLauncher(){

        activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()){ result ->
            if(result.resultCode == RESULT_OK){
                val intentFromResult = result.data
                if(intentFromResult != null){
                    val imageData = intentFromResult.data
                    if(imageData != null) {
                        try {
                            if (Build.VERSION.SDK_INT >= 28) {
                                val source = ImageDecoder.createSource(this@BookDetails.contentResolver, imageData)
                                selectedBitmap = ImageDecoder.decodeBitmap(source)
                                binding.imageView.setImageBitmap(selectedBitmap)
                            }else{
                                selectedBitmap = MediaStore.Images.Media.getBitmap(contentResolver,imageData)
                                binding.imageView.setImageBitmap(selectedBitmap)
                            }
                        }catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

        }
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){result ->
            if(result){
                //permission granted
                val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)

            }else{
                Toast.makeText(this@BookDetails,"Permission nedded",Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun makeSmallerBitmap(image : Bitmap ,maxSize : Int) : Bitmap{
        var width = image.width
        var height = image.height

        val bitmapRatio :Double = width.toDouble() / height.toDouble()

        if(bitmapRatio >1){
            //landscape
            width = maxSize
            val scaleHeight = width/bitmapRatio
            height = scaleHeight.toInt()

        }else{
            //portrait
            height = maxSize
            val scaleWidth = height*bitmapRatio
            width = scaleWidth.toInt()

        }

        return Bitmap.createScaledBitmap(image,width,height,true)
    }
}