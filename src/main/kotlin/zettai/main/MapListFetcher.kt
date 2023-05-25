package zettai.main

import zettai.core.ListName
import zettai.core.ToDoList
import zettai.core.ToDoListUpdatableFetcher
import zettai.core.User

typealias ToDoListStore = MutableMap<User, MutableMap<ListName, ToDoList>>

class MapListFetcher(private val store: ToDoListStore) : ToDoListUpdatableFetcher {

    override fun invoke(user: User, list: ListName) = store[user]?.get(list)

    override fun assignListToUser(user: User, list: ToDoList): ToDoList? {
        return store.compute(user) { _, value ->
            val listMap = value ?: mutableMapOf()
            listMap.apply { put(list.name, list) }
        }?.let { list }
    }

    override fun getAll(user: User): List<ListName>? = store[user]?.keys?.toList()
}