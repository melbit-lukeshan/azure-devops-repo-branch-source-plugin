/*
 * Copyright (C) 2008 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model


import com.google.common.base.Function
import com.google.common.base.Optional
import com.google.common.base.Preconditions.checkNotNull
import com.google.common.base.Predicate
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import org.kohsuke.accmod.Restricted
import org.kohsuke.accmod.restrictions.NoExternalUse
import javax.annotation.CheckReturnValue

/**
 * Mostly copypaste from guava's FluentIterable
 */
@Restricted(NoExternalUse::class)
abstract class FluentIterableWrapper<E> internal constructor(iterable: Iterable<E>) : Iterable<E> {
    private val iterable: Iterable<E>

    init {
        this.iterable = checkNotNull(iterable)
    }

    override fun iterator(): Iterator<E> {
        return iterable.iterator()
    }

    /**
     * Returns a fluent iterable whose iterators traverse first the elements of this fluent iterable,
     * followed by those of `other`. The iterators are not polled until necessary.
     *
     *
     * The returned iterable's `Iterator` supports `remove()` when the corresponding
     * `Iterator` supports it.
     */
    @CheckReturnValue
    fun append(other: Iterable<E>): FluentIterableWrapper<E> {
        return from(Iterables.concat(iterable, other))
    }

    /**
     * Returns the elements from this fluent iterable that satisfy a predicate. The
     * resulting fluent iterable's iterator does not support `remove()`.
     */
    @CheckReturnValue
    fun filter(predicate: Predicate<in E>): FluentIterableWrapper<E> {
        return from(Iterables.filter(iterable, predicate))
    }

    /**
     * Returns the elements from this fluent iterable that are instances of the supplied type. The
     * resulting fluent iterable's iterator does not support `remove()`.
     * @since 1.25.0
     */
    @CheckReturnValue
    fun <F : E> filter(clazz: Class<F>): FluentIterableWrapper<F> {
        return from(Iterables.filter(iterable, clazz))
    }

    /**
     * Returns a fluent iterable that applies `function` to each element of this
     * fluent iterable.
     *
     *
     * The returned fluent iterable's iterator supports `remove()` if this iterable's
     * iterator does. After a successful `remove()` call, this fluent iterable no longer
     * contains the corresponding element.
     */
    fun <T> transform(function: Function<in E, T>): FluentIterableWrapper<T> {
        return from(Iterables.transform(iterable, function))
    }

    /**
     * Applies `function` to each element of this fluent iterable and returns
     * a fluent iterable with the concatenated combination of results.  `function`
     * returns an Iterable of results.
     *
     *
     * The returned fluent iterable's iterator supports `remove()` if this
     * function-returned iterables' iterator does. After a successful `remove()` call,
     * the returned fluent iterable no longer contains the corresponding element.
     */
    fun <T> transformAndConcat(
            function: Function<in E, Iterable<T>>): FluentIterableWrapper<T> {
        return from(Iterables.concat(transform<Iterable<T>>(function)))
    }

    /**
     * Returns an [Optional] containing the first element in this fluent iterable that
     * satisfies the given predicate, if such an element exists.
     *
     *
     * **Warning:** avoid using a `predicate` that matches `null`. If `null`
     * is matched in this fluent iterable, a [NullPointerException] will be thrown.
     */
    fun firstMatch(predicate: Predicate<in E>): Optional<E> {
        return Iterables.tryFind(iterable, predicate)
    }

    /**
     * Returns an [Optional] containing the first element in this fluent iterable.
     * If the iterable is empty, `Optional.absent()` is returned.
     *
     * @throws NullPointerException if the first element is null; if this is a possibility, use
     * `iterator().next()` or [Iterables.getFirst] instead.
     */
    fun first(): Optional<E> {
        val iterator = iterable.iterator()
        return if (iterator.hasNext())
            Optional.of(iterator.next())
        else
            Optional.absent()
    }

    /**
     * Returns list from wrapped iterable
     */
    fun toList(): List<E> {
        return Lists.newArrayList(iterable)
    }

    /**
     * Returns an `ImmutableSet` containing all of the elements from this fluent iterable with
     * duplicates removed.
     */
    fun toSet(): ImmutableSet<E> {
        return ImmutableSet.copyOf(iterable)
    }

    companion object {

        /**
         * Returns a fluent iterable that wraps `iterable`, or `iterable` itself if it
         * is already a `FluentIterable`.
         */
        fun <E> from(iterable: Iterable<E>): FluentIterableWrapper<E> {
            return if (iterable is FluentIterableWrapper<*>)
                iterable as FluentIterableWrapper<E>
            else
                object : FluentIterableWrapper<E>(iterable) {

                }
        }
    }

}