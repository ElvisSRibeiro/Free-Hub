@file:Suppress("DEPRECATION")

package com.elvis.freehub

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Toast
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.database.DatabaseError
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.add_ticket.view.*
import kotlinx.android.synthetic.main.tweets_ticket.view.*
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    private var database= FirebaseDatabase.getInstance()
    private var myRef=database.reference
    private lateinit var storageReference: StorageReference

    

    var ListTweets=ArrayList<Ticket>()
    var adapter:MyTweetAdapter?=null
    var myemail:String?=null
    var UserUID:String?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var b:Bundle=intent.extras!!
        myemail=b.getString("email")
        UserUID=b.getString("uid")
        //dummy data
        ListTweets.add(Ticket("0","him","url","add"))
        //ListTweets.add(Ticket("0","texto","post1"))
        //ListTweets.add(Ticket("0","texto","post2"))
        //ListTweets.add(Ticket("0","texto","post3"))


        adapter= MyTweetAdapter(this,ListTweets)
        lvTweets.adapter=adapter
        LoadPosts()
    }


    inner class MyTweetAdapter:BaseAdapter{
        var listNotesAdapter=ArrayList<Ticket>()
        var context:Context?=null
        constructor(context:Context, listNotesAdapter:ArrayList<Ticket>):super(){
            this.listNotesAdapter=listNotesAdapter
            this.context=context
        }

        @SuppressLint("InflateParams")
        override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {

            var mytweet=listNotesAdapter[p0]

            if (mytweet.tweetPersonUID.equals("add")){
                var myView=layoutInflater.inflate(R.layout.add_ticket,null)
                myView.iv_attach.setOnClickListener( View.OnClickListener {
                loadImage()

                })
                myView.iv_post.setOnClickListener(View.OnClickListener {
                    //Upload to the server
                    if(myView.etPost.text!= null){
                        //val sdf = SimpleDateFormat("dd/M/yyyy hh:mm:ss")
                        //val currentDate = sdf.format(Date())

                        myRef.child("posts").push().setValue(
                                PostInfo(myemail?.let { it1 -> SplitString(it1) }!!,
                                        myView.etPost.text.toString(),
                                        DownloadURL!!

                                ))
                        myView.etPost.setText("")
                    }

                })
                return myView
            } else if(mytweet.tweetPersonUID.equals("loading")){
                var myView=layoutInflater.inflate(R.layout.loading_ticket,null)
                return myView
            }else{//faz o load dos posts na tela
                var myView=layoutInflater.inflate(R.layout.tweets_ticket,null)
                myView.txt_tweet.setText(mytweet.tweetText)
                myView.txtUserName.setText(mytweet.tweetPersonUID)
                Picasso.get().load(mytweet.tweetImageURL).into(myView.tweet_picture);

                return myView
            }
        }

        override fun getItem(p0: Int): Any {
            return listNotesAdapter[p0]
        }

        override fun getItemId(p0: Int): Long {
            return p0.toLong()
        }

        override fun getCount(): Int {

            return listNotesAdapter.size

        }

    }
    //Load Image
    val PICK_IMAGE_CODE=123
    fun loadImage() {
        var intent= Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

        startActivityForResult(intent,PICK_IMAGE_CODE)
    }
    //pick image
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode==PICK_IMAGE_CODE  && data!=null && resultCode == RESULT_OK){

            val selectedImage=data.data
            val filePathColum= arrayOf(MediaStore.Images.Media.DATA)
            val cursor= contentResolver.query(selectedImage!!,filePathColum,null,null,null)
            cursor!!.moveToFirst()
            val coulomIndex=cursor!!.getColumnIndex(filePathColum[0])
            val picturePath=cursor!!.getString(coulomIndex)
            cursor!!.close()
            UploadImage(BitmapFactory.decodeFile(picturePath))
        }

    }

    var DownloadURL:String?=""
    //-----------------------------------//
    //aqui que vai entrar o modo de enviar a imagem para o storage!!!!!!!!!!!!!
    //fun uploadPostPic() {
       // storageReference = FirebaseStorage.getInstance().getReference("imagePost/"+)
    //}
    @SuppressLint("SimpleDateFormat")
    fun UploadImage(bitmap:Bitmap) {
        ListTweets.add(0,Ticket("0","him","url","loading"))
        adapter!!.notifyDataSetChanged()

        if(bitmap !=null){
            val pd = ProgressDialog(this)
                pd.setTitle("Carregando")
                pd.show()

            val storage= FirebaseStorage.getInstance()
            val storgaRef=storage.getReferenceFromUrl("gs://free-hub-8cac8.appspot.com/")
            val df= SimpleDateFormat("ddMMyyHHmmss")
            val dataobj= Date()
            val imagePath= SplitString(myemail!!) + "."+ df.format(dataobj)+ ".jpg"
            val ImageRef=storgaRef.child("imagePost/"+imagePath )
            val baos= ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,baos)
            val data= baos.toByteArray()
            val uploadTask=ImageRef.putBytes(data)
            uploadTask.addOnSuccessListener { taskSnapshot ->
                taskSnapshot.storage.downloadUrl.addOnCompleteListener {
                    pd.dismiss()
                    DownloadURL= it.result.toString()
                    ListTweets.removeAt(0)
                    adapter!!.notifyDataSetChanged()

                }
            }.addOnFailureListener{ taskSnapshot ->
                pd.dismiss()
                Toast.makeText(applicationContext,"fail to upload", Toast.LENGTH_LONG).show()
            }.addOnProgressListener { taskSnapshot ->
                var progress = (100.0 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount
                pd.setMessage("Carregando ${progress.toInt()}%")
            }

        }


    }

    fun SplitString(email: String):String{
        val split=email.split("@")
        return split[0]
    }

    fun LoadPosts(){
        //Adicionar a função para ordenar of posts
        //Adicioanr a funcionalidade para limitar a quantidade de itens que são renderizados
        myRef.child("posts")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        try {

                            ListTweets.clear()
                            ListTweets.add(Ticket("0","him","url","add"))

                            var td = dataSnapshot!!.value as HashMap<String, Any>

                            for (key in td.keys){

                                var post=td[key] as HashMap<String, Any>

                                ListTweets.add(Ticket(key,
                                        post["text"] as String,
                                        post["postImage"] as String,
                                        post["userUID"] as String))


                            }

                            adapter!!.notifyDataSetChanged()
                        }catch (ex: Exception){}
                    }

                    override fun onCancelled(error: DatabaseError) {
                        TODO("Not yet implemented")
                    }
                })
    }

}
