package com.elvis.freehub

class Ticket{
    var tweetID:String?=null
    var tweetText:String?=null
    //var tweetImageURL:String?=null
    var tweetPersonUID:String?=null

    constructor(tweetID:String,tweetText:String,tweetPersonUID:String){
        this.tweetID=tweetID
        this.tweetText=tweetText
        //this.tweetImageURL=tweetImageURL
        this.tweetPersonUID=tweetPersonUID
    }
}

//tweetImageURL:String