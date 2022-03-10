package com.nukte.kotlinlibraryapp

import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.nukte.kotlinlibraryapp.databinding.ActivityMainBinding
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding
    private lateinit var bookList : ArrayList<Books>
    private lateinit var bookAdapter : BookAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        bookList = ArrayList<Books>()
        bookAdapter = BookAdapter(bookList)

        binding.recyclerView.layoutManager = LinearLayoutManager(this@MainActivity)

        try {
            val database = this.openOrCreateDatabase("Books", MODE_PRIVATE,null)

            var cursor = database.rawQuery("SELECT * FROM  books",null)
            var nameIx = cursor.getColumnIndex("bookname")
            var idIx = cursor.getColumnIndex("id")

            while (cursor.moveToNext()){
                val name = cursor.getString(nameIx)
                val id = cursor.getInt(idIx)
                val book = Books(name,id)
                bookList.add(book)

            }
            binding.recyclerView.adapter = bookAdapter
            bookAdapter.notifyDataSetChanged()

            cursor.close()
        }
        catch (e :Exception){
            e.printStackTrace()
        }

    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        //inflater
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.book_menu,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if(item.itemId == R.id.add_book_item){
            val intent = Intent(this@MainActivity,BookDetails::class.java)
            intent.putExtra("info","new")
            startActivity(intent)
        }
        return super.onOptionsItemSelected(item)
    }
}