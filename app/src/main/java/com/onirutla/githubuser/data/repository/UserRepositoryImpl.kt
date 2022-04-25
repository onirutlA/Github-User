package com.onirutla.githubuser.data.repository

import android.util.Log
import com.onirutla.githubuser.data.Resource
import com.onirutla.githubuser.data.source.UserRepository
import com.onirutla.githubuser.data.source.local.LocalDataSource
import com.onirutla.githubuser.data.source.local.entity.UserEntity
import com.onirutla.githubuser.data.source.remote.NetworkState
import com.onirutla.githubuser.data.source.remote.RemoteDataSource
import com.onirutla.githubuser.data.source.remote.response.toEntity
import com.onirutla.githubuser.util.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val remoteDataSource: RemoteDataSource,
    private val localDataSource: LocalDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UserRepository {

    override fun searchBy(username: String): Flow<Resource<List<UserEntity>>> =
        localDataSource.searchBy(username).mapNotNull { local ->
            if (local.isNullOrEmpty()) {
                when (val response = remoteDataSource.searchBy(username)) {
                    is NetworkState.Error -> Resource.Error(response.message)
                    is NetworkState.Success -> {
                        val entity = response.body.map { it.toEntity() }
                        localDataSource.insertUsers(entity)
                        Resource.Success(entity)
                    }
                }
            } else
                Resource.Success(local)
        }.onStart {
            emit(Resource.Loading())
        }.catch {
            Log.d("repo search", "$it")
            emit(Resource.Error(it.localizedMessage))
        }.flowOn(ioDispatcher)

    override fun getDetailBy(username: String): Flow<Resource<UserEntity>> {
        return localDataSource.getDetailBy(username).map {
            if (it.name.isEmpty()) {
                when (val response = remoteDataSource.getDetailBy(username)) {
                    is NetworkState.Error -> Resource.Error(response.message)
                    is NetworkState.Success -> {
                        val entity = response.body.toEntity()
                        localDataSource.insertUserDetail(entity)
                        Resource.Success(entity)
                    }
                }
            } else
                Resource.Success(it)
        }.onStart {
            emit(Resource.Loading())
        }.flowOn(ioDispatcher)
    }

    override fun getFollowerBy(username: String): Flow<Resource<List<UserEntity>>> = flow {
        when (val networkState = remoteDataSource.getFollowerBy(username)) {
            is NetworkState.Success -> {
                val entity = networkState.body.map { it.toEntity() }
                emit(Resource.Success(entity))
            }
            is NetworkState.Error -> emit(Resource.Error(message = networkState.message))
        }
    }.onStart {
        emit(Resource.Loading())
    }.catch {
        emit(Resource.Error(it.localizedMessage))
    }.flowOn(ioDispatcher)

    override fun getFollowingBy(username: String): Flow<Resource<List<UserEntity>>> = flow {
        when (val networkState = remoteDataSource.getFollowingBy(username)) {
            is NetworkState.Success -> {
                val entity = networkState.body.map { it.toEntity() }
                emit(Resource.Success(entity))
            }
            is NetworkState.Error -> emit(Resource.Error(message = networkState.message))
        }
    }.onStart {
        emit(Resource.Loading())
    }.catch {
        emit(Resource.Error(it.localizedMessage))
    }.flowOn(ioDispatcher)


    override fun getFavorite(): Flow<Resource<List<UserEntity>>> =
        localDataSource.getFavorite().mapNotNull {
            if (it.isEmpty())
                Resource.Error("There is no favorite user")
            else
                Resource.Success(it)
        }.onStart {
            emit(Resource.Loading())
        }.catch {
            emit(Resource.Error(it.localizedMessage))
        }.flowOn(ioDispatcher)

    override suspend fun setUserFavorite(userDto: UserEntity): UserEntity {
        return when (userDto.isFavorite) {
            true -> localDataSource.unFavorite(userDto)
            false -> localDataSource.favorite(userDto)
        }
    }

}
