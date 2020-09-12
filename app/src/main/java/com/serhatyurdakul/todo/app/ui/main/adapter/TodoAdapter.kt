package com.serhatyurdakul.todo.app.ui.main.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.serhatyurdakul.todo.R
import com.serhatyurdakul.todo.app.data.local.category.CategoryEntity
import com.serhatyurdakul.todo.app.data.local.todo.TodoEntity
import com.serhatyurdakul.todo.app.ui.main.callback.CategoryClickEvent
import com.serhatyurdakul.todo.app.ui.main.callback.TodoClickEvent
import com.serhatyurdakul.todo.app.ui.main.holder.CategoryHolder
import com.serhatyurdakul.todo.app.ui.main.holder.TodoHolder
import com.serhatyurdakul.todo.databinding.ItemCategoryBinding
import com.serhatyurdakul.todo.databinding.ItemTodoBinding

/*
* Recycler view Adpater.
* It shows the todoItems list.
* It needs a list of todoItems and a TodoClickEvent listener
*/
class TodoAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var list = ArrayList<Any>()
    private var listOfTodos = ArrayList<TodoEntity>()
    private var listOfCategories = ArrayList<CategoryEntity>()
    private var listener: TodoClickEvent? = null
    private var listenerCategory: CategoryClickEvent? = null

    private val VIEW_TYPE_TODO = 1
    private val VIEW_TYPE_CATEGORY = 2

    private var categoryVisible = true

    fun setTodoList(listOfTodo: List<TodoEntity>) {
        this.listOfTodos.clear()
        this.listOfTodos.addAll(listOfTodo)
        organizeList()

    }

    fun setCategoryList(listOfCategory: List<CategoryEntity>) {
        this.listOfCategories.clear()
        this.listOfCategories.addAll(listOfCategory)
        organizeList()

    }

    fun getToDoCount(): Int {

        return this.listOfTodos.size
    }

    fun showCategories() {
        if (categoryVisible) {
            categoryVisible = false
            organizeList()

        } else {
            categoryVisible = true
            organizeList()
        }


    }

    fun getCategoryListArray(): Array<String> {
        val strings = Array<String>(listOfCategories.size) { "it = $it" }
        var count = 0
        for (cat in listOfCategories) {
            strings[count++] = cat.title

        }
        return strings
    }

    private fun organizeList() {
        var leftOverCategories = ArrayList<CategoryEntity>()

        list.clear()
        if ((listOfCategories.size == 0 || !categoryVisible))
            list.addAll(listOfTodos)
        else {
            for (category in listOfCategories) {
                var isEmpty = true
                list.add(category)
                for (todo in listOfTodos) {
                    if (todo.category == category.title) {
                        list.add(todo)
                        isEmpty = false
                    }
                }
                if (isEmpty) {
                    list.remove(category)
                    leftOverCategories.add(category)
                }
            }

            list.addAll(leftOverCategories)

        }
        notifyDataSetChanged()
    }

    /*
        fun addTodoList(listOfTodo: List<TodoEntity>) {
            this.list.addAll(listOfTodo)
            notifyDataSetChanged()
        }

        fun addTodo(todo: TodoEntity) {
            this.list.add(todo)
            notifyDataSetChanged()
        }

        fun getTodoList(): ArrayList<TodoEntity> {
            return list
        }
    */
    fun getIncompleteTodoList(): ArrayList<TodoEntity> {
        val incompleteList = ArrayList<TodoEntity>()
        list.forEach {
            if (it is TodoEntity)
                if (!it.completed) incompleteList.add(it)
        }

        return incompleteList
    }

    fun getCompletedTodoList(): ArrayList<TodoEntity> {
        val completedList = ArrayList<TodoEntity>()
        list.forEach {
            if (it is TodoEntity)
                if (it.completed) completedList.add(it)
        }

        return completedList
    }

    fun setListeners(listener: TodoClickEvent, listenerCategory: CategoryClickEvent) {
        this.listener = listener
        this.listenerCategory = listenerCategory
    }

    fun clear() {
        list.clear()
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        if (list[position] is TodoEntity) {
            return VIEW_TYPE_TODO
        }
        return VIEW_TYPE_CATEGORY


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        if (viewType == VIEW_TYPE_TODO) {
            val binding = DataBindingUtil.inflate<ItemTodoBinding>(
                LayoutInflater.from(parent.context), R.layout.item_todo, parent, false
            )

            return TodoHolder(binding)
        } else {
            val binding = DataBindingUtil.inflate<ItemCategoryBinding>(
                LayoutInflater.from(parent.context), R.layout.item_category, parent, false
            )

            return CategoryHolder(binding)


        }
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {

        if (list[position] is TodoEntity) {
            val holder = viewHolder as TodoHolder

            holder.bind(list[position] as TodoEntity, listener)
        } else {
            val holder = viewHolder as CategoryHolder

            holder.bind(list[position] as CategoryEntity, listenerCategory)


        }


    }
}