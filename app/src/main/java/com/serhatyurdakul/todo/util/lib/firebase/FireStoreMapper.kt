package com.serhatyurdakul.todo.util.lib.firebase

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.serhatyurdakul.todo.app.data.local.Const
import com.serhatyurdakul.todo.app.data.local.category.CategoryEntity
import com.serhatyurdakul.todo.app.data.local.todo.TodoEntity
import com.serhatyurdakul.todo.app.data.local.user.UserEntity
import com.serhatyurdakul.todo.util.helper.FormatUtil
import java.util.*

/*
* This class performs the conversions between firestore object and kotlin data class
*/
interface FireStoreMapper {

    // map firebase user data to userObject
    fun toUserObject(user: FirebaseUser): HashMap<String, Any> {
        val userObject = HashMap<String, Any>()
        userObject[Const.Key.User.ID] = user.uid
        userObject[Const.Key.User.NAME] = user.displayName!!
        userObject[Const.Key.User.EMAIL] = user.email!!
        val image = if (user.photoUrl == null) "" else user.photoUrl.toString()
        userObject[Const.Key.User.IMAGE] = image

        return userObject
    }

    // map firebase user to user entity
    fun toUserEntity(user: FirebaseUser): UserEntity {
        val image = if (user.photoUrl == null) "" else user.photoUrl.toString()
        return UserEntity(
            user.uid,
            user.displayName,
            user.email,
            image
        )
    }

    // map firestore object to user entity
    fun toUserEntity(document: Map<String, Any>): UserEntity {
        return UserEntity(
            document[Const.Key.User.ID] as String,
            document[Const.Key.User.NAME] as String,
            document[Const.Key.User.EMAIL] as String,
            document[Const.Key.User.IMAGE] as String
        )
    }

    // map categoryEntity to firestore categoryObject
    fun toCategoryObject(category: CategoryEntity): HashMap<String, Any> {
        val categoryObject = HashMap<String, Any>()
        categoryObject[Const.Key.Category.COLOR] = category.color
        categoryObject[Const.Key.Category.TITLE] = category.title
        categoryObject[Const.Key.Category.USER] = category.user

        return categoryObject
    }


    // map firestore categoryObject to categoryEntity
    private fun toCategoryEntity(document: QueryDocumentSnapshot): CategoryEntity {
        val data = document.data
        val id = document.id
        val title = data[Const.Key.Category.TITLE] as String
        val user = data[Const.Key.Category.USER] as String
        val color = data[Const.Key.Category.COLOR] as String


        return CategoryEntity(
            id, title, user, color
        )
    }

    // map firestore categoryObject list to categoryEntity list
    fun toCategoryEntityList(data: QuerySnapshot): ArrayList<CategoryEntity> {
        val categoryEntityList = ArrayList<CategoryEntity>()
        for (document in data)
            categoryEntityList.add(toCategoryEntity(document))

        return categoryEntityList
    }


    // map todoEntity to firestore todoObject
    fun toTodoObject(todo: TodoEntity): HashMap<String, Any> {
        val todoObject = HashMap<String, Any>()
        todoObject[Const.Key.Todo.TODO] = todo.todo
        todoObject[Const.Key.Todo.COMPLETED] = todo.completed
        todoObject[Const.Key.Todo.DATE] = todo.date
        todoObject[Const.Key.Todo.DATEEPOCH] = todo.dateEpoch
        todoObject[Const.Key.Todo.USER] = todo.user
        todoObject[Const.Key.Todo.CREATED_AT] = FieldValue.serverTimestamp()
        todoObject[Const.Key.Todo.CATEGORY] = todo.category

        return todoObject
    }

    // map firestore todoObject to todoEntity
    private fun toTodoEntity(document: QueryDocumentSnapshot): TodoEntity {
        val todoId = document.id
        val data = document.data

        val todo = data[Const.Key.Todo.TODO] as String
        val completed = data[Const.Key.Todo.COMPLETED] as Boolean
        val date = data[Const.Key.Todo.DATE] as String
        val dateEpoch = data[Const.Key.Todo.DATEEPOCH] as Long
        val user = data[Const.Key.Todo.USER] as String
        val category = data[Const.Key.Todo.CATEGORY] as String
        val timestamp = data[Const.Key.Todo.CREATED_AT]
        var timestampToDate = Date()
        if (timestamp != null) timestampToDate = (timestamp as Timestamp).toDate()
        val createdAt = FormatUtil().formatDate(timestampToDate, FormatUtil.dd_MMM_yyyy)

        return TodoEntity(
            todoId, todo, completed, date,dateEpoch, user, createdAt, category
        )
    }

    // map firestore todoObject list to todoEntity list
    fun toTodoEntityList(data: QuerySnapshot): ArrayList<TodoEntity> {
        val todoEntityList = ArrayList<TodoEntity>()
        for (document in data)
            todoEntityList.add(toTodoEntity(document))

        return todoEntityList
    }
}