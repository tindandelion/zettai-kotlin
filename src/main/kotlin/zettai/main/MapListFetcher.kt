package zettai.main

import zettai.core.*

typealias ToDoListStore = MutableMap<User, MutableMap<ListName, ToDoList>>

class MapListFetcher(private val store: ToDoListStore) : ToDoListUpdatableFetcher {

    override fun getList(user: User, listName: ListName) = store[user]?.get(listName)

    override fun assignListToUser(user: User, list: ToDoList): ToDoList? {
        return store.compute(user) { _, value ->
            val listMap = value ?: mutableMapOf()
            listMap.apply { put(list.name, list) }
        }?.let { list }
    }

    override fun getAll(user: User): List<ListName>? = store[user]?.keys?.toList()
    override fun addItemToList(user: User, listName: ListName, item: ToDoItem): ToDoList? =
        getList(user, listName)?.run {
            val newList = addItem(item)
            assignListToUser(user, newList)
        }
}