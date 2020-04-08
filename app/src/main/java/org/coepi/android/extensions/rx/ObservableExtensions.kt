package org.coepi.android.extensions.rx

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams.fromPublisher
import io.reactivex.BackpressureStrategy.BUFFER
import io.reactivex.Notification
import io.reactivex.Observable
import io.reactivex.Observable.just
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import org.coepi.android.system.rx.OperationState
import org.coepi.android.system.rx.OperationState.Failure
import org.coepi.android.system.rx.OperationState.Progress
import org.coepi.android.system.rx.OperationState.Success
import org.coepi.android.system.rx.VoidOperationState

fun <T> Observable<T>.toLiveData(): LiveData<T> =
    fromPublisher(observeOn(mainThread()).toFlowable(BUFFER))

fun <T> Observable<Notification<T>>.toOperationState(): Observable<OperationState<T>> =
    flatMap { notification ->
        notification.toOperationState()?.let { just(it) } ?:
            just(Failure(Throwable(notification.error ?: Throwable("Unknown error"))))
    }

fun <T> Observable<OperationState<T>>.doOnNextSuccess(f: (T) -> Unit): Observable<OperationState<T>> =
    doOnNext { state ->
        if (state is Success) {
            f(state.data)
        }
    }

fun <T, U> Observable<OperationState<T>>.mapSuccess(f: (T) -> U): Observable<OperationState<U>> =
    map { state ->
        state.map(f)
    }

fun <T, U> Observable<OperationState<T>>.flatMapSuccess(f: (T) -> Observable<OperationState<U>>): Observable<OperationState<U>> =
    flatMap { state: OperationState<T> ->
        when (state) {
            is Success -> f(state.data)
            is Progress -> just(Progress)
            is Failure -> just(Failure(state.t))
        }
    }

fun <T> Observable<OperationState<T>>.success(): Observable<T> =
    flatMap {
        when (it) {
            is Success -> Observable.just<T>(it.data)
            else -> Observable.empty<T>()
        }
    }

fun Observable<VoidOperationState>.toIsInProgress(): Observable<Boolean> =
    map { it is Progress }

fun <T> Observable<OperationState<T>>.toIsLoading(): Observable<Boolean> =
    map { it is Progress }
