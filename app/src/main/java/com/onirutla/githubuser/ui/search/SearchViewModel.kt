package com.onirutla.githubuser.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onirutla.githubuser.data.Resource
import com.onirutla.githubuser.data.source.UserRepository
import com.onirutla.githubuser.data.source.local.entity.UserEntity
import com.onirutla.githubuser.util.MainDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@ExperimentalCoroutinesApi
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val userRepository: UserRepository,
    @MainDispatcher private val dispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _username = MutableSharedFlow<String>()

    val users: StateFlow<Resource<List<UserEntity>>> = _username.flatMapLatest {
        userRepository.searchBy(it)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Resource.Loading()
    )

    fun search(username: String) {
        viewModelScope.launch(dispatcher) {
            _username.emit(username)
        }
    }

}
