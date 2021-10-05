package com.elvis.freehub

class PostInfo {
    var UserUID:String?=null
    var text:String?=null
    var postImage:String?=null
    //var currentDate:String?=null
    constructor(UserUID: String, text: String?, postImage: String?){
        this.UserUID=UserUID
        this.text=text
        this.postImage=postImage
        //this.currentDate=currentDate

    }

}