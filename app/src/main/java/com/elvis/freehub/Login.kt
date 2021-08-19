package com.elvis.freehub

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_login.*
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

@Suppress("DEPRECATION")
class Login : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private var mAuth= FirebaseAuth.getInstance()

    private var database=FirebaseDatabase.getInstance()
    private var myRef=database.reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        mAuth= FirebaseAuth.getInstance()

        ivImagePerson.setOnClickListener(View.OnClickListener {
            checkPermission()
        })
    }


    fun LoginToFireBase(email:String,password:String){

        mAuth.createUserWithEmailAndPassword(email,password)
                .addOnCompleteListener(this){ task ->

                if (task.isSuccessful){
                    Toast.makeText(applicationContext,"Successful login",Toast.LENGTH_LONG).show()


                    SaveImageInFirebase()


                }else {
                    Toast.makeText(applicationContext,"por favor insira um email valido",Toast.LENGTH_LONG).show()
                }

            }

    }


    //tratamento da imagem antes de enviar para database
    fun SaveImageInFirebase() {
        val currentUser = mAuth!!.currentUser
        val email:String=currentUser!!.email.toString()
        val storage=FirebaseStorage.getInstance()
        val storageRef=storage.getReferenceFromUrl("gs://free-hub-8cac8.appspot.com")
        val df=SimpleDateFormat("ddMMyyHHmmss")
        val dataObj=Date()
        val imagePath= SplitString(email) + "." + df.format(dataObj)+ ".jpg"
        val ImageRef=storageRef.child("images/" + imagePath)
        ivImagePerson.isDrawingCacheEnabled=true
        ivImagePerson.buildDrawingCache()

        val drawable=ivImagePerson.drawable as BitmapDrawable
        val bitmap=drawable.bitmap
        val baos=ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data= baos.toByteArray()
        val uploadTask=ImageRef.putBytes(data)

        uploadTask.addOnFailureListener{
            Toast.makeText(applicationContext,"fail to upload", Toast.LENGTH_SHORT).show()
        }.addOnSuccessListener {taskSnapshot->

            var DownloadURL= taskSnapshot.storage.downloadUrl.toString()!!

            if (currentUser != null) {
                myRef.child("Users").child(currentUser.uid).child("email").setValue(currentUser.email)
            }
            if (currentUser != null) {
                myRef.child("Users").child(currentUser.uid).child("ProfileImage").setValue(DownloadURL)
            }
            LoadTweets()
        }

    }
    //split o email para dar nome a imagem
    fun SplitString(email: String):String{
        val split=email.split("@")
        return split[0]
    }

    override fun onStart() {
        super.onStart()
        LoadTweets()

    }
    fun LoadTweets() {
        var currentUser = mAuth.currentUser

        if (currentUser!=null){
            var intent = Intent(this, MainActivity::class.java)


            startActivity(intent)
        }
    }

    val READIMAGE: Int = 253
    fun checkPermission(){
        if(Build.VERSION.SDK_INT>=23){
            if(ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE)!=
                PackageManager.PERMISSION_GRANTED){

                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),READIMAGE)
                return
            }
        }
        loadImage()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode){
            READIMAGE->{
                if(grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    loadImage()
                }else{
                Toast.makeText(this,"cannot access your images", Toast.LENGTH_LONG).show()
                }
            }
            else->{
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }

    }

    val PICK_IMAGE_CODE=123
    fun loadImage() {
        var intent=Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

        startActivityForResult(intent,PICK_IMAGE_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode==PICK_IMAGE_CODE && data!=null && resultCode == RESULT_OK){
            val selectedImage=data.data
            val filePathColum=arrayOf(MediaStore.Images.Media.DATA)
            val cursor=
                selectedImage?.let { contentResolver.query(it, filePathColum, null, null, null ) }
            cursor?.moveToFirst()
            val columnIndex=cursor?.getColumnIndex(filePathColum[0])
            val picturePath= columnIndex?.let { cursor?.getString(it) }
            cursor?.close()
            ivImagePerson.setImageBitmap(BitmapFactory.decodeFile(picturePath))
        }
    }

    fun buLogin(view: View){
        LoginToFireBase(etEmail.text.toString(), etPassword.text.toString())
    }
}