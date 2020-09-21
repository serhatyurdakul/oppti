package com.serhatyurdakul.todo.util.lib.firebase

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.serhatyurdakul.todo.app.data.local.Const
import com.serhatyurdakul.todo.app.data.local.category.CategoryEntity
import com.serhatyurdakul.todo.app.data.local.todo.TodoEntity
import com.serhatyurdakul.todo.app.data.local.user.UserEntity
import com.serhatyurdakul.todo.util.lib.firebase.callback.*
import timber.log.Timber
import java.util.*

class FireStoreService : FireStoreMapper {

    // firestore reference
    private val db = FirebaseFirestore.getInstance()

    //todoList documents listener
    private var todoListener: ListenerRegistration? = null

    //categoryList documents listener
    private var categoryListener: ListenerRegistration? = null

    //todoList documents listener
    private var todoListenerCalendar: ListenerRegistration? = null

    //categoryList documents listener
    private var categoryListenerCalendar: ListenerRegistration? = null


    // create user from firebase auth user
    fun createUser(user: FirebaseUser, callback: CreateUserCallback) {
        val userDb = db.collection(Const.Collection.USER)

        userDb
            .whereEqualTo(Const.Key.User.ID, user.uid)
            .get()
            .addOnSuccessListener { result ->
                if (result == null || result.size() == 0) {
                    val userObject = toUserObject(user)
                    val userEntity = toUserEntity(user)

                    db.collection(Const.Collection.USER)
                        .add(userObject)
                        .addOnSuccessListener {
                            callback.onResponse(userEntity, null)
                        }
                        .addOnFailureListener {
                            callback.onResponse(
                                userEntity,
                                "Failed to create new user. Error: ${it.message}"
                            )
                        }
                } else {
                    for (document in result) {
                        val userEntity = toUserEntity(document.data)
                        callback.onResponse(userEntity, null)
                        break
                    }
                }
            }
            .addOnFailureListener {
                callback.onResponse(null, "Failed to check user. Error: ${it.message}")
            }
    }

    // map firebase user to user entity
    fun getUserEntity(user: FirebaseUser): UserEntity {
        return toUserEntity(user)
    }

    // add new categoryItem to firestore
    fun addCategory(category: CategoryEntity, callback: CategoryCallback) {
        val categoryObject = toCategoryObject(category)

        db.collection(Const.Collection.CATEGORY)
            .add(categoryObject)
            .addOnSuccessListener {
                category.id = it.id
                callback.onResponse(category, null)
            }
            .addOnFailureListener {
                Timber.e(it)
                callback.onResponse(category, "Failed. Error: ${it.message}")
            }
    }

    // update an existing categoryItem
    fun updateCategory(category: CategoryEntity, callback: CategoryCallback) {
        val categoryDoc = db.collection(Const.Collection.CATEGORY).document(category.id)

        val tasks = ArrayList<Task<Void>>()
        tasks.add(categoryDoc.update(Const.Key.Category.TITLE, category.title))
        tasks.add(categoryDoc.update(Const.Key.Category.COLOR, category.color))

        Tasks.whenAllSuccess<Void>(tasks).addOnSuccessListener {
            callback.onResponse(category, null)
        }.addOnFailureListener {
            Timber.e(it)
            callback.onResponse(null, "Opps! ${it.message}")
        }
    }


    // delete categoryItem
    fun deleteTodo(category: CategoryEntity, callback: CategoryCallback) {
        val categoryDb = db.collection(Const.Collection.CATEGORY).document(category.id)

        categoryDb
            .delete()
            .addOnSuccessListener {
                callback.onResponse(category, null)
            }
            .addOnFailureListener {
                Timber.e(it)
                callback.onResponse(category, "Opps! ${it.message}")
            }
    }


    // get categoryList of a particular user
    fun getCategoryList(user: UserEntity, callback: CategoryListCallback) {
        val categoryDb = db.collection(Const.Collection.CATEGORY)

        categoryDb
            .whereEqualTo(Const.Key.Category.USER, user.id)
            .get()
            .addOnSuccessListener {
                callback.onResponse(toCategoryEntityList(it), null)
            }
            .addOnFailureListener {
                callback.onResponse(null, "Failed. Error: ${it.message}")
            }
    }

    // register a listener for getting the live changes of categoryDocuments stored in firestore
    fun addCategoryListListener(user: UserEntity, callback: CategoryListCallback) {
        val query = db.collection(Const.Collection.CATEGORY)
            .whereEqualTo(Const.Key.Category.USER, user.id)

        categoryListener = query.addSnapshotListener(EventListener<QuerySnapshot> { value, e ->
            if (e != null) {
                Timber.e("Listen failed. %s", e.toString())
                callback.onResponse(null, "Failed. Error: ${e.message}")
                return@EventListener
            }

            callback.onResponse(toCategoryEntityList(value!!), null)
        })
    }

    // unregister the listener
    fun removeCategoryListListener() {
        categoryListener?.remove()
    }

    // register a listener for getting the live changes of categoryDocuments stored in firestore
    fun addCategoryListListenerCalendar(user: UserEntity, callback: CategoryListCallback) {
        val query = db.collection(Const.Collection.CATEGORY)
            .whereEqualTo(Const.Key.Category.USER, user.id)

        categoryListenerCalendar =
            query.addSnapshotListener(EventListener<QuerySnapshot> { value, e ->
                if (e != null) {
                    Timber.e("Listen failed. %s", e.toString())
                    callback.onResponse(null, "Failed. Error: ${e.message}")
                    return@EventListener
                }

                callback.onResponse(toCategoryEntityList(value!!), null)
            })
    }

    // unregister the listener
    fun removeCategoryListListenerCalendar() {
        categoryListenerCalendar?.remove()
    }


    // add new todoItem to firestore
    fun addTodo(todo: TodoEntity, callback: TodoCallback) {
        val todoObject = toTodoObject(todo)

        db.collection(Const.Collection.TODO)
            .add(todoObject)
            .addOnSuccessListener {
                todo.id = it.id
                callback.onResponse(todo, null)
            }
            .addOnFailureListener {
                Timber.e(it)
                callback.onResponse(todo, "Failed. Error: ${it.message}")
            }
    }

    // update an existing todoITem
    fun updateTodo(todo: TodoEntity, callback: TodoCallback) {
        val todoDoc = db.collection(Const.Collection.TODO).document(todo.id)

        val tasks = ArrayList<Task<Void>>()
        tasks.add(todoDoc.update(Const.Key.Todo.TODO, todo.todo))
        tasks.add(todoDoc.update(Const.Key.Todo.DATE, todo.date))
        tasks.add(todoDoc.update(Const.Key.Todo.CATEGORY, todo.category))

        Tasks.whenAllSuccess<Void>(tasks).addOnSuccessListener {
            callback.onResponse(todo, null)
        }.addOnFailureListener {
            Timber.e(it)
            callback.onResponse(null, "Opps! ${it.message}")
        }
    }

    // update the complete status of bulk todoItems
    fun markTodoListAsComplete(todoList: ArrayList<TodoEntity>, callback: TodoListCallback) {
        val todoDb = db.collection(Const.Collection.TODO)

        val tasks = ArrayList<Task<Void>>()
        todoList.forEach {
            tasks.add(todoDb.document(it.id).update(Const.Key.Todo.COMPLETED, it.completed))
        }

        Tasks.whenAllSuccess<Void>(tasks).addOnSuccessListener {
            callback.onResponse(todoList, null)
        }.addOnFailureListener {
            Timber.e(it)
            callback.onResponse(null, "Opps! ${it.message}")
        }
    }

    // delete todoItem
    fun deleteTodo(todo: TodoEntity, callback: TodoCallback) {
        val todoDb = db.collection(Const.Collection.TODO).document(todo.id)

        todoDb
            .delete()
            .addOnSuccessListener {
                callback.onResponse(todo, null)
            }
            .addOnFailureListener {
                Timber.e(it)
                callback.onResponse(todo, "Opps! ${it.message}")
            }
    }

    // delete categoryItem
    fun deleteCategory(category: CategoryEntity, callback: CategoryCallback) {
        val todoDb = db.collection(Const.Collection.CATEGORY).document(category.id)

        todoDb
            .delete()
            .addOnSuccessListener {
                callback.onResponse(category, null)
            }
            .addOnFailureListener {
                Timber.e(it)
                callback.onResponse(category, "Opps! ${it.message}")
            }
    }

    // delete bulk todoItems
    fun deleteTodoList(todoList: ArrayList<TodoEntity>, callback: TodoListCallback) {
        val todoDb = db.collection(Const.Collection.TODO)

        val tasks = ArrayList<Task<Void>>()
        todoList.forEach {
            tasks.add(todoDb.document(it.id).delete())
        }

        Tasks.whenAllSuccess<Void>(tasks).addOnSuccessListener {
            callback.onResponse(todoList, null)
        }.addOnFailureListener {
            Timber.e(it)
            callback.onResponse(null, "Opps! ${it.message}")
        }
    }

    // get todoList of a particular user
    fun getTodoList(user: UserEntity, callback: TodoListCallback) {
        val todoDb = db.collection(Const.Collection.TODO)

        todoDb
            .whereEqualTo(Const.Key.Todo.USER, user.id)
            .get()
            .addOnSuccessListener {
                callback.onResponse(toTodoEntityList(it), null)
            }
            .addOnFailureListener {
                callback.onResponse(null, "Failed. Error: ${it.message}")
            }
    }

    // register a listener for getting the live changes of todoDocuments stored in firestore
    fun addTodoListListener(user: UserEntity, callback: TodoListCallback) {
        val query = db.collection(Const.Collection.TODO)
            .whereEqualTo(Const.Key.Todo.USER, user.id)

        todoListener = query.addSnapshotListener(EventListener<QuerySnapshot> { value, e ->
            if (e != null) {
                Timber.e("Listen failed. %s", e.toString())
                callback.onResponse(null, "Failed. Error: ${e.message}")
                return@EventListener
            }

            callback.onResponse(toTodoEntityList(value!!), null)
        })
    }

    // unregister the listener
    fun removeTodoListListener() {
        todoListener?.remove()
    }

    // register a listener for getting the live changes of todoDocuments stored in firestore
    fun addTodoListListenerCalendar(user: UserEntity, callback: TodoListCallback) {
        val query = db.collection(Const.Collection.TODO)
            .whereEqualTo(Const.Key.Todo.USER, user.id)

        todoListenerCalendar = query.addSnapshotListener(EventListener<QuerySnapshot> { value, e ->
            if (e != null) {
                Timber.e("Listen failed. %s", e.toString())
                callback.onResponse(null, "Failed. Error: ${e.message}")
                return@EventListener
            }

            callback.onResponse(toTodoEntityList(value!!), null)
        })
    }

    // unregister the listener
    fun removeTodoListListenerCalendar() {
        todoListenerCalendar?.remove()
    }

}